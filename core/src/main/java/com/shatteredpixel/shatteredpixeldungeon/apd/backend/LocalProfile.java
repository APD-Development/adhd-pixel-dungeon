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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

// Who this installation is without an account: a device id always, a player id only after the player signs in (optional).
public final class LocalProfile {

	public final Id deviceId;
	//null until the player signs in to a community account (and again after they sign out)
	private Id playerId;

	public LocalProfile( Id deviceId ){
		if (deviceId.kind != Id.Kind.DEVICE) throw new IllegalArgumentException("not a device id");
		this.deviceId = deviceId;
	}

	public boolean signedIn(){
		return playerId != null;
	}

	public Id playerId(){
		return playerId;
	}

	public void signIn( Id player ){
		if (player.kind != Id.Kind.PLAYER) throw new IllegalArgumentException("not a player id");
		playerId = player;
	}

	public void signOut(){
		playerId = null;
	}
}
