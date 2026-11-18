/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

//Appends events to one run log on a background thread.
//
//DESIGN RULES
// - Nothing in this class ever throws into the caller and nothing blocks the caller for long. Callers on the game
//   thread only encode a frame into memory and put it on a queue.
// - A failure (disk full, bad path, I/O error) disables the writer. Later calls do nothing and return false. The
//   failure is available through failure() and an optional listener. It never affects gameplay.
// - If the queue is full an event is dropped and counted, and a GAP event with the count is written as soon as space
//   is available, so a missing stretch of history is always visible in the log.
// - The log is append-only. A log is never overwritten (create() refuses an existing file).
// - Frames are checksummed individually so a partly written tail can be recovered (see LogRecovery).
//
//The caller should close() the writer at the end of a run, which writes a seal frame. A log without a seal is
//simply "unsealed" (in progress, or recording stopped abruptly) and is read normally.
public final class RunLogWriter {

	public enum OpenStatus {
		OK,
		//reopened an existing log and cut off a damaged tail (the cut bytes were saved)
		RECOVERED_TAIL,
		FAILED_IO,
		FAILED_BAD_RUN_ID,
		//create() found a file already there and refuses to overwrite it
		FAILED_EXISTS,
		FAILED_MISSING,
		FAILED_BAD_HEADER,
		FAILED_UNSUPPORTED_CONTAINER,
		//the run already ended, a sealed log is never appended to
		FAILED_SEALED,
		//the log was recorded with a different telemetry version than the caller wants to append with
		FAILED_VERSION_MISMATCH
	}

	public interface FailureListener {
		void onFailure( String what, Throwable error );
	}

	public static final class Options {
		public int queueCapacity = DEFAULT_QUEUE_CAPACITY;
		public FailureListener listener;

		//test hooks
		CountDownLatch workerGate;
		long failAfterBytes = -1;

		public Options queueCapacity( int n ){
			queueCapacity = n;
			return this;
		}

		public Options listener( FailureListener l ){
			listener = l;
			return this;
		}
	}

	public static final int DEFAULT_QUEUE_CAPACITY = 16384;
	//a clock frame re-anchors readers every this many events
	static final int CLOCK_FRAME_INTERVAL = 128;
	//queue slots kept free for flush/close requests
	private static final int CONTROL_RESERVE = 6;
	//Closing may need a dropped-event gap, a seal and the final close request.
	private static final int CLOSE_CONTROL_RESERVE = 3;

	private static final class FlushRequest {
		final CountDownLatch done = new CountDownLatch(1);
		final boolean sync;
		FlushRequest( boolean sync ){
			this.sync = sync;
		}
	}

	private static final class CloseRequest {
		final CountDownLatch done = new CountDownLatch(1);
	}

	private final File file;
	private final String runId;
	private final TelemetryVersion telemetryVersion;
	private final Options options;
	private final OpenStatus openStatus;

	private final Object lock = new Object();
	private final ArrayBlockingQueue<Object> queue;
	private FileOutputStream fileOut;
	private BufferedOutputStream out;
	private Thread worker;

	//guarded by lock
	private final Map<String, Integer> stringIds = new HashMap<>();
	private int nextStringId;
	private long lastClock;
	private long eventCount;
	private int eventsSinceClockFrame = CLOCK_FRAME_INTERVAL; //forces a clock frame before the first event
	private long droppedPending;
	private long droppedTotal;
	private long acceptedTotal;
	private boolean closed;

	private volatile boolean disabled;
	private volatile Throwable failure;
	private long bytesWritten; //worker thread only
	private volatile long lostAfterFailure;

	private RunLogWriter( File file, String runId, TelemetryVersion version, Options options, OpenStatus status,
	                      boolean disabled, Throwable failure ){
		this.file = file;
		this.runId = runId;
		this.telemetryVersion = version;
		this.options = options;
		this.openStatus = status;
		this.disabled = disabled;
		this.failure = failure;
		this.queue = new ArrayBlockingQueue<>(Math.max(CONTROL_RESERVE + 4, options.queueCapacity));
	}

	//---- opening -------------------------------------------------------------------------------------------

