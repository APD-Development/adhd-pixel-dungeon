package com.shatteredpixel.shatteredpixeldungeon.apd.branding;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The update/patch notices, save warning, future-save warning, update description and victory thanks are shown to APD
 * players, so in every language they must name APD, never Shattered (credit to Shattered stays in the About screen).
 */
public class PlayerFacingBrandingTest {
	private static final String[] KEYS = {
		"scenes.welcomescene.update_intro", "scenes.welcomescene.update_msg", "scenes.welcomescene.patch_intro",
		"scenes.welcomescene.what_msg", "scenes.welcomescene.save_warning", "scenes.titlescene$changesbutton.desc",
		"ui.updatenotification$wndupdate.desc", "windows.wndvictorycongrats.thank_you"
	};

	@Test public void noticesNameApdInEveryLanguage() throws Exception {
		File messages = new File(System.getProperty("apd.game.src"), "../../../../assets/messages");
		if (!messages.isDirectory()) messages = new File("src/main/assets/messages");
		assertTrue(messages.getAbsolutePath(), messages.isDirectory());
		List<String> problems = new ArrayList<>();
		int checked = 0;
		for (File dir : messages.listFiles()) {
			if (!dir.isDirectory()) continue;
			for (File f : dir.listFiles((d, n) -> n.endsWith(".properties"))) {
				for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
					for (String k : KEYS) {
						if (line.startsWith(k + "=")) {
							checked++;
							if (line.contains("Shattered")) problems.add(f.getName() + ": " + k);
						}
					}
				}
			}
		}
		assertTrue("checked only " + checked, checked >= KEYS.length);
		assertTrue(problems.toString(), problems.isEmpty());
	}

	// Every message in every language: only the Pasty cake's lore line (Shattered's anniversary cake) may name Shattered.
	@Test public void noOtherMessageNamesShattered() throws Exception {
		File messages = new File(System.getProperty("apd.game.src"), "../../../../assets/messages");
		List<String> problems = new ArrayList<>();
		for (File dir : messages.listFiles()) {
			if (!dir.isDirectory()) continue;
			for (File f : dir.listFiles((d, n) -> n.endsWith(".properties"))) {
				for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
					String key = line.split("=", 2)[0];
					if (line.contains("Shattered") && !key.contains("pasty") && !key.endsWith("shattered_desc")) problems.add(f.getName() + ": " + key);
				}
			}
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}
}
