package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdCombat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Exact healing (telemetry 1.6) depends on every place that raises the hero's HP calling ApdCombat.onHeal. This test reads the game's
//source and fails when a heal is added, moved or forgotten: every line that raises an HP is either one of the pinned hooked heals
//below, or belongs to a file that is exempt for a stated reason (it heals something that is not the hero, or sets up a creature).
public class ExactHealingWiringTest {

	//file -> { lines that raise an HP, calls of ApdCombat.onHeal }
	private static final Map<String, int[]> HOOKED = new TreeMap<>();
	static {
		HOOKED.put("actors/blobs/WaterOfHealth.java", new int[]{ 1, 1 });
		HOOKED.put("actors/buffs/Healing.java", new int[]{ 1, 1 });
		HOOKED.put("actors/buffs/MagicalSleep.java", new int[]{ 1, 1 });
		HOOKED.put("actors/buffs/Regeneration.java", new int[]{ 1, 1 });
		HOOKED.put("actors/buffs/WellFed.java", new int[]{ 1, 1 });
		HOOKED.put("actors/hero/Talent.java", new int[]{ 1, 1 });
		HOOKED.put("actors/hero/abilities/duelist/Challenge.java", new int[]{ 1, 1 });
		HOOKED.put("actors/hero/abilities/duelist/ElementalStrike.java", new int[]{ 1, 1 });
		HOOKED.put("actors/mobs/Mob.java", new int[]{ -1, 1 });   //Soul Siphon; the mobs folder is otherwise exempt, so its lines are not counted
		HOOKED.put("items/Dewdrop.java", new int[]{ 1, 1 });
		HOOKED.put("items/armor/curses/Metabolism.java", new int[]{ 1, 1 });
		HOOKED.put("items/artifacts/ChaliceOfBlood.java", new int[]{ 1, 1 });
		HOOKED.put("items/food/FrozenCarpaccio.java", new int[]{ 1, 1 });
		HOOKED.put("items/food/Pasty.java", new int[]{ 1, 1 });
		HOOKED.put("items/food/PhantomMeat.java", new int[]{ 1, 1 });
		HOOKED.put("items/food/SupplyRation.java", new int[]{ 1, 1 });
		HOOKED.put("items/potions/elixirs/ElixirOfAquaticRejuvenation.java", new int[]{ 2, 1 });   //the second line is the clamp to the maximum
		HOOKED.put("items/quest/EscapeCrystal.java", new int[]{ 1, 1 });
		HOOKED.put("items/remains/TornPage.java", new int[]{ 1, 1 });
		HOOKED.put("items/wands/CursedWand.java", new int[]{ 1, 1 });
		HOOKED.put("items/wands/WandOfTransfusion.java", new int[]{ 1, 1 });
		HOOKED.put("items/weapon/enchantments/Vampiric.java", new int[]{ 1, 1 });
		HOOKED.put("levels/CityLevel.java", new int[]{ 1, 1 });
		HOOKED.put("plants/Sungrass.java", new int[]{ 2, 1 });                                       //the second line is the clamp to the maximum
	}

	//file -> why raising an HP there is not a heal of the hero that needs a hook
	private static final Map<String, String> EXEMPT = new TreeMap<>();
	static {
		EXEMPT.put("actors/buffs/Corruption.java", "a corrupted enemy is set to full HP");
		EXEMPT.put("actors/buffs/PrismaticGuard.java", "the HP of the mirror image");
		EXEMPT.put("actors/hero/Hero.java", "a higher maximum HP (levelling, rings), a revival: both are their own events, not heals");
		EXEMPT.put("actors/hero/abilities/mage/ElementalBlast.java", "heals allies");
		EXEMPT.put("actors/hero/abilities/rogue/ShadowClone.java", "sets up the clone");
		EXEMPT.put("actors/hero/abilities/rogue/SmokeBomb.java", "the shadow clone's HP");
		EXEMPT.put("actors/hero/spells/BlessSpell.java", "heals allies only: the hero gets a shield");
		EXEMPT.put("actors/hero/spells/HallowedGround.java", "heals allies only: the hero gets a shield");
		EXEMPT.put("actors/hero/spells/LayOnHands.java", "heals allies only: the hero gets a shield");
		EXEMPT.put("items/Honeypot.java", "the bee");
		EXEMPT.put("items/armor/curses/Multiplicity.java", "the copy");
		EXEMPT.put("items/artifacts/DriedRose.java", "the ghost");
		EXEMPT.put("items/scrolls/exotic/ScrollOfPrismaticImage.java", "the image");
		EXEMPT.put("items/spells/SummonElemental.java", "the summoned elemental");
		EXEMPT.put("items/wands/WandOfLivingEarth.java", "the guardian");
		EXEMPT.put("items/wands/WandOfWarding.java", "the ward");
		EXEMPT.put("levels/rooms/secret/SecretHoneypotRoom.java", "the bee");
	}

	private static final Pattern RAISES_HP = Pattern.compile(
			"\\bHP\\s*\\+=|\\bHP\\s*=\\s*Math\\.min\\s*\\(|\\bHP\\s*=\\s*[\\w.]*\\bHT\\b(?!\\s*=)|\\bHP\\s*=\\s*Math\\.round\\(\\s*[\\w.]*HT");

	private static File gameSrc(){
		return new File(System.getProperty("apd.game.src"));
	}

	private static List<File> javaFiles( File dir ){
		List<File> out = new ArrayList<>();
		File[] kids = dir.listFiles();
		if (kids == null) return out;
		for (File k : kids){
			if (k.isDirectory()) out.addAll(javaFiles(k));
			else if (k.getName().endsWith(".java")) out.add(k);
		}
		return out;
	}

