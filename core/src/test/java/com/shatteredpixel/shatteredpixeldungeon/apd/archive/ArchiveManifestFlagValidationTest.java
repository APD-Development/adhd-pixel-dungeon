package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

// A valid manifest checksum must not legitimize undefined sealed/redacted tokens.
public class ArchiveManifestFlagValidationTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static ArchiveManifest sample( byte[] log ){
		ArchiveManifest m = new ArchiveManifest();
		m.createdMillis = 1234;
		m.app = "APD";
		m.telemetry = "1.5";
		m.container = 1;
		ArchiveManifest.Entry e = new ArchiveManifest.Entry();
		e.path = "runs/a.apdlog";
		e.size = log.length;
		e.sha256 = ArchiveManifest.sha256Hex(log);
		e.kind = ArchiveManifest.KIND_RUN;
		e.runId = "run-a";
		e.sealed = true;
		e.redacted = true;
		e.status = "SEALED";
		m.files.add(e);
		return m;
	}

	// Recalculate the real SHA-256, so only field validation can refuse the input.
	private static byte[] withFlags( ArchiveManifest manifest, String sealed, String redacted ){
		StringBuilder body = new StringBuilder();
		for (String line : manifest.encode().split("\n")){
			if (line.startsWith("check\t")) break;
			if (line.startsWith("file\t")){
				String[] p = line.split("\t", -1);
				p[7] = sealed;
				p[8] = redacted;
				line = String.join("\t", p);
			}
			body.append(line).append('\n');
		}
		String text = body + "check\t"
				+ ArchiveManifest.sha256Hex(body.toString().getBytes(ArchiveManifest.utf8())) + "\n";
		return text.getBytes(ArchiveManifest.utf8());
	}

	private static void rejects( byte[] bytes, String message ){
		try {
			ArchiveManifest.parse(bytes);
			fail("invalid archive flag accepted");
		} catch (IllegalArgumentException expected){
			assertTrue(expected.getMessage(), expected.getMessage().contains(message));
		}
	}

	@Test
	public void invalidSealedTokensAreRefusedWithCorrectChecksums(){
		ArchiveManifest m = sample(new byte[]{1});
		for (String invalid : new String[]{"", "yes", "2", "-1", "true", " 1"}){
			rejects(withFlags(m, invalid, "1"), "invalid sealed flag");
		}
	}

	@Test
	public void invalidRedactedTokensAreRefusedWithCorrectChecksums(){
		ArchiveManifest m = sample(new byte[]{1});
		for (String invalid : new String[]{"", "no", "2", "-1", "false", "1 "}){
			rejects(withFlags(m, "1", invalid), "invalid redacted flag");
		}
	}

	@Test
	public void allDefinedBooleanFlagPairsStillRoundTrip(){
		ArchiveManifest m = sample(new byte[]{1});
		for (String sealed : new String[]{"0", "1"}){
			for (String redacted : new String[]{"0", "1"}){
				ArchiveManifest decoded = ArchiveManifest.parse(withFlags(m, sealed, redacted));
				assertEquals(1, decoded.files.size());
				assertEquals(sealed.equals("1"), decoded.files.get(0).sealed);
				assertEquals(redacted.equals("1"), decoded.files.get(0).redacted);
				assertEquals(m.files.get(0).sha256, decoded.files.get(0).sha256);
			}
		}
	}

	@Test
	public void invalidFlagStopsImportBeforeMutatingExistingHistory() throws Exception {
		File root = tmp.newFolder("history");
		File runs = new File(root, "runs");
		assertTrue(runs.mkdir());
		File prior = new File(runs, "keep.apdlog");
		byte[] original = "original history untouched".getBytes(ArchiveManifest.utf8());
		Files.write(prior.toPath(), original);
		byte[] log = ArchiveSupport.smallLog(tmp.newFolder("scratch"), "a", "run-a");
		ArchiveManifest manifest = sample(log);

		for (byte[] invalid : new byte[][]{
				withFlags(manifest, "invalid", "1"), withFlags(manifest, "1", "invalid")
		}){
			byte[] zip = ArchiveSupport.zip(
					ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
					Arrays.asList(invalid, log));
			ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));
			assertFalse(report.summary(), report.ok);
			assertEquals(0, report.imported);
			assertArrayEquals(original, Files.readAllBytes(prior.toPath()));
			assertFalse(new File(runs, "a.apdlog").exists());
			assertFalse(new File(root, "import-tmp").exists());
		}
	}
}
