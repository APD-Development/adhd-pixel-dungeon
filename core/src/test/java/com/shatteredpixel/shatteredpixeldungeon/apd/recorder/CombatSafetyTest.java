package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.CombatTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.watabou.utils.Random;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

//Telemetry must never change a fight: same dice, same results, same game state, with the recorder on, off or failing.
public class CombatSafetyTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	//plays a scripted fight under a seeded generator and returns everything that happened, then the next random numbers
	private static List<String> transcript( boolean record, boolean failing ) throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		ApdRecorder.configure(s.root, record);
		RecorderTestSupport.newRun(s, 4242L);
		RecorderTestSupport.enterLevel(1);
		new QuietSprite(Dungeon.hero);
		Dungeon.hero.damageInterrupt = false;
		if (failing) ApdRecorder.failureInjection = new RuntimeException("injected");
		List<String> out = new ArrayList<>();
		Random.pushGenerator(987654321L);
		try {
			Hero hero = Dungeon.hero;
			Rat rat = null;
			for (int round = 0; round < 60; round++){
				if (rat == null || !rat.isAlive()){
					rat = new Rat();
					rat.pos = hero.pos + 1;
					new QuietSprite(rat);
					Dungeon.level.heroFOV[rat.pos] = true;
					Dungeon.level.heroFOV[hero.pos] = true;
				}
				boolean h = hero.attack(rat);
				boolean m = rat.isAlive() && rat.attack(hero);
				out.add(round + ": " + h + " " + m + " ratHP=" + rat.HP + " heroHP=" + hero.HP + " exp=" + hero.exp);
				hero.HP = hero.HT;
				ItemTestSupport.turn();
			}
			hero.damage(3, hero.buff(Hunger.class));
			out.add("heroHP=" + hero.HP);
			out.add("next=" + Random.Int() + "," + Random.Int() + "," + Random.Float());
		} finally {
			Random.popGenerator();
			ApdRecorder.reset();
		}
		return out;
	}

	@Test
	public void aRecordedFightIsIdenticalToAnUnrecordedOneIncludingTheNextRandomNumbers() throws Exception {
		List<String> off = transcript(false, false);
		List<String> on = transcript(true, false);
		assertEquals(off, on);
		assertTrue(off.size() > 60);
	}

	@Test
	public void aFailingRecorderDoesNotChangeAFightEither() throws Exception {
		List<String> off = transcript(false, false);
		List<String> failing = transcript(true, true);
		assertEquals(off, failing);
	}

	@Test
	public void repeatedFailuresSwitchCombatRecordingOffWithoutAffectingTheGame() throws Exception {
		RecorderTestSupport.Session s = startFight(71L);
		ApdRecorder.failureInjection = new RuntimeException("injected");
		Rat rat = rat(1, true);
		fightToTheDeath(hero(), rat);
		assertFalse(rat.isAlive());
		assertTrue(ApdRecorder.status().sessionDisabled);
		assertFalse(ApdCombat.active);
	}

	@Test
	public void oddArgumentsNeverCountAsFailures() throws Exception {
		RecorderTestSupport.Session s = startFight(72L);
		ApdCombat.onAttackRolled(null, null, true);
		ApdCombat.onAttackDetail(null, null, 1, 1, 1);
		ApdCombat.onMagicRoll(null, null, true);
		ApdCombat.onDamage(null, null, 0, 0, 0, 0, 0);
		ApdCombat.onCharDied(null, null);
		ApdCombat.onXp(null, 5, null);
		ApdCombat.onHungerChanged(0, 0, 0);
		ApdCombat.onHeroDying(null);
		ItemTestSupport.turn();
		assertEquals(0, ApdRecorder.status().errorCount);
		ItemTestSupport.finish(s);
	}


	//a scripted run of the game's own healing (potion, food, regeneration), under a seeded generator: every HP, and the next random numbers
	private static List<String> healingTranscript( boolean record, boolean failing ) throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		ApdRecorder.configure(s.root, record);
		RecorderTestSupport.newRun(s, 5151L);
		RecorderTestSupport.enterLevel(1);
		new QuietSprite(Dungeon.hero);
		Dungeon.hero.damageInterrupt = false;
		if (failing) ApdRecorder.failureInjection = new RuntimeException("injected");
		List<String> out = new ArrayList<>();
		Random.pushGenerator(135792468L);
		try {
			Hero hero = Dungeon.hero;
			for (int round = 0; round < 40; round++){
				hero.HP = Math.max(1, hero.HP - 7);
				ItemTestSupport.turn();
				if (round % 3 == 0){
					PotionOfHealing.heal(hero);
					Healing healing = hero.buff(Healing.class);
					for (int i = 0; i < 20 && healing != null && hero.buff(Healing.class) != null; i++) healing.act();
				} else if (round % 3 == 1){
					PhantomMeat.effect(hero);
				} else {
					Regeneration regen = Buff.affect(hero, Regeneration.class);
					for (int i = 0; i < 60; i++) regen.act();
				}
				out.add(round + ": heroHP=" + hero.HP + "/" + hero.HT);
				ItemTestSupport.turn();
			}
			out.add("next=" + Random.Int() + "," + Random.Int() + "," + Random.Float());
		} finally {
			Random.popGenerator();
			ApdRecorder.reset();
		}
		return out;
	}

	@Test
	public void healingIsIdenticalWithTheRecorderOnOffOrFailing() throws Exception {
		List<String> off = healingTranscript(false, false);
		assertEquals(off, healingTranscript(true, false));
		assertEquals(off, healingTranscript(true, true));
		assertTrue(off.size() > 40);
	}

	@Test
	public void oddHealArgumentsNeverCountAsFailures() throws Exception {
		RecorderTestSupport.Session s = startFight(74L);
		ApdCombat.onHeal(null, 0, null, 0);
		ApdCombat.onHeal(hero(), 0, null, -1);
		ApdCombat.onHeal(hero(), Integer.MIN_VALUE, "", Integer.MAX_VALUE);
		ApdCombat.onHeal(hero(), Integer.MAX_VALUE, "regen", 1);
		ItemTestSupport.turn();
		assertEquals(0, ApdRecorder.status().errorCount);
		ItemTestSupport.finish(s);
	}

	@Test
	public void combatAfterTheLoadedSaveIsAnAbandonedBranchAndTheLoadInventsNothing() throws Exception {
		RecorderTestSupport.Session s = startFight(73L);
		String id = ApdRecorder.currentRunId();
		Dungeon.saveAll();                                    //the state the game will come back to
		hero().damage(4, hero().buff(Hunger.class));          //play that is never saved
		Rat rat = rat(1, true);
		fightToTheDeath(hero(), rat);
		s.restartApp();
		Dungeon.loadGame(s.slot);                             //back to the save, at full health
		ItemTestSupport.turn();
		ItemTestSupport.turn();
		ReadResult r = s.read(id);
		RunTimeline t = RunTimeline.of(r.events);
		assertEquals(1, t.rollbackCount());

		int all = of(r, EventTypes.DAMAGE).size() + of(r, EventTypes.ATTACK).size() + of(r, EventTypes.KILL).size();
		assertTrue(all > 0);
		for (ApdEvent e : t.canonical()){
			assertTrue("combat from the abandoned branch leaked into the canonical timeline: " + EventTypes.name(e.type),
					e.type != EventTypes.DAMAGE && e.type != EventTypes.ATTACK && e.type != EventTypes.KILL
							&& e.type != EventTypes.DAMAGE_DETAIL && e.type != EventTypes.XP_GAIN);
		}
		//the hero was at full health in the loaded save: nothing is "healed" or "lost" by loading it
		for (ApdEvent e : t.canonical()){
			assertTrue(e.type != EventTypes.HEAL && e.type != EventTypes.HP_CHANGE_INFERRED);
		}
	}

	@Test
	public void recordingACrowdedFightIsCheapAndSmall() throws Exception {
		double offMicros = timePerAttack(false);
		double onMicros = timePerAttack(true);
		System.out.println("APD-PERF combat: " + String.format("%.1f", offMicros) + " us/attack without recording, "
				+ String.format("%.1f", onMicros) + " us/attack with recording");
		assertTrue("recording must not make an attack more than 1 ms slower, was " + (onMicros - offMicros) + " us", onMicros - offMicros < 1000);
	}

	private static double timePerAttack( boolean record ) throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		ApdRecorder.configure(s.root, record);
		RecorderTestSupport.newRun(s, 8080L);
		RecorderTestSupport.enterLevel(1);
		new QuietSprite(Dungeon.hero);
		Dungeon.hero.damageInterrupt = false;
		Hero hero = Dungeon.hero;
		Rat rat = null;
		int n = 4000;
		//warm up
		for (int i = 0; i < 300; i++){
			if (rat == null || !rat.isAlive()){
				rat = new Rat();
				rat.pos = hero.pos + 1;
				new QuietSprite(rat);
				Dungeon.level.heroFOV[rat.pos] = true;
			}
			hero.attack(rat);
		}
		long t0 = System.nanoTime();
		for (int i = 0; i < n; i++){
			if (rat == null || !rat.isAlive()){
				rat = new Rat();
				rat.pos = hero.pos + 1;
				new QuietSprite(rat);
				Dungeon.level.heroFOV[rat.pos] = true;
			}
			hero.attack(rat);
		}
		long t1 = System.nanoTime();
		if (record){
			ApdRecorder.flushForTests();
			long bytes = s.log(ApdRecorder.currentRunId()).length();
			System.out.println("APD-PERF combat log: " + bytes + " bytes for " + (n + 300) + " attacks (" + (bytes / (n + 300)) + " B/attack)");
		}
		ApdRecorder.reset();
		return (t1 - t0) / 1000.0 / n;
	}
}
