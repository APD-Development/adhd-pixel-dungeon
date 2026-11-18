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

package com.shatteredpixel.shatteredpixeldungeon.apd.demo;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.watabou.noosa.Game;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

//INDEV-ONLY synthetic demo data for UIMSD: a made-up history so the screen can be tested without playing for hours.
//
//Demo data is kept apart from real history in every way that matters:
//  - it lives in its OWN folder (demoRoot(), a sibling of the real "apd" folder), never inside the real history;
//  - that folder carries a marker file, every demo log is named "demo-...", and every demo log says SYNTHETIC-DEMO-DATA in its header,
//    its run start (platform, seed text, ruleset text) and its game version, so it stays recognisable even if a file is copied;
//  - the recorder refuses to record into a folder with the marker, the importer refuses to read or write one, and the dataset
//    store of a REAL folder skips any "demo-" log it finds there;
//  - delete() only removes a folder that carries the marker and has the demo folder's name.
//Anything that exports, uploads, ranks or otherwise treats history as legitimate must call isSyntheticRoot() / isSyntheticLogId() /
//isSyntheticBuild() and reject the data.
//
//It is only offered by development builds (available()); release builds never show the controls.
public final class DemoData {

	public static final String BUILD_MARK = "SYNTHETIC-DEMO-DATA";
	public static final String VERSION_MARK = "SYNTHETIC-DEMO";
	public static final String RUN_PREFIX = DatasetStore.DEMO_LOG_PREFIX;
	public static final String MARKER_FILE = DatasetStore.DEMO_MARKER_FILE;
	public static final String DIR_NAME = "apd-demo-data";
	//the tutorial's sample history (same protections, its own folder, deleted when the tutorial ends)
	public static final String SAMPLE_DIR_NAME = "apd-tutorial-sample";

	//which data UIMSD shows. Not saved: every start of the app shows real data again.
	private static volatile boolean viewingDemo;

	private DemoData(){
	}

	//only development builds (their version name ends in INDEV) offer demo data
	public static boolean available(){
		String v = Game.version;
		return v != null && v.contains("INDEV");
	}

	public static boolean viewingDemo(){
		return viewingDemo;
	}

	public static void viewDemo( boolean demo ){
		viewingDemo = demo;
	}

	//where demo data lives for a given real history folder: next to it, never inside it
	public static File demoRootFor( File realRoot ){
		if (realRoot == null) return null;
		File parent = realRoot.getAbsoluteFile().getParentFile();
		return parent == null ? null : new File(parent, DIR_NAME);
	}

	public static File sampleRootFor( File realRoot ){
		if (realRoot == null) return null;
		File parent = realRoot.getAbsoluteFile().getParentFile();
		return parent == null ? null : new File(parent, SAMPLE_DIR_NAME);
	}

	public static boolean isSyntheticRoot( File root ){
		return root != null && new File(root, MARKER_FILE).isFile();
	}

	public static boolean isSyntheticLogId( String logId ){
		return logId != null && logId.startsWith(RUN_PREFIX);
	}

	public static boolean isSyntheticBuild( String writerBuild ){
		return BUILD_MARK.equals(writerBuild);
	}

	//Writes the marker. Called before anything else is written into a demo folder.
	static void writeMarker( File root ) throws IOException {
		if (!root.isDirectory() && !root.mkdirs()) throw new IOException("cannot create " + root);
		FileOutputStream out = new FileOutputStream(new File(root, MARKER_FILE));
		try {
			out.write(("SYNTHETIC DEMO DATA for testing UIMSD. Not real play history. Never export, upload, rank or import this.\n").getBytes("UTF-8"));
		} finally {
			out.close();
		}
	}

	//Removes ALL demo data. Only a folder that is named like the demo folder AND carries the marker is touched; anything else is
	//left alone and false is returned. Returns true when the demo data is gone (also when there was none).
	public static boolean delete( File root ){
		if (root == null || !root.exists()) return true;
		if ((!DIR_NAME.equals(root.getName()) && !SAMPLE_DIR_NAME.equals(root.getName())) || !isSyntheticRoot(root)) return false;
		deleteTree(root);
		return !root.exists();
	}

	private static void deleteTree( File f ){
		File[] kids = f.listFiles();
		if (kids != null) for (File k : kids) deleteTree(k);
		f.delete();
	}
}
