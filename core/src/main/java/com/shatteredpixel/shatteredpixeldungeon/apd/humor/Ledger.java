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

package com.shatteredpixel.shatteredpixeldungeon.apd.humor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// What this player has been shown, kept in one small local text file that is never uploaded or exported: per line id, how many times and
// when (a running counter and the clock), plus a few state values for the popup budget. It lives apart from run logs and the save, so it
// can never touch either. A damaged file is read as far as possible, but automatic saves fail closed so unknown original bytes are never
// discarded. Missing or unreadable storage never throws into the game.
public final class Ledger {
	private static final Object STORE_LOCK = new Object();

	public static final class Seen {
		public int count;
		public long lastSeq;
		public long lastAt;
		// when the player was FIRST really shown the line (a story's delay is counted from here)
		public long firstAt;
	}

	private final File file;
	private final Map<String, Seen> seen = new HashMap<>();
	private final Map<String, String> state = new HashMap<>();
	private long seq;
	private boolean incompleteLoad;

	// file == null keeps everything in memory (used by tests and when storage is unavailable)
	public Ledger( File file ){
		this.file = file;
		load();
	}

	private void load(){
		if (file == null || !file.isFile()) return;
		try (BufferedReader r = new BufferedReader( new InputStreamReader( new FileInputStream( file ), StandardCharsets.UTF_8 ) )){
			String row;
			while ((row = r.readLine()) != null){
				try {
					if (row.startsWith( "@seq=" )){
						seq = Math.max( seq, Long.parseLong( row.substring( 5 ) ) );
					} else if (row.startsWith( "@" )){
						int eq = row.indexOf( '=' );
						if (eq <= 1) throw new IllegalArgumentException( "damaged state row" );
						state.put( row.substring( 1, eq ), row.substring( eq + 1 ) );
					} else if (!row.startsWith( "#" ) && !row.isEmpty()){
						String[] p = row.split( "\t" );
						if (p.length < 4 || p.length > 5) throw new IllegalArgumentException( "damaged ledger row" );
						Seen s = new Seen();
						s.count = Integer.parseInt( p[1] );
						s.lastSeq = Long.parseLong( p[2] );
						s.lastAt = Long.parseLong( p[3] );
						//history files from before this column existed: the first sighting is unknown, so the last one stands in
						s.firstAt = p.length > 4 ? Long.parseLong( p[4] ) : s.lastAt;
						if (s.count > 0){
							seen.put( p[0], s );
							seq = Math.max( seq, s.lastSeq );
						}
					}
				} catch (RuntimeException badRow){
					//one damaged row is skipped, the rest still count
					incompleteLoad = true;
				}
			}
		} catch (IOException e){
			//Keep any rows read before the failure, but never rewrite an incompletely read file.
			incompleteLoad = true;
		}
	}

	public synchronized void save(){
		if (file == null || incompleteLoad || file.isDirectory()) return;
		synchronized (STORE_LOCK){
			File tmp = new File( file.getPath() + ".tmp" );
			try {
				File dir = file.getParentFile();
				if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return;
				try (Writer w = new OutputStreamWriter( new FileOutputStream( tmp ), StandardCharsets.UTF_8 )){
					w.write( "#apd-humor-ledger v1\n" );
					w.write( "@seq=" + seq + "\n" );
					for (Map.Entry<String, String> e : state.entrySet()) w.write( "@" + e.getKey() + "=" + e.getValue() + "\n" );
					for (Map.Entry<String, Seen> e : seen.entrySet()){
						Seen s = e.getValue();
						w.write( e.getKey() + "\t" + s.count + "\t" + s.lastSeq + "\t" + s.lastAt + "\t" + s.firstAt + "\n" );
					}
				}
				try {
					Files.move( tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
				} catch (AtomicMoveNotSupportedException e){
					Files.move( tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING );
				}
			} catch (IOException e){
				//the history is a nicety: a failed save must never reach the game
			} finally {
				if (tmp.isFile()) tmp.delete();
			}
		}
	}

	public synchronized void markShown( String id, long now ){
		Seen s = seen.get( id );
		if (s == null){
			s = new Seen();
			s.firstAt = now;
			seen.put( id, s );
		}
		s.count++;
		s.lastSeq = ++seq;
		s.lastAt = now;
	}

	public synchronized Seen get( String id ){
		return seen.get( id );
	}

	public synchronized long seq(){
		return seq;
	}

	// how many of the given ids this player has been shown at least once
	public synchronized int distinctSeenAmong( Set<String> ids ){
		int n = 0;
		for (String id : ids) if (seen.containsKey( id )) n++;
		return n;
	}

	public synchronized String state( String key ){
		return state.get( key );
	}

	public synchronized long stateLong( String key, long dflt ){
		try {
			String v = state.get( key );
			return v == null ? dflt : Long.parseLong( v );
		} catch (NumberFormatException e){
			return dflt;
		}
	}

	public synchronized void setState( String key, String value ){
		state.put( key, value );
	}

	// the player's explicit reset
	public synchronized void clear(){
		seen.clear();
		state.clear();
		seq = 0;
		incompleteLoad = false;
		save();
	}
}
