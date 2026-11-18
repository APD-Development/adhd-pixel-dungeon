/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Derives the facts of one run from its raw log. Pure: nothing is stored and nothing is guessed.
//
//Rules:
// - only the canonical timeline counts (rolled-back play is left out, its size is a fact of its own)
// - a statistic that the run's telemetry version could not record is UNKNOWN, not zero
// - when the log has gaps, or starts in the middle of a run, counts are marked INCOMPLETE (the real number may be higher)
// - an open run has no result
public final class RunFactsBuilder {

	private RunFactsBuilder(){}

	private static final TelemetryVersion V_ITEMS = new TelemetryVersion(1, 2);
	private static final TelemetryVersion V_COMBAT = new TelemetryVersion(1, 3);
	private static final TelemetryVersion V_GENERATION = new TelemetryVersion(1, 4);
	private static final TelemetryVersion V_LINK = new TelemetryVersion(1, 5);
	private static final TelemetryVersion V_EXACT_HEAL = new TelemetryVersion(1, 6);

	public static RunFacts fromLog( ReadResult r, String logId ){
		RunFacts f = new RunFacts(RunFacts.SOURCE_APD, logId);
		f.text(FactKeys.LOG_ID, logId, Fact.PROV_RECORDED);
		if (r == null || r.header == null || r.newerThanReader()){
			f.text(FactKeys.STATUS, "unreadable", Fact.PROV_RECORDED);
			if (r != null && r.header != null) f.text(FactKeys.TELEMETRY, r.header.telemetryVersion.toString(), Fact.PROV_RECORDED);
			return f;
		}
		TelemetryVersion v = r.header.telemetryVersion;
		f.text(FactKeys.TELEMETRY, v.toString(), Fact.PROV_RECORDED);
		f.text(FactKeys.STATUS, r.status == ReadStatus.SEALED ? (r.damagedBytes() > 0 ? "damaged" : "sealed")
				: r.status == ReadStatus.UNSEALED ? "open" : "damaged", Fact.PROV_RECORDED);

		RunTimeline timeline = RunTimeline.of(r.events);
		List<ApdEvent> events = timeline.canonical();
		f.num(FactKeys.ABANDONED_EVENTS, timeline.abandonedCount(), Fact.PROV_RECORDED);
		f.num(FactKeys.ROLLBACKS, timeline.rollbackCount(), Fact.PROV_RECORDED);

		boolean partial = false;
		for (ApdEvent e : events){
			if (e.type == EventTypes.RUN_START && LifecycleEvents.isPartial(e)) partial = true;
		}
		if (partial) f.num(FactKeys.PARTIAL, 1, Fact.PROV_RECORDED);
		//counts are lower bounds when events were dropped or the log starts mid-run
		int countProv = (r.gapEvents > 0 || partial || !r.status.isClean()) ? Fact.PROV_INCOMPLETE : Fact.PROV_RECORDED;

		boolean wantItems = v.compareTo(V_ITEMS) >= 0, wantCombat = v.compareTo(V_COMBAT) >= 0,
				wantGen = v.compareTo(V_GENERATION) >= 0, wantLink = v.compareTo(V_LINK) >= 0,
				wantExactHeal = v.compareTo(V_EXACT_HEAL) >= 0;
		//a copy made for an archive of a run that was not finished has no INTERNAL events: its sources are unknown, not zero
		boolean redacted = r.header.writerBuild != null && r.header.writerBuild.startsWith(LogHeader.REDACTED_PREFIX);
		long healedInferred = 0;
		long[] healedFrom = new long[CombatEvents.HEAL_CATEGORIES.length];

		long levelsEntered = 0, deepest = -1;
		long heroAttacks = 0, heroHits = 0, onHero = 0, hitsOnHero = 0, dealt = 0, taken = 0, heroKills = 0, enemyDeaths = 0,
				xp = 0, levelUps = 0, healed = 0, deaths = 0, revivals = 0, equipEvents = 0, genLevels = 0, genItems = 0, genMobs = 0;
		Set<String> registered = new HashSet<>(), acquired = new HashSet<>(), equipped = new HashSet<>();
		long generatedUpgrades=0,generatedEquipment=0;
		Set<String> countedGenerated=new HashSet<>();
		LifecycleEvents.Checkpoint lastCheckpoint = null;
		LifecycleEvents.RunEnd end = null;
		boolean endDetails = false;
		boolean started = false;

		for (ApdEvent e : events){
			try {
				switch (e.type){
					case EventTypes.RUN_START: {
						if (started) break;  //later readable segments of the same log never relabel the first
						LifecycleEvents.RunStart s = LifecycleEvents.decodeRunStart(e, r);
						// A malformed typed start contributes no setup facts, but must not hide a later readable start.
						started = true;
						f.text(FactKeys.RUN_ID, s.runId, Fact.PROV_RECORDED);
						f.num(FactKeys.SEGMENT, s.segment, Fact.PROV_RECORDED);
						f.text(FactKeys.APD_VERSION, s.apdVersionName, Fact.PROV_RECORDED);
						f.num(FactKeys.INITIAL_VERSION, s.initialVersion, Fact.PROV_RECORDED);
						//a redacted copy (from an archive of a run that was not finished) has its seed taken out: unknown, not 0
						boolean redactedCopy = r.header != null && r.header.writerBuild != null && r.header.writerBuild.startsWith(LogHeader.REDACTED_PREFIX);
						if (!redactedCopy){
							f.num(FactKeys.SEED, s.seed, Fact.PROV_RECORDED);
							f.text(FactKeys.CUSTOM_SEED, s.customSeedText, Fact.PROV_RECORDED);
						}
						f.num(FactKeys.SEED_TYPE, s.seedType, Fact.PROV_RECORDED);
						f.num(FactKeys.DAILY, s.seedType == LifecycleEvents.SEED_DAILY || s.seedType == LifecycleEvents.SEED_DAILY_REPLAY ? 1 : 0, Fact.PROV_RECORDED);
						f.num(FactKeys.EXTERNAL_MODS, s.externalModsPresent ? 1 : 0, Fact.PROV_RECORDED);
						f.num(FactKeys.CHALLENGES, s.challenges, Fact.PROV_RECORDED);
						f.text(FactKeys.HERO_CLASS, s.heroClass, Fact.PROV_RECORDED);
						//a run whose rules were not recorded (saved before they were part of the run) has neither: unknown, never today's setting
						if (!s.rulesetId.isEmpty()) f.text(FactKeys.RULESET, s.rulesetId, Fact.PROV_RECORDED);
						if (s.exoticChance != LifecycleEvents.EXOTIC_NOT_RECORDED) f.num(FactKeys.EXOTIC_CHANCE, s.exoticChance, Fact.PROV_RECORDED);
						//only a run that recorded its difficulty has the fact: for an older run it is unknown, never "3"
						if (s.difficulty > 0){
							f.num(FactKeys.DIFFICULTY, s.difficulty, Fact.PROV_RECORDED);
							f.num(FactKeys.DIFFICULTY_BALANCE, s.difficultyBalance, Fact.PROV_RECORDED);
						}
						CustomRuleFacts.record(f, s);
						f.num(FactKeys.START_MILLIS, s.realTimeMillis, Fact.PROV_RECORDED);
						break;
					}
					case EventTypes.RUN_END:
						end = LifecycleEvents.decodeRunEnd(e, r);
						endDetails = (e.flags & LifecycleEvents.FLAG_DETAILS_UNKNOWN) == 0;
						break;
					case EventTypes.LEVEL_ENTER: {
						LifecycleEvents.LevelEnter l = LifecycleEvents.decodeLevelEnter(e, r);
						levelsEntered++;
						if (l.branch == 0 && l.depth > deepest) deepest = l.depth;
						break;
					}
					case EventTypes.CHECKPOINT:
						lastCheckpoint = LifecycleEvents.decodeCheckpoint(e, r);
						break;
					case EventTypes.ATTACK: {
						CombatEvents.Attack a = CombatEvents.decodeAttack(e, r);
						if (a.attacker.kind == CombatEvents.KIND_HERO){
							heroAttacks++;
							if (a.hit) heroHits++;
						} else if (a.defender.kind == CombatEvents.KIND_HERO){
							onHero++;
							if (a.hit) hitsOnHero++;
						}
						break;
					}
					case EventTypes.DAMAGE: {
						CombatEvents.Damage d = CombatEvents.decodeDamage(e, r);
						if (d.target.kind == CombatEvents.KIND_HERO) taken += d.shown;
						else if (d.target.kind == CombatEvents.KIND_ENEMY) dealt += d.shown;
						break;
					}
					case EventTypes.KILL: {
						CombatEvents.Kill k = CombatEvents.decodeKill(e, r);
						if (k.victim.kind == CombatEvents.KIND_ENEMY){
							enemyDeaths++;
							if (k.killer.kind == CombatEvents.SRC_CHAR && k.killer.fighterKind == CombatEvents.KIND_HERO) heroKills++;
						}
						break;
					}
					case EventTypes.XP_GAIN:
						xp += CombatEvents.decodeXpGain(e, r).amount;
						break;
					case EventTypes.LEVEL_UP:
						levelUps++;
						break;
					case EventTypes.HEAL: {
						int amount = Math.max(0, CombatEvents.decodeHeal(e, r).amount);
						healed += amount;
						if ((e.flags & CombatEvents.FLAG_INFERRED) != 0) healedInferred += amount;
						break;
					}
					case EventTypes.HEAL_DETAIL: {
						CombatEvents.HealDetail d = CombatEvents.decodeHealDetail(e, r);
						String cat = CombatEvents.healCategory(d.source);
						for (int c = 0; c < healedFrom.length; c++) if (CombatEvents.HEAL_CATEGORIES[c].equals(cat)) healedFrom[c] += d.applied();
						break;
					}
					case EventTypes.HERO_DEATH:
						deaths++;
						break;
					case EventTypes.HERO_REVIVED:
						revivals++;
						break;
					case EventTypes.ITEM_REGISTERED:
						registered.add(ItemEvents.decodeRegistered(e, r).uid);
						break;
					case EventTypes.ITEM_ACQUIRED:
						acquired.add(ItemEvents.decodeAcquired(e, r).uid);
						break;
					case EventTypes.ITEM_EQUIPPED:
						equipped.add(ItemEvents.decodeEquip(e, r).uid);
						equipEvents++;
						break;
					case EventTypes.LEVEL_GENERATED:
						genLevels++;
						break;
					case EventTypes.ITEM_GENERATED:
						genItems++;
                        GenerationEvents.ItemGenerated generated=GenerationEvents.decodeItemGenerated(e,r);
                        if(generated.uid.isEmpty() || countedGenerated.add(generated.uid)){
                            long quantity=Math.max(0,generated.quantity);
                            if(generated.trueClass.endsWith(".ScrollOfUpgrade"))generatedUpgrades+=quantity;
                            String prefix="com.shatteredpixel.shatteredpixeldungeon.items.";
                            if(generated.trueClass.startsWith(prefix+"weapon.") || generated.trueClass.startsWith(prefix+"armor.")
                                || generated.trueClass.startsWith(prefix+"rings.") || generated.trueClass.startsWith(prefix+"artifacts."))generatedEquipment+=quantity;
                        }
                        break;
					case EventTypes.MOB_GENERATED:
						genMobs++;
						break;
					case EventTypes.RANKING_LINK:
						if (wantLink){
							LegacyEvents.RankingLink l = LegacyEvents.decodeRankingLink(e, r);
							if (!l.gameId.isEmpty()) f.text(FactKeys.RANKING_ID, l.gameId, Fact.PROV_RECORDED);
						}
						break;
					default:
				}
			} catch (RuntimeException bad){
				//an event this reader cannot decode contributes nothing; the facts stay lower bounds
				countProv = Fact.PROV_INCOMPLETE;
			}
		}

		if (end != null){
			f.num(FactKeys.OUTCOME, end.outcome, Fact.PROV_RECORDED);
			if (end.ascendedKnown) f.num("run.ascended", end.ascended ? 1 : 0, Fact.PROV_RECORDED);
			if (endDetails){
				if (!end.cause.isEmpty()) f.text(FactKeys.CAUSE, end.cause, Fact.PROV_RECORDED);
				f.num(FactKeys.DEPTH, end.depth, Fact.PROV_RECORDED);
				f.num(FactKeys.DEEPEST, end.deepest, Fact.PROV_RECORDED);
				f.num(FactKeys.HERO_LEVEL, end.heroLevel, Fact.PROV_RECORDED);
				f.num(FactKeys.SCORE, end.score, Fact.PROV_RECORDED);
				CustomRuleFacts.preserveScore(f,end);
                if(f.isKnown(CustomRuleFacts.ADJUSTED_SCORE))f.num(FactKeys.SCORE,f.numOr(CustomRuleFacts.ADJUSTED_SCORE,end.score),Fact.PROV_RECORDED);
				f.num(FactKeys.TURNS, end.turns, Fact.PROV_RECORDED);
			}
		}
		if (started){
			f.num(FactKeys.LEVELS_ENTERED, levelsEntered, countProv);
			if (deepest >= 0) f.num(FactKeys.DEEPEST_ENTERED, deepest, countProv);
		}
		if (lastCheckpoint != null){
			f.num(FactKeys.FINAL_HERO_LEVEL, lastCheckpoint.heroLevel, Fact.PROV_RECORDED);
			f.num(FactKeys.FINAL_HP_MAX, lastCheckpoint.hpMax, Fact.PROV_RECORDED);
			f.num(FactKeys.FINAL_STRENGTH, lastCheckpoint.strength, Fact.PROV_RECORDED);
			f.num(FactKeys.FINAL_GOLD, lastCheckpoint.gold, Fact.PROV_RECORDED);
			if (!lastCheckpoint.subClass.isEmpty()) f.text(FactKeys.FINAL_SUBCLASS, lastCheckpoint.subClass, Fact.PROV_RECORDED);
		}
		if (wantCombat){
			f.num(FactKeys.HERO_ATTACKS, heroAttacks, countProv);
			f.num(FactKeys.HERO_HITS, heroHits, countProv);
			f.num(FactKeys.ATTACKS_ON_HERO, onHero, countProv);
			f.num(FactKeys.HITS_ON_HERO, hitsOnHero, countProv);
			f.num(FactKeys.DAMAGE_DEALT, dealt, countProv);
			f.num(FactKeys.DAMAGE_TAKEN, taken, countProv);
			f.num(FactKeys.HERO_KILLS, heroKills, countProv);
			f.num(FactKeys.ENEMY_DEATHS, enemyDeaths, countProv);
			f.num(FactKeys.XP_GAINED, xp, countProv);
			f.num(FactKeys.LEVEL_UPS, levelUps, countProv);
			//Exact when the run was recorded with exact healing (1.6) and nothing was left to infer; a lower bound if events were lost
			int healProv = countProv == Fact.PROV_INCOMPLETE ? Fact.PROV_INCOMPLETE
					: (wantExactHeal && healedInferred == 0 ? Fact.PROV_RECORDED : Fact.PROV_INFERRED);
			f.num(FactKeys.HEALED, healed, healProv);
			f.num(FactKeys.HEALED_INFERRED, healedInferred, countProv == Fact.PROV_INCOMPLETE ? Fact.PROV_INCOMPLETE : Fact.PROV_RECORDED);
			if (wantExactHeal && !redacted){
				for (int c = 0; c < healedFrom.length; c++) f.num(FactKeys.healedFrom(CombatEvents.HEAL_CATEGORIES[c]), healedFrom[c], countProv);
			}
			f.num(FactKeys.HERO_DEATHS, deaths, countProv);
			f.num(FactKeys.REVIVALS, revivals, countProv);
		}
		if (wantItems){
			f.num(FactKeys.ITEMS_REGISTERED, registered.size(), countProv);
			f.num(FactKeys.ITEMS_ACQUIRED, acquired.size(), countProv);
			f.num(FactKeys.ITEMS_EQUIPPED, equipped.size(), countProv);
			f.num(FactKeys.EQUIP_EVENTS, equipEvents, countProv);
		}
		if (wantGen){
			f.num(FactKeys.GEN_LEVELS, genLevels, countProv);
			f.num(FactKeys.GEN_ITEMS, genItems, countProv);
			f.num(FactKeys.GEN_MOBS, genMobs, countProv);
            if(!redacted){f.num(CustomRuleFacts.GENERATED_UPGRADES,generatedUpgrades,countProv);f.num(CustomRuleFacts.GENERATED_EQUIPMENT,generatedEquipment,countProv);}
		}
		return f;
	}
}