	//Starts a new log. Never throws: on any problem the returned writer is disabled, see openStatus().
	public static RunLogWriter create( File file, TelemetryVersion version, String runId, String writerBuild,
	                                   long createdAtMillis ){
		return create(file, version, runId, writerBuild, createdAtMillis, new Options());
	}

	public static RunLogWriter create( File file, TelemetryVersion version, String runId, String writerBuild,
	                                   long createdAtMillis, Options options ){
		try {
			if (!StatsStore.isValidRunId(runId)){
				return disabledWriter(file, runId, version, options, OpenStatus.FAILED_BAD_RUN_ID,
						new IllegalArgumentException("bad run id"));
			}
			if (file.exists()){
				return disabledWriter(file, runId, version, options, OpenStatus.FAILED_EXISTS,
						new IOException("log already exists: " + file));
			}
			File parent = file.getAbsoluteFile().getParentFile();
			if (parent != null && !parent.isDirectory() && !parent.mkdirs()){
				return disabledWriter(file, runId, version, options, OpenStatus.FAILED_IO,
						new IOException("cannot create " + parent));
			}

			byte[] header = LogHeader.encode(version, createdAtMillis, runId, writerBuild);
			// Claim atomically: another writer/import may have created the log since exists().
			if (!file.createNewFile()){
				return disabledWriter(file, runId, version, options, OpenStatus.FAILED_EXISTS,
						new IOException("log already exists: " + file));
			}
			FileOutputStream fos = new FileOutputStream(file);
			try {
				fos.write(header);
				fos.flush();
			} catch (IOException e){
				try { fos.close(); } catch (IOException ignored){}
				throw e;
			}
			RunLogWriter w = new RunLogWriter(file, runId, version, options, OpenStatus.OK, false, null);
			w.start(fos, header.length);
			return w;
		} catch (Throwable t){
			return disabledWriter(file, runId, version, options, OpenStatus.FAILED_IO, t);
		}
	}

	//Continues an existing, unsealed log of the same telemetry version. A damaged tail is cut off (and saved) first,
	//and a GAP event records that. Never throws, see openStatus().
	public static RunLogWriter openForAppend( File file, TelemetryVersion version ){
		return openForAppend(file, version, new Options());
	}

	public static RunLogWriter openForAppend( File file, TelemetryVersion version, Options options ){
		try {
			if (!file.isFile()){
				return disabledWriter(file, "", version, options, OpenStatus.FAILED_MISSING, new IOException("no such log"));
			}
			// Appending only needs the header, string table, clock, event count and seal.
			// Validate every frame but do not retain the existing gameplay-event list.
			ReadOptions reopenScan = new ReadOptions().onlyRunStartEvents(true);
			ReadResult before = RunLogReader.read(file, reopenScan);
			if (before.status == ReadStatus.UNSUPPORTED_CONTAINER){
				return disabledWriter(file, "", version, options, OpenStatus.FAILED_UNSUPPORTED_CONTAINER,
						new IOException(before.problem));
			}
			if (before.header == null){
				return disabledWriter(file, "", version, options, OpenStatus.FAILED_BAD_HEADER,
						new IOException(before.problem));
			}
			String runId = before.header.runId;
			if (before.sealed){
				return disabledWriter(file, runId, version, options, OpenStatus.FAILED_SEALED, new IOException("log is sealed"));
			}
			//a run is recorded with one telemetry version. Never mix versions inside one log.
			if (!before.header.telemetryVersion.equals(version)){
				return disabledWriter(file, runId, version, options, OpenStatus.FAILED_VERSION_MISMATCH,
						new IOException("log is telemetry " + before.header.telemetryVersion + ", asked for " + version));
			}
			if (before.status == ReadStatus.SEAL_MISMATCH){
				return disabledWriter(file, runId, version, options, OpenStatus.FAILED_BAD_HEADER, new IOException(before.problem));
			}

			long discarded = 0;
			boolean recovered = false;
			if (!before.status.isClean()){
				LogRecovery.RepairReport rep = LogRecovery.repair(file);
				if (rep.error != null){
					return disabledWriter(file, runId, version, options, OpenStatus.FAILED_IO, new IOException(rep.error));
				}
				discarded = rep.discardedBytes;
				recovered = rep.truncated;
			}
			//re-read what is now in the file, this is the state we continue from
			ReadResult now = RunLogReader.read(file, reopenScan);
			if (!now.status.isClean()){
				return disabledWriter(file, runId, version, options, OpenStatus.FAILED_IO,
						new IOException("log still damaged after repair: " + now.problem));
			}

			RunLogWriter w = new RunLogWriter(file, runId, version, options,
					recovered ? OpenStatus.RECOVERED_TAIL : OpenStatus.OK, false, null);
			synchronized (w.lock){
				for (int i = 0; i < now.strings.size(); i++){
					String s = now.strings.get(i);
					if (s != null) w.stringIds.put(s, i);
				}
				w.nextStringId = now.strings.size();
				w.lastClock = now.lastClock;
				w.eventCount = now.eventCount;
			}
			w.start(new FileOutputStream(file, true), -1);
			if (recovered){
				w.noteGap(EventTypes.GAP_RECOVERED_TAIL, discarded);
			}
			return w;
		} catch (Throwable t){
			return disabledWriter(file, "", version, options, OpenStatus.FAILED_IO, t);
		}
	}

