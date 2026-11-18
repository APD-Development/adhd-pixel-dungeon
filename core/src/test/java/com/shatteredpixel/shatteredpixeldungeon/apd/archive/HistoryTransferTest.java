package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.HistoryTransfer;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

//The player's buttons: export through the platform's file chooser, import through it, and what happens when they cancel or fail.
public class HistoryTransferTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@After
	public void reset(){
		ApdFiles.provider = null;
		ApdRecorder.reset();
	}

	private static String await( Runner r ) throws Exception {
		final String[] out = new String[1];
		final CountDownLatch done = new CountDownLatch(1);
		r.run(new HistoryTransfer.Listener(){
			@Override
			public void finished( String message ){
				out[0] = message;
				done.countDown();
			}
		});
		assertTrue("no answer", done.await(30, TimeUnit.SECONDS));
		return out[0];
	}

	private interface Runner {
		void run( HistoryTransfer.Listener l );
	}

	@Test
	public void exportAndImportThroughTheFolderFallbackMoveTheHistory() throws Exception {
		File base = tmp.newFolder("device1");
		File root = new File(base, "apd");
		history(root);
		ApdRecorder.configure(root, false);
		String exported = await(new Runner(){ public void run( HistoryTransfer.Listener l ){ HistoryTransfer.export(l); } });
		assertTrue(exported, exported.startsWith("Exported 5 run logs"));
		File[] zips = new File(base, "exports").listFiles();
		assertEquals(1, zips.length);
		assertTrue(zips[0].getName().matches("apd-history-\\d{8}-\\d{4}\\.zip"));

		//another install: the archive is dropped into its imports folder
		File base2 = tmp.newFolder("device2");
		File root2 = new File(base2, "apd");
		root2.mkdirs();
		new File(base2, "imports").mkdirs();
		java.nio.file.Files.copy(zips[0].toPath(), new File(base2, "imports/history.zip").toPath());
		ApdRecorder.configure(root2, false);
		String imported = await(new Runner(){ public void run( HistoryTransfer.Listener l ){ HistoryTransfer.importHistory(l); } });
		assertTrue(imported, imported.startsWith("Imported 6 logs"));
		assertEquals(6, StorageMeter.measure(root2).runLogs + StorageMeter.measure(root2).importedFiles);
	}

	@Test
	public void cancellingOrFailingChangesNothingAndSaysSo() throws Exception {
		File root = new File(tmp.newFolder("d"), "apd");
		history(root);
		ApdRecorder.configure(root, false);
		java.util.Map<String, byte[]> before = files(root);
		ApdFiles.provider = new ApdFiles.Provider(){
			public void chooseExport( String n, ApdFiles.ExportTarget t ){ t.onCancelled(); }
			public void chooseImport( ApdFiles.ImportSource s ){ s.onFailed("no such file"); }
			public String where(){ return "nowhere"; }
		};
		assertTrue(await(new Runner(){ public void run( HistoryTransfer.Listener l ){ HistoryTransfer.export(l); } }).contains("cancelled"));
		String failed = await(new Runner(){ public void run( HistoryTransfer.Listener l ){ HistoryTransfer.importHistory(l); } });
		assertTrue(failed, failed.contains("no such file"));
		java.util.Map<String, byte[]> after = files(root);
		assertEquals(before.keySet(), after.keySet());
	}

	@Test
	public void withNoHistorySetUpNothingIsAttemptedAndNothingThrows() throws Exception {
		ApdRecorder.configure(null, false);
		assertTrue(await(new Runner(){ public void run( HistoryTransfer.Listener l ){ HistoryTransfer.export(l); } }).contains("not set up"));
		assertTrue(await(new Runner(){ public void run( HistoryTransfer.Listener l ){ HistoryTransfer.importHistory(l); } }).contains("not set up"));
		assertFalse(HistoryTransfer.storage().warnings.size() > 0);
	}
}
