package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.SelfTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

//Runs the statistics storage self-test on a real Android runtime and file system (an emulator in CI, a phone if run by hand).
@RunWith(AndroidJUnit4.class)
public class ApdStatsDeviceTest {

	@Test
	public void storageSelfTestPassesOnAndroid() {
		Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
		File work = new File(ctx.getFilesDir(), "apd-selftest-work");
		assertTrue(work.isDirectory() || work.mkdirs());
		SelfTest.Report r = SelfTest.run(work, 20000);
		com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdSelfTest.run(work, r);
		com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSelfTest.run(work, r);
		for (String line : r.text().split("\n")) Log.i("APD-SELFTEST", line);
		assertTrue(r.text(), r.passed);
		work.delete();
	}

	//The INDEV demo data on a real Android runtime: it generates, loads into UIMSD's datasets, stays apart from real history and
	//can be removed completely.
	@Test
	public void demoDataRoundTripsOnAndroid() throws Exception {
		Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
		File parent = new File(ctx.getFilesDir(), "apd-demo-test");
		File real = new File(parent, "apd");
		File demo = com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.demoRootFor(real);
		com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.delete(demo);
		assertTrue(real.isDirectory() || real.mkdirs());
		File realMarker = new File(real, "keep.txt");
		assertTrue(realMarker.exists() || realMarker.createNewFile());
		int runs = com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator.generate(demo,
				com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset.of(30, 0), System.currentTimeMillis());
		com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog cat = new com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore(demo).load();
		assertTrue(cat.dataset(com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets.RUNS).rows.size() >= runs - 2);
		assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.delete(demo));
		assertTrue(!demo.exists());
		assertTrue(realMarker.exists());
		realMarker.delete();
		real.delete();
		parent.delete();
	}
}
