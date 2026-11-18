package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

//The item identity hooks are one-line calls inside game classes. If a later edit drops one, identity quietly stops working
//on a real device where no test plays the game, so the presence of each one is checked here.
public class ItemHookWiringTest {

	private static String src( String path ) throws Exception {
		FileInputStream in = new FileInputStream(new File(System.getProperty("apd.game.src"), path));
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
			return new String(out.toByteArray(), Charset.forName("UTF-8"));
		} finally {
			in.close();
		}
	}

	private static void has( String path, String... needles ) throws Exception {
		String s = src(path);
		for (String n : needles) assertTrue(path + " is missing: " + n, s.contains(n));
	}

	@Test
	public void itemKeepsItsIdentityHooksAndClearsItOnCopies() throws Exception {
		has("items/Item.java",
				"public String apdUid", "public String apdCopyOf",
				"bundle.put(APD_UID, apdUid)", "apdUid = bundle.getString(APD_UID)", "\"apd_uid\"",
				"ApdItems.noteAction( this, action )", "ApdItems.noteAction( this, ApdItems.ACTION_DROP )",
				"ApdItems.onMerge( this, other, other.quantity )", "ApdItems.onSplit( this, split, amount )",
				"ApdItems.onDetached( this, detached )", "ApdItems.onDetached( this, this )");
		String item = src("items/Item.java");
		//split and duplicate copy through a bundle: the copy must lose the identity it just inherited
		int split = item.indexOf("public Item split(");
		int dupe = item.indexOf("public Item duplicate()");
		assertTrue(item.substring(split, dupe).contains("split.apdUid = null"));
		int detach = item.indexOf("public final Item detach(");
		assertTrue(item.substring(dupe, detach).contains("dupe.apdUid = null"));
		//reset() makes a bones item fit for a new run: it must not keep the old run's identity
		int reset = item.indexOf("public void reset()");
		assertTrue(item.substring(reset, reset + 400).contains("apdUid = null"));
		//both throw paths announce the throw before taking the item out of the bag
		int count = 0, from = 0;
		while ((from = item.indexOf("ApdItems.noteAction( Item.this, ApdItems.ACTION_THROW )", from)) >= 0){ count++; from++; }
		assertTrue(count == 2);
	}

	@Test
	public void theGameLoopAndSavesAreConnected() throws Exception {
		has("actors/hero/Hero.java", "ApdItems.onHeroTurn()", "ApdItems.noteAcquireContext( ItemEvents.ACQ_PICKUP )");
		has("Dungeon.java", "ApdRecorder.onBeforeGameSave()", "ApdRecorder.onGameSaved(");
		has("windows/WndTradeItem.java", "ApdItems.noteAcquireContext( ItemEvents.ACQ_SHOP )");
	}

	@Test
	public void transformationsCraftingAndDiscoveriesAreConnected() throws Exception {
		has("items/scrolls/ScrollOfTransmutation.java", "ApdItems.onTransformed( item, result, \"TRANSMUTE\" )");
		has("items/armor/ClassArmor.java", "ApdItems.onTransformed( armor, classArmor, \"CLASS_ARMOR\" )");
		has("scenes/AlchemyScene.java", "ApdItems.onCrafted( ingredients, result )");
		for (String p : new String[]{"items/potions/Potion.java", "items/scrolls/Scroll.java", "items/rings/Ring.java",
				"items/potions/exotic/ExoticPotion.java", "items/scrolls/exotic/ExoticScroll.java"}){
			has(p, "ApdItems.onClassKnown(");
		}
	}

	@Test
	public void combatAndHealthHooksAreConnected() throws Exception {
		has("actors/Char.java",
				"ApdCombat.onAttackRolled( this, enemy, true )",
				"ApdCombat.onAttackRolled( this, enemy, false )",
				"ApdCombat.onAttackDetail( this, enemy, Math.round(dmg), dr, effectiveDamage )",
				"ApdCombat.onMagicRoll( attacker, defender, result )",
				"ApdCombat.onDamage( this, src, apdRequested, apdHpBefore, apdShieldBefore, shielded, dmg )",
				"ApdCombat.onCharDied( this, src )");
		has("actors/hero/Hero.java",
				"ApdCombat.onHeroDying( cause )", "ApdCombat.onHeroRevived( ApdCombat.REVIVE_BLESSED_ANKH )",
				"ApdCombat.onHeroRevived( ApdCombat.REVIVE_RESURRECT )", "ApdCombat.onXp( this, exp, source )");
		has("actors/buffs/Hunger.java", "ApdCombat.onHungerChanged(");
		//the damage hook sits after shielding was applied and before death is processed, so it sees the real numbers
		String c = src("actors/Char.java");
		int shield = c.indexOf("dmg = ShieldBuff.processDamage(this, dmg, src);");
		int hook = c.indexOf("ApdCombat.onDamage(");
		int die = c.indexOf("die( src );", hook);
		assertTrue(shield > 0 && hook > shield && die > hook);
		//the public attack is recorded as soon as the roll is known, before the hit's consequences (defenseProc can reveal a
		//mimic); the numbers are recorded just before the damage is dealt
		int rolled = c.indexOf("ApdCombat.onAttackRolled( this, enemy, true )");
		int defense = c.indexOf("enemy.defenseProc( this, Math.round(dmg) )");
		int detail = c.indexOf("ApdCombat.onAttackDetail(");
		int deal = c.indexOf("enemy.damage( effectiveDamage, this );");
		assertTrue(rolled > 0 && defense > rolled && detail > defense && deal > detail);
	}

	@Test
	public void generationProvenanceHooksAreConnected() throws Exception {
		has("Dungeon.java", "ApdGen.onLevelGenerated( level )");
		has("levels/Level.java", "ApdGen.onLevelCreateStart()", "ApdGen.onBuildAttempt()", "ApdGen.setStage( ApdGen.STAGE_MOBS )",
				"ApdGen.setStage( ApdGen.STAGE_ITEMS )");
		has("actors/mobs/Mob.java", "ApdGen.onLootCreated( this, loot )");
		has("items/Heap.java", "ApdGen.onBonusDrop( b, pos )");
		has("actors/Actor.java", "public int apdPeekId()");
		//the finished level is read after create() and before it is returned
		String d = src("Dungeon.java");
		int create = d.indexOf("level.create();");
		int hook = d.indexOf("ApdGen.onLevelGenerated( level )");
		int ret = d.indexOf("return level;", hook);
		assertTrue(create > 0 && hook > create && ret > hook);
		//every public generator draw reports through the same wrapper pattern, and the real work stays in the Impl methods
		String g = src("items/Generator.java");
		for (String m : new String[]{"randomImpl()", "randomUsingDefaultsImpl()", "randomImpl( Category cat )",
				"randomUsingDefaultsImpl( Category cat )", "randomImpl( Class<? extends Item> cl )", "randomArtifactImpl()"}){
			assertTrue("Generator lost its wrapped method " + m, g.contains("private static") && g.contains(m));
		}
		int begins = 0, from = 0;
		while ((from = g.indexOf("ApdGen.begin()", from)) >= 0){ begins++; from++; }
		assertEquals(6, begins);
	}

	@Test
	public void legacyImportAndRankingLinkAreConnected() throws Exception {
		has("apd/recorder/RunRecorder.java", "emitRankingLink(clock, outcome)", "LegacyEvents.emitRankingLink");
		//the link is written after the run end event and before the log is sealed
		String r = src("apd/recorder/RunRecorder.java");
		int end = r.indexOf("LifecycleEvents.emitRunEnd(writer, clock, e);");
		int link = r.indexOf("emitRankingLink(clock, outcome);", end);
		int seal = r.indexOf("writer.close(true, CLOSE_WAIT_MILLIS);", link);
		assertTrue(end > 0 && link > end && seal > link);
		//the settings pane offers the import and it goes through the recorder facade
		has("windows/APDSettingsPane.java", "Import APD rankings", "ApdRecorder.importApdRankings()", "ApdRecorder.historyOverview()");
		//the importer reads, it does not touch the rankings file or the saves
		String imp = src("apd/recorder/LegacyImporter.java");
		assertFalse(imp.contains("Rankings.INSTANCE"));
		assertFalse(imp.contains("bundleToFile"));
	}
}
