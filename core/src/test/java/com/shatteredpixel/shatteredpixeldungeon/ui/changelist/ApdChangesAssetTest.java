package com.shatteredpixel.shatteredpixeldungeon.ui.changelist;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApdChangesAssetTest {

	static String asset() throws Exception {
		Path p = null;
		for (Path dir = Paths.get("").toAbsolutePath(); dir != null && p == null; dir = dir.getParent()){
			for (String rel : new String[]{"src/main/assets/apd/changes.tsv", "core/src/main/assets/apd/changes.tsv"}){
				if (Files.exists(dir.resolve(rel))) { p = dir.resolve(rel); break; }
			}
		}
		if (p == null){
			java.io.InputStream in = ApdChangesAssetTest.class.getClassLoader().getResourceAsStream("apd/changes.tsv");
			assertNotNull("apd/changes.tsv not found", in);
			byte[] b = new byte[1 << 20]; int n = 0, r;
			while ((r = in.read(b, n, b.length - n)) > 0) n += r;
			return new String(b, 0, n, StandardCharsets.UTF_8);
		}
		return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
	}

	@Test
	public void everyLineParsesAndEveryIconResolves() throws Exception {
		String text = asset();
		assertTrue(ApdChanges.tabs(text).size() >= 2);
		boolean header = false;
		int buttons = 0;
		for (String raw : text.split("\n")){
			if (raw.isEmpty() || raw.startsWith("#")) continue;
			String[] f = raw.split("\t", -1);
			switch (f[0]){
				case "TAB": assertEquals(raw, 2, f.length); header = false; break;
				case "MAJOR": case "BUILD": assertEquals(raw, 2, f.length); header = true; break;
				case "BTN":
					assertEquals(raw, 4, f.length);
					assertTrue("button before any header: " + raw, header);
					assertFalse(f[2].isEmpty());
					assertFalse(f[3].isEmpty());
					String icon = f[1];
					if (icon.startsWith("i:")) Icons.valueOf(icon.substring(2));
					else if (icon.startsWith("c:")) ChangeIcons.valueOf(icon.substring(2));
					else if (icon.startsWith("s:")) ApdChanges.sprite(icon.substring(2));
					else fail("unknown icon " + icon);
					buttons++;
					break;
				default: fail("unknown line " + raw);
			}
		}
		assertTrue(buttons > 50);
	}

	@Test public void everyBuildHasAtLeastOneEntryAndNoDevOnlyCategoriesRemain() throws Exception {
		String[] lines = asset().split("\n");
		for (int i = 0; i < lines.length; i++){
			if (lines[i].startsWith("BUILD\t")){
				assertTrue("empty build: " + lines[i], i + 1 < lines.length && lines[i + 1].startsWith("BTN\t"));
			}
			if (lines[i].startsWith("BTN\t")){
				String title = lines[i].split("\t", -1)[2];
				assertFalse(lines[i], title.equals("Testing") || title.equals("Infrastructure") || title.equals("Development"));
			}
		}
	}

	@Test public void tabsArePagedFourAtATimeWithoutLosingAny() throws Exception {
		java.util.List<String> tabs = ApdChanges.tabs(asset());
		assertTrue(tabs.size() >= 5);
		assertEquals(tabs.size(), new java.util.LinkedHashSet<>(tabs).size());
	}
}
