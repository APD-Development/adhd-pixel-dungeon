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

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Canon;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Evidence;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.EvidenceMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;

import java.io.File;

// The player's answer to an evidence request. Data is only ever queued for a request the player agreed to, before its deadline, for a run
// this device really submitted, and only if the local log still matches the digest that was sent then. Only the full run log is supported;
// anything else is declined here and nothing is built for it.
public final class EvidenceFlow {

	public enum Result { QUEUED, DECLINED, EXPIRED, LOG_CHANGED, NOT_SUBMITTED, UNSUPPORTED, TOO_LARGE, FAILED }

	public static final int MAX_LOG_BYTES = 8 * 1024 * 1024;

	private EvidenceFlow(){}

	public static Result answer( File historyRoot, EvidenceMessages.Asked asked, boolean agreed, long nowMillis ){
		Evidence.Request req = asked.request;
		Result result = decide(historyRoot, asked, agreed, nowMillis);
		//every outcome closes the request; only QUEUED leaves something to send
		if (result != Result.FAILED) EvidenceInbox.remove(historyRoot, req.submissionId, req.kind);
		return result;
	}

	private static Result decide( File historyRoot, EvidenceMessages.Asked asked, boolean agreed, long nowMillis ){
		Evidence.Request req = asked.request;
		if (historyRoot == null) return Result.FAILED;
		if (req.kind != Evidence.Kind.FULL_RUN_LOG) return Result.UNSUPPORTED;
		SubmittedLedger.Entry sent = SubmittedLedger.get(historyRoot, asked.runId);
		if (sent == null || !StatsStore.isValidRunId(asked.runId)) return Result.NOT_SUBMITTED;
		byte[] bytes;
		try {
			File log = new StatsStore(historyRoot).logFile(asked.runId);
			if (!log.isFile()) return Result.LOG_CHANGED;
			if (log.length() > MAX_LOG_BYTES) return Result.TOO_LARGE;
			bytes = SubmissionQueue.readAllBytes(log);
		} catch (java.io.IOException e){
			return Result.FAILED;
		}
		String local = Canon.hex(Canon.sha256(bytes));
		switch (Evidence.decide(req, agreed, nowMillis, local, sent.digestHex)){
			case EXPIRED: return Result.EXPIRED;
			case LOG_CHANGED: return Result.LOG_CHANGED;
			case NEEDS_CONSENT: return Result.DECLINED;
			default:
				Id id;
				try {
					id = Id.of(Id.Kind.OUTBOX, "ev_" + req.submissionId.value);
				} catch (IllegalArgumentException e){
					return Result.FAILED;
				}
				return SubmissionQueue.enqueueEnvelope(historyRoot, id, EvidenceMessages.dataToEnvelope(req.submissionId, req.kind, bytes), nowMillis) ? Result.QUEUED : Result.FAILED;
		}
	}
}
