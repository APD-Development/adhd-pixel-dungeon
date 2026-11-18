package com.watabou.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.Input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KeyEventBackPriorityTest {

	@Test
	public void noQueuedEventsOrOnlyOrdinaryEventsDoNotNeedEarlyDispatch(){
		assertFalse(KeyEvent.containsBackPress(Collections.emptyList()));
		assertFalse(KeyEvent.containsBackPress(Arrays.asList(
				new KeyEvent(Input.Keys.ENTER, true),
				new KeyEvent(Input.Keys.BACK, false))));
	}

	@Test
	public void queuedBackDownIsRecognizedWithOrWithoutRelease(){
		assertTrue(KeyEvent.containsBackPress(Collections.singletonList(
				new KeyEvent(Input.Keys.BACK, true))));
		assertTrue(KeyEvent.containsBackPress(Arrays.asList(
				new KeyEvent(Input.Keys.A, true),
				new KeyEvent(Input.Keys.BACK, true),
				new KeyEvent(Input.Keys.BACK, false))));
	}

	@Test
	public void PriorityCheckDoesNotConsumeReorderOrDuplicateInputs(){
		KeyEvent first = new KeyEvent(Input.Keys.A, true);
		KeyEvent back = new KeyEvent(Input.Keys.BACK, true, 31);
		KeyEvent release = new KeyEvent(Input.Keys.BACK, false, 31);
		List<KeyEvent> pending = new ArrayList<>(Arrays.asList(first, back, release));
		assertTrue(KeyEvent.containsBackPress(pending));
		assertTrue(KeyEvent.containsBackPress(pending));
		assertEquals(Arrays.asList(first, back, release), pending);
		assertEquals(31, pending.get(1).backTraceId);
	}
}
