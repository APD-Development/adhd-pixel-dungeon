package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;

//Guards against the Phase 2 regression where the APD controls existed but could not be found on a phone: they were only
//in the in-game menu, not in Options. Windows can't be rendered headless, so this checks the wiring in the source.
public class UiEntryPointsTest {

	private static String scene( String name ) throws Exception {
		return read(new File(System.getProperty("apd.windows.src"), "../scenes/" + name));
	}

	private static String windows( String name ) throws Exception {
		return read(new File(System.getProperty("apd.windows.src"), name));
	}

	private static String read( File f ) throws Exception {
		java.io.FileInputStream in = new java.io.FileInputStream(f);
		try {
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
			return new String(out.toByteArray(), Charset.forName("UTF-8"));
		} finally {
			in.close();
		}
	}

	@Test
	public void theApdPaneHasTheRecordingControls() throws Exception {
		String pane = windows("APDSettingsPane.java");
		assertTrue(pane.contains("\"Record run history\""));
		assertTrue(pane.contains("ApdRecorder.status().summary()"));
		assertTrue(pane.contains("\"Run self-test\""));
		assertTrue(pane.contains("SPDSettings.apdRecording("));
		//every control must be added to the pane and positioned
		for (String c : new String[]{"optExotic", "info", "chkRecord", "status", "btnTest", "btnImport", "btnUimsd", "storage", "btnExportHistory", "btnImportHistory"}){
			assertTrue(c + " not added", pane.contains("add( " + c + " )"));
			java.util.regex.Matcher route = java.util.regex.Pattern.compile("row\\(ApdSettingsSection\\.[A-Z]+,\\s*" + c + ",").matcher(pane);
			int routes=0;while(route.find())routes++;
			assertTrue(c + " must have exactly one reachable category route",routes==1);
		}
		assertTrue("Registered controls must be positioned by the category layout",pane.contains("r.control.setRect(x,pos,width,22)"));
		assertTrue("Every category must be offered by the index",pane.contains("for(ApdSettingsSection choice:ApdSettingsSection.values())"));
		assertTrue("Hidden sections cannot remain interactive",pane.contains("r.control.visible=r.control.active=false"));
	}

	@Test
	public void theTitleScreenHasADirectApdSettingsEntryAndOptionsDoesNot() throws Exception {
		String title = scene("TitleScene.java");
		assertTrue(title.contains("new APDMenuButton( \"APD Settings\""));
		assertTrue(title.contains("new WndAPDSettings()"));
		assertTrue(title.contains("add( btnAPDSettings )"));
		assertTrue("must be laid out", title.contains("btnAPDSettings.setRect("));
		assertTrue("must fade with the rest of the menu", title.contains("btnAPDSettings, btnCommunity, btnNews"));
		String options = windows("WndSettings.java");
		assertTrue("APD settings must stay out of Options", !options.contains("APDSettingsPane") && !options.contains("\"APD\""));
	}

	@Test
	public void theStatisticsButtonIsNamedUimsdAndTheAnnotationPointsAtIt() throws Exception {
		String title = scene("TitleScene.java");
		assertTrue(title.contains("new APDMenuButton( \"UIMSD\", Icons.STATS"));
		assertTrue("old name must be gone", !title.contains("new APDMenuButton( \"Statistics\""));
		assertTrue("no adult fallback outside its progression stage", title.contains("ANNOTATION_TEXT = \"\""));
		assertTrue(title.contains("UimsdGate.titleAnnotation( ANNOTATION_TEXT )"));
		assertTrue(title.contains("annotation.visible = !annotationText.isEmpty()"));
		assertTrue(title.contains("add( annotation )"));
		assertTrue("must be placed relative to the grid", title.contains("annotation.place("));
		assertTrue("must fade with the menu", title.contains("annotation.alpha(alpha)"));
		assertTrue("layout must come from the tested TitleGeometry", title.contains("TitleGeometry.portrait(") && title.contains("TitleGeometry.landscape("));
		//the annotation is a plain Component with no PointerArea or button, so it can't take taps from anything
		int a = title.indexOf("class APDAnnotation");
		int b = title.indexOf("class APDMenuButton");
		assertTrue(a > 0 && b > a);
		assertTrue(!title.substring(a, b).contains("PointerArea") && !title.substring(a, b).contains("Button"));
	}

	@Test
	public void apdDocsUseTheUimsdName() throws Exception {
		for (String p : new String[]{"apd/apd_changes.txt", "apd/apd_news.txt", "apd/apd_records_history.txt"}){
			String t = read(new File(p)); //tests run from the assets folder
			assertTrue(p + " should mention UIMSD", t.contains("UIMSD"));
			assertTrue(p + " should say what it stands for", t.contains("Ultimate Infinite Mega Statistics Database"));
		}
		assertTrue(!read(new File("apd/apd_changes.txt")).contains("Play / Statistics"));
	}

	@Test
	public void theInGameMenuAlsoOpensThePane() throws Exception {
		assertTrue(windows("WndGame.java").contains("new WndAPDSettings()"));
		assertTrue(windows("WndAPDSettings.java").contains("new APDSettingsPane()"));
	}
}
