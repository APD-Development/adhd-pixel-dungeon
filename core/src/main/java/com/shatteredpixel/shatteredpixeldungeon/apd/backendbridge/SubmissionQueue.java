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

package com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge;

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Canon;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.OutboxStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.RunSubmissionCodec;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFactsBuilder;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

// Local queue of runs the player asked to submit. Nothing here touches a network and nothing sends the queue yet.
public final class SubmissionQueue {

	public static final String FILE_NAME = "submission_queue.txt";

	public enum Result { QUEUED, ALREADY_QUEUED, REFUSED, FAILED }

	public static final class Queued {
		public final Result result;
		public final RunSubmissionFactory.Refusal refusal;   //only for REFUSED by the factory
		public final String detail;

		Queued( Result result, RunSubmissionFactory.Refusal refusal, String detail ){
			this.result = result;
			this.refusal = refusal;
			this.detail = detail;
		}
	}

	//The queue file is read, changed and written back by several callers (the run sheet, Send now on its own thread, evidence answers). Every
	//change happens under this lock, and a send never holds it while it talks to the server.
	private static final Object LOCK = new Object();

	private SubmissionQueue(){}

	public static File fileIn( File historyRoot ){
		return new File(historyRoot, FILE_NAME);
	}

	// A partial read is useful for inspection, but must never rewrite away damaged entries.
	private static void loadForUpdate( Outbox box, File historyRoot ) throws IOException {
		if (OutboxStore.load(box, fileIn(historyRoot)).skipped > 0){
			throw new IOException("The queue contains unreadable entries; its existing data was preserved");
		}
	}

	public static String readProblem( File historyRoot ){
		if (historyRoot == null) return null;
		synchronized (LOCK){
			try {
				if (OutboxStore.load(new Outbox(), fileIn(historyRoot)).skipped > 0){
					return "The queue has unreadable entries. Its count is incomplete; existing data was preserved. Queue updates and sending are blocked.";
				}
			} catch (IOException e){
				return "The queue could not be read. Its count is unknown; existing data was preserved.";
			}
			return null;
		}
	}

	public static int pending( File historyRoot ){
		if (historyRoot == null) return 0;
		synchronized (LOCK){
			try {
				Outbox box = new Outbox();
				OutboxStore.load(box, fileIn(historyRoot));
				return box.size();
			} catch (IOException e){
				return 0;
			}
		}
	}

	//Builds the submission from the run's own log and queues it. A run is only taken from a single, complete log: runs spread over several
	//log segments are refused for now (merging segments for a submission is not designed yet).
	public static Queued enqueueRun( File historyRoot, String runId, String appBuild, long nowMillis ){
		if (historyRoot == null || !StatsStore.isValidRunId(runId)) return new Queued(Result.REFUSED, null, "no such run");
		File log = new StatsStore(historyRoot).logFile(runId);
		byte[] bytes;
		try {
			bytes = readAll(log);
		} catch (IOException e){
			return new Queued(Result.REFUSED, null, "the run's log cannot be read");
		}
		RunFacts facts = RunFactsBuilder.fromLog(RunLogReader.parse(bytes), runId);
		if (facts.numOr(FactKeys.SEGMENT, 0) != 0) return new Queued(Result.REFUSED, RunSubmissionFactory.Refusal.HISTORY_STARTS_MID_RUN, "this run is spread over several log segments");
		RunSubmissionFactory.Outcome o = RunSubmissionFactory.from(facts, Canon.hex(Canon.sha256(bytes)), appBuild);
		if (o.submission == null) return new Queued(Result.REFUSED, o.refusal, o.detail);
		Id id;
		try {
			id = Id.of(Id.Kind.OUTBOX, "ob_" + runId);
		} catch (IllegalArgumentException e){
			return new Queued(Result.REFUSED, null, "the run id is too long to queue");
		}
		synchronized (LOCK){
			try {
				Outbox box = new Outbox();
				loadForUpdate(box, historyRoot);
				int before = box.size();
				box.enqueue(id, RunSubmissionCodec.toEnvelope(o.submission), nowMillis);
				if (box.size() == before) return new Queued(Result.ALREADY_QUEUED, null, "");
				OutboxStore.save(box, fileIn(historyRoot));
				return new Queued(Result.QUEUED, null, "");
			} catch (IOException e){
				return new Queued(Result.FAILED, null, "the queue could not be saved: " + e.getMessage());
			}
		}
	}

