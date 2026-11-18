package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlchemyRecordingTest {

	private static class NamedItem extends Item {
		private final String shownName;

		NamedItem( String shownName, int quantity ){
			this.shownName = shownName;
			quantity(quantity);
		}

		@Override
		public String name(){
			return shownName;
		}
	}

	private static final class HiddenIngredientClass extends NamedItem {
		HiddenIngredientClass( String shownName, int quantity ){
			super(shownName, quantity);
		}
	}

	private static final class HiddenResultClass extends NamedItem {
		HiddenResultClass( String shownName, int quantity ){
			super(shownName, quantity);
		}
	}

	@Before
	@After
	public void cleanUp(){
		ApdRecorder.reset();
		while (ApdPlayRestoring.isRestoring()) ApdPlay.restoreEnd();
	}

	private static List<FactEvents.Observation> crafts( ReadResult result ){
		List<FactEvents.Observation> out = new ArrayList<>();
		for (ApdEvent event : result.events){
			if (!FactEvents.isFactEvent(event.type)) continue;
			for (FactEvents.Observation observation : FactEvents.decode(event, result)){
				if (observation.key.equals("alchemy.craft")) out.add(observation);
			}
		}
		return out;
	}

	@Test
	public void successfulCraftRecordsDisplayedDetailsAndEnergySplit() throws Exception {
		RecorderTestSupport.Session session = ItemTestSupport.startRun(201L);
		String runId = ApdRecorder.currentRunId();
		Dungeon.depth = 4;

		Item first = new HiddenIngredientClass("unidentified ingredient", 2);
		Item second = new NamedItem("known seed", 1);
		Item result = new HiddenResultClass("mysterious brew", 3);
		ApdPlay.onAlchemyCrafted(Arrays.asList(first, second), result, 7, 5, 2);

		ApdRecorder.flushForReading();
		List<FactEvents.Observation> got = crafts(session.read(runId));
		assertEquals(1, got.size());
		FactEvents.Observation craft = got.get(0);
		assertEquals("mysterious brew", craft.subject);
		assertEquals("", craft.actor);
		assertArrayEquals(new long[]{ 4, 3, 7, 5, 2 }, craft.values);
		assertArrayEquals(new String[]{ "2x unidentified ingredient + 1x known seed" }, craft.tags);
		String publicText = craft.subject + Arrays.toString(craft.tags);
		assertFalse(publicText.contains("HiddenIngredientClass"));
		assertFalse(publicText.contains("HiddenResultClass"));
	}

	@Test
	public void failedAndRestoreTimeCraftsRecordNothing() throws Exception {
		RecorderTestSupport.Session session = ItemTestSupport.startRun(202L);
		String runId = ApdRecorder.currentRunId();
		Item ingredient = new HiddenIngredientClass("unidentified ingredient", 1);

		ApdPlay.onAlchemyCrafted(Arrays.asList(ingredient), null, 4, 4, 0);
		ApdPlay.restoreBegin();
		try {
			ApdPlay.onAlchemyCrafted(Arrays.asList(ingredient), new HiddenResultClass("restored result", 1), 4, 4, 0);
		} finally {
			ApdPlay.restoreEnd();
		}

		ApdRecorder.flushForReading();
		assertEquals(0, crafts(session.read(runId)).size());
	}

	private static final class ApdPlayRestoring {
		static boolean isRestoring(){
			try {
				java.lang.reflect.Field field = ApdPlay.class.getDeclaredField("restoring");
				field.setAccessible(true);
				return field.getInt(null) > 0;
			} catch (Throwable ignored){
				return false;
			}
		}
	}
}