	private static RunLogWriter disabledWriter( File file, String runId, TelemetryVersion version, Options options,
	                                            OpenStatus status, Throwable why ){
		notifyListener(options, "open: " + status, why);
		return new RunLogWriter(file, runId, version, options, status, true, why);
	}

	private void start( FileOutputStream fos, int alreadyWritten ){
		this.fileOut = fos;
		this.out = new BufferedOutputStream(fos, 32 * 1024);
		this.bytesWritten = Math.max(0, alreadyWritten);
		Thread t = new Thread(new Runnable(){
			@Override
			public void run(){
				workerLoop();
			}
		}, "apd-stats-writer");
		t.setDaemon(true);
		this.worker = t;
		t.start();
	}

	//---- writing -------------------------------------------------------------------------------------------

	//Starts building an event. The returned builder never throws.
	public EventBuilder event( int type ){
		boolean inert = disabled || closed || type == EventTypes.INVALID || type < 0;
		return new EventBuilder(this, type, inert);
	}

	//Writes an event whose payload is already encoded. Returns whether it was accepted.
	public boolean append( int type, int flags, long clock, byte[] payload ){
		if (disabled || closed || type <= 0) return false;
		try {
			return accept(type, flags, true, clock, payload == null ? new byte[0] : payload, new int[0], new String[0], 0);
		} catch (Throwable t){
			reportFailure("append()", t);
			return false;
		}
	}

	//Builds the frames for one event and puts them on the queue as one all-or-nothing group: new string table entries,
	//a GAP event if events were dropped before, a clock frame if one is due, and the event itself.
	boolean accept( int type, int flags, boolean hasClock, long clock, byte[] payload,
	                int[] stringCuts, String[] strings, int stringCount ){
		synchronized (lock){
			if (disabled || closed){
				if (disabled) lostAfterFailure++;
				return false;
			}
			//how many of the event's strings are new to this log
			int newStrings = 0;
			for (int i = 0; i < stringCount; i++){
				if (!stringIds.containsKey(strings[i]) && !isEarlierInList(strings, i)) newStrings++;
			}
			boolean needClock = eventsSinceClockFrame >= CLOCK_FRAME_INTERVAL;
			int needed = newStrings + 1 + (needClock ? 1 : 0) + (droppedPending > 0 ? 1 : 0);
			if (queue.remainingCapacity() < needed + CONTROL_RESERVE){
				droppedPending++;
				droppedTotal++;
				return false;
			}

			//resolve strings to ids, writing definitions for new ones, and assemble the final payload
			ByteSink finalPayload = new ByteSink(payload.length + 8 * stringCount);
			int copied = 0;
			for (int i = 0; i < stringCount; i++){
				Integer id = stringIds.get(strings[i]);
				if (id == null){
					id = nextStringId++;
					stringIds.put(strings[i], id);
					queue.add(LogFormat.stringFrame(id, strings[i]));
				}
				finalPayload.writeBytes(payload, copied, stringCuts[i] - copied);
				finalPayload.writeUnsigned(id);
				copied = stringCuts[i];
			}
			finalPayload.writeBytes(payload, copied, payload.length - copied);

			if (droppedPending > 0){
				queue.add(LogFormat.eventFrame(EventTypes.GAP, 0, 0, gapPayload(EventTypes.GAP_DROPPED_EVENTS, droppedPending)));
				eventCount++;
				eventsSinceClockFrame++;
				droppedPending = 0;
			}
			if (needClock){
				queue.add(LogFormat.clockFrame(lastClock));
				eventsSinceClockFrame = 0;
			}
			long c = hasClock ? clock : lastClock;
			queue.add(LogFormat.eventFrame(type, flags, c - lastClock, finalPayload.toByteArray()));
			lastClock = c;
			eventCount++;
			eventsSinceClockFrame++;
			acceptedTotal++;
			return true;
		}
	}

