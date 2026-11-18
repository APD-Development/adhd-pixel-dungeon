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


package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef.Merge;

//The shipped datasets and views. This is the only place that knows what Runs, Items, Enemies and Deaths contain; the engine, the
//view model and the screen only read these definitions. Adding a dataset is adding a DatasetDef here, a row builder in
//RowBuilders, and (optionally) views.
//
//PUBLIC columns are built only from what the player could see. INTERNAL columns come from hidden truth and are never shown for a
//run that is not permanently finished.
public final class Datasets {

	private Datasets(){}

	public static final String RUNS = "runs", ITEMS = "items", ENEMIES = "enemies", DEATHS = "deaths";

	//---- shared words ----
	private static ColumnDef yesNo( ColumnDef c ){
		return c.label(0, "No").label(1, "Yes");
	}

	//---- Runs ----

	static DatasetDef runs(){
		DatasetDef d = new DatasetDef(RUNS, "Runs", "Every run: recorded by APD, or taken from APD's own rankings file.");
		d.countLabel = "Runs";
		d.add(ColumnDef.text(FactKeys.RUN_ID, "Run id").group("Run").describe("The run's identifier."));
		d.add(ColumnDef.num(FactKeys.START_MILLIS, "Started").format(ColumnDef.Format.DATE_MILLIS).shown().group("Run"));
		d.add(ColumnDef.text(FactKeys.HERO_CLASS, "Class").shown().format(ColumnDef.Format.TITLE_CASE).group("Run"));
		d.add(ColumnDef.enumeration(FactKeys.OUTCOME, "Outcome").label(0, "Died").label(1, "Won").label(2, "Abandoned").shown()
				.merge(Merge.LAST).group("Result"));
		d.add(ColumnDef.num(FactKeys.DEEPEST, "Deepest").shown().merge(Merge.MAX).group("Result"));
		d.add(yesNo(ColumnDef.enumeration("run.ascended", "Ascended")).merge(Merge.LAST).group("Result")
				.describe("Whether a winning run completed the ascent. ? means this result was not recorded."));
		d.add(ColumnDef.num(FactKeys.HERO_LEVEL, "Level").shown().merge(Merge.LAST).group("Result"));
		d.add(ColumnDef.num(FactKeys.SCORE, "Score").shown().merge(Merge.LAST).group("Result"));
		d.add(ColumnDef.num(FactKeys.TURNS, "Turns").shown().merge(Merge.LAST).group("Result"));
		d.add(ColumnDef.num(FactKeys.DEPTH, "Ended on floor").merge(Merge.LAST).group("Result"));
		d.add(ColumnDef.text(FactKeys.CAUSE, "Ended by").merge(Merge.LAST).group("Result"));
		d.add(ColumnDef.enumeration("run.source", "Source").label(1, "Recorded").label(2, "Imported ranking").group("Run"));
		d.add(ColumnDef.text(FactKeys.STATUS, "Status").merge(Merge.LAST).group("Run"));
		d.add(ColumnDef.num("run.segments", "Recording parts").merge(Merge.SUM).group("Run")
				.describe("A run recorded by several versions of APD is recorded in several parts. They are shown as one run."));
		d.add(ColumnDef.num(FactKeys.CHALLENGES, "Challenges").group("Rules").describe("Which challenges were on. The number adds up the challenges chosen; 0 means none."));
		d.add(ColumnDef.num(FactKeys.EXOTIC_CHANCE, "Exotic chance %").group("Rules").describe("The exotic potion and scroll chance that was set when the run began. ? means the run never recorded it."));
		//unknown (?) for a run recorded before telemetry 1.7: such a run was played at the baseline balance but its difficulty was never recorded
		d.add(ColumnDef.enumeration(FactKeys.DIFFICULTY, "Difficulty").since(1, 7).group("Rules")
				.label(1, "1 - Easiest").label(2, "2 - Easier").label(3, "3 - Standard").label(4, "4 - Harder").label(5, "5 - Hardest")
				.describe("The difficulty the run was started with. ? means it was not recorded (a run from before difficulty existed)."));
		d.add(ColumnDef.num(FactKeys.DIFFICULTY_BALANCE, "Balance revision").since(1, 7).group("Rules")
				.describe("Which version of the difficulty balance the run was played with: 0 = none (difficulty 3, the normal game), 1 and up = a balance version. Runs of different versions are never counted together unless you choose all rules."));
		d.add(ColumnDef.text(FactKeys.RULESET, "Rules code").group("Rules").describe("A code for the exact rules the run was played under: difficulty, challenges and exotic-item chance. Runs with the same code were played the same way."));

        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.VERSION, "Custom rules version").since(1,11).group("Custom Rules"));
        d.add(ColumnDef.text(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.IDENTITY, "Exact custom settings").since(1,11).group("Custom Rules"));
        for (String slider : com.shatteredpixel.shatteredpixeldungeon.apd.stats.RecordedRuleSettings.KEYS) {
            String label=slider.toLowerCase(java.util.Locale.ROOT).replace('_',' ');
            label=Character.toUpperCase(label.charAt(0))+label.substring(1);
            d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.key(slider),
                label+(slider.equals("UPGRADE_SCROLLS") ? " count" : " %"))
                .since(1,11).group("Custom Rules").describe("The exact setting locked when this run began. ? means not recorded."));
        }
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.SCROLL_PERCENT, "Upgrade scroll availability %")
            .format(ColumnDef.Format.HUNDREDTHS).since(1,11).group("Custom Rules"));
        d.add(ColumnDef.text(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.SCORE_STATUS, "Custom scoring status").since(1,11).group("Custom Rules"));
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.GENERATED_UPGRADES,"Upgrade scrolls generated").internal().merge(Merge.SUM).since(1,4).group("Custom Rules"));
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.GENERATED_EQUIPMENT,"Equipment generated").internal().merge(Merge.SUM).since(1,4).group("Custom Rules"));
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.SCROLL_PERCENT_NUMERATOR,"Scroll availability numerator").since(1,11).group("Custom Rules"));
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.SCROLL_PERCENT_DENOMINATOR,"Scroll availability denominator").since(1,11).group("Custom Rules"));
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.SCORING_RECORD_VERSION,"Scoring record version").since(1,11).group("Custom Rules"));
        d.add(ColumnDef.text(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.MULTIPLIER_EXACT,"Exact custom multiplier").since(1,11).group("Custom Rules"));
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.ORIGINAL_SCORE,"Original earned score").group("Custom Rules"));
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.MULTIPLIER,"Custom Rules Multiplier").format(ColumnDef.Format.HUNDREDTHS).since(1,11).group("Custom Rules"));
        d.add(ColumnDef.text(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.CALCULATION_VERSION,"Custom score calculation version").since(1,11).group("Custom Rules"));
        d.add(ColumnDef.num(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.ADJUSTED_SCORE,"Adjusted score").since(1,11).group("Custom Rules"));
		d.add(yesNo(ColumnDef.enumeration(FactKeys.DAILY, "Daily")).group("Rules"));
		d.add(ColumnDef.enumeration(FactKeys.SEED_TYPE, "Run type").label(0, "Random").label(1, "Custom").label(2, "Daily").label(3, "Daily replay").group("Rules")
				.describe("How the run's levels were chosen: at random, a custom seed, or the daily. The seed itself is hidden information."));
		d.add(yesNo(ColumnDef.enumeration(FactKeys.EXTERNAL_MODS, "Modded")).group("Rules").describe("Yes if an unofficial mod was present. No until APD has a mod system."));
		d.add(ColumnDef.text(FactKeys.APD_VERSION, "APD version").group("Run"));
		d.add(ColumnDef.text("run.telemetry", "Recording version").group("Run").describe("The version of APD's recording this run was recorded with. Older versions recorded less detail."));
		d.add(ColumnDef.num(FactKeys.INITIAL_VERSION, "Game build number").group("Run"));
		d.add(ColumnDef.text(FactKeys.GAME_VERSION, "Game version").group("Run"));
		d.add(yesNo(ColumnDef.enumeration(FactKeys.PARTIAL, "Started mid-run")).merge(Merge.ANY).group("Run"));
		d.add(ColumnDef.num(FactKeys.ABANDONED_EVENTS, "Rolled-back events").internal().merge(Merge.SUM).group("Hidden")
				.describe("How many recorded events were rolled back. It counts hidden events too."));
		d.add(ColumnDef.num(FactKeys.ROLLBACKS, "Rollbacks").merge(Merge.SUM).group("Run"));
		d.add(yesNo(ColumnDef.enumeration(FactKeys.ASCENDING, "Ascended")).group("Result"));
		//journey
		d.add(ColumnDef.num(FactKeys.LEVELS_ENTERED, "Levels entered").merge(Merge.SUM).group("Journey"));
		d.add(ColumnDef.num(FactKeys.DEEPEST_ENTERED, "Deepest entered").merge(Merge.MAX).group("Journey"));
		d.add(ColumnDef.num(FactKeys.FINAL_HERO_LEVEL, "Final level").merge(Merge.LAST).group("Journey"));
		d.add(ColumnDef.num(FactKeys.FINAL_HP_MAX, "Final max HP").merge(Merge.LAST).group("Journey"));
		d.add(ColumnDef.num(FactKeys.FINAL_STRENGTH, "Final strength").merge(Merge.LAST).group("Journey"));
		d.add(ColumnDef.num(FactKeys.FINAL_GOLD, "Final gold").merge(Merge.LAST).group("Journey"));
		d.add(ColumnDef.text(FactKeys.FINAL_SUBCLASS, "Subclass").merge(Merge.LAST).group("Journey"));
		//combat (telemetry 1.3)
		String c = "Combat";
		d.add(ColumnDef.num(FactKeys.HERO_ATTACKS, "Hero attacks").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.HERO_HITS, "Hero hits").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.ATTACKS_ON_HERO, "Attacks on hero").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.HITS_ON_HERO, "Hits on hero").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.DAMAGE_DEALT, "Damage dealt").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.DAMAGE_TAKEN, "Damage taken").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.HERO_KILLS, "Hero kills").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.ENEMY_DEATHS, "Enemy deaths").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.XP_GAINED, "XP gained").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.LEVEL_UPS, "Level ups").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.HEALED, "Healed").merge(Merge.SUM).since(1, 3).group(c)
				.describe("HP the hero healed. Exact for runs recorded with recording version 1.6 or later; a ~ means part of it was only worked out from changes in the hero's health between turns (older runs, or a heal the game does not report)."));
		d.add(ColumnDef.num(FactKeys.HEALED_INFERRED, "Healed (worked-out part)").merge(Merge.SUM).since(1, 3).group(c)
				.describe("The part of the healing that was worked out from changes in the hero's health between turns, not recorded exactly."));
		d.add(ColumnDef.num(FactKeys.HERO_DEATHS, "Hero deaths").merge(Merge.SUM).since(1, 3).group(c));
		d.add(ColumnDef.num(FactKeys.REVIVALS, "Revivals").merge(Merge.SUM).since(1, 3).group(c));
		//items (telemetry 1.2)
		d.add(ColumnDef.num(FactKeys.ITEMS_ACQUIRED, "Items acquired").merge(Merge.LOWER).since(1, 2).group("Items"));
		d.add(ColumnDef.num(FactKeys.ITEMS_EQUIPPED, "Items equipped").merge(Merge.LOWER).since(1, 2).group("Items"));
		d.add(ColumnDef.num(FactKeys.EQUIP_EVENTS, "Equip actions").merge(Merge.SUM).since(1, 2).group("Items"));
		//hidden truth
		d.add(ColumnDef.num(FactKeys.ITEMS_REGISTERED, "All items in the run").internal().merge(Merge.LOWER).since(1, 2).group("Hidden")
				.describe("Every item the run ever contained, seen or not."));
		d.add(ColumnDef.num(FactKeys.GEN_LEVELS, "Levels generated").internal().merge(Merge.SUM).since(1, 4).group("Hidden"));
		d.add(ColumnDef.num(FactKeys.GEN_ITEMS, "Items generated").internal().merge(Merge.SUM).since(1, 4).group("Hidden"));
		d.add(ColumnDef.num(FactKeys.GEN_MOBS, "Enemies generated").internal().merge(Merge.SUM).since(1, 4).group("Hidden"));
		String[] healLabels = { "regeneration", "potions and elixirs", "food", "plants", "gear (artifacts, enchantments, remains)",
				"skills and talents", "wands", "other (sleep, wells, vault exits)" };
		for (int h = 0; h < healLabels.length; h++){
			d.add(ColumnDef.num(FactKeys.healedFrom(com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents.HEAL_CATEGORIES[h]),
					"Healed by " + healLabels[h]).internal().merge(Merge.SUM).since(1, 6).group("Hidden")
					.describe("HP healed by this kind of source (exact, for runs recorded with recording version 1.6 or later). Which source healed can be hidden information, so this is shown only for finished runs."));
		}
		d.add(ColumnDef.num(FactKeys.SEED, "Seed").internal().group("Hidden"));
		d.add(ColumnDef.text(FactKeys.CUSTOM_SEED, "Custom seed").internal().group("Hidden"));
		//what the rankings file kept (imported records only)
		String l = "Imported ranking data";
		String[][] legacy = {
				{"legacy.goldCollected", "Gold collected"}, {"legacy.deepestFloor", "Deepest floor (stats)"}, {"legacy.highestAscent", "Highest ascent"},
				{"legacy.enemiesSlain", "Enemies slain"}, {"legacy.foodEaten", "Food eaten"}, {"legacy.itemsCrafted", "Items crafted"},
				{"legacy.piranhasKilled", "Piranhas killed"}, {"legacy.hazardAssistedKills", "Hazard kills"}, {"legacy.ankhsUsed", "Ankhs used"},
				{"legacy.upgradesUsed", "Upgrades used"}, {"legacy.sneakAttacks", "Sneak attacks"}, {"legacy.thrownAssists", "Thrown assists"},
				{"legacy.progressScore", "Progress score"}, {"legacy.heldItemValue", "Held item value"}, {"legacy.treasureScore", "Treasure score"},
				{"legacy.exploreScore", "Explore score"}, {"legacy.bossScore", "Boss score"}, {"legacy.questScore", "Quest score"},
				{"legacy.totalScore", "Total score"}, {"legacy.winMultiplierPct", "Win multiplier %"}, {"legacy.chalMultiplierPct", "Challenge multiplier %"},
				{"legacy.itemTypesDiscovered", "Item types discovered"}, {"legacy.armorTier", "Armor tier"}, {"legacy.rankDepth", "Ranking depth"},
				{"legacy.experience", "Experience"}, {"loadout.packItems", "Items kept in pack"}};
		for (String[] n : legacy) d.add(ColumnDef.num(n[0], n[1]).group(l));
		d.add(ColumnDef.num("legacy.durationMilli", "Duration (turns)").format(ColumnDef.Format.THOUSANDTHS).group(l));
		d.add(yesNo(ColumnDef.enumeration("legacy.amuletObtained", "Amulet obtained")).group(l));
		d.add(yesNo(ColumnDef.enumeration("legacy.gameWon", "Game won")).group(l));
		d.add(yesNo(ColumnDef.enumeration("legacy.ascended", "Ascended (stats)")).group(l));
		for (String slot : new String[]{"weapon", "armor", "artifact", "misc", "ring", "second_wep"}){
			String name = slot.equals("second_wep") ? "Second weapon" : Character.toUpperCase(slot.charAt(0)) + slot.substring(1);
			d.add(ColumnDef.text("loadout." + slot, name).group("Final equipment"));
			d.add(ColumnDef.num("loadout." + slot + ".level", name + " level").group("Final equipment"));
		}
		d.link(new LinkDef("Items in this run", ITEMS, FactKeys.RUN_ID, "item.run"));
		d.link(new LinkDef("Enemies met in this run", ENEMIES, FactKeys.RUN_ID, "enemy.run"));
		d.link(new LinkDef("Deaths in this run", DEATHS, FactKeys.RUN_ID, "death.run"));
		return d;
	}

	//---- Items ----

	static Map<Long, String> categories(){
		Map<Long, String> m = new LinkedHashMap<>();
		m.put((long) ItemEvents.CAT_OTHER, "Other");
		m.put((long) ItemEvents.CAT_WEAPON, "Weapon");
		m.put((long) ItemEvents.CAT_MISSILE, "Missile");
		m.put((long) ItemEvents.CAT_ARMOR, "Armor");
		m.put((long) ItemEvents.CAT_RING, "Ring");
		m.put((long) ItemEvents.CAT_ARTIFACT, "Artifact");
		m.put((long) ItemEvents.CAT_WAND, "Wand");
		m.put((long) ItemEvents.CAT_POTION, "Potion");
		m.put((long) ItemEvents.CAT_SCROLL, "Scroll");
		m.put((long) ItemEvents.CAT_SEED, "Seed");
		m.put((long) ItemEvents.CAT_STONE, "Stone");
		m.put((long) ItemEvents.CAT_FOOD, "Food");
		m.put((long) ItemEvents.CAT_BAG, "Bag");
		m.put((long) ItemEvents.CAT_TRINKET, "Trinket");
		m.put((long) ItemEvents.CAT_SPELL, "Spell");
		m.put((long) ItemEvents.CAT_BOMB, "Bomb");
		m.put((long) ItemEvents.CAT_TOOL, "Tool");
		return m;
	}

	public static final int FATE_NEVER_SEEN = 0, FATE_LEFT_BEHIND = 1, FATE_NEVER_EQUIPPED = 2, FATE_EQUIPPED = 3;

	static DatasetDef items(){
		DatasetDef d = new DatasetDef(ITEMS, "Items", "Every item of every run: what the player saw of it, and what became of it.");
		d.minTelemetry = new com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion(1, 2);
		d.add(ColumnDef.text("item.run", "Run").group("Item").describe("The run the item belongs to."));
		d.add(ColumnDef.text("item.name", "Item").shown().merge(Merge.LAST).group("Item")
				.describe("What the game called it the last time the player looked: unidentified items keep their unidentified name."));
		ColumnDef cat = ColumnDef.enumeration("item.category", "Kind").shown().merge(Merge.LAST).group("Item");
		for (Map.Entry<Long, String> e : categories().entrySet()) cat.label(e.getKey(), e.getValue());
		d.add(cat);
		d.add(ColumnDef.text("item.type", "Type").shown().merge(Merge.LAST).format(ColumnDef.Format.CLASS_NAME).group("Item")
				.describe("The kind of item, as the game defines it, once the player knows it: every glaive is a Glaive whatever its enchantment, "
						+ "and a ring or potion is its real kind once its kind is known. Until then (and for runs recorded before the kind was "
						+ "recorded) it is the name the player saw, which is the best that can honestly be said."));
		d.add(ColumnDef.enumeration("item.fate", "What happened").shown().merge(Merge.MAX).group("Journey")
				.label(FATE_NEVER_SEEN, "Never seen").label(FATE_LEFT_BEHIND, "Seen, left behind")
				.label(FATE_NEVER_EQUIPPED, "Acquired, never equipped").label(FATE_EQUIPPED, "Equipped"));
		d.add(ColumnDef.num("item.firstSeenDepth", "First seen on floor").shown().group("Journey"));
		d.add(ColumnDef.num("item.acquiredDepth", "Acquired on floor").group("Journey"));
		d.add(ColumnDef.num("item.acquiredTurn", "Acquired at turn").group("Journey"));
		d.add(ColumnDef.num("item.equipCount", "Times equipped").shown().merge(Merge.SUM).group("Equipment"));
		d.add(ColumnDef.num("item.turnsEquipped", "Turns equipped").shown().merge(Merge.SUM).group("Equipment")
				.describe("Time spent equipped. If the run was still going, only up to the last recorded moment (a lower bound)."));
		d.add(ColumnDef.num("item.floorsEquipped", "Floors equipped on").merge(Merge.LOWER).group("Equipment"));
		d.add(yesNo(ColumnDef.enumeration("item.seen", "Ever seen")).merge(Merge.ANY).group("Journey"));
		d.add(yesNo(ColumnDef.enumeration("item.acquired", "Ever acquired")).merge(Merge.ANY).group("Journey"));
		d.add(yesNo(ColumnDef.enumeration("item.equipped", "Ever equipped")).merge(Merge.ANY).group("Equipment"));
		d.add(ColumnDef.num("item.visibleLevel", "Level (as you knew it)").merge(Merge.LAST).group("Item")
				.describe("Only when the player knew the item's level. Never revealed afterwards."));
		d.add(yesNo(ColumnDef.enumeration("item.identified", "Identified")).merge(Merge.LAST).group("Item"));
		//hidden truth
		d.add(ColumnDef.text("item.trueClass", "True kind").internal().group("Hidden"));
		d.add(ColumnDef.num("item.trueLevel", "True level").internal().merge(Merge.LAST).group("Hidden"));
		d.add(yesNo(ColumnDef.enumeration("item.cursed", "Truly cursed")).internal().merge(Merge.LAST).group("Hidden"));
		d.add(ColumnDef.text("item.enchant", "True enchantment").internal().merge(Merge.LAST).group("Hidden"));
		//what the item was when the game generated it (only where the log's first record of it is its generation): never the later state
		d.add(ColumnDef.num("item.genLevel", "True level when generated").internal().merge(Merge.SAME).group("Hidden"));
		d.add(yesNo(ColumnDef.enumeration("item.genCursed", "Truly cursed when generated")).internal().merge(Merge.SAME).group("Hidden"));
		d.add(ColumnDef.text("item.genEnchant", "True enchantment when generated").internal().merge(Merge.SAME).group("Hidden"));
		d.add(ColumnDef.enumeration("item.origin", "Origin").internal().group("Hidden")
				.label(ItemEvents.ORIGIN_OBSERVED, "Observed").label(ItemEvents.ORIGIN_START_KIT, "Start kit")
				.label(ItemEvents.ORIGIN_ADOPTED, "Adopted").label(ItemEvents.ORIGIN_SPLIT, "Split").label(ItemEvents.ORIGIN_COPY, "Copy")
				.label(ItemEvents.ORIGIN_TRANSFORMED, "Transformed").label(ItemEvents.ORIGIN_CRAFTED, "Crafted")
				.label(ItemEvents.ORIGIN_GENERATED, "Generated with a level"));
		d.add(ColumnDef.num("item.genDepth", "Generated on floor").internal().group("Hidden"));
		d.add(ColumnDef.text("item.container", "Placed in").internal().group("Hidden"));
		d.add(ColumnDef.text("item.room", "Room").internal().group("Hidden"));
		d.add(ColumnDef.text("item.drawKind", "How it was drawn").internal().group("Hidden"));
		d.add(ColumnDef.text("item.requestedCategory", "Category asked for").internal().group("Hidden"));
		addRunFacts(d, "item");
		d.countLabel = "Items";
		itemSheet(d);
		d.link(new LinkDef("The run", RUNS, "item.run", FactKeys.RUN_ID));
		return d;
	}

	//The facts of the run an item or enemy row belongs to, so that "how did runs with this end" can be asked of the row itself.
	//Filled in when the datasets are put together (see DatasetStore), with the run's own provenance; unknown when the run's is.
	static final String[][] RUN_FACTS = {
			{ "runOutcome", FactKeys.OUTCOME }, { "runClass", FactKeys.HERO_CLASS }, { "runDeepest", FactKeys.DEEPEST }, { "runChallenges", FactKeys.CHALLENGES } };

	static void addRunFacts( DatasetDef d, String prefix ){
		d.add(ColumnDef.enumeration(prefix + ".runOutcome", "Run outcome").label(0, "Died").label(1, "Won").label(2, "Abandoned").group("Run")
				.describe("How the run this row belongs to ended. Unknown while the run is not over."));
		d.add(ColumnDef.text(prefix + ".runClass", "Run class").group("Run"));
		d.add(ColumnDef.num(prefix + ".runDeepest", "Run deepest floor").group("Run"));
		d.add(ColumnDef.num(prefix + ".runChallenges", "Run challenges").group("Run"));
	}

	//---- Enemies ----

	static DatasetDef enemies(){
		DatasetDef d = new DatasetDef(ENEMIES, "Enemies", "Enemy types per run: how they fought you and what became of them.");
		d.minTelemetry = new com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion(1, 3);
		d.add(ColumnDef.text("enemy.run", "Run").group("Enemy"));
		d.add(ColumnDef.text("enemy.name", "Enemy").shown().group("Enemy").describe("What the game called it when you met it."));
		d.add(ColumnDef.num("enemy.attacksOnHero", "Attacks on hero").shown().merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.hitsOnHero", "Hits on hero").shown().merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.attacksByHero", "Hero attacks on it").merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.hitsByHero", "Hero hits on it").merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.damageToHero", "Damage to hero").shown().merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.damageFromHero", "Damage from hero").shown().merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.kills", "Killed by hero").shown().merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.deaths", "Seen dying").merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.heroDeaths", "Times it killed the hero").merge(Merge.SUM).group("Fights"));
		d.add(ColumnDef.num("enemy.boss", "Boss").group("Enemy").describe("1 if this was a boss fight, 0 if not."));
		d.add(ColumnDef.text("enemy.class", "True kind").internal().group("Hidden"));
		d.add(ColumnDef.num("enemy.spawned", "Generated in total").internal().merge(Merge.SUM).since(1, 4).group("Hidden"));
		d.add(ColumnDef.num("enemy.expValue", "XP value").internal().group("Hidden"));
		addRunFacts(d, "enemy");
		d.countLabel = "Encounters";
		enemySheet(d);
		d.link(new LinkDef("The run", RUNS, "enemy.run", FactKeys.RUN_ID));
		return d;
	}

	//---- Deaths ----

	static DatasetDef deaths(){
		DatasetDef d = new DatasetDef(DEATHS, "Deaths", "Every time the hero died, and what ended each run.");
		d.countLabel = "Deaths";
		d.minTelemetry = new com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion(1, 1);
		d.add(ColumnDef.text("death.run", "Run").group("Death"));
		d.add(ColumnDef.num("death.depth", "Floor").shown().group("Death"));
		d.add(ColumnDef.num("death.turn", "Turn").shown().group("Death"));
		d.add(ColumnDef.text("death.cause", "Killed by").shown().group("Death"));
		d.add(ColumnDef.enumeration("death.causeKind", "Cause kind").shown().group("Death")
				.label(CombatEvents.SRC_CHAR, "Creature").label(CombatEvents.SRC_BUFF, "Status effect").label(CombatEvents.SRC_BLOB, "Gas or terrain")
				.label(CombatEvents.SRC_TRAP, "Trap").label(CombatEvents.SRC_ITEM, "Item").label(CombatEvents.SRC_ENCHANT, "Enchantment")
				.label(CombatEvents.SRC_CLASS, "Hero ability").label(CombatEvents.SRC_OTHER, "Other"));
		d.add(yesNo(ColumnDef.enumeration("death.revived", "Revived")).shown().group("Death"));
		d.add(yesNo(ColumnDef.enumeration("death.final", "Ended the run")).shown().group("Death"));
		d.add(ColumnDef.num("death.heroLevel", "Hero level").group("Death"));
		d.link(new LinkDef("The run", RUNS, "death.run", FactKeys.RUN_ID));
		return d;
	}

	//---- views ----

	private static GroupBy runStats( String... keys ){
		return new GroupBy(Arrays.asList(keys), Arrays.asList(Aggregate.count(), Aggregate.rate(FactKeys.OUTCOME, 1),
				Aggregate.of(Aggregate.Fn.AVG, FactKeys.SCORE), Aggregate.of(Aggregate.Fn.AVG, FactKeys.DEEPEST),
				Aggregate.of(Aggregate.Fn.MAX, FactKeys.DEEPEST), Aggregate.of(Aggregate.Fn.AVG, FactKeys.HERO_LEVEL)));
	}

	//---- statistics sheets and the dense aggregates of the aggregate-first tables ----

	private static final String IRUN = "item.run";

	//each run counted once however many instances it had
	private static Aggregate perRun( Aggregate a, String runColumn ){
		return a.perDistinct(runColumn);
	}

	private static Aggregate acquiredRate(){
		return Aggregate.rate("item.acquired", 1).where("item.seen", 1).labeled("Acq %");
	}

	private static Aggregate equippedRate(){
		return Aggregate.rate("item.equipped", 1).where("item.acquired", 1).labeled("Equip %");
	}

	private static Aggregate itemRuns(){
		return Aggregate.distinct(IRUN).labeled("Runs");
	}

	private static Aggregate itemWins(){
		return perRun(Aggregate.count().where("item.runOutcome", 1), IRUN).labeled("Wins");
	}

	private static Aggregate itemWinRate(){
		return perRun(Aggregate.rate("item.runOutcome", 1), IRUN).labeled("Win %");
	}

	private static void itemSheet( DatasetDef d ){
		d.stat(StatLine.section("How many"));
		d.stat(StatLine.of("Items", Aggregate.count(), "Every individual one of them that the player met, in all runs shown."));
		d.stat(StatLine.of("Runs containing it", Aggregate.distinct(IRUN), "In how many different runs at least one turned up."));
		d.stat(StatLine.section("What the player did with them"));
		d.stat(StatLine.of("Acquired", Aggregate.rate("item.acquired", 1).where("item.seen", 1), "Of the items seen, how many were picked up."));
		d.stat(StatLine.of("Equipped", Aggregate.rate("item.equipped", 1).where("item.acquired", 1), "Of the items acquired, how many were ever equipped."));
		d.stat(StatLine.of("Equipped, of those seen", Aggregate.rate("item.equipped", 1).where("item.seen", 1), "Of the items seen (not only acquired), how many were ever equipped."));
		d.stat(StatLine.of("Identified", Aggregate.rate("item.identified", 1), "Of the items, how many the player had identified when last looked at."));
		d.stat(StatLine.section("How the runs went (each run counted once)"));
		d.stat(StatLine.of("Runs won", perRun(Aggregate.count().where("item.runOutcome", 1), IRUN), "Runs containing it that were won."));
		d.stat(StatLine.of("Runs died in", perRun(Aggregate.count().where("item.runOutcome", 0), IRUN), "Runs containing it that ended in death."));
		d.stat(StatLine.of("Win rate, runs containing it", perRun(Aggregate.rate("item.runOutcome", 1), IRUN), "Of the runs where one turned up, how many were won."));
		d.stat(StatLine.of("Win rate, runs that acquired it", perRun(Aggregate.rate("item.runOutcome", 1).where("item.acquired", 1), IRUN), "Of the runs that picked one up, how many were won."));
		d.stat(StatLine.of("Win rate, runs that equipped it", perRun(Aggregate.rate("item.runOutcome", 1).where("item.equipped", 1), IRUN), "Of the runs that equipped one, how many were won."));
		d.stat(StatLine.section("Where"));
		d.stat(StatLine.of("Floor first seen, average", Aggregate.of(Aggregate.Fn.AVG, "item.firstSeenDepth"), ""));
		d.stat(StatLine.of("Floor first seen, median", Aggregate.of(Aggregate.Fn.MEDIAN, "item.firstSeenDepth"), ""));
		d.stat(StatLine.of("Floor acquired, average", Aggregate.of(Aggregate.Fn.AVG, "item.acquiredDepth"), "Items that were never acquired have none."));
		d.stat(StatLine.of("Floor acquired, median", Aggregate.of(Aggregate.Fn.MEDIAN, "item.acquiredDepth"), ""));
		d.stat(StatLine.section("Time equipped (only items that were equipped)"));
		d.stat(StatLine.of("Turns equipped, average", Aggregate.of(Aggregate.Fn.AVG, "item.turnsEquipped").where("item.equipped", 1), "A run that was still going counts only up to its last recorded moment."));
		d.stat(StatLine.of("Turns equipped, median", Aggregate.of(Aggregate.Fn.MEDIAN, "item.turnsEquipped").where("item.equipped", 1), ""));
		d.stat(StatLine.of("Times equipped, average", Aggregate.of(Aggregate.Fn.AVG, "item.equipCount").where("item.equipped", 1), ""));
		d.stat(StatLine.of("Floors equipped on, average", Aggregate.of(Aggregate.Fn.AVG, "item.floorsEquipped").where("item.equipped", 1), ""));
		d.stat(StatLine.section("Upgrades (only where the player knew the level)"));
		d.stat(StatLine.of("Level, average", Aggregate.of(Aggregate.Fn.AVG, "item.visibleLevel"), "Levels the player never learned are not counted, and never counted as zero."));
		d.stat(StatLine.of("Level, median", Aggregate.of(Aggregate.Fn.MEDIAN, "item.visibleLevel"), ""));
		d.stat(StatLine.of("Level, highest", Aggregate.of(Aggregate.Fn.MAX, "item.visibleLevel"), ""));
		d.stat(StatLine.of("Items with a known level", Aggregate.of(Aggregate.Fn.COUNT_KNOWN, "item.visibleLevel"), ""));
	}

	private static final String ERUN = "enemy.run";

	private static void enemySheet( DatasetDef d ){
		d.stat(StatLine.section("How many"));
		d.stat(StatLine.of("Encounters", Aggregate.count(), "One per run in which it was met."));
		d.stat(StatLine.of("Runs met in", Aggregate.distinct(ERUN), "In how many different runs it turned up."));
		d.stat(StatLine.section("Its attacks on the hero"));
		d.stat(StatLine.of("Its hit rate on the hero", Aggregate.ratio("enemy.hitsOnHero", "enemy.attacksOnHero"), "Hits per attack."));
		d.stat(StatLine.of("Attacks per encounter", Aggregate.of(Aggregate.Fn.AVG, "enemy.attacksOnHero"), ""));
		d.stat(StatLine.of("Damage to the hero per encounter", Aggregate.of(Aggregate.Fn.AVG, "enemy.damageToHero"), ""));
		d.stat(StatLine.of("Damage to the hero, median", Aggregate.of(Aggregate.Fn.MEDIAN, "enemy.damageToHero"), ""));
		d.stat(StatLine.section("The hero's attacks on it"));
		d.stat(StatLine.of("The hero's hit rate on it", Aggregate.ratio("enemy.hitsByHero", "enemy.attacksByHero"), "Hits per attack."));
		d.stat(StatLine.of("Damage from the hero per encounter", Aggregate.of(Aggregate.Fn.AVG, "enemy.damageFromHero"), ""));
		d.stat(StatLine.of("Killed by the hero, total", Aggregate.of(Aggregate.Fn.SUM, "enemy.kills"), ""));
		d.stat(StatLine.of("Killed by the hero per encounter", Aggregate.of(Aggregate.Fn.AVG, "enemy.kills"), ""));
		d.stat(StatLine.section("Deadliness"));
		d.stat(StatLine.of("Times it killed the hero", Aggregate.of(Aggregate.Fn.SUM, "enemy.heroDeaths"), ""));
		d.stat(StatLine.of("Killed the hero, per encounter", Aggregate.of(Aggregate.Fn.AVG, "enemy.heroDeaths"), ""));
		d.stat(StatLine.section("How the runs went (each run counted once)"));
		d.stat(StatLine.of("Runs won", perRun(Aggregate.count().where("enemy.runOutcome", 1), ERUN), "Runs it was met in that were won."));
		d.stat(StatLine.of("Runs died in", perRun(Aggregate.count().where("enemy.runOutcome", 0), ERUN), "Runs it was met in that ended in death."));
		d.stat(StatLine.of("Win rate, runs it was met in", perRun(Aggregate.rate("enemy.runOutcome", 1), ERUN), ""));
	}

	//the dense columns of an aggregate-first item table: Type | Instances | Runs | Acq % | Equip % | Wins | Win % | Floor | Level | Turns | ID %
	private static GroupBy itemTable( String... keys ){
		return new GroupBy(Arrays.asList(keys), Arrays.asList(Aggregate.count(), itemRuns(), acquiredRate(), equippedRate(), itemWins(), itemWinRate(),
				Aggregate.of(Aggregate.Fn.AVG, "item.firstSeenDepth").labeled("Avg floor"),
				Aggregate.of(Aggregate.Fn.AVG, "item.visibleLevel").labeled("Avg lvl"),
				Aggregate.of(Aggregate.Fn.AVG, "item.turnsEquipped").where("item.equipped", 1).labeled("Avg turns"),
				Aggregate.rate("item.identified", 1).labeled("ID %")));
	}

	private static GroupBy enemyTable(){
		return new GroupBy(Arrays.asList("enemy.name"), Arrays.asList(Aggregate.count(), Aggregate.distinct(ERUN).labeled("Runs"),
				Aggregate.ratio("enemy.hitsOnHero", "enemy.attacksOnHero").labeled("Hit %"),
				Aggregate.ratio("enemy.hitsByHero", "enemy.attacksByHero").labeled("Our hit %"),
				Aggregate.of(Aggregate.Fn.AVG, "enemy.damageToHero").labeled("Dmg taken"),
				Aggregate.of(Aggregate.Fn.AVG, "enemy.damageFromHero").labeled("Dmg dealt"),
				Aggregate.of(Aggregate.Fn.SUM, "enemy.kills").labeled("Kills"),
				Aggregate.of(Aggregate.Fn.SUM, "enemy.heroDeaths").labeled("Killed us"),
				perRun(Aggregate.rate("enemy.runOutcome", 1), ERUN).labeled("Win %")));
	}

	//every kind of thing is shown, however few there are (each statistic says how many values it rests on)
	private static final int ALL_TYPES = 1;

	private static ViewDef itemView( String id, String title, String description, long... categories ){
		Query instances = Query.of(ITEMS).addFilter(Filter.num("item.category", Op.IN, categories));
		Query q = instances.withGroup(itemTable("item.type")).withMinSample(ALL_TYPES).withSorts(Arrays.asList(new Sort("count", true)));
		return new ViewDef(id, title, description, q).withInstances(instances);
	}

	public static final int DEFAULT_MIN_SAMPLE = 5;

	static List<ViewDef> views(){
		List<ViewDef> v = new ArrayList<>();
		v.add(new ViewDef("runs", "Runs", "Every run, newest first.", Query.of(RUNS).withSorts(Arrays.asList(new Sort(FactKeys.START_MILLIS, true)))));
		v.add(new ViewDef("items", "Items", "Every kind of item, with what became of each.",
				Query.of(ITEMS).withGroup(itemTable("item.category", "item.type")).withMinSample(ALL_TYPES).withSorts(Arrays.asList(new Sort("count", true))))
				.withInstances(Query.of(ITEMS)));
		v.add(itemView("weapons", "Weapons", "Each kind of weapon. Tap one for its statistics, then its individual weapons and the runs it was in.", ItemEvents.CAT_WEAPON, ItemEvents.CAT_MISSILE));
		v.add(itemView("armor", "Armor", "Each kind of armor, with its statistics, individual pieces and runs.", ItemEvents.CAT_ARMOR));
		v.add(itemView("rings", "Rings", "Each kind of ring.", ItemEvents.CAT_RING));
		v.add(itemView("trinkets", "Trinkets", "Each kind of trinket.", ItemEvents.CAT_TRINKET));
		v.add(itemView("artifacts", "Artifacts", "Each kind of artifact.", ItemEvents.CAT_ARTIFACT));
		v.add(new ViewDef("enemies", "Enemies", "Each kind of enemy. Tap one for its statistics, then each meeting with it (one per run) and the runs.",
				Query.of(ENEMIES).withGroup(enemyTable()).withMinSample(ALL_TYPES).withSorts(Arrays.asList(new Sort("count", true))))
				.withInstances(Query.of(ENEMIES)));
		v.add(new ViewDef("bosses", "Bosses", "Only the bosses, with how fights against each went. Tap one for its statistics, then each meeting and the runs.",
				Query.of(ENEMIES).withScope(Scope.DEFAULT.withFinishedOnly(true)).withFilters(Arrays.asList(Filter.num("enemy.boss", Op.EQ, 1))).withGroup(enemyTable()).withMinSample(ALL_TYPES)
						.withSorts(Arrays.asList(new Sort("count", true))))
				.withInstances(Query.of(ENEMIES).withScope(Scope.DEFAULT.withFinishedOnly(true)).withFilters(Arrays.asList(Filter.num("enemy.boss", Op.EQ, 1)))));
		v.add(new ViewDef("deaths", "Deaths", "Every death.", Query.of(DEATHS)));
		v.add(new ViewDef("characters", "Characters", "Runs grouped by hero class.",
				Query.of(RUNS).withGroup(runStats(FactKeys.HERO_CLASS)).withMinSample(DEFAULT_MIN_SAMPLE)
						.withSorts(Arrays.asList(new Sort("count", true)))));
		//challenges, the exotic chance, the difficulty and its balance revision are what make up a ruleset, so grouping by all of them never mixes rulesets
		v.add(new ViewDef("challenges", "Challenges", "Runs grouped by challenges, exotic-item chance and difficulty. Each group was played under one set of rules.",
				Query.of(RUNS).withScope(Scope.DEFAULT.withRules(Scope.Rules.ALL, ""))
						.withGroup(runStats(FactKeys.CHALLENGES, FactKeys.EXOTIC_CHANCE, FactKeys.DIFFICULTY, FactKeys.DIFFICULTY_BALANCE)).withMinSample(DEFAULT_MIN_SAMPLE)
						.withSorts(Arrays.asList(new Sort("count", true)))));
		//difficulty is part of the ruleset identity, so each group of a non-baseline level is its own ruleset; level 3 shares its ruleset with
		//runs recorded before difficulty existed (same balance), which show as the unknown group
		v.add(new ViewDef("difficulty", "Difficulty", "Runs grouped by difficulty, whatever other rules they used. The unknown group is runs recorded before difficulty existed.",
				Query.of(RUNS).withScope(Scope.DEFAULT.withRules(Scope.Rules.ALL, ""))
						.withGroup(runStats(FactKeys.DIFFICULTY, FactKeys.DIFFICULTY_BALANCE)).withMinSample(DEFAULT_MIN_SAMPLE)
						.withSorts(Arrays.asList(new Sort("count", true)))));
		//the sections of the view picker
		for (ViewDef view : v){
			switch (view.id){
				case "runs": case "characters": case "challenges": case "difficulty": case "deaths": view.group("Your runs"); break;
				case "items": case "weapons": case "armor": case "rings": case "trinkets": case "artifacts": view.group("Your gear"); break;
				case "enemies": case "bosses": view.group("Enemies"); break;
				default:
			}
		}
		return Collections.unmodifiableList(v);
	}

	public static List<DatasetDef> defs(){
		List<DatasetDef> l = new ArrayList<>();
		l.add(runs());
		l.add(items());
		l.add(enemies());
		l.add(deaths());
		return l;
	}

	//A table for a dataset that is generated rather than hand-made (the play-by-play datasets and one per registered kind of fact), so the
	//advanced tools see every new dataset the moment it has rows. Newest runs' rows come first as they were recorded; sorting is up to the player.
	public static ViewDef generatedView( DatasetDef def, String group ){
		return new ViewDef(def.id, def.title, def.description, Query.of(def.id)).group(group);
	}

	public static List<ViewDef> allViews(){
		return views();
	}
}


