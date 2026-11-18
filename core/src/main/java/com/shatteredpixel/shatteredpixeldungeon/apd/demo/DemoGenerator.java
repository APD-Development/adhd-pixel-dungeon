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

package com.shatteredpixel.shatteredpixeldungeon.apd.demo;

import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.io.File;
import java.io.IOException;
import java.util.Random;

//Writes the INDEV-ONLY demo history (see DemoData) through the real log writer, so the demo logs go through exactly the same
//reading, row building, merging and hidden-information masking as real ones. It is deterministic: the same call writes the same
//history. It uses its own java.util.Random, never the game's random numbers.
//
//The history is varied on purpose: every class, wins, deaths and abandoned runs, shallow to full-depth runs, short to very long
//runs, several challenge sets and rulesets (most match the ruleset given, a few do not), items of every kind that have views,
//enemies of every depth, runs recorded by older telemetry (so some columns are unknown), runs that were resumed in several
//segments, runs that only started recording part-way (lower bounds), runs with a gap, a run that changed ruleset part-way, and
//runs that are NOT finished (so the hidden-information rules have something to protect).
public final class DemoGenerator {

	public static final int RUNS = 72;
	//enough for every kind of view (wins and deaths, several classes, items, enemies, an unfinished run) without being a lot to read
	public static final int SAMPLE_RUNS = 30;

	private static final String[] CLASSES = { "WARRIOR", "MAGE", "ROGUE", "HUNTRESS", "DUELIST" };
	private static final int[] CHALLENGE_SETS = { 0, 0, 0, 0, 1, 2, 4, 8, 16, 3, 5, 32, 64 };
	private static final int[] EXOTIC = { 0, 30, 30, 30, 100 };

	//{class, shown name when unidentified, true name, category}
	private static final Object[][] WEAPONS = {
			{ "items.weapon.melee.Sword", "sword", ItemEvents.CAT_WEAPON }, { "items.weapon.melee.Mace", "mace", ItemEvents.CAT_WEAPON },
			{ "items.weapon.melee.Spear", "spear", ItemEvents.CAT_WEAPON }, { "items.weapon.melee.Dagger", "dagger", ItemEvents.CAT_WEAPON },
			{ "items.weapon.melee.Quarterstaff", "quarterstaff", ItemEvents.CAT_WEAPON }, { "items.weapon.melee.Glaive", "glaive", ItemEvents.CAT_WEAPON },
			{ "items.weapon.melee.WarHammer", "war hammer", ItemEvents.CAT_WEAPON }, { "items.weapon.melee.Rapier", "rapier", ItemEvents.CAT_WEAPON },
			{ "items.weapon.melee.Whip", "whip", ItemEvents.CAT_WEAPON }, { "items.weapon.melee.Greataxe", "greataxe", ItemEvents.CAT_WEAPON },
			{ "items.weapon.missiles.ThrowingKnife", "throwing knife", ItemEvents.CAT_MISSILE }, { "items.weapon.missiles.Javelin", "javelin", ItemEvents.CAT_MISSILE } };
	private static final Object[][] ARMOR = {
			{ "items.armor.ClothArmor", "cloth armor", ItemEvents.CAT_ARMOR }, { "items.armor.LeatherArmor", "leather armor", ItemEvents.CAT_ARMOR },
			{ "items.armor.MailArmor", "mail armor", ItemEvents.CAT_ARMOR }, { "items.armor.ScaleArmor", "scale armor", ItemEvents.CAT_ARMOR },
			{ "items.armor.PlateArmor", "plate armor", ItemEvents.CAT_ARMOR } };
	private static final String[] RINGS = { "Haste", "Might", "Wealth", "Force", "Evasion", "Energy", "Tenacity", "Sharpshooting", "Furor", "Accuracy" };
	private static final String[] GEMS = { "garnet", "ruby", "topaz", "emerald", "onyx", "opal", "tourmaline", "sapphire", "amethyst", "quartz" };
	private static final String[] TRINKETS = { "Rat Skull", "Dimensional Sundial", "Wondrous Resin", "Exotic Crystals", "Mossy Clump", "Parchment Scrap",
			"Petri Dish", "Shardy Wishbone", "Cracked Spyglass", "Mystical Cheese" };
	private static final String[] ARTIFACTS = { "Cloak of Shadows", "Horn of Plenty", "Talisman of Foresight", "Chalice of Blood", "Sandals of Nature",
			"Unstable Spellbook", "Alchemist's Toolkit", "Dried Rose", "Ethereal Chains", "Cape of Thorns" };

