package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadStatus;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

//A run that is not finished is archived only as a redacted copy: nothing hidden may be in it, and what the player could see must be.
public class LogRedactorTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final TelemetryVersion V = TelemetryVersion.CURRENT;

	//the same run played twice: only the HIDDEN facts differ (the seed, the true class, the room, a hidden mob)
	private byte[] run( String name, long seed, String customSeed, String secretClass, String room, boolean ended, boolean extraHidden ) throws Exception {
		File root = tmp.newFolder(name);
		TestLogs l = new TestLogs(root, "run-x", V, 1000).seeds(seed, customSeed).start("run-x", 0, "MAGE", "r1-aaaaaaaaaaaa", 0, 30, false).enter(1);
		l.registered("o-ring", secretClass, 3, true, ItemEvents.ORIGIN_GENERATED).generated("o-ring", GenerationEvents.C_CHEST, room);
		l.seen("o-ring", "ruby ring", ItemEvents.CAT_RING).acquired("o-ring", "ruby ring", ItemEvents.CAT_RING, false, 0);
		l.mobGenerated("actors.mobs.Rat");
		if (extraHidden){
			l.mobGenerated("actors.mobs.Goo");
			l.registered("o-secret", "items.armor.PlateArmor", 9, false, ItemEvents.ORIGIN_GENERATED);
			l.hiddenAt(500);
		}
		l.tick(40).enter(2).heroAttacks("rat", "actors.mobs.Rat", true).damageFromHero("rat", "actors.mobs.Rat", 4);
		if (ended) l.end(LifecycleEvents.OUTCOME_DIED, "rat", 2, 3, 50);
		l.close(ended);
		return Files.readAllBytes(l.file.toPath());
	}

	private static boolean contains( byte[] data, String text ){
		byte[] t = text.getBytes(java.nio.charset.Charset.forName("UTF-8"));
		outer:
		for (int i = 0; i + t.length <= data.length; i++){
			for (int j = 0; j < t.length; j++) if (data[i + j] != t[j]) continue outer;
			return true;
		}
		return false;
	}

	@Test
	public void nothingHiddenIsInTheRedactedCopyAndTheCopyIsExactlyWhatThePlayerCouldKnow() throws Exception {
		byte[] raw = run("a", 111222333L, "SECRET-SEED-TEXT", "items.rings.RingOfHaste", "Vault of Haste", false, false);
		assertTrue(contains(raw, "items.rings.RingOfHaste"));   //the raw log holds it
		LogRedactor.Result r = LogRedactor.redact(raw);
		assertNotNull(r);
		for (String hidden : new String[]{ "items.rings.RingOfHaste", "Vault of Haste", "SECRET-SEED-TEXT", "actors.mobs.Rat" }){
			assertFalse(hidden + " is in the redacted copy", contains(r.bytes, hidden));
		}
		ReadResult red = RunLogReader.parse(r.bytes);
		assertEquals(ReadStatus.UNSEALED, red.status);
		assertTrue(LogRedactor.isRedacted(red.header.writerBuild));
		assertEquals("run-x", red.header.runId);
		assertEquals(1000, red.header.createdAtMillis);
		for (ApdEvent e : red.events) assertEquals(EventTypes.Visibility.PUBLIC, EventTypes.visibility(e.type));
		assertTrue(r.eventsLeftOut > 0);
	}

	@Test
	public void theRedactedCopyDoesNotDependOnAnyHiddenInformation() throws Exception {
		//two runs that look identical to the player but differ in everything hidden give byte-identical redacted copies
		byte[] a = run("a", 111L, "alpha", "items.rings.RingOfHaste", "Room A", false, false);
		byte[] b = run("b", 999999999999L, "beta", "items.rings.RingOfElements", "Treasury B", false, true);
		assertTrue("the raw logs differ", !java.util.Arrays.equals(a, b));
		assertArrayEquals(LogRedactor.redact(a).bytes, LogRedactor.redact(b).bytes);
	}

	@Test
	public void theClockOfEveryKeptEventIsExactlyTheOriginals() throws Exception {
		byte[] raw = run("c", 5, "", "items.rings.RingOfHaste", "Room", false, true);
		ReadResult orig = RunLogReader.parse(raw);
		ReadResult red = RunLogReader.parse(LogRedactor.redact(raw).bytes);
		List<ApdEvent> publicOnes = new ArrayList<>();
		for (ApdEvent e : orig.events) if (EventTypes.visibility(e.type) == EventTypes.Visibility.PUBLIC) publicOnes.add(e);
		assertEquals(publicOnes.size(), red.events.size());
		for (int i = 0; i < publicOnes.size(); i++){
			assertEquals(publicOnes.get(i).type, red.events.get(i).type);
			assertEquals("clock of event " + i, publicOnes.get(i).clock, red.events.get(i).clock);
			assertEquals(publicOnes.get(i).flags, red.events.get(i).flags);
			if (publicOnes.get(i).type == EventTypes.ITEM_SEEN){
				//the string ids are renumbered, what they say is not
				ItemEvents.Seen a = ItemEvents.decodeSeen(publicOnes.get(i), orig), b = ItemEvents.decodeSeen(red.events.get(i), red);
				assertEquals(a.uid, b.uid);
				assertEquals(a.state.name, b.state.name);
				assertEquals(a.where.depth, b.where.depth);
			}
		}
	}

	@Test
	public void theStringTableHoldsExactlyTheStringsTheKeptEventsUse() throws Exception {
		byte[] raw = run("h", 1, "", "items.rings.RingOfHaste", "Room", false, true);
		ReadResult orig = RunLogReader.parse(raw);
		ReadResult red = RunLogReader.parse(LogRedactor.redact(raw).bytes);
		assertTrue(red.strings.size() < orig.strings.size());
		for (String s : red.strings) assertNotNull(s);
		//no string is the empty placeholder of an unused one, except the empty custom seed of the run start
		int empty = 0;
		for (String s : red.strings) if (s.isEmpty()) empty++;
		assertTrue(empty <= 1);
	}

	@Test
	public void theRunStartLosesOnlyItsSeed() throws Exception {
		byte[] raw = run("d", 123456789L, "my custom seed", "items.rings.RingOfHaste", "Room", false, false);
		ReadResult orig = RunLogReader.parse(raw);
		ReadResult red = RunLogReader.parse(LogRedactor.redact(raw).bytes);
		LifecycleEvents.RunStart a = null, b = null;
		for (ApdEvent e : orig.events) if (e.type == EventTypes.RUN_START) a = LifecycleEvents.decodeRunStart(e, orig);
		for (ApdEvent e : red.events) if (e.type == EventTypes.RUN_START) b = LifecycleEvents.decodeRunStart(e, red);
		assertEquals(123456789L, a.seed);
		assertEquals("my custom seed", a.customSeedText);
		assertEquals(0, b.seed);
		assertEquals("", b.customSeedText);
		assertEquals(a.runId, b.runId);
		assertEquals(a.segment, b.segment);
		assertEquals(a.heroClass, b.heroClass);
		assertEquals(a.rulesetId, b.rulesetId);
		assertEquals(a.rulesetText, b.rulesetText);
		assertEquals(a.challenges, b.challenges);
		assertEquals(a.exoticChance, b.exoticChance);
		assertEquals(a.seedType, b.seedType);
		assertEquals(a.apdVersionName, b.apdVersionName);
		assertEquals(a.platform, b.platform);
		assertEquals(a.realTimeMillis, b.realTimeMillis);
	}

	@Test
	public void theRunStartLayoutThisRedactorReadsIsTheOneThatIsWritten() throws Exception {
		//re-encoding without redacting must give back the original bytes, so the day emitRunStart changes this test fails
		byte[] raw = run("e", 42L, "x", "items.rings.RingOfHaste", "Room", false, false);
		ReadResult orig = RunLogReader.parse(raw);
		for (ApdEvent e : orig.events){
			if (e.type == EventTypes.RUN_START) assertArrayEquals(e.payload, LogRedactor.runStartPayload(e.payload, false, 0));
		}
	}

	@Test
	public void aSealedLogStaysSealedWithTheRightCount() throws Exception {
		byte[] raw = run("f", 1, "", "items.rings.RingOfHaste", "Room", true, true);
		ReadResult red = RunLogReader.parse(LogRedactor.redact(raw).bytes);
		assertEquals(ReadStatus.SEALED, red.status);
		assertEquals(red.eventCount, red.sealEventCount);
		boolean ended = false;
		for (ApdEvent e : red.events) if (e.type == EventTypes.RUN_END) ended = true;
		assertTrue("the run's end is public and stays", ended);
	}

	@Test
	public void everyPublicEventTypeIsAuditedAndEveryInternalOrUnknownOneIsLeftOut() throws Exception {
		for (java.util.Map.Entry<Integer, String> t : EventTypes.all().entrySet()){
			boolean isPublic = EventTypes.visibility(t.getKey()) == EventTypes.Visibility.PUBLIC;
			assertEquals(t.getValue() + " must be audited exactly when it is PUBLIC (audit its fields, then list it in LogRedactor)",
					isPublic, LogRedactor.isAudited(t.getKey()));
		}
		//a type nobody has heard of is INTERNAL and so left out
		assertFalse(LogRedactor.isAudited(60000));
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(60000));
	}

	@Test
	public void aDamagedLogGivesItsCleanPrefixAndAnUnreadableFileGivesNull() throws Exception {
		byte[] raw = run("g", 1, "", "items.rings.RingOfHaste", "Room", false, false);
		byte[] cut = java.util.Arrays.copyOf(raw, raw.length - 7);
		LogRedactor.Result r = LogRedactor.redact(cut);
		assertNotNull(r);
		assertTrue(RunLogReader.parse(r.bytes).events.size() > 0);
		assertEquals(null, LogRedactor.redact(new byte[]{ 1, 2, 3, 4, 5 }));
		assertEquals(null, LogRedactor.redact(new byte[0]));
	}
}
