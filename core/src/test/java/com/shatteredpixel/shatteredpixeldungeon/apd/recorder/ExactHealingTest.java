package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.CombatTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;

import org.junit.After;
import org.junit.Test;

import java.util.List;

//Telemetry 1.6: the game's own healing code reports what it really healed, and the look at the hero between turns only fills in what the
//hooks do not know about.
public class ExactHealingTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	private static List<CombatEvents.Heal> heals( ReadResult r ){
		List<CombatEvents.Heal> out = new java.util.ArrayList<>();
		for (ApdEvent e : of(r, EventTypes.HEAL)) out.add(CombatEvents.decodeHeal(e, r));
		return out;
	}

	private static List<CombatEvents.HealDetail> details( ReadResult r ){
		List<CombatEvents.HealDetail> out = new java.util.ArrayList<>();
		for (ApdEvent e : of(r, EventTypes.HEAL_DETAIL)) out.add(CombatEvents.decodeHealDetail(e, r));
		return out;
	}

	private static boolean inferred( ApdEvent e ){
		return (e.flags & CombatEvents.FLAG_INFERRED) != 0;
	}

	@Test
	public void aReportedHealIsExactNamesItsSourceInTheHiddenEventAndIsNotCountedAgain() throws Exception {
		RecorderTestSupport.Session s = startFight(901L);
		Hero hero = hero();
		hero.damage(5, hero.buff(Hunger.class));
		ItemTestSupport.turn();
		int before = hero.HP;
		hero.HP += 4;
		ApdCombat.onHeal(hero, before, ApdCombat.HEAL_PLANT_SUNGRASS, 4);
		ItemTestSupport.turn();       //the look between turns must not count it a second time
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);

		List<ApdEvent> events = of(r, EventTypes.HEAL);
		assertEquals("one heal, not one from the hook and one from the look", 1, events.size());
		assertFalse("a reported heal is exact: no INFERRED flag", inferred(events.get(0)));
		CombatEvents.Heal h = CombatEvents.decodeHeal(events.get(0), r);
		assertEquals(4, h.amount);
		assertEquals(before + 4, h.hpAfter);
		assertEquals("the visible event does not carry the source's request", -1, h.overheal);

		List<CombatEvents.HealDetail> d = details(r);
		assertEquals(1, d.size());
		assertEquals(ApdCombat.HEAL_PLANT_SUNGRASS, d.get(0).source);
		assertEquals(4, d.get(0).requested);
		assertEquals(4, d.get(0).applied());
		assertEquals("the source is hidden information", com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes.Visibility.INTERNAL,
				EventTypes.visibility(EventTypes.HEAL_DETAIL));
		assertTrue(of(r, EventTypes.HP_CHANGE_INFERRED).isEmpty());
	}

	@Test
	public void theAmountIsWhatTheGameReallyAppliedNotWhatWasAskedFor() throws Exception {
		RecorderTestSupport.Session s = startFight(902L);
		Hero hero = hero();
		hero.damage(3, hero.buff(Hunger.class));
		ItemTestSupport.turn();
		int before = hero.HP;
		int ask = 50;
		hero.HP = Math.min(hero.HT, hero.HP + ask);   //capped by the maximum, like the game's heals
		ApdCombat.onHeal(hero, before, ApdCombat.HEAL_FOOD_PASTY, ask);
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		assertEquals(3, heals(r).get(0).amount);
		CombatEvents.HealDetail d = details(r).get(0);
		assertEquals(3, d.applied());
		assertEquals("what it asked for is kept, so the overheal can be told", 50, d.requested);
	}

	@Test
	public void aHealTheHooksDoNotKnowAboutIsStillNoticedAndMarkedInferredBesideExactOnes() throws Exception {
		RecorderTestSupport.Session s = startFight(903L);
		Hero hero = hero();
		hero.damage(5, hero.buff(Hunger.class));
		ItemTestSupport.turn();
		int b1 = hero.HP;
		hero.HP += 2;                                         //an unhooked heal ...
		int b2 = hero.HP;
		hero.HP += 3;                                         //... and a hooked one in the same turn
		ApdCombat.onHeal(hero, b2, ApdCombat.HEAL_REGEN, -1);
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);

		int exact = 0, guessed = 0;
		for (ApdEvent e : of(r, EventTypes.HEAL)){
			int amount = CombatEvents.decodeHeal(e, r).amount;
			if (inferred(e)) guessed += amount; else exact += amount;
		}
		assertEquals("the hooked heal is exact", 3, exact);
		assertEquals("nothing is lost: the rest is inferred", 2, guessed);
		assertEquals(1, details(r).size());
		assertTrue(b1 < b2);
	}

	@Test
	public void aHealEarlierInTheTurnIsNotSwallowedByLaterDamageAnyMore() throws Exception {
		RecorderTestSupport.Session s = startFight(904L);
		Hero hero = hero();
		hero.damage(5, hero.buff(Hunger.class));
		ItemTestSupport.turn();
		int before = hero.HP;
		hero.HP += 3;
		ApdCombat.onHeal(hero, before, ApdCombat.HEAL_FOOD_SUPPLY_RATION, 5);
		hero.damage(2, hero.buff(Hunger.class));              //the damage hook resets the look's baseline
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		assertEquals(1, heals(r).size());
		assertEquals(3, heals(r).get(0).amount);
		assertTrue("damage the game explained is not an unexplained change", of(r, EventTypes.HP_CHANGE_INFERRED).isEmpty());
	}

	@Test
	public void healingOtherCreaturesAndHealsThatChangeNothingAreNotRecorded() throws Exception {
		RecorderTestSupport.Session s = startFight(905L);
		Hero hero = hero();
		Rat rat = rat(1, true);
		int ratBefore = rat.HP;
		rat.HP = Math.min(rat.HT, rat.HP + 1);
		ApdCombat.onHeal(rat, ratBefore, ApdCombat.HEAL_WAND_TRANSFUSION, 1);
		ApdCombat.onHeal(hero, hero.HP, ApdCombat.HEAL_REGEN, 1);   //at full HP nothing was applied
		ApdCombat.onHeal(null, 0, ApdCombat.HEAL_REGEN, 1);
		ApdCombat.onHeal(hero, hero.HP + 5, "regen", 1);            //a before-HP above the current one is nonsense: nothing healed
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);
		assertTrue(of(r, EventTypes.HEAL).isEmpty());
		assertTrue(of(r, EventTypes.HEAL_DETAIL).isEmpty());
	}

	@Test
	public void theGamesOwnHealingCodeReportsThroughItsHooks() throws Exception {
		RecorderTestSupport.Session s = startFight(906L);
		Hero hero = hero();
		hero.HP -= hero.HT / 2;   //a hit that big makes the game flash the screen, which needs a scene: lower the HP directly
		ItemTestSupport.turn();

		//the Healing effect (Potion of Healing): ticks until the heal is used up
		PotionOfHealing.heal(hero);
		Healing healing = hero.buff(Healing.class);
		for (int i = 0; i < 50 && hero.buff(Healing.class) != null && hero.HP < hero.HT; i++) healing.act();
		ItemTestSupport.turn();

		//food that heals a quarter of the maximum
		hero.HP -= hero.HT / 2;
		ItemTestSupport.turn();
		PhantomMeat.effect(hero);
		ItemTestSupport.turn();

		//natural regeneration
		hero.HP -= 5;
		ItemTestSupport.turn();
		Regeneration regen = Buff.affect(hero, Regeneration.class);
		int hp = hero.HP;
		for (int i = 0; i < 400 && hero.HP == hp; i++) regen.act();
		ItemTestSupport.turn();

		//well fed
		hero.HP -= 3;
		ItemTestSupport.turn();
		WellFed fed = Buff.affect(hero, WellFed.class);
		fed.reset();                  // what eating does: 450 turns, one HP every 18
		hp = hero.HP;
		for (int i = 0; i < 40 && hero.HP == hp; i++) fed.act();
		ItemTestSupport.turn();

		ReadResult r = ItemTestSupport.finish(s);
		java.util.Set<String> sources = new java.util.HashSet<>();
		for (CombatEvents.HealDetail d : details(r)) sources.add(d.source);
		assertTrue(sources.toString(), sources.contains(ApdCombat.HEAL_POTION_HEALING));
		assertTrue(sources.toString(), sources.contains(ApdCombat.HEAL_FOOD_PHANTOM_MEAT));
		assertTrue(sources.toString(), sources.contains(ApdCombat.HEAL_REGEN));
		assertTrue(sources.toString(), sources.contains(ApdCombat.HEAL_FOOD_WELL_FED));
		//and none of it needed guessing
		for (ApdEvent e : of(r, EventTypes.HEAL)) assertFalse("a healed amount the hooks reported must not be inferred", inferred(e));
		//every exact heal has its detail, and they agree
		assertEquals(of(r, EventTypes.HEAL).size(), of(r, EventTypes.HEAL_DETAIL).size());
		List<CombatEvents.Heal> h = heals(r);
		List<CombatEvents.HealDetail> d = details(r);
		for (int i = 0; i < h.size(); i++){
			assertEquals("the detail repeats what its heal says was applied", d.get(i).applied(), h.get(i).amount);
		}
	}
}
