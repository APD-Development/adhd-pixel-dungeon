package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.watabou.input.BackTrace;

import org.junit.After;
import org.junit.Test;

import java.util.List;

public class BackTraceTest {

	@After
	public void reset(){
		BackTrace.resetForTest();
	}

	@Test
	public void disabledTracingIsACompleteNoOp(){
		BackTrace.resetForTest();
		long route = BackTrace.route("android-system");
		BackTrace.accepted(route);
		BackTrace.stage(route, "window-hide-end", "example.Window");
		assertEquals(0, route);
		assertTrue(BackTrace.snapshot().isEmpty());
	}

	@Test
	public void acceptedBackKeepsOneCorrelationAcrossThePipeline(){
		BackTrace.setEnabled(true);
		long route = BackTrace.route("android-system");
		BackTrace.accepted(route);
		BackTrace.stage(route, "key-down-enqueued", "");
		BackTrace.beginDispatch(route);
		BackTrace.stage(route, "window-hide-begin", "example.Window");
		BackTrace.stage(route, "window-hide-end", "example.Window");
		BackTrace.endDispatch(route);

		String lines = String.join("\n", BackTrace.snapshot());
		assertTrue(lines.contains("route=" + route + " stage=route"));
		assertTrue(lines.contains("route=" + route + " stage=accepted"));
		assertTrue(lines.contains("route=" + route + " stage=render-dispatch-begin"));
		assertTrue(lines.contains("route=" + route + " stage=window-hide-end"));
		assertTrue(lines.contains("route=" + route + " stage=render-dispatch-end"));
		assertEquals(0, BackTrace.currentDispatch());
	}

	@Test
	public void rejectedDuplicateDoesNotReplaceTheAcceptedRoute(){
		BackTrace.setEnabled(true);
		long accepted = BackTrace.route("android-system");
		BackTrace.accepted(accepted);
		long duplicate = BackTrace.route("gdx-key");
		BackTrace.rejected(duplicate, "BackGate duplicate");
		BackTrace.stage(accepted, "window-hide-end", "example.Window");

		List<String> lines = BackTrace.snapshot();
		String all = String.join("\n", lines);
		assertTrue(all.contains("route=" + duplicate + " stage=rejected"));
		assertTrue(all.contains("route=" + accepted + " stage=window-hide-end"));
	}
}