	private static Id queueId( String runId ){
		if (!StatsStore.isValidRunId(runId)) return null;
		try {
			return Id.of(Id.Kind.OUTBOX, "ob_" + runId);
		} catch (IllegalArgumentException e){
			return null;
		}
	}

	private static Outbox.Item find( File historyRoot, String runId ){
		Id id = queueId(runId);
		if (historyRoot == null || id == null) return null;
		synchronized (LOCK){
			try {
				Outbox box = new Outbox();
				OutboxStore.load(box, fileIn(historyRoot));
				for (Outbox.Item i : box.items()) if (i.id.equals(id)) return i;
			} catch (IOException ignored){
			}
			return null;
		}
	}

	public static final class Entry {
		public final String id, text;

		Entry( String id, String text ){
			this.id = id;
			this.text = text;
		}
	}

	//everything in the queue, each as the exact text that would be sent (for the report of what APD holds)
	public static java.util.List<Entry> entries( File historyRoot ){
		java.util.List<Entry> out = new java.util.ArrayList<>();
		if (historyRoot == null) return out;
		synchronized (LOCK){
			try {
				Outbox box = new Outbox();
				OutboxStore.load(box, fileIn(historyRoot));
				for (Outbox.Item i : box.items()){
					out.add(new Entry(i.id.value, i.envelope.schema + " " + i.envelope.major + "." + i.envelope.minor + ": " + i.envelope.payload));
				}
			} catch (IOException ignored){
			}
		}
		return out;
	}

	//is an item with this queue id waiting (any kind of item, not only runs)
	static boolean hasItem( File historyRoot, Id id ){
		if (historyRoot == null) return false;
		synchronized (LOCK){
			try {
				Outbox box = new Outbox();
				OutboxStore.load(box, fileIn(historyRoot));
				for (Outbox.Item i : box.items()) if (i.id.equals(id)) return true;
			} catch (IOException ignored){
			}
			return false;
		}
	}

	public static boolean isQueued( File historyRoot, String runId ){
		return find(historyRoot, runId) != null;
	}

	//The exact text that would be sent for a queued run, so the player can read it before anything ever leaves the device. null if the run is not queued.
	public static String preview( File historyRoot, String runId ){
		Outbox.Item i = find(historyRoot, runId);
		return i == null ? null : i.envelope.schema + " " + i.envelope.major + "." + i.envelope.minor + "\n" + i.envelope.payload;
	}

	//Takes a run back out of the queue. Returns false if it was not queued or the queue could not be saved.
	public static boolean withdraw( File historyRoot, String runId ){
		Id id = queueId(runId);
		if (historyRoot == null || id == null) return false;
		synchronized (LOCK){
			try {
				Outbox box = new Outbox();
				loadForUpdate(box, historyRoot);
				if (!box.remove(id)) return false;
				OutboxStore.save(box, fileIn(historyRoot));
				return true;
			} catch (IOException e){
				return false;
			}
		}
	}

	//Sends what is due through 'gateway' and saves what is left. With the offline gateway (the only one that exists) nothing changes. Never throws.
	public static Outbox.FlushResult flush( File historyRoot, Gateway gateway, long nowMillis ){
		return flush(historyRoot, gateway, nowMillis, false);
	}