	//{shown name, class, first depth, last depth}
	private static final Object[][] ENEMIES = {
			{ "rat", "actors.mobs.Rat", 1, 5 }, { "gnoll scout", "actors.mobs.Gnoll", 1, 5 }, { "snake", "actors.mobs.Snake", 1, 5 },
			{ "sewer crab", "actors.mobs.Crab", 2, 6 }, { "swarm of flies", "actors.mobs.Swarm", 3, 8 }, { "gnoll thief", "actors.mobs.Thief", 3, 10 },
			{ "skeleton", "actors.mobs.Skeleton", 6, 10 }, { "vampire bat", "actors.mobs.Bat", 6, 14 }, { "gnoll brute", "actors.mobs.Brute", 7, 14 },
			{ "dwarf warlock", "actors.mobs.Warlock", 11, 16 }, { "dwarf monk", "actors.mobs.Monk", 11, 16 }, { "stone golem", "actors.mobs.Golem", 12, 18 },
			{ "ghoul", "actors.mobs.Ghoul", 16, 22 }, { "evil eye", "actors.mobs.Eye", 17, 24 }, { "succubus", "actors.mobs.Succubus", 17, 24 },
			{ "scorpio", "actors.mobs.Scorpio", 18, 25 }, { "demon spawner", "actors.mobs.DemonSpawner", 21, 26 }, { "ripper demon", "actors.mobs.RipperDemon", 21, 26 } };
	private static final String[] ENCHANTS = { "blazing", "chilling", "shocking", "vampiric", "grim", "lucky", "projecting", "unstable" };
	private static final String[] ODD_CAUSES = { "starvation", "fall", "toxic trap", "burning", "explosion", "drowning" };

	private final File root;
	private final Ruleset current;
	private final long base;
	private final Random rnd = new Random(20260607L);
	//healing has its own random numbers so adding it did not change how any other part of the demo history looks
	private final Random healRnd = new Random(20261007L);

	private static final String[] HEAL_SOURCES = {
			"regen", "regen", "regen", "regen", "potion.healing", "potion.healing", "food.wellFed", "food.supplyRation", "food.phantomMeat",
			"plant.sungrass", "plant.dewdrop", "plant.dewdrop", "gear.chalice", "gear.vampiric", "skill.heartyMeal", "wand.transfusion", "other.sleep" };

	private final int runs;

	private DemoGenerator( File root, Ruleset current, long now, int runs ){
		this.root = root;
		this.current = current;
		this.base = now;
		this.runs = runs;
	}

	//Wipes any earlier demo data in 'root' and writes a new demo history there. 'root' must be a demo folder (named like it, empty
	//or carrying the marker); anything else is refused and nothing is written. Returns the number of runs written.
	public static int generate( File root, Ruleset current, long now ) throws IOException {
		if (root == null || !DemoData.DIR_NAME.equals(root.getName())) throw new IOException("not a demo folder: " + root);
		return write(root, current, now, RUNS);
	}

	public static final int UNLOCK_RUNS = 10;

	//The ten demo runs that unlock UIMSD ("I LIKE CONFUSION"): the same generator, the same demo folder and the same protections as demo data,
	//so they can never be mistaken for the player's own history. Release builds use it too.
	public static int generateUnlockSet( File root, Ruleset current, long now ) throws IOException {
		if (root == null || !DemoData.DIR_NAME.equals(root.getName())) throw new IOException("not a demo folder: " + root);
		return write(root, current, now, UNLOCK_RUNS);
	}

