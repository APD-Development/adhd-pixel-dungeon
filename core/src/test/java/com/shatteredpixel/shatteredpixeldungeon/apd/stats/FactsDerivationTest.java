package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class FactsDerivationTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	//a small run log written through the real writer, counting events so a SAVE's index is known
	private final class Log {
		final RunLogWriter w;
		final File file;
		int events;
		long clock;

		Log( String id, TelemetryVersion v ){
			file = new File(tmp.getRoot(), id + ".apdlog");
			w = RunLogWriter.create(file, v, id, "test", 1);
		}

		Log start( String runId, boolean partial ){
			LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
			s.runId = runId; s.heroClass = "WARRIOR"; s.seed = 77; s.challenges = 2; s.exoticChance = 30; s.apdVersionName = "4.0.8-apd";
			LifecycleEvents.emitRunStart(w, clock++, partial, s);
			events++;
			return this;
		}

		Log malformedStart(){
			w.event(EventTypes.RUN_START).clock(clock++).emit();
			events++;
			return this;
		}

		Log recordedStart( String runId ){
			LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
			s.runId = runId; s.heroClass = "CLERIC"; s.seed = 991; s.challenges = 5;
			s.apdVersionName = "4.0.8-apd"; s.difficulty = 4; s.difficultyBalance = 1;
			s.customRules = CustomDungeonRules.defaults().preferenceText();
			LifecycleEvents.emitRunStart(w, clock++, false, s);
			events++;
			return this;
		}

		Log enter( int depth ){
			LifecycleEvents.LevelEnter l = new LifecycleEvents.LevelEnter();
			l.depth = depth;
			LifecycleEvents.emitLevelEnter(w, clock++, l);
			events++;
			return this;
		}

		//a short fight: the hero hits a rat for 5 and kills it, the rat hit the hero for 3
		Log fight(){
			CombatEvents.Attack a = new CombatEvents.Attack();
			a.attacker.kind = CombatEvents.KIND_HERO; a.defender.kind = CombatEvents.KIND_ENEMY; a.hit = true;
			CombatEvents.emitAttack(w, clock++, a);
			CombatEvents.Damage d = new CombatEvents.Damage();
			d.target.kind = CombatEvents.KIND_ENEMY; d.shown = 5;
			CombatEvents.emitDamage(w, clock++, d);
			CombatEvents.Attack b = new CombatEvents.Attack();
			b.attacker.kind = CombatEvents.KIND_ENEMY; b.defender.kind = CombatEvents.KIND_HERO; b.hit = false;
			CombatEvents.emitAttack(w, clock++, b);
			CombatEvents.Damage e = new CombatEvents.Damage();
			e.target.kind = CombatEvents.KIND_HERO; e.shown = 3;
			CombatEvents.emitDamage(w, clock++, e);
			CombatEvents.Kill k = new CombatEvents.Kill();
			k.victim.kind = CombatEvents.KIND_ENEMY; k.killer.kind = CombatEvents.SRC_CHAR; k.killer.fighterKind = CombatEvents.KIND_HERO;
			CombatEvents.emitKill(w, clock++, k);
			CombatEvents.XpGain x = new CombatEvents.XpGain();
			x.amount = 4;
			CombatEvents.emitXpGain(w, clock++, x);
			CombatEvents.emitLevelUp(w, clock++, new CombatEvents.LevelUp());
			CombatEvents.Heal h = new CombatEvents.Heal();
			h.amount = 6;
			CombatEvents.emitHeal(w, clock++, h);
			events += 8;
			return this;
		}

		Log items(){
			ItemEvents.Registered r = new ItemEvents.Registered();
			r.uid = "u1";
			ItemEvents.emitRegistered(w, clock++, 0, r);
			ItemEvents.Acquired a = new ItemEvents.Acquired();
			a.uid = "u1";
			ItemEvents.emitAcquired(w, clock++, 0, a);
			ItemEvents.Equip q = new ItemEvents.Equip();
			q.uid = "u1";
			ItemEvents.emitEquip(w, clock++, 0, true, q);
			events += 3;
			return this;
		}

		Log generated(){
			GenerationEvents.LevelGenerated g = new GenerationEvents.LevelGenerated();
			GenerationEvents.emitLevelGenerated(w, clock++, g);
			GenerationEvents.ItemGenerated i = new GenerationEvents.ItemGenerated();
			i.uid = "u9";
			GenerationEvents.emitItemGenerated(w, clock++, i);
			events += 2;
			return this;
		}

		Log gap(){
			w.event(EventTypes.GAP).clock(clock++).unsigned(EventTypes.GAP_DROPPED_EVENTS).unsigned(3).emit();
			events++;
			return this;
		}

		int save(){
			LifecycleEvents.emitSave(w, clock++, new LifecycleEvents.Save());
			return events++;
		}

		Log rollbackTo( int saveIndex ){
			LifecycleEvents.Rollback r = new LifecycleEvents.Rollback();
			r.kind = LifecycleEvents.ROLLBACK_MATCHED;
			r.matchedSaveIndex = saveIndex;
			LifecycleEvents.emitRollback(w, clock++, r);
			events++;
			return this;
		}

		Log end( int outcome ){
			LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
			e.outcome = outcome; e.cause = "Rat"; e.depth = 2; e.deepest = 3; e.heroLevel = 5; e.score = 1234; e.turns = 400;
			LifecycleEvents.emitRunEnd(w, clock++, e);
			events++;
			return this;
		}

		RunFacts facts( boolean seal ) throws Exception {
			w.close(seal);
			return RunFactsBuilder.fromLog(RunLogReader.read(file), file.getName().replace(".apdlog", ""));
		}
	}

	@Test
	public void aCompleteCurrentRunHasEveryFamilyOfFacts() throws Exception {
		RunFacts f = new Log("a", TelemetryVersion.CURRENT).start("run-a", false).enter(1).enter(2).generated().items().fight()
				.end(LifecycleEvents.OUTCOME_DIED).facts(true);
		assertEquals("sealed", f.textOr(FactKeys.STATUS, ""));
		assertEquals("run-a", f.textOr(FactKeys.RUN_ID, ""));
		assertEquals("WARRIOR", f.textOr(FactKeys.HERO_CLASS, ""));
		assertEquals(LifecycleEvents.OUTCOME_DIED, f.numOr(FactKeys.OUTCOME, -1));
		assertEquals(1234, f.numOr(FactKeys.SCORE, -1));
		assertEquals(3, f.numOr(FactKeys.DEEPEST, -1));
		assertEquals(2, f.numOr(FactKeys.LEVELS_ENTERED, -1));
		assertEquals(1, f.numOr(FactKeys.HERO_ATTACKS, -1));
		assertEquals(1, f.numOr(FactKeys.HERO_HITS, -1));
		assertEquals(1, f.numOr(FactKeys.ATTACKS_ON_HERO, -1));
		assertEquals(0, f.numOr(FactKeys.HITS_ON_HERO, -1));
		assertEquals(5, f.numOr(FactKeys.DAMAGE_DEALT, -1));
		assertEquals(3, f.numOr(FactKeys.DAMAGE_TAKEN, -1));
		assertEquals(1, f.numOr(FactKeys.HERO_KILLS, -1));
		assertEquals(4, f.numOr(FactKeys.XP_GAINED, -1));
		assertEquals(1, f.numOr(FactKeys.LEVEL_UPS, -1));
		assertEquals(6, f.numOr(FactKeys.HEALED, -1));
		assertEquals(Fact.PROV_INFERRED, f.get(FactKeys.HEALED).prov);   //healing is inferred, and says so
		assertEquals(Fact.PROV_RECORDED, f.get(FactKeys.DAMAGE_DEALT).prov);
		assertEquals(1, f.numOr(FactKeys.ITEMS_EQUIPPED, -1));
		assertEquals(1, f.numOr(FactKeys.GEN_ITEMS, -1));
		assertEquals(1, f.numOr(FactKeys.GEN_LEVELS, -1));
	}

	@Test
	public void whatAnOlderTelemetryVersionCouldNotRecordIsUnknownNotZero() throws Exception {
		//telemetry 1.1 knew about the lifecycle only
		RunFacts f = new Log("old", new TelemetryVersion(1, 1)).start("run-old", false).enter(1).end(LifecycleEvents.OUTCOME_WON).facts(true);
		assertEquals(LifecycleEvents.OUTCOME_WON, f.numOr(FactKeys.OUTCOME, -1));
		assertEquals(1, f.numOr(FactKeys.LEVELS_ENTERED, -1));
		for (String key : new String[]{FactKeys.HERO_ATTACKS, FactKeys.DAMAGE_DEALT, FactKeys.HERO_KILLS, FactKeys.HEALED,
				FactKeys.ITEMS_ACQUIRED, FactKeys.ITEMS_EQUIPPED, FactKeys.GEN_ITEMS, FactKeys.GEN_LEVELS}){
			assertFalse(key + " must be unknown for telemetry 1.1, not zero", f.isKnown(key));
		}
		//1.2 knew items but not combat or generation
		RunFacts g = new Log("v12", new TelemetryVersion(1, 2)).start("run-12", false).items().end(LifecycleEvents.OUTCOME_DIED).facts(true);
		assertTrue(g.isKnown(FactKeys.ITEMS_ACQUIRED));
		assertFalse(g.isKnown(FactKeys.HERO_ATTACKS));
		assertFalse(g.isKnown(FactKeys.GEN_ITEMS));
	}

	@Test
	public void aRunThatRecordedNothingOfAKindHasAKnownZero() throws Exception {
		//a current-version run with no fights: zero attacks is a fact, because the recorder would have seen them
		RunFacts f = new Log("calm", TelemetryVersion.CURRENT).start("run-calm", false).enter(1).end(LifecycleEvents.OUTCOME_ABANDONED).facts(true);
		assertTrue(f.isKnown(FactKeys.HERO_ATTACKS));
		assertEquals(0, f.numOr(FactKeys.HERO_ATTACKS, -1));
	}

	@Test
	public void gapsAndPartialLogsMakeCountsLowerBounds() throws Exception {
		RunFacts gap = new Log("gap", TelemetryVersion.CURRENT).start("run-g", false).fight().gap().fight().end(LifecycleEvents.OUTCOME_DIED).facts(true);
		assertEquals(2, gap.numOr(FactKeys.HERO_ATTACKS, -1));
		assertEquals(Fact.PROV_INCOMPLETE, gap.get(FactKeys.HERO_ATTACKS).prov);
		//the setup facts are still exact
		assertEquals(Fact.PROV_RECORDED, gap.get(FactKeys.SCORE).prov);

		RunFacts partial = new Log("part", TelemetryVersion.CURRENT).start("run-p", true).fight().facts(false);
		assertEquals(1, partial.numOr(FactKeys.PARTIAL, 0));
		assertEquals(Fact.PROV_INCOMPLETE, partial.get(FactKeys.HERO_ATTACKS).prov);
	}

	@Test
	public void anOpenRunHasNoResult() throws Exception {
		RunFacts f = new Log("open", TelemetryVersion.CURRENT).start("run-o", false).enter(1).fight().facts(false);
		assertEquals("open", f.textOr(FactKeys.STATUS, ""));
		assertFalse(f.isKnown(FactKeys.OUTCOME));
		assertFalse(f.isKnown(FactKeys.SCORE));
		assertTrue(f.isKnown(FactKeys.HERO_ATTACKS));
	}

	@Test
	public void rolledBackPlayIsLeftOutOfTheFactsButCounted() throws Exception {
		Log l = new Log("rb", TelemetryVersion.CURRENT).start("run-r", false).fight();
		int save = l.save();
		l.fight().fight();                //two more fights that are then rolled back
		l.rollbackTo(save).fight();      //and one after
		RunFacts f = l.end(LifecycleEvents.OUTCOME_DIED).facts(true);
		assertEquals("the rolled back fights do not count", 2, f.numOr(FactKeys.HERO_ATTACKS, -1));
		assertTrue(f.numOr(FactKeys.ABANDONED_EVENTS, 0) >= 16);
		assertEquals(1, f.numOr(FactKeys.ROLLBACKS, 0));
	}

	@Test
	public void aRunEndWithOnlyAnOutcomeKeepsTheOtherResultFieldsUnknown() throws Exception {
		Log l = new Log("adopt", TelemetryVersion.CURRENT).start("run-x", false);
		LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
		e.outcome = LifecycleEvents.OUTCOME_ABANDONED;
		LifecycleEvents.emitRunEnd(l.w, 9, e, LifecycleEvents.FLAG_DETAILS_UNKNOWN);
		RunFacts f = l.facts(true);
		assertEquals(LifecycleEvents.OUTCOME_ABANDONED, f.numOr(FactKeys.OUTCOME, -1));
		assertFalse(f.isKnown(FactKeys.SCORE));
		assertFalse(f.isKnown(FactKeys.DEEPEST));
		assertFalse(f.isKnown(FactKeys.TURNS));
	}


	@Test
	public void malformedFirstStartDoesNotHideLaterReadableStart() throws Exception {
		RunFacts f = new Log("malformed-start", TelemetryVersion.CURRENT)
				.malformedStart().recordedStart("run-readable").end(LifecycleEvents.OUTCOME_ABANDONED).facts(true);
		assertEquals("run-readable", f.textOr(FactKeys.RUN_ID, ""));
		assertEquals("CLERIC", f.textOr(FactKeys.HERO_CLASS, ""));
		assertEquals(991, f.numOr(FactKeys.SEED, -1));
		assertEquals(4, f.numOr(FactKeys.DIFFICULTY, -1));
		assertEquals(15, f.numOr(CustomRuleFacts.key("UPGRADE_SCROLLS"), -1));
		assertEquals(Fact.PROV_INCOMPLETE, f.get(FactKeys.HERO_ATTACKS).prov);
	}


	@Test
	public void anUnreadableLogGivesARowThatSaysSo() throws Exception {
		File f = new File(tmp.getRoot(), "junk.apdlog");
		LogTestSupport.write(f, new byte[]{1, 2, 3, 4, 5});
		RunFacts r = RunFactsBuilder.fromLog(RunLogReader.read(f), "junk");
		assertEquals("unreadable", r.textOr(FactKeys.STATUS, ""));
		assertFalse(r.isKnown(FactKeys.OUTCOME));
	}
}