	private static String rel( File f ){
		return gameSrc().toURI().relativize(f.toURI()).getPath();
	}

	private static String read( File f ) throws Exception {
		return new String(Files.readAllBytes(f.toPath()), "UTF-8");
	}

	@Test
	public void everyLineThatRaisesAnHpIsAPinnedHookedHealOrAnExplainedExemption() throws Exception {
		List<String> problems = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (File f : javaFiles(gameSrc())){
			String r = rel(f);
			if (r.startsWith("apd/")) continue;
			String src = read(f);
			int calls = 0;
			for (int i = src.indexOf("ApdCombat.onHeal("); i >= 0; i = src.indexOf("ApdCombat.onHeal(", i + 1)) calls++;
			int lines = 0;
			for (String line : src.split("\n")){
				if (line.trim().startsWith("//")) continue;
				if (RAISES_HP.matcher(line).find()) lines++;
			}
			boolean inMobs = r.startsWith("actors/mobs/");
			if (HOOKED.containsKey(r)){
				seen.add(r);
				int[] want = HOOKED.get(r);
				if (want[0] >= 0 && lines != want[0]) problems.add(r + ": " + lines + " lines raise an HP, expected " + want[0] + " (a heal was added or removed: hook it, then update this list)");
				if (calls != want[1]) problems.add(r + ": " + calls + " calls of ApdCombat.onHeal, expected " + want[1]);
			} else if (EXEMPT.containsKey(r)){
				seen.add(r);
				if (calls != 0) problems.add(r + " is listed as exempt but calls ApdCombat.onHeal: move it to the hooked list");
			} else if (inMobs){
				if (calls != 0) problems.add(r + " calls ApdCombat.onHeal but is not in the hooked list");
				//the creatures' own HP is not the hero's: everything in the mobs folder is exempt
			} else if (lines > 0 || calls > 0){
				problems.add(r + ": raises an HP (" + lines + " lines, " + calls + " hooks) but is neither a pinned hooked heal nor an explained exemption: "
						+ "if it can heal the hero call ApdCombat.onHeal(target, hpBefore, ApdCombat.HEAL_..., requested) right after it, "
						+ "then add the file to HOOKED in this test; otherwise add it to EXEMPT with the reason");
			}
		}
		for (String k : HOOKED.keySet()) if (!seen.contains(k)) problems.add("listed as hooked but not found: " + k);
		for (String k : EXEMPT.keySet()) if (!seen.contains(k)) problems.add("listed as exempt but not found: " + k);
		assertTrue(problems.toString(), problems.isEmpty());
	}

	//every call passes one of ApdCombat's constants (never a loose string) and the HP from just before the heal that its own file captured
	@Test
	public void everyHookPassesAKnownSourceAndTheHpFromJustBeforeTheHeal() throws Exception {
		Pattern call = Pattern.compile("ApdCombat\\.onHeal\\(\\s*([^,]+),\\s*(\\w+),\\s*(ApdCombat\\.HEAL_\\w+|\"[^\"]*\"|\\w+),\\s*([^;]*)\\);");
		int total = 0;
		for (File f : javaFiles(gameSrc())){
			String r = rel(f);
			if (r.startsWith("apd/")) continue;
			String src = read(f);
			Matcher m = call.matcher(src);
			while (m.find()){
				total++;
				assertTrue(r + ": the source must be one of ApdCombat's HEAL_ constants, not " + m.group(3), m.group(3).startsWith("ApdCombat.HEAL_"));
				String hpVar = m.group(2);
				assertTrue(r + ": the HP before the heal must be captured before the call (" + hpVar + ")", hpVar.startsWith("apdHpBefore"));
				int decl = src.indexOf("int " + hpVar + " =");
				assertTrue(r + ": " + hpVar + " is never captured", decl >= 0 && decl < m.start());
			}
		}
		assertTrue("expected the hero's heal sources to be hooked, found " + total, total >= 24);
	}

	//the keys are stored in the log: unique, each in a category UIMSD knows, and every one of them is used by some hook
	@Test
	public void healKeysAreUniqueCategorisedAndAllInUse() throws Exception {
		Set<String> values = new HashSet<>();
		List<String> names = new ArrayList<>();
		for (Field fld : ApdCombat.class.getDeclaredFields()){
			if (!fld.getName().startsWith("HEAL_") || !Modifier.isStatic(fld.getModifiers()) || fld.getType() != String.class) continue;
			String value = (String) fld.get(null);
			assertTrue("duplicate heal key " + value, values.add(value));
			String category = CombatEvents.healCategory(value);
			boolean known = false;
			for (String c : CombatEvents.HEAL_CATEGORIES) if (c.equals(category)) known = true;
			assertTrue(value, known);
			String prefix = value.contains(".") ? value.substring(0, value.indexOf('.')) : value;
			assertEquals("the key's own prefix is its category: " + value, prefix, category);
			names.add(fld.getName());
		}
		assertTrue(names.size() >= 20);
		StringBuilder all = new StringBuilder();
		for (File f : javaFiles(gameSrc())) if (!rel(f).startsWith("apd/")) all.append(read(f));
		String game = all.toString();
		for (String n : names) assertTrue("no game code reports " + n, game.contains("ApdCombat." + n));
		assertEquals("an unknown source counts as other", CombatEvents.HC_OTHER, CombatEvents.healCategory("something.new"));
		assertEquals(CombatEvents.HC_OTHER, CombatEvents.healCategory(null));
		assertFalse(CombatEvents.healCategory("potion.healing").equals(CombatEvents.healCategory("food.pasty")));
	}
}
