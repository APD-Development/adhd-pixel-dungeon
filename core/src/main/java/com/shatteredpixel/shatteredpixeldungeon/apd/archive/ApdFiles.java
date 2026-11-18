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

package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

//How the app reaches a file the player chooses: where an archive is saved and which one is imported. The game code only talks to
//this; the Android build supplies the system file picker (the Storage Access Framework), and everything else gets a plain-folder
//fallback so the feature works on desktop too. A provider never reads or writes anything on its own: it hands over a stream once the
//player has chosen, or says they did not.
public final class ApdFiles {

	private ApdFiles(){}

	public interface ExportTarget {
		//the stream to write the archive to; the receiver must close it
		void onStream( OutputStream out, String displayName );
		void onCancelled();
		void onFailed( String why );
	}

	public interface ImportSource {
		//the stream to read the archive from; the receiver must close it
		void onStream( InputStream in, String displayName );
		void onCancelled();
		void onFailed( String why );
	}

	public interface Provider {
		void chooseExport( String suggestedName, ExportTarget target );
		//the same for a file of another type (a report); a provider that cannot tell the system the type just saves the file
		default void chooseExport( String suggestedName, String mimeType, ExportTarget target ){
			chooseExport(suggestedName, target);
		}
		//Hands a piece of text to the system's share sheet (messaging, mail, notes...). False when this platform has none.
		default boolean shareText( String subject, String text ){
			return false;
		}
		void chooseImport( ImportSource source );
		//a few words on where files go, for the confirmation messages ("the folder you choose", ...)
		String where();
	}

	//set by the platform at start-up (Android installs the system file picker)
	public static volatile Provider provider;

	public static Provider provider( File fallbackBase ){
		Provider p = provider;
		return p != null ? p : new FolderProvider(fallbackBase);
	}

	//Saves into "exports/" and imports the newest ".zip" of "imports/" under a base folder: for desktop builds and tests.
	public static final class FolderProvider implements Provider {
		private final File base;

		public FolderProvider( File base ){
			this.base = base;
		}

		@Override
		public void chooseExport( String suggestedName, ExportTarget target ){
			try {
				File dir = new File(base, "exports");
				if (!dir.isDirectory() && !dir.mkdirs()){
					target.onFailed("cannot create " + dir);
					return;
				}
				File f = new File(dir, suggestedName);
				if (!isDirectExportFile(dir, f)){
					target.onFailed("unsafe export destination: " + f);
					return;
				}
				target.onStream(new FileOutputStream(f), f.getPath());
			} catch (Exception e){
				target.onFailed(String.valueOf(e));
			}
		}

		// Reject aliases beneath exports/ so a fallback write cannot truncate a
		// different file. A symlinked base storage root itself remains valid.
		private boolean isDirectExportFile( File dir, File file ){
			try {
				File expectedDir = new File(base.getCanonicalFile(), "exports");
				return dir.getCanonicalFile().equals(expectedDir)
						&& file.getCanonicalFile().equals(new File(expectedDir, file.getName()));
			} catch (java.io.IOException unreadable){
				return false;
			}
		}

		@Override
		public void chooseImport( ImportSource source ){
			try {
				File dir = new File(base, "imports");
				if (!isDirectImportDirectory(dir)){
					source.onFailed("unsafe import directory: " + dir);
					return;
				}
				File[] zips = dir.listFiles();
				File newest = null;
				if (zips != null){
					Arrays.sort(zips);
					// Prefer the newest file by modification time. Keep the previous name order for ties.
					for (File z : zips){
						if (z.isFile() && z.getName().endsWith(".zip") && isDirectImportFile(dir, z)
								&& (newest == null || z.lastModified() >= newest.lastModified())) newest = z;
					}
				}
				if (newest == null){
					source.onFailed("put an archive (a .zip) in " + dir + " first");
					return;
				}
				source.onStream(new FileInputStream(newest), newest.getPath());
			} catch (Exception e){
				source.onFailed(String.valueOf(e));
			}
		}

		// The imports/ directory itself must belong directly to the configured
		// storage root. Otherwise file-level checks would trust an external alias.
		// A deliberately symlinked base storage root is still supported.
		private boolean isDirectImportDirectory( File dir ){
			try {
				return dir.getCanonicalFile().equals(new File(base.getCanonicalFile(), "imports"));
			} catch (java.io.IOException unreadable){
				return false;
			}
		}

		// A .zip alias can point outside imports/ even though isFile() says true.
		// Allow a symlinked base storage root, but only select direct files inside
		// its canonical imports/ directory, matching APD's history ownership rule.
		private static boolean isDirectImportFile( File dir, File file ){
			try {
				return file.getCanonicalFile().equals(new File(dir.getCanonicalFile(), file.getName()));
			} catch (java.io.IOException unreadable){
				return false;
			}
		}

		@Override
		public String where(){
			return "the exports/ and imports/ folders in " + base;
		}
	}
}