	private static boolean isEarlierInList( String[] strings, int index ){
		for (int i = 0; i < index; i++){
			if (strings[i].equals(strings[index])) return true;
		}
		return false;
	}

	private static byte[] gapPayload( int reason, long amount ){
		return new ByteSink().writeUnsigned(reason).writeUnsigned(Math.max(0, amount)).toByteArray();
	}

	//records a gap right now (used after tail recovery)
	private void noteGap( int reason, long amount ){
		synchronized (lock){
			if (disabled || closed) return;
			queue.add(LogFormat.eventFrame(EventTypes.GAP, 0, 0, gapPayload(reason, amount)));
			eventCount++;
			eventsSinceClockFrame++;
		}
	}

	//Waits until everything accepted so far has been handed to the operating system (and, with sync, forced to disk).
	//Returns false if the writer is disabled or the wait timed out. Intended for save points, not for every event.
	public boolean flush( long timeoutMillis, boolean sync ){
		if (disabled) return false;
		try {
			FlushRequest req = new FlushRequest(sync);
			if (!enqueueControl(req, timeoutMillis)) return false;
			return req.done.await(timeoutMillis, TimeUnit.MILLISECONDS) && !disabled;
		} catch (Throwable t){
			reportFailure("flush()", t);
			return false;
		}
	}

