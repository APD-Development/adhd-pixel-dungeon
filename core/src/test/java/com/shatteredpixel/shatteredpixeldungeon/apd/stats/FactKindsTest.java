package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

//The fact-kind registry: the contract between a mechanic and UIMSD. Keys are permanent, so a kind can never be reused for something else.
public class FactKindsTest {

	@Test
	public void fingerprintRemainsCompatibleWithExistingCacheIdentities(){
		assertEquals(uncachedFingerprint(), FactKinds.fingerprint());
		FactKind kind = FactKind.builder("test.memo.compatibility", "Compatibility")
				.internal().value("amount", "Amount", "", FactKind.THOUSANDTHS).tag("where", "Where").build();
		FactKinds.register(kind);
		String fingerprint = FactKinds.fingerprint();
		assertEquals(uncachedFingerprint(), fingerprint);
		assertSame("unchanged declarations reuse their computed identity", fingerprint, FactKinds.fingerprint());
		FactKinds.register(kind);
		assertSame(fingerprint, FactKinds.fingerprint());
		try {
			FactKinds.register(FactKind.builder(kind.key, "Replacement").build());
			fail("duplicate key accepted");
		} catch (IllegalStateException expected) {
			assertSame(fingerprint, FactKinds.fingerprint());
		}
	}

	@Test(timeout = 10000)
	public void completedRegistrationCannotLeaveConcurrentReadersWithAStaleIdentity() throws Exception {
		java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
		java.util.concurrent.CyclicBarrier phase = new java.util.concurrent.CyclicBarrier(2);
		try {
			java.util.concurrent.Future<?> reader = workers.submit(() -> {
				try {
					for (int i = 0; i < 20; i++) {
						FactKinds.fingerprint();
						phase.await(2, java.util.concurrent.TimeUnit.SECONDS);
						phase.await(2, java.util.concurrent.TimeUnit.SECONDS);
						assertEquals(uncachedFingerprint(), FactKinds.fingerprint());
						phase.await(2, java.util.concurrent.TimeUnit.SECONDS);
					}
				} catch (Exception e) { throw new RuntimeException(e); }
			});
			for (int i = 0; i < 20; i++) {
				phase.await(2, java.util.concurrent.TimeUnit.SECONDS);
				FactKinds.register(FactKind.builder("test.memo.concurrent.n" + i, "Observation").value("n", "N").build());
				phase.await(2, java.util.concurrent.TimeUnit.SECONDS);
				phase.await(2, java.util.concurrent.TimeUnit.SECONDS);
			}
			reader.get(2, java.util.concurrent.TimeUnit.SECONDS);
		} finally {
			workers.shutdownNow();
			assertTrue(workers.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS));
		}
	}

	private static String uncachedFingerprint(){
		long hash = 1125899906842597L;
		for (FactKind kind : FactKinds.all()) {
			hash = mixIdentity(hash, kind.key);
			hash = mixIdentity(hash, kind.isInternal() ? "I" : "P");
			for (FactKind.Field field : kind.values()) hash = mixIdentity(hash, "v" + field.key + field.scale);
			for (FactKind.Field field : kind.tags()) hash = mixIdentity(hash, "t" + field.key);
		}
		return Long.toHexString(hash);
	}

	private static long mixIdentity(long hash, String value){
		for (int i = 0; i < value.length(); i++) hash = 31 * hash + value.charAt(i);
		return hash * 31 + 7;
	}

	@Test
	public void aKindCanBeRegisteredAndFound(){
		FactKind k = FactKind.builder("test.kindsA.step", "Step").area("Movement").subject("Terrain").value("x", "X").value("y", "Y").tag("note", "Note").series().build();
		assertSame(k, FactKinds.register(k));
		assertSame(k, FactKinds.get("test.kindsA.step"));
		assertTrue(FactKinds.all().contains(k));
		assertEquals(EventTypes.FACT_SERIES, k.eventType());
		assertEquals("Steps", k.plural);
	}

	@Test
	public void theSameKeyCannotBeRegisteredForTwoMeanings(){
		FactKinds.register(FactKind.builder("test.kindsB.cast", "Cast").build());
		try {
			FactKinds.register(FactKind.builder("test.kindsB.cast", "Something else").build());
			fail("a key was reused");
		} catch (IllegalStateException expected){
			//good
		}
	}

	@Test
	public void badKeysAreRefused(){
		for (String bad : new String[]{ "", "nodot", "Up.case", "a b.c", ".x", "a.", null }){
			try {
				FactKind.builder(bad, "X");
				fail("accepted " + bad);
			} catch (IllegalArgumentException expected){
				//good
			}
		}
	}

	@Test
	public void internalKindsUseTheInternalEventTypes(){
		FactKind a = FactKind.builder("test.kindsC.hidden", "Hidden").internal().build();
		FactKind b = FactKind.builder("test.kindsC.hiddenSeries", "Hidden series").internal().series().build();
		assertEquals(EventTypes.FACT_INTERNAL, a.eventType());
		assertEquals(EventTypes.FACT_SERIES_INTERNAL, b.eventType());
		assertTrue(a.isInternal());
	}

	@Test
	public void theFingerprintChangesWhenTheRegistryDoes(){
		String before = FactKinds.fingerprint();
		FactKinds.register(FactKind.builder("test.kindsD.fresh", "Fresh").value("n", "N").build());
		assertNotEquals(before, FactKinds.fingerprint());
	}

	//keys are never removed: the manifest lists every key a released build declared
	@Test
	public void everyKeyOfTheManifestIsStillRegisteredWithTheSameShape() throws Exception {
		Set<String> seen = new HashSet<>();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/apd/fact_kind_manifest.txt"), "UTF-8"))){
			String line;
			while ((line = in.readLine()) != null){
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) continue;
				//key|visibility|series|values|tags  (values and tags are comma-separated field keys, in order)
				String[] p = line.split("\\|", -1);
				FactKind k = FactKinds.get(p[0]);
				assertNotNull("fact kind " + p[0] + " was removed or renamed; keys are permanent", k);
				assertEquals(p[0] + " visibility changed", p[1], k.isInternal() ? "I" : "P");
				assertEquals(p[0] + " series changed", p[2], k.series ? "S" : "-");
				String[] vs = p[3].isEmpty() ? new String[0] : p[3].split(",");
				String[] ts = p[4].isEmpty() ? new String[0] : p[4].split(",");
				assertTrue(p[0] + " lost value fields", k.values().size() >= vs.length);
				assertTrue(p[0] + " lost tag fields", k.tags().size() >= ts.length);
				for (int i = 0; i < vs.length; i++) assertEquals(p[0] + " value " + i + " changed", vs[i], k.values().get(i).key);
				for (int i = 0; i < ts.length; i++) assertEquals(p[0] + " tag " + i + " changed", ts[i], k.tags().get(i).key);
				seen.add(p[0]);
			}
		}
		//every shipped kind (not a test kind) must be in the manifest, or it could be renamed unnoticed
		for (FactKind k : FactKinds.all()){
			if (k.key.startsWith("test.")) continue;
			assertTrue("new fact kind " + k.key + " must be added to fact_kind_manifest.txt", seen.contains(k.key));
		}
	}
}
