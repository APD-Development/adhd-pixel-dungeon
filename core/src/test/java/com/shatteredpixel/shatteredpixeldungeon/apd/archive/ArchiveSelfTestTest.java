package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.SelfTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

//The self-test that runs on the phone also passes here.
public class ArchiveSelfTestTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void theDeviceSelfTestPasses() throws Exception {
		SelfTest.Report r = new SelfTest.Report();
		ArchiveSelfTest.run(tmp.newFolder("work"), r);
		assertTrue(r.text(), r.passed);
		assertTrue(r.text().contains("history archive"));
		assertTrue("it cleans up after itself", tmp.getRoot().listFiles()[0].listFiles().length == 0);
	}
}