	//The small made-up history the UIMSD tutorial uses when the player has too little of their own: the same generator and the same
	//protections as demo data (its own marked folder, "demo-" logs, never recorded into, exported, imported or counted as history),
	//in a folder of its own so it never touches the demo data a development build may be showing. Release builds use it too.
	public static int generateSample( File root, Ruleset current, long now ) throws IOException {
		if (root == null || !DemoData.SAMPLE_DIR_NAME.equals(root.getName())) throw new IOException("not a tutorial sample folder: " + root);
		return write(root, current, now, SAMPLE_RUNS);
	}

	private static int write( File root, Ruleset current, long now, int runs ) throws IOException {
		if (root.exists() && !DemoData.isSyntheticRoot(root)){
			String[] kids = root.list();
			if (kids != null && kids.length > 0) throw new IOException("refusing to write into a folder that is not demo data: " + root);
		}
		if (root.exists() && !DemoData.delete(root)) throw new IOException("could not clear the old demo data");
		DemoData.writeMarker(root);
		return new DemoGenerator(root, current, now, runs).run();
	}

	private int run() throws IOException {
		int logs = 0;
		for (int i = 0; i < runs; i++) logs += oneRun(i);
		return runs;
	}

	private int pick( int n ){
		return rnd.nextInt(n);
	}

	private static TelemetryVersion version( int i ){
		//most runs are recent; some are older so newer columns are unknown for them
		int k = i % 12;
		if (k == 0) return new TelemetryVersion(1, 1);
		if (k == 1) return new TelemetryVersion(1, 2);
		if (k == 2 || k == 3) return new TelemetryVersion(1, 3);
		if (k == 4) return new TelemetryVersion(1, 4);
		return TelemetryVersion.CURRENT;
	}

	private static boolean has( TelemetryVersion v, int minor ){
		return v.major == 1 && v.minor >= minor;
	}

