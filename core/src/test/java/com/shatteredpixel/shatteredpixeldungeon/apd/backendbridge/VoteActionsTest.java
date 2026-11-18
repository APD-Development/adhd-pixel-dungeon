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

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.CommunityMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.OutboxStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Voting;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VoteActionsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Id ROUND = Id.of(Id.Kind.VOTE_ROUND, "vr_1");

	private static Set<String> set( String... s ){
		return new HashSet<>(Arrays.asList(s));
	}

	private VoteMessages.Offer offer( long opens, long closes ){
		return new VoteMessages.Offer(new Voting.Round(ROUND, set("a", "b", "c"), 2, opens, closes), "Next character");
	}

	@Test
	public void roundsRoundTripAndBadOnesAreRefused(){
		VoteMessages.Offer o = offer(10, 20);
		VoteMessages.Offer back = VoteMessages.offerFromEnvelope(VoteMessages.offerToEnvelope(o));
		assertNotNull(back);
		assertEquals(ROUND, back.round.id);
		assertEquals(set("a", "b", "c"), back.round.options);
		assertEquals(2, back.round.maxChoices);
		assertEquals("Next character", back.title);
		assertNull("closes before it opens", VoteMessages.offerFromEnvelope(new Envelope(VoteMessages.ROUND_SCHEMA, 1, 0, "id=vr_1\noptions=a\nmaxChoices=1\nopensAtMillis=5\nclosesAtMillis=5\n")));
		assertNull("no options", VoteMessages.offerFromEnvelope(new Envelope(VoteMessages.ROUND_SCHEMA, 1, 0, "id=vr_1\noptions=\nmaxChoices=1\nopensAtMillis=1\nclosesAtMillis=5\n")));
		assertNull("another major version", VoteMessages.offerFromEnvelope(new Envelope(VoteMessages.ROUND_SCHEMA, 2, 0, VoteMessages.offerToEnvelope(o).payload)));
	}

	@Test
	public void aBallotIsCheckedOnTheDeviceQueuedAndReplacedBeforeItIsSent() throws Exception {
		long now = System.currentTimeMillis();
		assertTrue(VoteActions.store(tmp.getRoot(), offer(now - 1000, now + 100_000)));
		assertTrue("storing the same round again updates it", VoteActions.store(tmp.getRoot(), offer(now - 1000, now + 200_000)));
		assertEquals(1, VoteActions.offers(tmp.getRoot()).size());

		assertEquals(VoteActions.Result.NOT_SIGNED_IN, VoteActions.cast(tmp.getRoot(), ROUND, set("a"), now));
		new SessionStore(tmp.getRoot()).signIn(Id.of(Id.Kind.PLAYER, "pl_ann"), "tok", now + 1_000_000);
		assertEquals(VoteActions.Result.NO_SUCH_ROUND, VoteActions.cast(tmp.getRoot(), Id.of(Id.Kind.VOTE_ROUND, "vr_9"), set("a"), now));
		assertEquals(VoteActions.Result.UNKNOWN_OPTION, VoteActions.cast(tmp.getRoot(), ROUND, set("zzz"), now));
		assertEquals(VoteActions.Result.TOO_MANY_CHOICES, VoteActions.cast(tmp.getRoot(), ROUND, set("a", "b", "c"), now));
		assertEquals(VoteActions.Result.EMPTY, VoteActions.cast(tmp.getRoot(), ROUND, set(), now));
		assertEquals(VoteActions.Result.CLOSED, VoteActions.cast(tmp.getRoot(), ROUND, set("a"), now + 300_000));
		assertEquals("nothing invalid was queued", 0, SubmissionQueue.pending(tmp.getRoot()));

		assertEquals(VoteActions.Result.QUEUED, VoteActions.cast(tmp.getRoot(), ROUND, set("a"), now));
		assertEquals("changing your mind replaces the queued ballot", VoteActions.Result.QUEUED, VoteActions.cast(tmp.getRoot(), ROUND, set("b", "c"), now + 5));
		Outbox box = new Outbox();
		OutboxStore.load(box, SubmissionQueue.fileIn(tmp.getRoot()));
		assertEquals(1, box.size());
		CommunityMessages.BallotRequest b = CommunityMessages.ballotFromEnvelope(box.items().get(0).envelope);
		assertEquals(set("b", "c"), b.choices);
		assertFalse("the ballot does not name the voter", box.items().get(0).envelope.payload.contains("pl_ann"));
	}

	@Test
	public void aNotYetOpenRoundRefusesTheBallot(){
		long now = System.currentTimeMillis();
		VoteActions.store(tmp.getRoot(), offer(now + 50_000, now + 100_000));
		new SessionStore(tmp.getRoot()).signIn(Id.of(Id.Kind.PLAYER, "pl_ann"), "tok", now + 60_000);
		assertEquals(VoteActions.Result.NOT_OPEN, VoteActions.cast(tmp.getRoot(), ROUND, set("a"), now));
	}
}
