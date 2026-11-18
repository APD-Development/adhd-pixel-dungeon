package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.LogRows;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.RowBuilders;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

public class DamagedFactsProvenanceTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private int sequence;

	private byte[] itemLog( boolean sealed ) throws Exception {
		String id = "damaged-facts-" + sequence++;
		File file = new File(tmp.getRoot(), id + ".apdlog");
		RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT, id, "test", 1);

		LifecycleEvents.RunStart start = new LifecycleEvents.RunStart();
		start.runId = id;
		start.heroClass = "WARRIOR";
		start.seed = 77;
		start.challenges = 0;
		start.exoticChance = 0;
		start.apdVersionName = "4.0.8-apd";
		assertTrue(LifecycleEvents.emitRunStart(writer, 0, false, start));

		ItemEvents.Registered registered = new ItemEvents.Registered();
		registered.uid = "u1";
		assertTrue(ItemEvents.emitRegistered(writer, 1, 0, registered));

		ItemEvents.Acquired acquired = new ItemEvents.Acquired();
		acquired.uid = "u1";
		assertTrue(ItemEvents.emitAcquired(writer, 2, 0, acquired));

		assertTrue(writer.close(sealed));
		return LogTestSupport.bytes(file);
	}

	private void assertCountProvenance( ReadResult read, int expected ) {
		RunFacts facts = RunFactsBuilder.fromLog(read, "facts");
		assertEquals(1, facts.numOr(FactKeys.ITEMS_ACQUIRED, -1));
		assertEquals(expected, facts.get(FactKeys.ITEMS_ACQUIRED).prov);

		LogRows rows = RowBuilders.fromLog(read, "rows");
		assertEquals(1, rows.of(Datasets.ITEMS).size());
		Row item = rows.of(Datasets.ITEMS).get(0);
		DatasetDef items = RowBuilders.def(Datasets.ITEMS);
		int acquired = items.indexOf("item.acquired");
		assertEquals(1, item.num(acquired));
		assertEquals(expected, item.prov(acquired));
	}

	@Test
	public void cleanUnsealedCountsRemainExact() throws Exception {
		ReadResult read = RunLogReader.parse(itemLog(false));
		assertEquals(ReadStatus.UNSEALED, read.status);
		assertCountProvenance(read, Fact.PROV_RECORDED);
	}

	@Test
	public void truncatedTailMakesSurvivingCountsLowerBounds() throws Exception {
		byte[] frame = LogFormat.eventFrame(LogTestSupport.T_PLAIN, 0, 3, new byte[]{7});
		byte[] partial = Arrays.copyOf(frame, frame.length - 1);
		ReadResult read = RunLogReader.parse(LogTestSupport.concat(itemLog(false), partial));
		assertEquals(ReadStatus.TRUNCATED, read.status);
		assertCountProvenance(read, Fact.PROV_INCOMPLETE);
	}

	@Test
	public void corruptTailMakesSurvivingCountsLowerBounds() throws Exception {
		byte[] junk = new byte[37];
		new Random(1).nextBytes(junk);
		junk[0] = 0;
		ReadResult read = RunLogReader.parse(LogTestSupport.concat(itemLog(false), junk));
		assertEquals(ReadStatus.CORRUPT, read.status);
		assertCountProvenance(read, Fact.PROV_INCOMPLETE);
	}

	@Test
	public void sealMismatchMakesSurvivingCountsLowerBounds() throws Exception {
		ReadResult read = RunLogReader.parse(LogTestSupport.concat(itemLog(false), LogFormat.sealFrame(99, 3)));
		assertEquals(ReadStatus.SEAL_MISMATCH, read.status);
		assertCountProvenance(read, Fact.PROV_INCOMPLETE);
	}

	@Test
	public void trailingGarbageAfterValidSealKeepsCountsExact() throws Exception {
		ReadResult read = RunLogReader.parse(LogTestSupport.concat(itemLog(true), new byte[]{1, 2, 3}));
		assertEquals(ReadStatus.SEALED, read.status);
		assertEquals(3, read.damagedBytes());
		assertCountProvenance(read, Fact.PROV_RECORDED);
	}

	@Test
	public void changedDerivationsInvalidateDisposableCaches() {
		assertEquals(18, RowBuilders.BUILDER_VERSION);
		assertEquals(11, DerivedStore.DERIVATION_VERSION);
	}

	@Test
	public void resynchronizedLaterItemsKeepTheMissingEventWarning() throws Exception {
		byte[] full = itemLog(false);
		ReadResult clean = RunLogReader.parse(full);
		ApdEvent acquired = clean.events.get(clean.events.size() - 1);
		byte[] later = LogFormat.eventFrame(acquired.type, acquired.flags, 1, acquired.payload);
		byte[] damaged = Arrays.copyOf(full, full.length + later.length);
		System.arraycopy(later, 0, damaged, full.length, later.length);
		damaged[full.length - 1] ^= 0x7f;
		ReadResult recovered = RunLogReader.parse(damaged, new ReadOptions().resync(true));
		assertEquals(ReadStatus.CORRUPT, recovered.status);
		assertEquals(1, recovered.resyncs);
		assertTrue(recovered.skippedBytes > 0);
		assertEquals(clean.events.size(), recovered.events.size());
		assertCountProvenance(recovered, Fact.PROV_INCOMPLETE);
	}
}


