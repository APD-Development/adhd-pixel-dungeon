package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.CombatTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;

import org.junit.After;
import org.junit.Test;

import java.util.List;

public class CombatTelemetryTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	@Test
	public void killingARatIsRecordedAsAttacksDamageAKillAndExperience() throws Exception {
		RecorderTestSupport.Session s = startFight(51L);
		Hero hero = hero();
		Rat rat = rat(1, true);
		String ratName = rat.name();
		int attacks = fightToTheDeath(hero, rat);
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);

		//every attack the hero made is there, hits and misses, with the weapon the player sees
		List<CombatEvents.Attack> a = attacks(r);
		assertEquals(attacks, a.size());
		for (CombatEvents.Attack x : a){
			assertEquals(CombatEvents.KIND_HERO, x.attacker.kind);
			assertEquals(CombatEvents.KIND_ENEMY, x.defender.kind);
			assertEquals(ratName, x.defender.name);
			assertFalse(x.magic);
			assertFalse(x.thrown);
			assertEquals(hero.belongings.weapon().name(), x.weaponName);
			assertEquals(hero.belongings.weapon().apdUid, x.weaponUid);
		}
		assertTrue("the last attack must have hit", a.get(a.size() - 1).hit);

		//the damage the rat took adds up to its HP and ends with the kill
		List<CombatEvents.Damage> d = damages(r);
		assertFalse(d.isEmpty());
		int total = 0;
		for (CombatEvents.Damage x : d){
			total += x.shown;
			assertEquals(ratName, x.target.name);
			assertEquals(CombatEvents.SRC_CHAR, x.source.kind);
			assertEquals(CombatEvents.KIND_HERO, x.source.fighterKind);
			//the rat's own HP is hidden from the player, so a public event never carries it
			assertEquals(-1, x.heroHp);
			assertEquals(-1, x.shieldAbsorbed);
		}
		assertTrue(total >= 8); //a rat has 8 HP
		assertTrue(d.get(d.size() - 1).killed);

		List<ApdEvent> kills = of(r, EventTypes.KILL);
		assertEquals(1, kills.size());
		CombatEvents.Kill k = CombatEvents.decodeKill(kills.get(0), r);
		assertEquals(ratName, k.victim.name);
		assertEquals(CombatEvents.KIND_HERO, k.killer.fighterKind);

		List<ApdEvent> xp = of(r, EventTypes.XP_GAIN);
		assertEquals(1, xp.size());
		CombatEvents.XpGain g = CombatEvents.decodeXpGain(xp.get(0), r);
		assertEquals(CombatEvents.XP_KILL, g.category);
		assertEquals(1, g.amount);
	}

	@Test
	public void theTruthBehindAFightIsInternalOnly() throws Exception {
		RecorderTestSupport.Session s = startFight(52L);
		Rat rat = rat(1, true);
		fightToTheDeath(hero(), rat);
		ReadResult r = ItemTestSupport.finish(s);

		List<CombatEvents.DamageDetail> d = damageDetails(r);
		assertEquals(damages(r).size(), d.size());
		CombatEvents.DamageDetail first = d.get(0);
		assertTrue(first.targetClass.endsWith("Rat"));
		assertEquals(8, first.hpBefore);       //the exact enemy HP lives only in the INTERNAL event
		assertEquals(8, first.hpMax);
		//A rat can soak up a weak hit (damage 0 after its armor), so the first blow need not hurt. The exact HP must agree with the game's own
		//damage number every time: HP went down exactly when damage was dealt.
		for (CombatEvents.DamageDetail x : d){
			assertEquals(x.hpLost > 0, x.hpAfter < x.hpBefore);
			assertTrue(x.hpAfter <= x.hpBefore);
			//hpLost is the game's own number (what the damage text shows), so a killing blow can exceed the HP that was left
			assertTrue(x.hpLost >= x.hpBefore - x.hpAfter);
		}
		assertTrue("the fight ends with the rat's HP gone", d.get(d.size() - 1).hpAfter <= 0);
		assertTrue(first.publicEmitted);
		for (CombatEvents.AttackDetail x : attackDetails(r)){
			assertTrue(x.defenderClass.endsWith("Rat"));
			assertTrue(x.publicEmitted);
		}
		boolean haveHit = false;
		for (CombatEvents.AttackDetail x : attackDetails(r)) if (x.hit){ haveHit = true; assertTrue(x.rolled > 0); }
		assertTrue(haveHit);
		ApdEvent killDetail = of(r, EventTypes.KILL_DETAIL).get(0);
		CombatEvents.KillDetail kd = CombatEvents.decodeKillDetail(killDetail, r);
		assertTrue(kd.victimClass.endsWith("Rat"));
		assertEquals(1, kd.expValue);
		//and the events are classified so views can tell them apart
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(EventTypes.DAMAGE_DETAIL));
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(EventTypes.ATTACK_DETAIL));
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(EventTypes.KILL_DETAIL));
		assertEquals(EventTypes.Visibility.PUBLIC, EventTypes.visibility(EventTypes.DAMAGE));
	}

	@Test
	public void aFightThePlayerCannotSeeLeavesNoPublicTrace() throws Exception {
		RecorderTestSupport.Session s = startFight(53L);
		//two creatures far from the hero's view: an ally and an enemy fighting
		Rat ally = rat(10, false);
		ally.alignment = com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;
		Rat enemy = rat(11, false);
		fightToTheDeath(ally, enemy);
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);

		assertTrue("nothing public about a fight no one saw", attacks(r).isEmpty());
		assertTrue(damages(r).isEmpty());
		assertTrue(of(r, EventTypes.KILL).isEmpty());
		//but the truth is kept, marked as never shown
		assertFalse(attackDetails(r).isEmpty());
		for (CombatEvents.AttackDetail x : attackDetails(r)) assertFalse(x.publicEmitted);
		for (CombatEvents.DamageDetail x : damageDetails(r)) assertFalse(x.publicEmitted);
		assertEquals(1, of(r, EventTypes.KILL_DETAIL).size());
		assertFalse(CombatEvents.decodeKillDetail(of(r, EventTypes.KILL_DETAIL).get(0), r).publicEmitted);
	}

	@Test
	public void theSameFightBecomesPublicOnceThePlayerCanSeeIt() throws Exception {
		RecorderTestSupport.Session s = startFight(54L);
		Rat ally = rat(10, false);
		ally.alignment = com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;
		Rat enemy = rat(11, false);
		Dungeon.level.heroFOV[enemy.pos] = true; //the hero can see one of them
		fightToTheDeath(ally, enemy);
		ReadResult r = ItemTestSupport.finish(s);
		assertFalse(attacks(r).isEmpty());
		assertEquals(CombatEvents.KIND_ALLY, attacks(r).get(0).attacker.kind);
		assertEquals(1, of(r, EventTypes.KILL).size());
	}

	@Test
	public void starvationEatsShieldingFirstAndTheDamageEventShowsIt() throws Exception {
		RecorderTestSupport.Session s = startFight(55L);
		Hero hero = hero();
		Buff.affect(hero, Barrier.class).setShield(10);
		int hpBefore = hero.HP;
		hero.damage(4, hero.buff(Hunger.class));    //APD-0001: starvation consumes shielding
		assertEquals(hpBefore, hero.HP);
		ReadResult r = ItemTestSupport.finish(s);

		CombatEvents.Damage d = damages(r).get(0);
		assertEquals(CombatEvents.KIND_HERO, d.target.kind);
		assertEquals(CombatEvents.SRC_BUFF, d.source.kind);
		assertEquals(4, d.shown);
		assertEquals(4, d.shieldAbsorbed);
		assertEquals(hpBefore, d.heroHp);
		assertEquals(6, d.heroShield);
		CombatEvents.DamageDetail x = damageDetails(r).get(0);
		assertEquals(10, x.shieldBefore);
		assertEquals(6, x.shieldAfter);
		assertEquals(0, x.hpLost);
		assertEquals(4, x.absorbed);
		assertEquals(4, x.requested);
	}

	@Test
	public void damageThatWouldKillShowsTheHeroHpAndTheEventAfterwardsDoesNotInventAHeal() throws Exception {
		RecorderTestSupport.Session s = startFight(56L);
		Hero hero = hero();
		hero.damage(5, hero.buff(Hunger.class));
		ItemTestSupport.turn();
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		assertTrue("damage the game explained is not an unexplained change", of(r, EventTypes.HP_CHANGE_INFERRED).isEmpty());
		assertTrue(of(r, EventTypes.HEAL).isEmpty());
	}

	@Test
	public void healingIsInferredAndEveryHealIsMarkedAsInferred() throws Exception {
		RecorderTestSupport.Session s = startFight(57L);
		Hero hero = hero();
		hero.damage(4, hero.buff(Hunger.class));
		ItemTestSupport.turn();
		int before = hero.HP;
		hero.HP += 3;                    //how most of SPD's ~40 heals work: a direct HP write
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		List<ApdEvent> heals = of(r, EventTypes.HEAL);
		assertEquals(1, heals.size());
		assertEquals("healing is inferred, the event must say so", CombatEvents.FLAG_INFERRED, heals.get(0).flags & CombatEvents.FLAG_INFERRED);
		CombatEvents.Heal h = CombatEvents.decodeHeal(heals.get(0), r);
		assertEquals(3, h.amount);
		assertEquals(before + 3, h.hpAfter);
		assertEquals(-1, h.overheal);    //unknown, never guessed as zero
	}

	@Test
	public void anUnexplainedHpLossIsRecordedAsInferredToo() throws Exception {
		RecorderTestSupport.Session s = startFight(58L);
		Hero hero = hero();
		hero.HP -= 2;
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		List<ApdEvent> e = of(r, EventTypes.HP_CHANGE_INFERRED);
		assertEquals(1, e.size());
		assertEquals(CombatEvents.FLAG_INFERRED, e.get(0).flags & CombatEvents.FLAG_INFERRED);
		assertEquals(-2, CombatEvents.decodeHpChangeInferred(e.get(0), r).delta);
	}

	@Test
	public void levellingUpIsALevelUpAndANewMaximumButNotAHeal() throws Exception {
		RecorderTestSupport.Session s = startFight(59L);
		Hero hero = hero();
		int oldMax = hero.HT;
		int level = hero.lvl;
		hero.earnExp(hero.maxExp(), PotionOfExperience.class);
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		assertTrue(hero.lvl > level);
		CombatEvents.XpGain g = CombatEvents.decodeXpGain(of(r, EventTypes.XP_GAIN).get(0), r);
		assertEquals(CombatEvents.XP_POTION, g.category);
		assertEquals(level, g.levelBefore);
		CombatEvents.LevelUp l = CombatEvents.decodeLevelUp(of(r, EventTypes.LEVEL_UP).get(0), r);
		assertEquals(level, l.oldLevel);
		assertEquals(hero.lvl, l.newLevel);
		CombatEvents.MaxHpChange m = CombatEvents.decodeMaxHpChange(of(r, EventTypes.MAX_HP_CHANGE).get(0), r);
		assertEquals(oldMax, m.oldMax);
		assertEquals(hero.HT, m.newMax);
		assertTrue("the HP that came with a higher maximum is not healing", of(r, EventTypes.HEAL).isEmpty());
	}

	@Test
	public void shieldGainsAreNamedByTheShieldEffect() throws Exception {
		RecorderTestSupport.Session s = startFight(60L);
		Hero hero = hero();
		ItemTestSupport.turn();
		Buff.affect(hero, Barrier.class).setShield(7);
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		List<ApdEvent> e = of(r, EventTypes.SHIELD_GAIN);
		assertEquals(1, e.size());
		CombatEvents.ShieldGain g = CombatEvents.decodeShieldGain(e.get(0), r);
		assertEquals(7, g.amount);
		assertEquals(7, g.shieldAfter);
		assertFalse(g.source.isEmpty());
		assertEquals(CombatEvents.FLAG_INFERRED, e.get(0).flags & CombatEvents.FLAG_INFERRED);
	}

	@Test
	public void hungerChangesAreExactAndStateTransitionsAreRecorded() throws Exception {
		RecorderTestSupport.Session s = startFight(61L);
		Hero hero = hero();
		Hunger hunger = hero.buff(Hunger.class);
		ItemTestSupport.turn();
		hunger.affectHunger(-320, true);         //hungrier by 320: over the HUNGRY mark
		ItemTestSupport.turn();
		hunger.satisfy(150);                     //a meal
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);

		List<ApdEvent> changes = of(r, EventTypes.HUNGER_CHANGE);
		assertEquals(2, changes.size());
		CombatEvents.HungerChange c1 = CombatEvents.decodeHungerChange(changes.get(0), r);
		assertEquals(-320, c1.energy);
		assertEquals(0, c1.before);
		assertEquals(320, c1.after);
		CombatEvents.HungerChange c2 = CombatEvents.decodeHungerChange(changes.get(1), r);
		assertEquals(150, c2.energy);
		assertEquals(320, c2.before);
		assertEquals(170, c2.after);

		List<ApdEvent> states = of(r, EventTypes.HUNGER_STATE);
		assertEquals(2, states.size());
		assertEquals(CombatEvents.HUNGER_HUNGRY, CombatEvents.decodeHungerState(states.get(0), r).state);
		assertEquals(CombatEvents.HUNGER_NORMAL, CombatEvents.decodeHungerState(states.get(1), r).state);
	}

	@Test
	public void deathAndRevivalHaveTheirOwnEvents() throws Exception {
		RecorderTestSupport.Session s = startFight(62L);
		Hero hero = hero();
		//Hero.die needs the game scene, so the hooks are called the way Hero.die calls them
		ApdCombat.onHeroDying(hero.buff(Hunger.class));
		hero.HP = hero.HT / 4;
		ApdCombat.onHeroRevived(ApdCombat.REVIVE_BLESSED_ANKH);
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		CombatEvents.HeroDeath d = CombatEvents.decodeHeroDeath(of(r, EventTypes.HERO_DEATH).get(0), r);
		assertEquals(CombatEvents.SRC_BUFF, d.cause.kind);
		CombatEvents.HeroRevived rv = CombatEvents.decodeHeroRevived(of(r, EventTypes.HERO_REVIVED).get(0), r);
		assertEquals(CombatEvents.REVIVE_BLESSED_ANKH, rv.kind);
		assertEquals(hero.HT / 4, rv.hpAfter);
		assertTrue("being revived is not a heal", of(r, EventTypes.HEAL).isEmpty());
	}

	@Test
	public void combatEventsCarryTheDepthAndCannotBeMistakenForAnotherLevel() throws Exception {
		RecorderTestSupport.Session s = startFight(63L);
		RecorderTestSupport.enterLevel(3);
		new QuietSprite(hero());
		Rat rat = rat(1, true);
		fightToTheDeath(hero(), rat);
		ReadResult r = ItemTestSupport.finish(s);
		for (CombatEvents.Attack a : attacks(r)) assertEquals(3, a.where.depth);
		assertNotEquals(0, damages(r).size());
	}

	@Test
	public void anUnrevealedMimicIsNamedAsTheChestThePlayerSees() throws Exception {
		RecorderTestSupport.Session s = startFight(64L);
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic mimic = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic();
		mimic.pos = hero().pos + 1;
		mimic.HP = mimic.HT = 20;
		new QuietSprite(mimic);
		mimic.alignment = com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.NEUTRAL;
		Dungeon.level.heroFOV[mimic.pos] = true;
		Dungeon.level.heroFOV[hero().pos] = true;
		String disguised = mimic.name();
		hero().attack(mimic);
		ReadResult r = ItemTestSupport.finish(s);
		CombatEvents.Attack a = attacks(r).get(0);
		assertEquals("the player is attacking what looks like a chest", disguised, a.defender.name);
		assertEquals(CombatEvents.KIND_NEUTRAL, a.defender.kind);
		//the true class is only in the internal detail
		assertTrue(attackDetails(r).get(0).defenderClass.endsWith("Mimic"));
		assertFalse(disguised.toLowerCase().contains("mimic"));
	}
}