	private int oneRun( int i ) throws IOException {
		String runId = DemoData.RUN_PREFIX + String.format("%03d", i);
		TelemetryVersion v = version(i);
		String cls = CLASSES[pick(CLASSES.length)];
		int challenges = CHALLENGE_SETS[pick(CHALLENGE_SETS.length)];
		int exotic = EXOTIC[pick(EXOTIC.length)];
		//most runs use the ruleset the player is on now, so the default view is full; the rest are other rulesets
		boolean currentRules = i % 5 != 0;
		//difficulty (telemetry 1.7 runs only; older runs never recorded it). Runs on the current rules are difficulty 3; some of the others
		//were played at other levels, which makes them other rulesets. No random draw: the demo history's other content stays as it was.
		int[] otherLevels = { 2, 4, 5, 1 };
		int diff = !has(v, 7) ? 0 : currentRules ? 3 : otherLevels[(i / 5) % otherLevels.length];
		String rules = currentRules ? current.id : Ruleset.of(exotic, challenges, diff == 0 ? 3 : diff).id;
		int ch = currentRules ? 0 : challenges;
		int ex = currentRules ? current.exoticChance : exotic;
		long created = base - (long) (runs - i) * 86_400_000L / 2 + pick(3_000_000);

		//how it ends
		int roll = pick(100);
		boolean unfinished = i == 7 || i == 31 || i == 58;
		int outcome = roll < 17 ? LifecycleEvents.OUTCOME_WON : roll < 24 ? LifecycleEvents.OUTCOME_ABANDONED : LifecycleEvents.OUTCOME_DIED;
		int deepest = outcome == LifecycleEvents.OUTCOME_WON ? 26 : 1 + (int) Math.round(Math.pow(rnd.nextDouble(), 1.6) * 24);
		if (unfinished) deepest = 2 + pick(8);
		boolean partial = i % 9 == 4;          //only started recording part-way: lower bounds
		boolean twoSegments = i % 10 == 3 && !unfinished;
		boolean gap = i % 13 == 6;
		boolean rulesChange = i == 20;
		int turnsPerFloor = 120 + pick(900);
		int heroLevel = Math.min(30, 1 + deepest + pick(3));

		String mainId = runId;
		DemoLog log = new DemoLog(root, mainId, v, created);
		log.difficulty = diff;
		log.difficultyBalance = diff == 0 ? 0 : com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty.balanceRevision(diff);
		log.start(runId, 0, cls, rules, ch, ex, partial);
		boolean items = has(v, 2), combat = has(v, 3), generation = has(v, 4);
		String lastEnemy = "rat";
		int uidCounter = 0;
		String weaponUid = null, armorUid = null, ringUid = null;
		java.util.Set<String> knownRings = new java.util.HashSet<>();
		int logs = 1;

		for (int d = 1; d <= deepest; d++){
			log.enter(d);
			log.tick(turnsPerFloor / 2 + pick(turnsPerFloor));
			if (rulesChange && d == 3) log.rulesetChange(rules, Ruleset.of(30, 1).id);
			if (gap && d == 4) log.gap();

			if (items){
				int found = 1 + pick(3);
				for (int k = 0; k < found; k++){
					String uid = runId + "-i" + (uidCounter++);
					Object[] it = randomItem(d);
					String cls2 = (String) it[0];
					String shown = (String) it[1];
					int cat = (Integer) it[2];
					//a known enchantment changes the name the game shows, not the kind of item
					if ((cat == ItemEvents.CAT_WEAPON || cat == ItemEvents.CAT_MISSILE) && pick(3) == 0) shown = ENCHANTS[pick(ENCHANTS.length)] + " " + shown;
					//a ring shows its gem until the player learns its kind
					if (cat == ItemEvents.CAT_RING){
						String real = (String) it[3];
						if (!knownRings.contains(cls2) && pick(3) == 0){
							log.classKnown(shown, cls2, cat);
							knownRings.add(cls2);
						}
						if (knownRings.contains(cls2)) shown = real;
					}
					int trueLevel = cat == ItemEvents.CAT_TRINKET || cat == ItemEvents.CAT_ARTIFACT ? pick(4) : pick(6) - 1;
					boolean cursed = trueLevel < 0 && pick(2) == 0;
					boolean known = pick(3) == 0;
					log.registered(uid, cls2, trueLevel, cursed, ItemEvents.ORIGIN_OBSERVED);
					if (generation) log.generated(uid, pick(2), "room" + pick(9));
					log.seen(uid, shown, cat);
					log.tick(2 + pick(8));
					log.acquired(uid, shown, cat, known, known ? Math.max(0, trueLevel) : 0);
					if (pick(4) == 0) log.truthChanged(uid, trueLevel + 1, false);
					if (cat == ItemEvents.CAT_WEAPON || cat == ItemEvents.CAT_MISSILE){
						if (weaponUid == null || pick(3) == 0){
							if (weaponUid != null) log.unequip(weaponUid, "weapon", ItemEvents.CAT_WEAPON);
							log.equip(uid, shown, cat);
							weaponUid = uid;
						}
					} else if (cat == ItemEvents.CAT_ARMOR){
						if (armorUid == null || pick(3) == 0){
							if (armorUid != null) log.unequip(armorUid, "armor", ItemEvents.CAT_ARMOR);
							log.equip(uid, shown, cat);
							armorUid = uid;
						}
					} else if (cat == ItemEvents.CAT_RING && ringUid == null){
						log.equip(uid, shown, cat);
						ringUid = uid;
					}
				}
			}

			if (generation){
				for (int m = 0; m < 2 + pick(3); m++) log.mobGenerated((String) pickEnemy(d)[1]);
				if (pick(3) == 0) log.hiddenAt(40 + pick(200));
			}

			if (combat){
				int fights = 1 + pick(4);
				for (int f = 0; f < fights; f++){
					Object[] e = pickEnemy(d);
					String name = (String) e[0], ecls = (String) e[1];
					lastEnemy = name;
					int rounds = 1 + pick(4);
					for (int r = 0; r < rounds; r++){
						log.heroAttacks(name, ecls, pick(4) != 0);
						log.damageFromHero(name, ecls, 1 + pick(6 + d));
						log.enemyAttacks(name, ecls, pick(3) != 0);
						if (pick(2) == 0) log.damageToHero(name, ecls, 1 + pick(4 + d));
					}
					if (!(unfinished && d == deepest && f == fights - 1)) log.heroKills(name, ecls, 1 + d / 2 + pick(5));
					log.tick(3 + pick(10));
				}
				//healing: exact (with the true source, hidden) since telemetry 1.6, inferred before; the odd heal is still only inferred
				for (int hl = 0, heals = healRnd.nextInt(4); hl < heals; hl++){
					int amount = 1 + healRnd.nextInt(12);
					if (has(v, 6)){
						log.heal(amount, healRnd.nextInt(8) != 0, HEAL_SOURCES[healRnd.nextInt(HEAL_SOURCES.length)]);
					} else {
						log.heal(amount, false, "");
					}
					log.tick(1 + healRnd.nextInt(4));
				}
			}
		}

		if (twoSegments){
			//play was stopped and resumed: a second log of the same run
			log.close(false);
			DemoLog second = new DemoLog(root, runId + "-s1", v, created + 600_000L);
			second.clock = log.clock + 5;
			second.start(runId, 1, cls, rules, ch, ex, false);
			second.enter(deepest);
			second.tick(100 + pick(300));
			finish(second, outcome, lastEnemy, deepest, heroLevel, v);
			logs++;
		} else if (unfinished){
			log.close(false);
		} else {
			finish(log, outcome, lastEnemy, deepest, heroLevel, v);
		}
		return logs;
	}

