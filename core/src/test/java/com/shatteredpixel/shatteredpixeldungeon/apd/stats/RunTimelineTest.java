package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

//Abandoned-branch rules on hand-built event lists, no files and no game.
public class RunTimelineTest {

	private final List<ApdEvent> events = new ArrayList<>();

	private ApdEvent add( int type, byte[] payload ){
		ApdEvent e = new ApdEvent(events.size(), type, 0, events.size(), payload);
		events.add(e);
		return e;
	}

	private ApdEvent game(){
		return add(EventTypes.LEVEL_ENTER, new byte[0]);
	}

	private ApdEvent save(){
		return add(EventTypes.SAVE, new byte[0]);
	}

	private ApdEvent rollback( int kind, long matchedIndex ){
		ByteSink b = new ByteSink().writeUnsigned(kind).writeSigned(matchedIndex).writeUnsigned(0).writeSigned(0);
		return add(EventTypes.ROLLBACK, b.toByteArray());
	}

	@Test
	public void noRollbackMeansEverythingIsCanonical(){
		game(); save(); game();
		RunTimeline t = RunTimeline.of(events);
		assertEquals(0, t.abandonedCount());
		assertEquals(3, t.canonical().size());
		assertEquals(-1, t.uncertainFromIndex());
	}

	@Test
	public void matchedRollbackAbandonsEverythingBetweenTheSaveAndTheRollback(){
		game();                         //0
		ApdEvent s = save();            //1
		ApdEvent a = game();            //2  abandoned
		ApdEvent b = game();            //3  abandoned
		ApdEvent gap = add(EventTypes.GAP, new byte[0]);      //4  recording meta: never abandoned
		ApdEvent rb = rollback(LifecycleEvents.ROLLBACK_MATCHED, s.index);  //5
		ApdEvent session = add(EventTypes.SESSION_START, new byte[0]);      //6
		ApdEvent after = game();        //7

		RunTimeline t = RunTimeline.of(events);
		assertTrue(t.isAbandoned(a) && t.isAbandoned(b));
		assertFalse(t.isAbandoned(gap));
		assertFalse(t.isAbandoned(rb));
		assertFalse(t.isAbandoned(session));
		assertFalse(t.isAbandoned(after));
		assertFalse("the save the game returned to is still real", t.isAbandoned(s));
		assertEquals(2, t.abandonedCount());
		assertEquals(events.size() - 2, t.canonical().size());
		assertEquals(2, t.abandoned().size());
		assertEquals(1, t.rollbackCount());
	}

	@Test
	public void saveMarkersInsideTheAbandonedBranchAreAbandonedToo(){
		ApdEvent s1 = save();           //0
		game();                         //1
		save();                         //2  a later save that no longer exists
		game();                         //3
		rollback(LifecycleEvents.ROLLBACK_MATCHED, s1.index);  //4
		RunTimeline t = RunTimeline.of(events);
		assertEquals(3, t.abandonedCount());
		assertTrue(t.isAbandoned(events.get(2)));
	}

	@Test
	public void secondRollbackToTheSameSaveAbandonsThePlayAfterTheFirstOne(){
		ApdEvent s = save();                                                        //0
		game(); game();                                                             //1,2 abandoned by rollback 1
		ApdEvent rb1 = rollback(LifecycleEvents.ROLLBACK_MATCHED, s.index);         //3
		ApdEvent session1 = add(EventTypes.SESSION_START, new byte[0]);             //4
		ApdEvent branchB = game();                                                  //5 abandoned by rollback 2
		ApdEvent rb2 = rollback(LifecycleEvents.ROLLBACK_MATCHED, s.index);         //6
		ApdEvent survivor = game();                                                 //7

		RunTimeline t = RunTimeline.of(events);
		assertEquals(3, t.abandonedCount());
		assertTrue(t.isAbandoned(events.get(1)) && t.isAbandoned(events.get(2)) && t.isAbandoned(branchB));
		assertFalse(t.isAbandoned(rb1) || t.isAbandoned(rb2) || t.isAbandoned(session1) || t.isAbandoned(survivor));
		assertEquals(2, t.rollbackCount());
	}

	@Test
	public void rollbackToAnEarlierSaveAlsoAbandonsLaterSavesAndTheirRollbacksMeta(){
		ApdEvent s1 = save();                                                       //0
		game();                                                                     //1
		ApdEvent s2 = save();                                                       //2
		game();                                                                     //3
		rollback(LifecycleEvents.ROLLBACK_MATCHED, s2.index);                       //4 abandons 3
		game();                                                                     //5
		rollback(LifecycleEvents.ROLLBACK_MATCHED, s1.index);                       //6 abandons 1,2,3(already),5
		RunTimeline t = RunTimeline.of(events);
		for (int i : new int[]{1, 2, 3, 5}) assertTrue("event " + i, t.isAbandoned(events.get(i)));
		assertFalse(t.isAbandoned(events.get(0)));
		assertFalse(t.isAbandoned(events.get(4)));
		assertFalse(t.isAbandoned(events.get(6)));
	}

	@Test
	public void unmatchedRollbackAbandonsNothingButFlagsUncertainty(){
		save(); game();
		ApdEvent rb = rollback(LifecycleEvents.ROLLBACK_UNMATCHED, -1);
		game();
		RunTimeline t = RunTimeline.of(events);
		assertEquals(0, t.abandonedCount());
		assertEquals(rb.index, t.uncertainFromIndex());
	}

	@Test
	public void aDamagedRollbackEventAbandonsNothing(){
		save(); game();
		add(EventTypes.ROLLBACK, new byte[]{1}); //too short to decode
		RunTimeline t = RunTimeline.of(events);
		assertEquals(0, t.abandonedCount());
		assertEquals(1, t.rollbackCount());
	}

	@Test
	public void rollbackWithAMarkerThatIsNotInTheListAbandonsNothingBeforeIt(){
		game();
		rollback(LifecycleEvents.ROLLBACK_MATCHED, 99);
		RunTimeline t = RunTimeline.of(events);
		assertEquals(0, t.abandonedCount());
	}
}
