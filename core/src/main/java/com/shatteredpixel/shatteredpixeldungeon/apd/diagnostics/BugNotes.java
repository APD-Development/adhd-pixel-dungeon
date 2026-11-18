package com.shatteredpixel.shatteredpixeldungeon.apd.diagnostics;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.watabou.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Developer build: one-line notes the player jots down mid-run. Stored offline, added to every bug report. */
public final class BugNotes {
	private BugNotes(){}

	static final long MAX_BYTES = 256 * 1024;
	static File dirOverride;

	static synchronized File file(){
		File dir = dirOverride != null ? dirOverride : FileUtils.getFileHandle("apd-diagnostics").file();
		dir.mkdirs();
		return new File(dir, "bug-notes.txt");
	}

	/** The Bug Note menu button exists only in the Developer build. */
	public static boolean buttonVisible(){
		return com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.testBuild();
	}

	/** Context line: UTC time, floor, turn and hero class when a run is active. */
	public static String context(){
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
		f.setTimeZone(TimeZone.getTimeZone("UTC"));
		StringBuilder b = new StringBuilder(f.format(new Date())).append(" UTC");
		try {
			if (Dungeon.hero != null){
				b.append(" | floor ").append(Dungeon.depth)
				 .append(" | turn ").append((long) Math.floor(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()))
				 .append(" | ").append(Dungeon.hero.heroClass.name());
				if (Dungeon.seed != 0) b.append(" | seed ").append(com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed.convertToCode(Dungeon.seed));
			}
		} catch (RuntimeException ignored){ }
		return b.toString();
	}

	/** Appends a note. Returns false when nothing was written. */
	public static synchronized boolean add(String note){
		if (note == null) return false;
		String clean = note.replace('\r', ' ').replace('\n', ' ').trim();
		if (clean.isEmpty()) return false;
		File f = file();
		if (f.length() > MAX_BYTES) return false;
		try (OutputStream out = new FileOutputStream(f, true)){
			out.write((context() + "\n  " + clean + "\n").getBytes(StandardCharsets.UTF_8));
			return true;
		} catch (IOException e){
			return false;
		}
	}

	/** All saved notes, or an empty string. */
	public static synchronized String read(){
		File f = file();
		if (!f.isFile()) return "";
		try { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }
		catch (IOException e){ return ""; }
	}

	public static synchronized int count(){
		String t = read();
		int n = 0;
		for (String l : t.split("\n")) if (l.startsWith("  ")) n++;
		return n;
	}

	public static synchronized boolean clear(){
		File f = file();
		return !f.exists() || f.delete();
	}
}
