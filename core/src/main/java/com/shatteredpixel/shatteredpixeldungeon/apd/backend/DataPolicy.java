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

import java.util.HashMap;
import java.util.Map;

// Where each schema may live. A schema that isn't listed can't be sent.
public final class DataPolicy {

	public enum DataClass {
		//never leaves the device unless the player exports it themselves (files they hold)
		LOCAL_ONLY,
		//sent only to the player's own account (their devices), never shown to others
		ACCOUNT_PRIVATE,
		//sent when the player chooses to submit it, then shown to others
		PUBLIC_ON_SUBMIT
	}

	private static final Map<String, DataClass> SCHEMAS = new HashMap<>();
	static {
		SCHEMAS.put("apd.run-log", DataClass.LOCAL_ONLY);                 //raw telemetry of runs
		SCHEMAS.put("apd.uimsd-history", DataClass.LOCAL_ONLY);           //the player's whole statistics database
		SCHEMAS.put("apd.floor-replay", DataClass.LOCAL_ONLY);            //seed/floor reconstruction data
		SCHEMAS.put("apd.settings-sync", DataClass.ACCOUNT_PRIVATE);      //preferences kept in step between devices
		SCHEMAS.put(RunSubmission.SCHEMA, DataClass.PUBLIC_ON_SUBMIT);    //summary of one finished run
		SCHEMAS.put("apd.content-submission", DataClass.PUBLIC_ON_SUBMIT);
		SCHEMAS.put("apd.vote-ballot", DataClass.ACCOUNT_PRIVATE);        //who voted for what is not shown; only tallies are
		SCHEMAS.put("apd.report", DataClass.ACCOUNT_PRIVATE);             //visible to moderators only
		SCHEMAS.put(AccountRequests.EXPORT_SCHEMA, DataClass.ACCOUNT_PRIVATE);
		SCHEMAS.put(AccountRequests.DELETE_SCHEMA, DataClass.ACCOUNT_PRIVATE);
		SCHEMAS.put(EvidenceMessages.REQUEST_SCHEMA, DataClass.ACCOUNT_PRIVATE);
		SCHEMAS.put(VoteMessages.ROUND_SCHEMA, DataClass.ACCOUNT_PRIVATE);
		SCHEMAS.put(EvidenceMessages.DATA_SCHEMA, DataClass.ACCOUNT_PRIVATE);   //raw run data: only ever built for a request the player agreed to
	}

	private DataPolicy(){}

	//null if the schema is unknown
	public static DataClass classOf( String schema ){
		return SCHEMAS.get(schema);
	}

	//may an envelope of this schema be queued for sending at all?
	public static boolean mayLeaveDevice( String schema ){
		DataClass c = SCHEMAS.get(schema);
		return c != null && c != DataClass.LOCAL_ONLY;
	}

	public static java.util.Set<String> schemas(){
		return java.util.Collections.unmodifiableSet(SCHEMAS.keySet());
	}
}
