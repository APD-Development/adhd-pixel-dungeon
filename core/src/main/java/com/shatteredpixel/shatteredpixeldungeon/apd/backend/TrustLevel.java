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

// How far a record can be believed. The client may only label its own data CLIENT_CLAIM; higher levels are assigned server-side.
// DEVELOPER_ISSUED is a separate axis (who issued it), not "above" a replay check.
public enum TrustLevel {
	//the app said so; anyone who can modify the app can say anything
	CLIENT_CLAIM,
	//the server received and stored it unmodified; says nothing about whether it is true
	SERVER_RECEIVED,
	//the server checked it for internal consistency and plausibility against its own data
	SERVER_CHECKED,
	//the run was reproduced from evidence by the server (or independent reviewers); strongest for rankings
	REPLAY_VERIFIED,
	//signed by developer authority; never producible by gameplay code
	DEVELOPER_ISSUED;

	//DEVELOPER_ISSUED is a different axis (who issued it), not "stronger than a replay"; it is only ever compared with itself
	public boolean satisfies( TrustLevel needed ){
		if (needed == DEVELOPER_ISSUED || this == DEVELOPER_ISSUED) return this == needed;
		return ordinal() >= needed.ordinal();
	}
}
