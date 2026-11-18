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

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.CommunityMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Voting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Votes the server has opened for this player, and casting a ballot. A ballot is checked on the device with the same rules the server uses
// (open, not closed, known options, not too many), then queued like any other item; casting again before it is sent replaces the queued
// ballot. Nothing is sent until the player presses Send, and a ballot never names the voter: the server takes that from the session.
public final class VoteActions {

	public static final String FILE_NAME = "open_votes.txt";
	public static final int MAX_STORED = 20;

	public enum Result { QUEUED, NOT_SIGNED_IN, NO_SUCH_ROUND, NOT_OPEN, CLOSED, UNKNOWN_OPTION, TOO_MANY_CHOICES, EMPTY, FAILED }

	private VoteActions(){}

	private static File fileIn( File historyRoot ){
		return new File(historyRoot, FILE_NAME);
	}

	public static List<VoteMessages.Offer> offers( File historyRoot ){
		List<VoteMessages.Offer> out = new ArrayList<>();
		if (historyRoot == null) return out;
		for (String[] r : TabFile.read(fileIn(historyRoot), 1)){
			VoteMessages.Offer o = VoteMessages.offerFromEnvelope(new Envelope(VoteMessages.ROUND_SCHEMA, VoteMessages.MAJOR, VoteMessages.MINOR, r[0]));
			if (o != null) out.add(o);
		}
		return out;
	}

	//Stores an offer the server sent. A round already stored is updated in place (the server may extend it); the store holds at most MAX_STORED.
	public static boolean store( File historyRoot, VoteMessages.Offer offer ){
		if (historyRoot == null) return false;
		List<VoteMessages.Offer> all = offers(historyRoot);
		boolean replaced = false;
		for (int i = 0; i < all.size(); i++){
			if (all.get(i).round.id.equals(offer.round.id)){
				all.set(i, offer);
				replaced = true;
			}
		}
		if (!replaced){
			if (all.size() >= MAX_STORED) return false;
			all.add(offer);
		}
		List<String[]> rows = new ArrayList<>();
		for (VoteMessages.Offer o : all) rows.add(new String[]{ VoteMessages.offerToEnvelope(o).payload });
		try {
			TabFile.write(fileIn(historyRoot), rows);
			return true;
		} catch (IOException e){
			return false;
		}
	}

	public static Result cast( File historyRoot, Id round, Set<String> choices, long nowMillis ){
		SessionStore.Session session = new SessionStore(historyRoot).current(nowMillis);
		if (session == null) return Result.NOT_SIGNED_IN;
		VoteMessages.Offer offer = null;
		for (VoteMessages.Offer o : offers(historyRoot)) if (o.round.id.equals(round)) offer = o;
		if (offer == null) return Result.NO_SUCH_ROUND;
		switch (new Voting(offer.round).cast(session.player, choices, nowMillis)){
			case NOT_OPEN: return Result.NOT_OPEN;
			case CLOSED: return Result.CLOSED;
			case UNKNOWN_OPTION: return Result.UNKNOWN_OPTION;
			case TOO_MANY_CHOICES: return Result.TOO_MANY_CHOICES;
			case EMPTY: return Result.EMPTY;
			default:
		}
		CommunityMessages.BallotRequest ballot = new CommunityMessages.BallotRequest(round, choices);
		Id id;
		try {
			id = Id.of(Id.Kind.OUTBOX, ballot.idempotencyKey());
		} catch (IllegalArgumentException e){
			return Result.FAILED;
		}
		return SubmissionQueue.replaceEnvelope(historyRoot, id, CommunityMessages.ballotToEnvelope(ballot), nowMillis) ? Result.QUEUED : Result.FAILED;
	}
}