	//Adds a control request under the same lock as event frames, so the free-slot arithmetic in accept() is exact.
	//Waits (briefly) for room if the queue is saturated.
	private boolean enqueueControl( Object request, long timeoutMillis ) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (true){
			synchronized (lock){
				if (closed || disabled) return false;
				if (queue.remainingCapacity() > CLOSE_CONTROL_RESERVE){
					queue.add(request);
					return true;
				}
			}
			if (System.currentTimeMillis() >= deadline) return false;
			Thread.sleep(2);
		}
	}

	public boolean flush(){
		return flush(10_000, false);
	}

	//Ends the log. With seal=true a seal frame marks it as complete (use at the end of a run); with seal=false the log
	//is left unsealed so recording can continue later with openForAppend. Safe to call repeatedly.
	//Returns true if everything was written without a failure.
	public boolean close( boolean seal ){
		return close(seal, 30_000);
	}

	//Like close(seal) but waits at most about timeoutMillis for the writer thread, so a stuck disk cannot hold the
	//caller. If the wait runs out the thread still finishes writing (and sealing) in the background.
	public boolean close( boolean seal, long timeoutMillis ){
		try {
			synchronized (lock){
				if (closed) return !disabled && worker != null && !worker.isAlive();
				closed = true; //from now on accept() refuses events
			}
			CloseRequest req = new CloseRequest();
			long deadline = System.currentTimeMillis() + Math.max(1, timeoutMillis);
			boolean queued = false;
			while (!queued){
				synchronized (lock){
					int needed = 1 + (!disabled && seal ? 1 : 0) + (!disabled && droppedPending > 0 ? 1 : 0);
					if (queue.remainingCapacity() >= needed){
						if (!disabled){
							if (droppedPending > 0){
								queue.add(LogFormat.eventFrame(EventTypes.GAP, 0, 0, gapPayload(EventTypes.GAP_DROPPED_EVENTS, droppedPending)));
								eventCount++;
								droppedPending = 0;
							}
							if (seal) queue.add(LogFormat.sealFrame(eventCount, lastClock));
						}
						queue.add(req);
						queued = true;
					}
				}
				if (!queued){
					if (System.currentTimeMillis() >= deadline) break;
					Thread.sleep(2);
				}
			}
			if (queued && worker != null){
				if (req.done.await(Math.max(1, timeoutMillis), TimeUnit.MILLISECONDS)){
					worker.join(Math.max(1, Math.min(timeoutMillis, 5_000)));
				}
			}
			return queued && req.done.getCount() == 0 && !disabled;
		} catch (Throwable t){
			reportFailure("close()", t);
			return false;
		}
	}

	public boolean close(){
		return close(true);
	}

	//the writer's own thread, so a test can wait for exactly it (null if it never started)
	Thread workerThread(){
		return worker;
	}

	//---- status --------------------------------------------------------------------------------------------

	public OpenStatus openStatus(){
		return openStatus;
	}

	//true if nothing will be written (failed to open, or failed since)
	public boolean isDisabled(){
		return disabled;
	}

	public Throwable failure(){
		return failure;
	}

	public File file(){
		return file;
	}

	public String runId(){
		return runId;
	}

	public TelemetryVersion telemetryVersion(){
		return telemetryVersion;
	}

	public long eventsAccepted(){
		synchronized (lock){
			return acceptedTotal;
		}
	}

	//events dropped because the queue was full
	public long eventsDropped(){
		synchronized (lock){
			return droppedTotal;
		}
	}

	//events refused because the writer had failed
	public long eventsLostAfterFailure(){
		return lostAfterFailure;
	}

	//---- internals -----------------------------------------------------------------------------------------

	//An event was built wrongly (a caller bug). The event is dropped, the writer keeps working.
	void reportEventProblem( String what, Throwable t ){
		notifyListener(options, what, t == null ? new IllegalArgumentException(what) : t);
	}

	void reportFailure( String what, Throwable t ){
		if (failure == null) failure = t;
		disabled = true;
		notifyListener(options, what, t);
	}

	private static void notifyListener( Options options, String what, Throwable t ){
		try {
			if (options != null && options.listener != null) options.listener.onFailure(what, t);
		} catch (Throwable ignored){
			//a broken listener must not matter either
		}
	}

	private void workerLoop(){
		try {
			if (options.workerGate != null) options.workerGate.await();
			while (true){
				Object item = queue.take();
				if (item instanceof byte[]){
					if (!disabled) writeFrame((byte[]) item);
					else lostAfterFailure++;
				} else if (item instanceof FlushRequest){
					FlushRequest f = (FlushRequest) item;
					if (!disabled) flushOut(f.sync);
					f.done.countDown();
				} else if (item instanceof CloseRequest){
					if (!disabled) flushOut(true);
					closeStreams();
					((CloseRequest) item).done.countDown();
					return;
				}
				if (queue.isEmpty() && !disabled) flushOut(false);
			}
		} catch (InterruptedException e){
			Thread.currentThread().interrupt();
		} catch (Throwable t){
			reportFailure("writer thread", t);
		} finally {
			closeStreams();
		}
	}

	private void writeFrame( byte[] frame ){
		try {
			long limit = options.failAfterBytes;
			if (limit >= 0 && bytesWritten + frame.length > limit){
				//test hook: write a partial frame, like a disk that fills up mid-write
				int part = (int) Math.max(0, limit - bytesWritten);
				out.write(frame, 0, part);
				out.flush();
				bytesWritten += part;
				throw new IOException("simulated write failure");
			}
			out.write(frame);
			bytesWritten += frame.length;
		} catch (Throwable t){
			reportFailure("write", t);
		}
	}

	private void flushOut( boolean sync ){
		try {
			out.flush();
			if (sync) fileOut.getFD().sync();
		} catch (Throwable t){
			reportFailure("flush", t);
		}
	}

	private void closeStreams(){
		try {
			if (out != null) out.close();
		} catch (Throwable ignored){
		}
		out = null;
	}
}
