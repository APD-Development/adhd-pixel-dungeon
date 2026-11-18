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

// Wire forms for the evidence exchange. The server asks (request), the app answers with its decision, and only when that is PROVIDE does the
// data itself follow, under its own schema, which is the only way raw run data can ever be sent (see DataPolicy).
public final class EvidenceMessages {

	public static final String REQUEST_SCHEMA = "apd.evidence-request";
	public static final String DATA_SCHEMA = "apd.evidence-data";
	public static final int MAJOR = 1, MINOR = 1;

	//A request together with the run it is about. The run id is the one the player submitted, so the app knows which local log to look at.
	public static final class Asked {
		public final Evidence.Request request;
		public final String runId;

		public Asked( Evidence.Request request, String runId ){
			if (runId == null || runId.isEmpty() || runId.length() > 80 || runId.indexOf('\n') >= 0) throw new IllegalArgumentException("bad run id");
			this.request = request;
			this.runId = runId;
		}
	}

	private EvidenceMessages(){}

	public static Envelope askedToEnvelope( Asked a ){
		Evidence.Request r = a.request;
		return new Envelope(REQUEST_SCHEMA, MAJOR, MINOR, "submission=" + r.submissionId.value + "\nkind=" + r.kind.name() + "\ndeadlineMillis=" + r.deadlineMillis + "\nrun=" + a.runId + "\n");
	}

	//null if the envelope is not a readable request that names its run (a 1.0 request does not, so it cannot be answered)
	public static Asked askedFromEnvelope( Envelope e ){
		Evidence.Request r = requestFromEnvelope(e);
		if (r == null) return null;
		for (String line : e.payload.split("\n")){
			if (line.startsWith("run=")){
				try {
					return new Asked(r, line.substring(4));
				} catch (IllegalArgumentException ex){
					return null;
				}
			}
		}
		return null;
	}

	public static Envelope requestToEnvelope( Evidence.Request r ){
		return new Envelope(REQUEST_SCHEMA, MAJOR, MINOR, "submission=" + r.submissionId.value + "\nkind=" + r.kind.name() + "\ndeadlineMillis=" + r.deadlineMillis + "\n");
	}

	//null if the envelope is not a readable evidence request
	public static Evidence.Request requestFromEnvelope( Envelope e ){
		if (!REQUEST_SCHEMA.equals(e.schema) || e.compatibleWith(MAJOR, MINOR) == Envelope.Compat.UNSUPPORTED_MAJOR) return null;
		Map<String, String> m = new HashMap<>();
		for (String line : e.payload.split("\n")){
			int i = line.indexOf('=');
			if (i > 0) m.put(line.substring(0, i), line.substring(i + 1));
		}
		try {
			return new Evidence.Request(Id.of(Id.Kind.SUBMISSION, m.get("submission")), Evidence.Kind.valueOf(m.get("kind")), Long.parseLong(m.get("deadlineMillis")));
		} catch (RuntimeException ex){
			return null;
		}
	}

	//The data envelope for an agreed request. The payload is the base64 of the bytes, with the submission it answers; the digest lets the
	//server check it against what was submitted before it reads anything.
	public static Envelope dataToEnvelope( Id submissionId, Evidence.Kind kind, byte[] data ){
		if (submissionId.kind != Id.Kind.SUBMISSION) throw new IllegalArgumentException("not a submission id");
		return new Envelope(DATA_SCHEMA, MAJOR, MINOR, "submission=" + submissionId.value + "\nkind=" + kind.name() + "\ndigest=" + Canon.hex(Canon.sha256(data))
				+ "\ndata=" + java.util.Base64.getEncoder().encodeToString(data) + "\n");
	}
}
