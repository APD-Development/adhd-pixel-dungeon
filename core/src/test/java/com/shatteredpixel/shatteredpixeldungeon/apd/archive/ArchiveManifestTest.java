package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ArchiveManifestTest {

	private ArchiveManifest sample(){
		ArchiveManifest m = new ArchiveManifest();
		m.createdMillis = 1234;
		m.app = "4.0.8-apd";
		m.telemetry = "1.5";
		m.container = 1;
		ArchiveManifest.Entry e = new ArchiveManifest.Entry();
		e.path = "runs/run-1.apdlog";
		e.size = 99;
		e.sha256 = ArchiveManifest.sha256Hex(new byte[]{ 1 });
		e.runId = "run-1";
		e.telemetry = "1.5";
		e.sealed = true;
		e.events = 12;
		e.status = "SEALED";
		m.files.add(e);
		m.notes.add("hello");
		return m;
	}

	private static void rejects( byte[] data, String why ){
		try {
			ArchiveManifest.parse(data);
			fail("accepted: " + why);
		} catch (IllegalArgumentException expected){
		}
	}

	@Test
	public void aManifestRoundTrips(){
		ArchiveManifest m = ArchiveManifest.parse(sample().encode().getBytes(ArchiveManifest.utf8()));
		assertEquals(1234, m.createdMillis);
		assertEquals("4.0.8-apd", m.app);
		assertEquals(1, m.files.size());
		ArchiveManifest.Entry e = m.files.get(0);
		assertEquals("runs/run-1.apdlog", e.path);
		assertEquals(99, e.size);
		assertTrue(e.sealed);
		assertFalse(e.redacted);
		assertEquals("run-1", e.logId());
		assertEquals(99, m.totalBytes());
	}

	@Test
	public void anyChangeToTheManifestIsCaught(){
		String text = sample().encode();
		rejects(text.replace("99", "98").getBytes(ArchiveManifest.utf8()), "a size changed");
		rejects(text.replace("run-1", "run-2").getBytes(ArchiveManifest.utf8()), "a run id changed");
		rejects(text.substring(0, text.indexOf("check")).getBytes(ArchiveManifest.utf8()), "no checksum");
		rejects("garbage".getBytes(ArchiveManifest.utf8()), "garbage");
		rejects(new byte[0], "empty");
	}

	@Test
	public void aNewerFormatIsRefusedAndUnknownLinesAreIgnored(){
		ArchiveManifest m = sample();
		m.format = 99;
		rejects(m.encode().getBytes(ArchiveManifest.utf8()), "a newer format");
		//a line from a newer writer (with a valid checksum) is skipped
		String body = sample().encode();
		String withoutCheck = body.substring(0, body.indexOf("check\t"));
		String extra = withoutCheck + "futureline\tvalue\n";
		String full = extra + "check\t" + ArchiveManifest.sha256Hex(extra.getBytes(ArchiveManifest.utf8())) + "\n";
		assertEquals(1, ArchiveManifest.parse(full.getBytes(ArchiveManifest.utf8())).files.size());
	}

	@Test
	public void theTotalsMustMatchTheFileList(){
		String body = sample().encode();
		String withoutCheck = body.substring(0, body.indexOf("check\t")).replace("count\t1\t99", "count\t2\t99");
		String full = withoutCheck + "check\t" + ArchiveManifest.sha256Hex(withoutCheck.getBytes(ArchiveManifest.utf8())) + "\n";
		rejects(full.getBytes(ArchiveManifest.utf8()), "wrong totals");
	}

	@Test
	public void checksumValidFileSizeOverflowIsRejectedButExactLongLimitParses(){
		ArchiveManifest m = sample();
		m.files.get(0).size = Long.MAX_VALUE;
		ArchiveManifest.Entry second = new ArchiveManifest.Entry();
		second.path = "legacy/second.apdlog";
		second.kind = ArchiveManifest.KIND_IMPORTED;
		second.size = 1;
		m.files.add(second);
		//The encoded checksum and count are both valid, but signed addition wraps to a negative total.
		rejects(m.encode().getBytes(ArchiveManifest.utf8()), "overflowed signed manifest total");
		m.files.get(0).size = Long.MAX_VALUE - 1;
		ArchiveManifest boundary = ArchiveManifest.parse(m.encode().getBytes(ArchiveManifest.utf8()));
		assertEquals(Long.MAX_VALUE, boundary.totalBytes());
	}

	@Test
	public void onlyPlainRunAndImportedLogPathsAreEverAllowed(){
		assertTrue(ArchiveManifest.isValidPath("runs/run-1.apdlog"));
		assertTrue(ArchiveManifest.isValidPath("legacy/import-1.apdlog"));
		for (String bad : new String[]{ "../runs/a.apdlog", "runs/../a.apdlog", "runs/../../etc/passwd", "/runs/a.apdlog", "runs/a/b.apdlog",
				"runs/a.txt", "runs/.apdlog", "runs/a b.apdlog", "other/a.apdlog", "MANIFEST.txt", "runs\\a.apdlog", "runs/a.apdlog/", "",
				"derived/rows/a.apdr", "index.apdi", "runs/%2e%2e.apdlog", "runs/a\u0000.apdlog" }){
			assertFalse(bad, ArchiveManifest.isValidPath(bad));
		}
		assertFalse(ArchiveManifest.isValidPath(null));
	}
}
