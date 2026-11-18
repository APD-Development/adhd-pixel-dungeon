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

package com.shatteredpixel.shatteredpixeldungeon.android;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;

import java.io.InputStream;
import java.io.OutputStream;

//APD: where the history archive is saved and read from, through Android's system file picker (the Storage Access Framework): the
//player chooses the file, and the app gets a stream to exactly that file and nothing else. It needs no storage permission.
//The waiting request is kept in static fields so that the answer still arrives if the activity is recreated while the picker is open.
public class AndroidFileProvider implements ApdFiles.Provider, AndroidEventListener {

	private static final int REQUEST_EXPORT = 0x4150, REQUEST_IMPORT = 0x4151;

	private static volatile ApdFiles.ExportTarget pendingExport;
	private static volatile ApdFiles.ImportSource pendingImport;

	private final AndroidApplication activity;

	public AndroidFileProvider( AndroidApplication activity ){
		this.activity = activity;
		activity.addAndroidEventListener( this );
	}

	@Override
	public void chooseExport( final String suggestedName, final ApdFiles.ExportTarget target ){
		chooseExport( suggestedName, "application/zip", target );
	}

	@Override
	public boolean shareText( final String subject, final String text ){
		try {
			activity.runOnUiThread( new Runnable() {
				@Override
				public void run() {
					try {
						Intent send = new Intent( Intent.ACTION_SEND );
						send.setType( "text/plain" );
						send.putExtra( Intent.EXTRA_SUBJECT, subject );
						send.putExtra( Intent.EXTRA_TEXT, text );
						activity.startActivity( Intent.createChooser( send, subject ) );
					} catch (Throwable ignored) {
						//no app to share with: the report can still be saved as a file
					}
				}
			} );
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	@Override
	public void chooseExport( final String suggestedName, final String mimeType, final ApdFiles.ExportTarget target ){
		pendingExport = target;
		activity.runOnUiThread( new Runnable() {
			@Override
			public void run() {
				try {
					Intent intent = new Intent( Intent.ACTION_CREATE_DOCUMENT );
					intent.addCategory( Intent.CATEGORY_OPENABLE );
					intent.setType( mimeType );
					intent.putExtra( Intent.EXTRA_TITLE, suggestedName );
					activity.startActivityForResult( intent, REQUEST_EXPORT );
				} catch (Throwable e) {
					pendingExport = null;
					target.onFailed( String.valueOf( e ) );
				}
			}
		} );
	}

	@Override
	public void chooseImport( final ApdFiles.ImportSource source ){
		pendingImport = source;
		activity.runOnUiThread( new Runnable() {
			@Override
			public void run() {
				try {
					Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT );
					intent.addCategory( Intent.CATEGORY_OPENABLE );
					//archives are .zip, but some file apps label them with other types, so any file can be chosen and is then checked
					intent.setType( "*/*" );
					activity.startActivityForResult( intent, REQUEST_IMPORT );
				} catch (Throwable e) {
					pendingImport = null;
					source.onFailed( String.valueOf( e ) );
				}
			}
		} );
	}

	@Override
	public String where(){
		return "the place you choose";
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data ){
		if (requestCode == REQUEST_EXPORT){
			ApdFiles.ExportTarget target = pendingExport;
			pendingExport = null;
			if (target != null) deliverExport( activity.getContentResolver(), target, resultCode, data );
		} else if (requestCode == REQUEST_IMPORT){
			ApdFiles.ImportSource source = pendingImport;
			pendingImport = null;
			if (source != null) deliverImport( activity.getContentResolver(), source, resultCode, data );
		}
	}

	//What happens when the picker has answered, apart from the picker itself: open exactly the file the player chose and hand over its
	//stream, or say they cancelled or that it failed. Separate so that the device tests run this same code with a file chosen by the test.
	public static void deliverExport( ContentResolver resolver, ApdFiles.ExportTarget target, int resultCode, Intent data ){
		if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null){
			target.onCancelled();
			return;
		}
		try {
			Uri uri = data.getData();
			//"wt": a new or existing document is written from the start, never appended to
			OutputStream out = resolver.openOutputStream( uri, "wt" );
			if (out == null) target.onFailed( "the file could not be opened for writing" );
			else target.onStream( out, String.valueOf( uri.getLastPathSegment() ) );
		} catch (Throwable e) {
			target.onFailed( String.valueOf( e ) );
		}
	}

	public static void deliverImport( ContentResolver resolver, ApdFiles.ImportSource source, int resultCode, Intent data ){
		if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null){
			source.onCancelled();
			return;
		}
		try {
			Uri uri = data.getData();
			InputStream in = resolver.openInputStream( uri );
			if (in == null) source.onFailed( "the file could not be opened for reading" );
			else source.onStream( in, String.valueOf( uri.getLastPathSegment() ) );
		} catch (Throwable e) {
			source.onFailed( String.valueOf( e ) );
		}
	}
}
