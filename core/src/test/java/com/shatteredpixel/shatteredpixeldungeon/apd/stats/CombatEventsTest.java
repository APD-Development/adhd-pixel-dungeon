package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class CombatEventsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static CombatEvents.FighterRef actor( int kind, String name, int pos ){
		CombatEvents.FighterRef a = new CombatEvents.FighterRef();
		a.kind = kind;
		a.name = name;
		a.pos = pos;
		return a;
	}

	@Test
	public void everyCombatEventRoundTrips() throws Exception {
		File f = new File(tmp.getRoot(), "c.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "c", "test", 1);
		CombatEvents.Where where = new CombatEvents.Where();
		where.depth = 5;
		where.branch = 1;

		CombatEvents.Attack a = new CombatEvents.Attack();
		a.where = where; a.attacker = actor(CombatEvents.KIND_HERO, "you", 10); a.defender = actor(CombatEvents.KIND_ENEMY, "rat", 11);
		a.hit = true; a.magic = false; a.thrown = true; a.weaponName = "dart"; a.weaponUid = "w1";
		CombatEvents.emitAttack(w, 1, a);
		CombatEvents.AttackDetail ad = new CombatEvents.AttackDetail();
		ad.where = where; ad.attackerClass = "Hero"; ad.attackerId = 3; ad.defenderClass = "actors.mobs.Rat"; ad.defenderId = -7; ad.hit = true;
		ad.rolled = 9; ad.armorReduction = 2; ad.effective = 7; ad.publicEmitted = true;
		CombatEvents.emitAttackDetail(w, 1, ad);
		CombatEvents.Damage d = new CombatEvents.Damage();
		d.where = where; d.target = actor(CombatEvents.KIND_HERO, "you", 10);
		d.source.kind = CombatEvents.SRC_BUFF; d.source.label = "Hunger"; d.shown = 4; d.killed = false;
		d.heroHp = 16; d.heroHpMax = 20; d.heroShield = 6; d.shieldAbsorbed = 4;
		CombatEvents.emitDamage(w, 2, d);
		CombatEvents.DamageDetail dd = new CombatEvents.DamageDetail();
		dd.where = where; dd.targetClass = "Hero"; dd.targetId = 3; dd.targetPos = 10; dd.hpBefore = 16; dd.hpAfter = 16; dd.hpMax = 20;
		dd.shieldBefore = 10; dd.shieldAfter = 6; dd.requested = 4; dd.absorbed = 4; dd.hpLost = 0; dd.srcKind = CombatEvents.SRC_BUFF;
		dd.srcClass = "actors.buffs.Hunger"; dd.publicEmitted = true;
		CombatEvents.emitDamageDetail(w, 2, dd);
		CombatEvents.Heal h = new CombatEvents.Heal();
		h.where = where; h.amount = 3; h.hpAfter = 19; h.hpMax = 20; h.guess = "Regeneration";
		CombatEvents.emitHeal(w, 3, h);
		CombatEvents.MaxHpChange m = new CombatEvents.MaxHpChange();
		m.where = where; m.oldMax = 20; m.newMax = 25; m.hpAfter = 25; m.guess = "level up";
		CombatEvents.emitMaxHpChange(w, 3, m);
		CombatEvents.ShieldGain sg = new CombatEvents.ShieldGain();
		sg.where = where; sg.amount = 7; sg.shieldAfter = 7; sg.source = "Barrier";
		CombatEvents.emitShieldGain(w, 3, sg);
		CombatEvents.HungerChange hc = new CombatEvents.HungerChange();
		hc.where = where; hc.energy = 300; hc.before = 350; hc.after = 50; hc.cause = "EAT food";
		CombatEvents.emitHungerChange(w, 4, hc);
		CombatEvents.HungerState hs = new CombatEvents.HungerState();
		hs.where = where; hs.state = CombatEvents.HUNGER_NORMAL; hs.previous = CombatEvents.HUNGER_HUNGRY;
		CombatEvents.emitHungerState(w, 4, hs);
		CombatEvents.XpGain xg = new CombatEvents.XpGain();
		xg.where = where; xg.amount = 4; xg.expBefore = 3; xg.maxExp = 10; xg.levelBefore = 1; xg.category = CombatEvents.XP_KILL;
		CombatEvents.emitXpGain(w, 5, xg);
		CombatEvents.LevelUp lu = new CombatEvents.LevelUp();
		lu.where = where; lu.oldLevel = 1; lu.newLevel = 2;
		CombatEvents.emitLevelUp(w, 5, lu);
		CombatEvents.Kill k = new CombatEvents.Kill();
		k.where = where; k.victim = actor(CombatEvents.KIND_ENEMY, "rat", 11); k.killer.kind = CombatEvents.SRC_CHAR; k.killer.label = "you";
		CombatEvents.emitKill(w, 5, k);
		CombatEvents.KillDetail kd = new CombatEvents.KillDetail();
		kd.where = where; kd.victimClass = "actors.mobs.Rat"; kd.victimId = 9; kd.victimKind = CombatEvents.KIND_ENEMY; kd.expValue = 1; kd.killerClass = "Hero"; kd.publicEmitted = true;
		CombatEvents.emitKillDetail(w, 5, kd);
		CombatEvents.HeroDeath hd = new CombatEvents.HeroDeath();
		hd.where = where; hd.cause.kind = CombatEvents.SRC_CLASS; hd.cause.label = "Burning"; hd.pos = 44;
		CombatEvents.emitHeroDeath(w, 6, hd);
		CombatEvents.HeroRevived hr = new CombatEvents.HeroRevived();
		hr.where = where; hr.kind = CombatEvents.REVIVE_BLESSED_ANKH; hr.hpAfter = 5; hr.hpMax = 20;
		CombatEvents.emitHeroRevived(w, 6, hr);
		CombatEvents.HpChangeInferred hi = new CombatEvents.HpChangeInferred();
		hi.where = where; hi.delta = -2; hi.hpAfter = 18; hi.hpMax = 20;
		CombatEvents.emitHpChangeInferred(w, 7, hi);
		w.close();

		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(16, r.events.size());
		CombatEvents.Attack a2 = CombatEvents.decodeAttack(r.events.get(0), r);
		assertEquals("rat", a2.defender.name);
		assertTrue(a2.thrown);
		assertEquals("w1", a2.weaponUid);
		CombatEvents.AttackDetail ad2 = CombatEvents.decodeAttackDetail(r.events.get(1), r);
		assertEquals(-7, ad2.defenderId);
		assertEquals(2, ad2.armorReduction);
		CombatEvents.Damage d2 = CombatEvents.decodeDamage(r.events.get(2), r);
		assertEquals(4, d2.shieldAbsorbed);
		assertEquals("Hunger", d2.source.label);
		CombatEvents.DamageDetail dd2 = CombatEvents.decodeDamageDetail(r.events.get(3), r);
		assertEquals(10, dd2.shieldBefore);
		CombatEvents.Heal h2 = CombatEvents.decodeHeal(r.events.get(4), r);
		assertEquals(-1, h2.overheal);
		assertEquals(CombatEvents.FLAG_INFERRED, r.events.get(4).flags);
		assertEquals(25, CombatEvents.decodeMaxHpChange(r.events.get(5), r).newMax);
		assertEquals("Barrier", CombatEvents.decodeShieldGain(r.events.get(6), r).source);
		assertEquals("EAT food", CombatEvents.decodeHungerChange(r.events.get(7), r).cause);
		assertEquals(CombatEvents.HUNGER_HUNGRY, CombatEvents.decodeHungerState(r.events.get(8), r).previous);
		assertEquals(10, CombatEvents.decodeXpGain(r.events.get(9), r).maxExp);
		assertEquals(2, CombatEvents.decodeLevelUp(r.events.get(10), r).newLevel);
		assertEquals("you", CombatEvents.decodeKill(r.events.get(11), r).killer.label);
		assertEquals(1, CombatEvents.decodeKillDetail(r.events.get(12), r).expValue);
		assertEquals(44, CombatEvents.decodeHeroDeath(r.events.get(13), r).pos);
		assertEquals(5, CombatEvents.decodeHeroRevived(r.events.get(14), r).hpAfter);
		assertEquals(-2, CombatEvents.decodeHpChangeInferred(r.events.get(15), r).delta);
	}

	@Test
	public void inferredEventsAlwaysCarryTheInferredFlagAndDirectOnesNever() throws Exception {
		File f = new File(tmp.getRoot(), "i.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "i", "test", 1);
		CombatEvents.Where where = new CombatEvents.Where();
		CombatEvents.emitHeal(w, 1, new CombatEvents.Heal());
		CombatEvents.emitMaxHpChange(w, 1, new CombatEvents.MaxHpChange());
		CombatEvents.emitShieldGain(w, 1, new CombatEvents.ShieldGain());
		CombatEvents.emitHpChangeInferred(w, 1, new CombatEvents.HpChangeInferred());
		CombatEvents.emitDamage(w, 1, new CombatEvents.Damage());
		CombatEvents.emitHungerChange(w, 1, new CombatEvents.HungerChange());
		CombatEvents.emitKill(w, 1, new CombatEvents.Kill());
		w.close();
		ReadResult r = RunLogReader.read(f);
		for (int i = 0; i < 4; i++) assertEquals(EventTypes.name(r.events.get(i).type), CombatEvents.FLAG_INFERRED, r.events.get(i).flags);
		for (int i = 4; i < 7; i++) assertEquals(EventTypes.name(r.events.get(i).type), 0, r.events.get(i).flags);
	}

	@Test
	public void aPublicDamageEventHasNoFieldForAnEnemysExactHealth() throws Exception {
		//the only HP fields in a PUBLIC damage event belong to the hero, and are -1 for anyone else
		CombatEvents.Damage d = new CombatEvents.Damage();
		assertEquals(-1, d.heroHp);
		assertEquals(-1, d.heroHpMax);
		assertEquals(-1, d.heroShield);
		assertEquals(-1, d.shieldAbsorbed);
		File f = new File(tmp.getRoot(), "p.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "p", "test", 1);
		d.target = actor(CombatEvents.KIND_ENEMY, "goo", 3);
		d.shown = 12;
		CombatEvents.emitDamage(w, 1, d);
		w.close();
		CombatEvents.Damage back = CombatEvents.decodeDamage(RunLogReader.read(f).events.get(0), RunLogReader.read(f));
		assertEquals(-1, back.heroHp);
		assertFalse(back.killed);
		assertEquals(12, back.shown);
	}
}
