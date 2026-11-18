package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SelfTestTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void theDeviceSelfTestPassesOnThisMachineAndCleansUpAfterItself() throws Exception {
		SelfTest.Report r = SelfTest.run(tmp.getRoot());
		System.out.println("APD-SELFTEST\n" + r.text());
		assertTrue(r.text(), r.passed);
		assertTrue(r.lines.size() > 10);
		assertEquals("the self-test must remove its own files", 0, tmp.getRoot().list().length);
	}

	@Test
	public void aFolderThatCannotBeWrittenGivesAFailedReportNotAnException() throws Exception {
		java.io.File file = tmp.newFile("a-file");
		SelfTest.Report r = SelfTest.run(new java.io.File(file, "inside"));
		assertTrue(!r.passed);
	}
}