	private void finish( DemoLog log, int outcome, String lastEnemy, int deepest, int heroLevel, TelemetryVersion v ){
		if (outcome == LifecycleEvents.OUTCOME_DIED && has(v, 3)){
			if (pick(5) == 0) log.heroDies(ODD_CAUSES[pick(ODD_CAUSES.length)], CombatEvents.SRC_OTHER);
			else log.heroDies(lastEnemy, CombatEvents.SRC_CHAR);
		}
		long score = outcome == LifecycleEvents.OUTCOME_WON ? 90_000 + pick(60_000) : (long) deepest * 1_000 + pick(2_000);
		String cause = outcome == LifecycleEvents.OUTCOME_DIED ? lastEnemy : outcome == LifecycleEvents.OUTCOME_WON ? "" : "abandoned";
		log.end(outcome, cause, deepest, heroLevel, score);
		log.close(true);
	}

	private Object[] pickEnemy( int depth ){
		int n = 0;
		for (Object[] e : ENEMIES) if (depth >= (Integer) e[2] && depth <= (Integer) e[3]) n++;
		int k = pick(Math.max(1, n));
		for (Object[] e : ENEMIES){
			if (depth >= (Integer) e[2] && depth <= (Integer) e[3]){
				if (k-- == 0) return e;
			}
		}
		return ENEMIES[0];
	}

	//{true class, shown name, category}
	private Object[] randomItem( int depth ){
		int kind = pick(10);
		if (kind < 3) return WEAPONS[pick(WEAPONS.length)];
		if (kind < 5) return ARMOR[pick(ARMOR.length)];
		if (kind < 7){
			int r = pick(RINGS.length);
			return new Object[]{ "items.rings.RingOf" + RINGS[r], GEMS[r] + " ring", ItemEvents.CAT_RING, "ring of " + RINGS[r].toLowerCase() };
		}
		if (kind < 8) return new Object[]{ "items.trinkets." + TRINKETS[pick(TRINKETS.length)].replace(" ", ""), TRINKETS[pick(TRINKETS.length)], ItemEvents.CAT_TRINKET };
		if (kind < 9) {
			String a = ARTIFACTS[pick(ARTIFACTS.length)];
			return new Object[]{ "items.artifacts." + a.replace(" ", "").replace("'", ""), a, ItemEvents.CAT_ARTIFACT };
		}
		return new Object[]{ "items.food.Ration", "ration of food", ItemEvents.CAT_FOOD };
	}
}
