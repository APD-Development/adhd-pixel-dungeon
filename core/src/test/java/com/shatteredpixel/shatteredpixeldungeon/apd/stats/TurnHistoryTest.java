package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TurnHistoryTest {

	private static ApdEvent event( long index, int type, long turn ){
		return new ApdEvent(index, type, 0, turn, new byte[0]);
	}

	@Test
	public void opensAtTurnOneAndDoesNotRelabelRunPreamble(){
		TurnHistory history = TurnHistory.fromEvents(Arrays.asList(
				event(0, EventTypes.RUN_START, 0),
				event(1, EventTypes.LEVEL_ENTER, 1),
				event(2, EventTypes.RUN_END, 18)));

		assertTrue(history.hasRecordedTurns());
		assertEquals(1, history.turn());
		assertEquals(18, history.lastTurn());
		assertEquals(1, history.beforeFirstTurn().size());
		assertEquals(EventTypes.RUN_START, history.beforeFirstTurn().get(0).type);
		assertEquals(EventTypes.LEVEL_ENTER, history.events().get(0).type);
	}

	@Test
	public void presetAndCustomJumpsVisitExactTurnsAndClamp(){
		TurnHistory history = TurnHistory.fromEvents(Arrays.asList(
				event(0, EventTypes.LEVEL_ENTER, 1),
				event(1, EventTypes.RUN_END, 2500)));

		assertTrue(TurnHistory.isPresetStep(1));
		assertTrue(TurnHistory.isPresetStep(10));
		assertTrue(TurnHistory.isPresetStep(100));
		assertTrue(TurnHistory.isPresetStep(1000));
		assertFalse(TurnHistory.isPresetStep(37));

		history.forward(10);
		assertEquals(11, history.turn());
		assertTrue("sparse histories must not invent an event", history.events().isEmpty());
		history.forward(1000).forward(37);
		assertEquals(1048, history.turn());
		history.goTo(9999);
		assertEquals(2500, history.turn());
		history.back(1000).back(2000);
		assertEquals(1, history.turn());
	}

	@Test
	public void duplicateTurnEventsKeepRawOrder(){
		TurnHistory history = TurnHistory.fromEvents(Arrays.asList(
				event(7, EventTypes.LEVEL_ENTER, 5),
				event(8, EventTypes.SAVE, 5),
				event(9, EventTypes.RUN_END, 5))).goTo(5);

		assertEquals(3, history.events().size());
		assertEquals(7, history.events().get(0).index);
		assertEquals(8, history.events().get(1).index);
		assertEquals(9, history.events().get(2).index);
	}

	@Test
	public void rawSegmentsResolveRollbacksBeforeTheirIndexesAreCombined(){
		ReadResult first = new ReadResult();
		first.events.add(event(0, EventTypes.LEVEL_ENTER, 1));
		first.events.add(event(1, EventTypes.SAVE, 4));
		ReadResult second = new ReadResult();
		second.events.add(event(0, EventTypes.SESSION_START, 4));
		second.events.add(event(1, EventTypes.LEVEL_ENTER, 5));

		TurnHistory history = TurnHistory.fromSegments(Arrays.asList(first, second)).goTo(4);
		assertEquals(2, history.events().size());
		assertEquals(EventTypes.SAVE, history.events().get(0).type);
		assertEquals(EventTypes.SESSION_START, history.events().get(1).type);
		assertEquals(5, history.lastTurn());
	}

	@Test
	public void rolledBackPlayIsNotShownAsCanonicalHistory(){
		List<ApdEvent> events = new ArrayList<>();
		events.add(event(0, EventTypes.SAVE, 1));
		events.add(event(1, EventTypes.LEVEL_ENTER, 2));
		events.add(event(2, EventTypes.LEVEL_EXIT, 3));
		byte[] payload = new ByteSink()
				.writeUnsigned(LifecycleEvents.ROLLBACK_MATCHED)
				.writeSigned(0).writeUnsigned(0).writeSigned(0).toByteArray();
		events.add(new ApdEvent(3, EventTypes.ROLLBACK, 0, 1, payload));
		events.add(event(4, EventTypes.LEVEL_ENTER, 2));

		TurnHistory history = TurnHistory.fromEvents(events);
		assertEquals(2, history.lastTurn());
		assertEquals(1, history.eventsAt(2).size());
		assertEquals(4, history.eventsAt(2).get(0).index);
		assertEquals(2, history.eventsAt(1).size());
	}

	@Test
	public void emptyOrTurnZeroOnlyHistoryIsSafe(){
		TurnHistory empty = TurnHistory.fromEvents(Collections.<ApdEvent>emptyList());
		assertFalse(empty.hasRecordedTurns());
		assertEquals(1, empty.turn());
		assertEquals(1, empty.lastTurn());
		assertTrue(empty.events().isEmpty());
		empty.forward(Long.MAX_VALUE).back(Long.MAX_VALUE);
		assertEquals(1, empty.turn());

		TurnHistory onlyPreamble = TurnHistory.fromEvents(Collections.singletonList(
				event(0, EventTypes.RUN_START, 0)));
		assertFalse(onlyPreamble.hasRecordedTurns());
		assertEquals(1, onlyPreamble.beforeFirstTurn().size());
	}

	@Test
	public void hugeCustomJumpsCannotOverflow(){
		TurnHistory history = TurnHistory.fromEvents(Collections.singletonList(
				event(0, EventTypes.RUN_END, Long.MAX_VALUE)));

		history.goTo(Long.MAX_VALUE - 4).forward(10);
		assertEquals(Long.MAX_VALUE, history.turn());
		history.back(Long.MAX_VALUE);
		assertEquals(1, history.turn());
	}

	@Test(expected = IllegalArgumentException.class)
	public void zeroCustomJumpIsRejected(){
		TurnHistory.fromEvents(Collections.<ApdEvent>emptyList()).forward(0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeCustomJumpIsRejected(){
		TurnHistory.fromEvents(Collections.<ApdEvent>emptyList()).back(-1);
	}
}
