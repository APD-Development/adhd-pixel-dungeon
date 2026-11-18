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

package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveExporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveImporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.StorageMeter;
import com.watabou.noosa.Game;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//The player's export and import of their history, and the storage meter, joined to the recorder. Everything runs off the game thread
//and reports in plain words through a Listener; nothing here can touch a run in progress (the recorder keeps its own log and the import only
//ever adds files). Never throws.
public final class HistoryTransfer {

	private HistoryTransfer(){}

	public interface Listener {
		//called on a background thread
		void finished( String message );
	}

	public static String suggestedName( long nowMillis ){
		return "apd-history-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date(nowMillis)) + ".zip";
	}

	public static StorageMeter.Report storage(){
		return StorageMeter.measure(ApdRecorder.historyRoot());
	}

	public static void export( final Listener listener ){
		final File root = ApdRecorder.historyRoot();
		if (root == null){
			listener.finished("History is not set up, so there is nothing to export.");
			return;
		}
		try {
			ApdFiles.provider(root.getParentFile()).chooseExport(suggestedName(System.currentTimeMillis()), new ApdFiles.ExportTarget(){
				@Override
				public void onStream( final OutputStream out, final String displayName ){
					Thread t = new Thread(new Runnable(){
						@Override
						public void run(){
							String message;
							try {
								ApdRecorder.flushForReading();
								ArchiveExporter.Report r = ArchiveExporter.export(root, out, Game.version, System.currentTimeMillis());
								try { out.close(); } catch (Throwable ignored){}
								message = r.summary();
							} catch (Throwable e){
								message = "Export failed: " + e;
							}
							listener.finished(message);
						}
					}, "apd-export");
					t.setDaemon(true);
					t.start();
				}

				@Override
				public void onCancelled(){
					listener.finished("Export cancelled. Nothing was written.");
				}

				@Override
				public void onFailed( String why ){
					listener.finished("Export failed: " + why);
				}
			});
		} catch (Throwable e){
			listener.finished("Export failed: " + e);
		}
	}

	public static void importHistory( final Listener listener ){
		final File root = ApdRecorder.historyRoot();
		if (root == null){
			listener.finished("History is not set up, so nothing can be imported.");
			return;
		}
		try {
			ApdFiles.provider(root.getParentFile()).chooseImport(new ApdFiles.ImportSource(){
				@Override
				public void onStream( final InputStream in, final String displayName ){
					Thread t = new Thread(new Runnable(){
						@Override
						public void run(){
							String message;
							try {
								ApdRecorder.flushForReading();
								ArchiveImporter.Report r = ArchiveImporter.importArchive(root, in);
								try { in.close(); } catch (Throwable ignored){}
								message = r.summary();
							} catch (Throwable e){
								message = "Import failed: " + e;
							}
							listener.finished(message);
						}
					}, "apd-import");
					t.setDaemon(true);
					t.start();
				}

				@Override
				public void onCancelled(){
					listener.finished("Import cancelled. Nothing was changed.");
				}

				@Override
				public void onFailed( String why ){
					listener.finished("Import failed: " + why);
				}
			});
		} catch (Throwable e){
			listener.finished("Import failed: " + e);
		}
	}
}
