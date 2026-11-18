package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class GenerationEventsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void everyGenerationEventRoundTrips() throws Exception {
		File f = new File(tmp.getRoot(), "g.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "g", "test", 1);
		GenerationEvents.Where where = new GenerationEvents.Where();
		where.depth = 6;
		where.branch = 0;

		GenerationEvents.LevelGenerated g = new GenerationEvents.LevelGenerated();
		g.where = where; g.levelClass = "levels.PrisonLevel"; g.feeling = "DARK"; g.buildAttempts = 2; g.draws = 14; g.heaps = 12;
		g.itemsFound = 15; g.mobs = 9; g.trapsVisible = 3; g.trapsHidden = 4; g.intro = true; g.exoticChance = 30; g.challenges = 5;
		g.adventurersPagesFound = 2; g.alchemyPagesFound = 1;
		g.heroItems.add("items.armor.ClothArmor"); g.heroItems.add("items.Food");
		g.rooms.add("EntranceRoom"); g.rooms.add("SecretLibraryRoom");
		g.limitedNames.add("STRENGTH_POTIONS"); g.limitedCounts.add(1);
		g.categoryNames.add("WEAPON"); g.categoryDropped.add(7);
		GenerationEvents.emitLevelGenerated(w, 1, g);

		GenerationEvents.ItemGenerated i = new GenerationEvents.ItemGenerated();
		i.uid = "abcd"; i.where = where; i.container = GenerationEvents.C_LOCKED_CHEST; i.pos = 321; i.heapType = 3; i.hidden = true;
		i.room = "TreasuryRoom"; i.owner = "Mimic"; i.stage = GenerationEvents.STAGE_ITEMS; i.drawKind = GenerationEvents.DRAW_CATEGORY;
		i.requestedCategory = "WEP_T3"; i.actualCategory = "WEP_T3"; i.categoryDropped = 4; i.trueClass = "items.weapon.melee.Sword";
		i.trueLevel = -2; i.cursed = true; i.enchant = "Vampiric"; i.quantity = 1;
		GenerationEvents.emitItemGenerated(w, 1, i);

		GenerationEvents.MobGenerated m = new GenerationEvents.MobGenerated();
		m.where = where; m.mobClass = "actors.mobs.Gnoll"; m.id = 0; m.pos = 55; m.hp = 12; m.hpMax = 12; m.state = GenerationEvents.STATE_SLEEPING;
		m.alignment = 0; m.champion.add("Blazing"); m.room = "StandardRoom"; m.carries = 2;
		GenerationEvents.emitMobGenerated(w, 1, m);

		GenerationEvents.MobIdAssigned a = new GenerationEvents.MobIdAssigned();
		a.where = where; a.mobClass = "actors.mobs.Gnoll"; a.pos = 55; a.id = 42;
		GenerationEvents.emitMobIdAssigned(w, 2, a);
		w.close();

		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(4, r.events.size());
		GenerationEvents.LevelGenerated g2 = GenerationEvents.decodeLevelGenerated(r.events.get(0), r);
		assertEquals("DARK", g2.feeling);
		assertEquals(2, g2.buildAttempts);
		assertTrue(g2.intro);
		assertEquals(30, g2.exoticChance);
		assertEquals(2, g2.heroItems.size());
		assertEquals("SecretLibraryRoom", g2.rooms.get(1));
		assertEquals(1, (int) g2.limitedCounts.get(0));
		assertEquals(7, (int) g2.categoryDropped.get(0));
		GenerationEvents.ItemGenerated i2 = GenerationEvents.decodeItemGenerated(r.events.get(1), r);
		assertEquals(GenerationEvents.C_LOCKED_CHEST, i2.container);
		assertEquals("Vampiric", i2.enchant);
		assertEquals(-2, i2.trueLevel);
		assertEquals(GenerationEvents.DRAW_CATEGORY, i2.drawKind);
		assertTrue(i2.hidden);
		GenerationEvents.MobGenerated m2 = GenerationEvents.decodeMobGenerated(r.events.get(2), r);
		assertEquals("Blazing", m2.champion.get(0));
		assertEquals(2, m2.carries);
		assertEquals(42, GenerationEvents.decodeMobIdAssigned(r.events.get(3), r).id);
	}

	@Test
	public void anItemThatWasNotDrawnSaysSoInsteadOfClaimingACategory() {
		GenerationEvents.ItemGenerated i = new GenerationEvents.ItemGenerated();
		assertEquals(GenerationEvents.DRAW_NONE, i.drawKind);
		assertEquals("", i.requestedCategory);
		assertEquals(-1, i.categoryDropped);
	}
}
