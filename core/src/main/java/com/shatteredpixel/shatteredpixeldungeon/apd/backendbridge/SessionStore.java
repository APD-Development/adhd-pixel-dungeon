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

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.HttpGateway;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// The signed-in player's session on this device: their player id and the server's session token with its expiry. How a player gets a session
// (which sign-in provider) is not decided and not in here; whatever signs the player in hands the result to signIn(). The token lives in a file
// in the app's private storage and is never written to the run history or sent anywhere except to the server as a bearer token. Signing out
// deletes it. Nothing about the session touches saves or the run history.
public final class SessionStore {

	public static final String FILE_NAME = "session.txt";

	private final File file;

	public SessionStore( File historyRoot ){
		this.file = new File(historyRoot, FILE_NAME);
	}

	public static final class Session {
		public final Id player;
		public final String token;
		public final long expiresAtMillis;

		Session( Id player, String token, long expiresAtMillis ){
			this.player = player;
			this.token = token;
			this.expiresAtMillis = expiresAtMillis;
		}
	}

	//false if the arguments are not a usable session or it could not be written
	public boolean signIn( Id player, String token, long expiresAtMillis ){
		if (player == null || player.kind != Id.Kind.PLAYER || token == null || token.isEmpty() || token.length() > 4000 || token.indexOf('\n') >= 0) return false;
		List<String[]> rows = new ArrayList<>();
		rows.add(new String[]{ player.value, token, String.valueOf(expiresAtMillis) });
		try {
			TabFile.write(file, rows);
			return true;
		} catch (IOException e){
			return false;
		}
	}

	public void signOut(){
		if (file.exists()) file.delete();
	}

	//null when nobody is signed in or the session has run out (an expired session is removed)
	public Session current( long nowMillis ){
		List<String[]> rows = TabFile.read(file, 3);
		if (rows.isEmpty()) return null;
		try {
			String[] r = rows.get(0);
			long exp = Long.parseLong(r[2]);
			if (nowMillis >= exp){
				signOut();
				return null;
			}
			return new Session(Id.of(Id.Kind.PLAYER, r[0]), r[1], exp);
		} catch (RuntimeException e){
			return null;
		}
	}

	public HttpGateway.TokenSource tokenSource(){
		return new HttpGateway.TokenSource(){
			@Override
			public String token(){
				Session s = current(System.currentTimeMillis());
				return s == null ? null : s.token;
			}
		};
	}
}
