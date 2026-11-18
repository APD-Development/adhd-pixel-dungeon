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

package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// The player's named views: a saved query under a name. Question-builder views may also retain the exact friendly Question which produced the query.
// Both forms stay correct as history grows. Local to the device; the file is replaced atomically and damaged lines are skipped on load.
public final class SavedViews {

	public static final int MAX_VIEWS = 100, MAX_NAME = 60;
	public static final String FILE_NAME = "saved_views.txt";
	public static final String LOAD_WARNING = "Some saved views could not be read. The original file is preserved. Saving, renaming and deleting views are paused to avoid losing those entries.";

	public static final class View {
		public final String name, encodedQuery, encodedQuestion;
		public final long createdAtMillis;

		View( String name, String encodedQuery, String encodedQuestion, long createdAtMillis ){
			this.name = name;
			this.encodedQuery = encodedQuery;
			this.encodedQuestion = encodedQuestion == null ? "" : encodedQuestion;
			this.createdAtMillis = createdAtMillis;
		}

		//the table query; null if this build cannot read it any more
		public Query query(){
			try {
				return QueryCodec.decode(encodedQuery);
			} catch (RuntimeException e){
				return null;
			}
		}

		//The exact friendly question, when this view came from the question builder.
		//Older table-only views and future question formats this build cannot read return null.
		public Question question(){
			if (encodedQuestion.isEmpty()) return null;
			try {
				return QuestionCodec.decode(encodedQuestion);
			} catch (RuntimeException e){
				return null;
			}
		}
	}

	public enum Result { SAVED, REPLACED, TOO_MANY, BAD_NAME }

	//Separate SavedViews objects target the same on-disk file. Serialize the tiny
	//atomic-replacement section so they cannot truncate or rename one shared temp file concurrently.
	private static final Object STORE_LOCK = new Object();

	interface StoreHook {
		void afterTempOpened( File temp ) throws InterruptedException;
	}

	private final List<View> views = new ArrayList<>();
	public int skippedOnLoad;

	public static File fileIn( File historyRoot ){
		return new File(historyRoot, FILE_NAME);
	}

	//the name as it will be kept (trimmed, one line), or null if it cannot be a name
	public static String cleanName( String raw ){
		if (raw == null) return null;
		String n = raw.trim();
		if (n.isEmpty() || n.length() > MAX_NAME) return null;
		for (int i = 0; i < n.length(); i++) if (Character.isISOControl(n.charAt(i))) return null;
		return n;
	}

	public List<View> list(){
		List<View> out = new ArrayList<>(views);
		Collections.sort(out, new Comparator<View>(){
			@Override
			public int compare( View a, View b ){
				return a.name.compareToIgnoreCase(b.name);
			}
		});
		return out;
	}

	public View get( String name ){
		String n = cleanName(name);
		if (n == null) return null;
		for (View v : views) if (v.name.equalsIgnoreCase(n)) return v;
		return null;
	}

	//saving under a name that exists replaces that view (the player chose the name)
	public Result save( String name, Query q, long nowMillis ){
		return saveEncoded(name, QueryCodec.encode(q), "", nowMillis);
	}

	//Save the exact friendly question and its executable query together. Hidden selections
	//are sanitized once at this visibility boundary, so reopened wording and execution agree.
	public Result saveQuestion( String name, Question question, DatasetDef def, long nowMillis ){
		if (question == null || def == null) return Result.BAD_NAME;
		Question effective = Questions.withInternalVisibility(question, def, question.showInternal);
		return saveEncoded(name, QueryCodec.encode(Questions.toQuery(effective, def)),
				QuestionCodec.encode(effective), nowMillis);
	}

	private Result saveEncoded( String name, String encodedQuery, String encodedQuestion, long nowMillis ){
		String n = cleanName(name);
		if (n == null) return Result.BAD_NAME;
		View old = get(n);
		if (old == null && views.size() >= MAX_VIEWS) return Result.TOO_MANY;
		View v = new View(n, encodedQuery, encodedQuestion, nowMillis);
		if (old != null){
			views.set(views.indexOf(old), v);
			return Result.REPLACED;
		}
		views.add(v);
		return Result.SAVED;
	}

	public boolean delete( String name ){
		View v = get(name);
		return v != null && views.remove(v);
	}

	//false if the new name is not valid or is already taken by another view
	public boolean rename( String from, String to ){
		View v = get(from);
		String n = cleanName(to);
		if (v == null || n == null) return false;
		View other = get(n);
		if (other != null && other != v) return false;
		views.set(views.indexOf(v), new View(n, v.encodedQuery, v.encodedQuestion, v.createdAtMillis));
		return true;
	}

	public void store( File file ) throws IOException {
		store(file, null);
	}

	//Package-private hook makes the cross-instance temp-file ordering regression deterministic.
	void store( File file, StoreHook hook ) throws IOException {
		//A partial read is not a complete replacement: retain every original byte.
		if (skippedOnLoad > 0) throw new IOException(LOAD_WARNING);
		synchronized (STORE_LOCK){
			if (file.isDirectory()) throw new IOException("not a saved-view file: " + file);
			File parent = file.getAbsoluteFile().getParentFile();
			if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()){
				throw new IOException("saved-view folder could not be created: " + parent);
			}
			File tmp = new File(file.getPath() + ".tmp");
			try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8))){
				if (hook != null){
					try {
						hook.afterTempOpened(tmp);
					} catch (InterruptedException interrupted){
						Thread.currentThread().interrupt();
						throw new IOException("interrupted while saving views", interrupted);
					}
				}
				for (View v : views){
					w.write(b64(v.name) + "\t" + v.createdAtMillis + "\t" + b64(v.encodedQuery));
					if (!v.encodedQuestion.isEmpty()) w.write("\t" + b64(v.encodedQuestion));
					w.newLine();
				}
			}
			//The temporary file is a sibling. Keep the previous file if this
			//filesystem refuses replacement; deleting it first can lose saved views.
			if (!tmp.renameTo(file)) throw new IOException("cannot replace " + file);
		}
	}

	//a missing file is no views; never throws for damaged content
	public static SavedViews load( File file ){
		SavedViews s = new SavedViews();
		if (file == null || !file.exists()) return s;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))){
			String line;
			while ((line = in.readLine()) != null){
				if (line.isEmpty()) continue;
				try {
					String[] p = line.split("\t", -1);
					if (p.length != 3 && p.length != 4) throw new IllegalArgumentException("fields");
					String name = cleanName(unb64(p[0]));
					if (name == null || s.get(name) != null || s.views.size() >= MAX_VIEWS) throw new IllegalArgumentException("name");
					s.views.add(new View(name, unb64(p[2]), p.length == 4 ? unb64(p[3]) : "", Long.parseLong(p[1])));
				} catch (RuntimeException e){
					s.skippedOnLoad++;
				}
			}
		} catch (IOException e){
			s.skippedOnLoad++;
		}
		return s;
	}

	private static String b64( String s ){
		return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}

	private static String unb64( String s ){
		try {
			return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(Base64.getDecoder().decode(s))).toString();
		} catch (java.nio.charset.CharacterCodingException e){
			throw new IllegalArgumentException("invalid saved-view UTF-8", e);
		}
	}
}