	//ignoreBackoff: the player pressed "send now", so every queued item is tried regardless of its retry schedule
	public static Outbox.FlushResult flush( File historyRoot, Gateway gateway, long nowMillis, boolean ignoreBackoff ){
		Outbox.FlushResult none = new Outbox.FlushResult();
		if (historyRoot == null) return none;
		Outbox box = new Outbox();
		synchronized (LOCK){
			try {
				loadForUpdate(box, historyRoot);
			} catch (IOException e){
				return none;
			}
		}
		if (box.size() == 0) return none;
		if (ignoreBackoff) for (Outbox.Item i : box.items()) i.nextAttemptAtMillis = nowMillis;
		//the copy 'box' is private to this call, so talking to the server does not hold the lock
		Outbox.FlushResult r = box.flush(gateway, nowMillis);
		synchronized (LOCK){
			// An acknowledgement has already happened, even if the local queue cannot be updated.
			for (Outbox.Item sent : r.sentItems) recordSent(historyRoot, sent, nowMillis);
			try {
				//apply the outcome to the queue as it is NOW: runs queued or taken back while the server was being asked are not lost
				Outbox now = new Outbox();
				loadForUpdate(now, historyRoot);
				java.util.List<Outbox.Item> completed = new java.util.ArrayList<>(r.sentItems);
				completed.addAll(r.rejectedItems);
				for (Outbox.Item i : new java.util.ArrayList<>(now.items())){
					boolean finished = false;
					for (Outbox.Item tried : completed) if (sameQueuedItem(tried, i)){
						finished = true;
						break;
					}
					if (finished){
						now.remove(i.id);
						continue;
					}
					for (Outbox.Item tried : box.items()){
						if (sameQueuedItem(tried, i)){
							i.attempts = tried.attempts;
							i.nextAttemptAtMillis = tried.nextAttemptAtMillis;
							i.lastDetail = tried.lastDetail;
						}
					}
				}
				OutboxStore.save(now, fileIn(historyRoot));
			} catch (IOException e){
				return r;
			}
		}
		return r;
	}

	//A newer ballot/settings envelope may reuse the id while a send is in flight.
	//Only merge the result into the exact queued version that was actually tried.
	private static boolean sameQueuedItem( Outbox.Item a, Outbox.Item b ){
		return a.id.equals(b.id) && a.createdAtMillis == b.createdAtMillis
				&& a.envelope.schema.equals(b.envelope.schema)
				&& a.envelope.major == b.envelope.major && a.envelope.minor == b.envelope.minor
				&& a.envelope.payload.equals(b.envelope.payload);
	}

	//remembers what was sent so a later evidence request can be checked against it
	private static void recordSent( File historyRoot, Outbox.Item sent, long nowMillis ){
		if (!sent.id.value.startsWith("ob_")) return;
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.RunSubmission s = RunSubmissionCodec.fromEnvelope(sent.envelope);
		if (s != null) SubmittedLedger.record(historyRoot, sent.id.value.substring(3), s.evidenceDigestHex, nowMillis);
	}

	static byte[] readAllBytes( File f ) throws IOException {
		return readAll(f);
	}

	//like enqueueEnvelope, but an item already waiting under the same id is replaced (a new ballot for the same round)
	static boolean replaceEnvelope( File historyRoot, Id id, com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope e, long nowMillis ){
		synchronized (LOCK){
			try {
				Outbox box = new Outbox();
				loadForUpdate(box, historyRoot);
				box.remove(id);
				box.enqueue(id, e, nowMillis);
				OutboxStore.save(box, fileIn(historyRoot));
				return true;
			} catch (IOException | IllegalArgumentException ex){
				return false;
			}
		}
	}

	//queues an envelope that is not a run submission (evidence data the player agreed to send); false if it could not be queued or saved
	static boolean enqueueEnvelope( File historyRoot, Id id, com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope e, long nowMillis ){
		synchronized (LOCK){
			try {
				Outbox box = new Outbox();
				loadForUpdate(box, historyRoot);
				box.enqueue(id, e, nowMillis);
				OutboxStore.save(box, fileIn(historyRoot));
				return true;
			} catch (IOException | IllegalArgumentException ex){
				return false;
			}
		}
	}

	private static byte[] readAll( File f ) throws IOException {
		try (InputStream in = new FileInputStream(f)){
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
			return out.toByteArray();
		}
	}
}
