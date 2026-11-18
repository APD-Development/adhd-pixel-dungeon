package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ByteSink;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

//Recorder recovery must isolate damaged lifecycle payloads just as the statistics readers do.
public class RunRecorderMalformedLifecycleTest {

	@Test
	public void malformedLifecyclePayloadCannotHideLaterRulesetState() throws Exception {
		ReadResult log = new ReadResult();
		log.strings.add("old-rules");
		log.strings.add("new-rules");
		log.strings.add("new-rules-text");
		log.events.add(new ApdEvent(0, EventTypes.RUN_START, 0, 0, new byte[0]));
		byte[] change = new ByteSink().writeUnsigned(0).writeUnsigned(1).writeUnsigned(2).writeUnsigned(4).toByteArray();
		log.events.add(new ApdEvent(1, EventTypes.RULESET_CHANGE, 0, 1, change));

		Constructor<RunRecorder> constructor = RunRecorder.class.getDeclaredConstructor(
				StatsStore.class, int.class, String.class, String.class, int.class);
		constructor.setAccessible(true);
		RunRecorder recorder = constructor.newInstance(null, 0, "segment", "logical", 0);
		Method restore = RunRecorder.class.getDeclaredMethod("restoreTrackedState", ReadResult.class, RunTimeline.class);
		restore.setAccessible(true);
		restore.invoke(recorder, log, RunTimeline.of(log.events));

		assertEquals("new-rules", field(recorder, "rulesetId"));
		assertEquals(1, field(recorder, "rulesetChanges"));
	}

	private static Object field( RunRecorder recorder, String name ) throws Exception {
		Field field = RunRecorder.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(recorder);
	}
}
