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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Row-level datasets for what the run log already records, so it can be counted, totalled, averaged, grouped and compared per row and per floor
//instead of only as run totals: every floor visit (Floors) and every attack, damage, heal, shield, experience gain and kill.
//
//Floors is also what "per floor / per turn / per run" is measured against (see PerUnit): a floor is a row whether or not anything happened on
//it, so an average over floors is never inflated by leaving out the quiet ones. Each Floors row also counts, for EVERY registered fact kind,
//how many observations happened during the visit (generated from the registry).
//
//Like every dataset these are views of the raw log, rebuilt from it at any time. PUBLIC columns come only from PUBLIC events; what only an
//INTERNAL event knows is in an INTERNAL column.
public final class EventDatasets {

	private EventDatasets(){}

	public static final String FLOORS = "floors", ATTACKS = "attacks", DAMAGE = "damage", HEALS = "heals", SHIELDS = "shields",
			XP = "xp", KILLS = "kills", EQUIPMENT = "equipment";

	public static final String[] IDS = { FLOORS, ATTACKS, DAMAGE, HEALS, SHIELDS, XP, KILLS, EQUIPMENT };

	private static final TelemetryVersion V11 = new TelemetryVersion(1, 1), V12 = new TelemetryVersion(1, 2), V13 = new TelemetryVersion(1, 3);

	public static boolean isEventDataset( String id ){
		for (String s : IDS) if (s.equals(id)) return true;
		return false;
	}

	//the column prefix of a dataset ("floor", "attack", ...)
	public static String prefixOf( String id ){
		switch (id){
			case FLOORS: return "floor";
			case ATTACKS: return "attack";
			case DAMAGE: return "damage";
			case HEALS: return "heal";
			case SHIELDS: return "shield";
			case XP: return "xp";
			case EQUIPMENT: return "equip";
			default: return "kill";
		}
	}

	private static ColumnDef yesNo( ColumnDef c ){
		return c.label(0, "No").label(1, "Yes");
	}

	private static ColumnDef fighter( ColumnDef c ){
		return c.label(CombatEvents.KIND_HERO, "Hero").label(CombatEvents.KIND_ALLY, "Ally").label(CombatEvents.KIND_ENEMY, "Enemy")
				.label(CombatEvents.KIND_NEUTRAL, "Neutral").label(CombatEvents.KIND_NONE, "None");
	}

	private static ColumnDef sourceKind( ColumnDef c ){
		return c.label(CombatEvents.SRC_CHAR, "Creature").label(CombatEvents.SRC_BUFF, "Status effect").label(CombatEvents.SRC_BLOB, "Gas or terrain")
				.label(CombatEvents.SRC_TRAP, "Trap").label(CombatEvents.SRC_ITEM, "Item").label(CombatEvents.SRC_ENCHANT, "Enchantment")
				.label(CombatEvents.SRC_CLASS, "Hero ability").label(CombatEvents.SRC_OTHER, "Other");
	}

	//who the run was and how it went, on every row, so any row can be asked "in runs like ..."
	private static void context( DatasetDef d, String p ){
		Datasets.addRunFacts(d, p);
		d.add(ColumnDef.num(p + ".runDifficulty", "Run difficulty").group("Run").describe("The difficulty (1-5) the run this row belongs to was played on."));
	}

	private static void when( DatasetDef d, String p, boolean turn ){
		d.add(ColumnDef.text(p + ".run", "Run").group("When"));
		if (turn) d.add(ColumnDef.num(p + ".turn", "Turn").shown().group("When").describe("The game turn it happened on."));
		d.add(ColumnDef.num(p + ".depth", "Floor").shown().group("When"));
		d.add(ColumnDef.num(p + ".branch", "Branch").group("When"));
	}

	//---- definitions ----

	static DatasetDef floors(){
		DatasetDef d = new DatasetDef(FLOORS, "Floors", "Every visit to a floor: how long, what happened there.");
		d.minTelemetry = V11;
		d.countLabel = "Floor visits";
		d.add(ColumnDef.text("floor.run", "Run").group("Visit"));
		d.add(ColumnDef.num("floor.visit", "Visit").group("Visit").describe("1 for the first time the run entered a floor, counting every visit in the run."));
		d.add(ColumnDef.num("floor.depth", "Floor").shown().group("Visit"));
		d.add(ColumnDef.num("floor.branch", "Branch").group("Visit"));
		d.add(ColumnDef.text("floor.entry", "How you arrived").group("Visit"));
		d.add(ColumnDef.text("floor.levelClass", "Kind of floor").group("Visit"));
		d.add(ColumnDef.text("floor.feeling", "Feeling").group("Visit"));
		d.add(ColumnDef.num("floor.enterTurn", "Entered on turn").group("Visit"));
		d.add(ColumnDef.num("floor.exitTurn", "Left on turn").group("Visit"));
		d.add(ColumnDef.num("floor.turns", "Turns spent").shown().group("Visit"));
		d.add(ColumnDef.enumeration("floor.exit", "How it ended").group("Visit").label(0, "Left the floor").label(1, "The run ended here").label(2, "Still there"));
		d.add(ColumnDef.num("floor.activeMillis", "Time played here (ms)").since(1, 8).group("Visit")
				.describe("Real time spent playing while on this floor. Older recordings do not have it."));
		ColumnDef[] sums = {
				ColumnDef.num("floor.attacksByHero", "Hero attacks").group("Fighting"),
				ColumnDef.num("floor.hitsByHero", "Hero hits").group("Fighting"),
				ColumnDef.num("floor.attacksOnHero", "Attacks on hero").group("Fighting"),
				ColumnDef.num("floor.hitsOnHero", "Hits on hero").group("Fighting"),
				ColumnDef.num("floor.damageDealt", "Damage dealt by hero").shown().group("Fighting"),
				ColumnDef.num("floor.damageTaken", "Damage taken by hero").shown().group("Fighting"),
				ColumnDef.num("floor.kills", "Enemies that died").shown().group("Fighting"),
				ColumnDef.num("floor.healed", "HP healed").group("Health"),
				ColumnDef.num("floor.shielded", "Shield gained").group("Health"),
				ColumnDef.num("floor.xp", "Experience gained").group("Growth"),
				ColumnDef.num("floor.levelUps", "Levels gained").group("Growth"),
				ColumnDef.num("floor.deaths", "Hero deaths").group("Fighting"),
				ColumnDef.num("floor.itemsSeen", "Items seen").since(1, 2).group("Items"),
				ColumnDef.num("floor.itemsAcquired", "Items picked up").since(1, 2).group("Items") };
		for (ColumnDef c : sums) d.add(c);
		//one count per registered kind of fact: how many times it happened during the visit
		for (FactKind k : FactKinds.all()){
			ColumnDef c = ColumnDef.num(factCountColumn(k), k.plural).group(k.area).describe("How many times this was recorded during the visit.");
			c.since(k.since.major, k.since.minor);
			if (k.isInternal()) c.internal();
			d.add(c);
		}
		context(d, "floor");
		d.link(new LinkDef("The run", Datasets.RUNS, "floor.run", com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.RUN_ID));
		return d;
	}

	public static String factCountColumn( FactKind k ){
		return "floor.n." + k.key;
	}

	static DatasetDef attacks(){
		DatasetDef d = new DatasetDef(ATTACKS, "Attacks", "Every attack, by the hero or against it, hit or miss.");
		d.minTelemetry = V13;
		d.countLabel = "Attacks";
		when(d, "attack", true);
		d.add(fighter(ColumnDef.enumeration("attack.byKind", "Attacker is")).shown().group("Attack"));
		d.add(ColumnDef.text("attack.attacker", "Attacker").shown().group("Attack"));
		d.add(ColumnDef.text("attack.defender", "Defender").shown().group("Attack"));
		d.add(yesNo(ColumnDef.enumeration("attack.hit", "Hit")).shown().group("Attack"));
		d.add(yesNo(ColumnDef.enumeration("attack.magic", "Spell or bolt")).group("Attack"));
		d.add(yesNo(ColumnDef.enumeration("attack.thrown", "Thrown")).group("Attack"));
		d.add(ColumnDef.text("attack.weapon", "Weapon").group("Attack").describe("Hero attacks: the weapon as you saw it."));
		d.add(ColumnDef.num("attack.rolled", "Damage rolled").internal().group("Hidden"));
		d.add(ColumnDef.num("attack.armor", "Armor took off").internal().group("Hidden"));
		d.add(ColumnDef.num("attack.effective", "Damage after armor").internal().group("Hidden"));
		d.add(ColumnDef.text("attack.attackerClass", "True attacker").internal().group("Hidden"));
		d.add(ColumnDef.text("attack.defenderClass", "True defender").internal().group("Hidden"));
		context(d, "attack");
		d.link(new LinkDef("The run", Datasets.RUNS, "attack.run", com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.RUN_ID));
		return d;
	}

	static DatasetDef damage(){
		DatasetDef d = new DatasetDef(DAMAGE, "Damage", "Every time something took damage that you could see.");
		d.minTelemetry = V13;
		d.countLabel = "Damage events";
		when(d, "damage", true);
		d.add(fighter(ColumnDef.enumeration("damage.targetKind", "Target is")).shown().group("Damage"));
		d.add(ColumnDef.text("damage.target", "Target").shown().group("Damage"));
		d.add(ColumnDef.text("damage.source", "Caused by").shown().group("Damage"));
		d.add(sourceKind(ColumnDef.enumeration("damage.sourceKind", "Cause kind")).group("Damage"));
		d.add(ColumnDef.num("damage.amount", "Damage").shown().group("Damage").describe("The number the game shows: HP lost plus what shielding absorbed."));
		d.add(yesNo(ColumnDef.enumeration("damage.killed", "Killed it")).group("Damage"));
		d.add(ColumnDef.num("damage.heroHp", "Hero HP after").group("Hero").describe("Only when the hero was hit."));
		d.add(ColumnDef.num("damage.heroHpMax", "Hero max HP").group("Hero"));
		d.add(ColumnDef.num("damage.shieldAbsorbed", "Absorbed by shield").group("Hero"));
		context(d, "damage");
		d.link(new LinkDef("The run", Datasets.RUNS, "damage.run", com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.RUN_ID));
		return d;
	}

	static DatasetDef heals(){
		DatasetDef d = new DatasetDef(HEALS, "Healing", "Every time the hero healed.");
		d.minTelemetry = V13;
		d.countLabel = "Heals";
		when(d, "heal", true);
		d.add(ColumnDef.num("heal.amount", "HP healed").shown().group("Heal"));
		d.add(ColumnDef.num("heal.hpAfter", "HP after").group("Heal"));
		d.add(ColumnDef.num("heal.hpMax", "Max HP").group("Heal"));
		d.add(yesNo(ColumnDef.enumeration("heal.inferred", "Worked out afterwards")).group("Heal")
				.describe("Older recordings could only see that HP went up, not why."));
		d.add(ColumnDef.text("heal.source", "What healed").internal().since(1, 6).group("Hidden"));
		context(d, "heal");
		d.link(new LinkDef("The run", Datasets.RUNS, "heal.run", com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.RUN_ID));
		return d;
	}

	static DatasetDef shields(){
		DatasetDef d = new DatasetDef(SHIELDS, "Shielding", "Every time the hero gained shield.");
		d.minTelemetry = V13;
		d.countLabel = "Shield gains";
		when(d, "shield", true);
		d.add(ColumnDef.num("shield.amount", "Shield gained").shown().group("Shield"));
		d.add(ColumnDef.num("shield.after", "Shield after").group("Shield"));
		d.add(ColumnDef.text("shield.source", "From").shown().group("Shield"));
		context(d, "shield");
		d.link(new LinkDef("The run", Datasets.RUNS, "shield.run", com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.RUN_ID));
		return d;
	}

	static DatasetDef xp(){
		DatasetDef d = new DatasetDef(XP, "Experience", "Every time the hero gained experience.");
		d.minTelemetry = V13;
		d.countLabel = "Gains";
		when(d, "xp", true);
		d.add(ColumnDef.num("xp.amount", "Experience").shown().group("Gain"));
		d.add(ColumnDef.num("xp.levelBefore", "Level before").group("Gain"));
		d.add(ColumnDef.enumeration("xp.category", "From").shown().group("Gain").label(CombatEvents.XP_KILL, "A kill").label(CombatEvents.XP_POTION, "Potion of Experience")
				.label(CombatEvents.XP_ASCENSION, "Ascension").label(CombatEvents.XP_OTHER, "Something else"));
		context(d, "xp");
		d.link(new LinkDef("The run", Datasets.RUNS, "xp.run", com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.RUN_ID));
		return d;
	}

	static DatasetDef equipment(){
		DatasetDef d = new DatasetDef(EQUIPMENT, "Equipment", "Everything the hero actually wore or wielded, and for how long.");
		d.minTelemetry = V12;
		d.countLabel = "Times worn";
		d.add(ColumnDef.text("equip.run", "Run").group("When"));
		d.add(ColumnDef.text("equip.item", "Item").shown().group("Item").describe("What the game called it when it was put on."));
		ColumnDef cat = ColumnDef.enumeration("equip.category", "Kind").shown().group("Item");
		for (Map.Entry<Long, String> e : Datasets.categories().entrySet()) cat.label(e.getKey(), e.getValue());
		d.add(cat);
		d.add(ColumnDef.enumeration("equip.slot", "Worn as").shown().group("Item").label(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.SLOT_WEAPON, "Weapon")
				.label(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.SLOT_ARMOR, "Armor").label(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.SLOT_ARTIFACT, "Artifact")
				.label(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.SLOT_MISC, "Misc").label(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.SLOT_RING, "Ring")
				.label(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.SLOT_SECOND_WEAPON, "Second weapon"));
		d.add(ColumnDef.num("equip.level", "Level you could see").group("Item").describe("Only when the level was known at the time."));
		d.add(yesNo(ColumnDef.enumeration("equip.cursedKnown", "Curse known")).group("Item"));
		d.add(yesNo(ColumnDef.enumeration("equip.identified", "Identified")).group("Item"));
		d.add(ColumnDef.text("equip.uid", "Item id").group("Item").describe("Joins this row to the item's whole history."));
		d.add(ColumnDef.num("equip.depth", "Floor put on").shown().group("When"));
		d.add(ColumnDef.num("equip.start", "Put on at turn").shown().group("When"));
		d.add(ColumnDef.num("equip.end", "Taken off at turn").group("When"));
		d.add(ColumnDef.num("equip.turns", "Turns worn").shown().group("When").describe("A lower bound while it is still worn."));
		d.add(yesNo(ColumnDef.enumeration("equip.stillWorn", "Still worn")).group("When"));
		context(d, "equip");
		d.link(new LinkDef("The run", Datasets.RUNS, "equip.run", com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.RUN_ID));
		return d;
	}

	static DatasetDef kills(){
		DatasetDef d = new DatasetDef(KILLS, "Kills", "Everything you saw die.");
		d.minTelemetry = V13;
		d.countLabel = "Kills";
		when(d, "kill", true);
		d.add(fighter(ColumnDef.enumeration("kill.victimKind", "Victim is")).shown().group("Kill"));
		d.add(ColumnDef.text("kill.victim", "Victim").shown().group("Kill"));
		d.add(ColumnDef.text("kill.killer", "Killed by").shown().group("Kill"));
		d.add(sourceKind(ColumnDef.enumeration("kill.killerKind", "Cause kind")).group("Kill"));
		d.add(ColumnDef.text("kill.victimClass", "True kind").internal().group("Hidden"));
		d.add(ColumnDef.num("kill.expValue", "XP it was worth").internal().group("Hidden"));
		context(d, "kill");
		d.link(new LinkDef("The run", Datasets.RUNS, "kill.run", com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.RUN_ID));
		return d;
	}

	public static List<DatasetDef> defs(){
		List<DatasetDef> l = new ArrayList<>();
		l.add(floors());
		l.add(attacks());
		l.add(damage());
		l.add(heals());
		l.add(shields());
		l.add(xp());
		l.add(kills());
		l.add(equipment());
		return l;
	}

	private static final Map<String, DatasetDef> CACHE = new HashMap<>();
	private static String cacheFingerprint = "";

	public static synchronized DatasetDef def( String id ){
		String fp = FactKinds.fingerprint();
		if (!fp.equals(cacheFingerprint)){
			CACHE.clear();
			cacheFingerprint = fp;
		}
		DatasetDef d = CACHE.get(id);
		if (d == null){
			switch (id){
				case FLOORS: d = floors(); break;
				case ATTACKS: d = attacks(); break;
				case DAMAGE: d = damage(); break;
				case HEALS: d = heals(); break;
				case SHIELDS: d = shields(); break;
				case XP: d = xp(); break;
				case KILLS: d = kills(); break;
				case EQUIPMENT: d = equipment(); break;
				default: return null;
			}
			CACHE.put(id, d);
		}
		return d;
	}

	//---- rows ----

	private static boolean atLeast( TelemetryVersion v, TelemetryVersion min ){
		return v.compareTo(min) >= 0;
	}

	private static Row newRow( DatasetDef d, LogMeta meta, String kind, int n ){
		Row row = new Row(meta.runId + "/" + kind + "/" + n + "@" + meta.logId, null, meta.runId, d.size());
		row.finished = meta.hasRunEnd;
		row.ruleset = meta.ruleset;
		row.telemetry = meta.telemetry;
		return row;
	}

	private static void num( Row row, DatasetDef d, String col, long v, int prov ){
		int i = d.indexOf(col);
		if (i >= 0) row.setNum(i, v, prov);
	}

	private static void text( Row row, DatasetDef d, String col, String v, int prov ){
		int i = d.indexOf(col);
		if (i >= 0) row.setText(i, v == null ? "" : v, prov);
	}

	private static void base( Row row, DatasetDef d, String p, LogMeta meta, long turn, int depth, int branch ){
		text(row, d, p + ".run", meta.runId, Fact.PROV_RECORDED);
		num(row, d, p + ".turn", turn, Fact.PROV_RECORDED);
		num(row, d, p + ".depth", depth, Fact.PROV_RECORDED);
		num(row, d, p + ".branch", branch, Fact.PROV_RECORDED);
	}

	private static final class Visit {
		int n, depth, branch;
		String entry = "", levelClass = "", feeling = "";
		long enter;
		long activeStart;
		boolean activeStartKnown;
		long[] sums = new long[SUM_COLUMNS.length];
		Map<String, Integer> facts = new HashMap<>();
	}

	private static final String[] SUM_COLUMNS = { "attacksByHero", "hitsByHero", "attacksOnHero", "hitsOnHero", "damageDealt", "damageTaken", "kills",
			"healed", "shielded", "xp", "levelUps", "deaths", "itemsSeen", "itemsAcquired" };
	private static final int A_BY = 0, H_BY = 1, A_ON = 2, H_ON = 3, DMG_OUT = 4, DMG_IN = 5, KILLS_I = 6, HEALED = 7, SHIELDED = 8, XP_I = 9,
			LEVEL_UPS = 10, DEATHS = 11, SEEN = 12, ACQ = 13;

	//the ApdEvent right after index i if it has this type (a detail event is written right after its public event), else null
	private static ApdEvent detailAfter( List<ApdEvent> events, int i, int type ){
		for (int k = i + 1; k < events.size() && k <= i + 4; k++){
			if (events.get(k).type == type) return events.get(k);
			int t = events.get(k).type;
			if (t == EventTypes.GAP || t == EventTypes.ATTACK || t == EventTypes.DAMAGE || t == EventTypes.KILL || t == EventTypes.HEAL) break;
		}
		return null;
	}

	//Adds the rows of every event dataset for one log. 'events' is the canonical timeline. Never throws for a bad event: it is skipped.
	static void addRows( LogRows out, List<ApdEvent> events, ReadResult r, LogMeta meta, TelemetryVersion v, int countProv ){
		DatasetDef eq = def(EQUIPMENT), fl = def(FLOORS), at = def(ATTACKS), dm = def(DAMAGE), he = def(HEALS), sh = def(SHIELDS), xpD = def(XP), ki = def(KILLS);
		boolean combat = atLeast(v, V13), items = atLeast(v, V12);
		List<Row> floorRows = new ArrayList<>(), atRows = new ArrayList<>(), dmRows = new ArrayList<>(), heRows = new ArrayList<>(),
				shRows = new ArrayList<>(), xpRows = new ArrayList<>(), kiRows = new ArrayList<>();
		List<Row> eqRows = new ArrayList<>();
		Map<String, Row> worn = new HashMap<>();
		int nEq = 0;
		Visit cur = null;
		int visits = 0, nAt = 0, nDm = 0, nHe = 0, nSh = 0, nXp = 0, nKi = 0;
		long lastClock = 0;
		//time actually played: the log holds the running total of the current stretch (it starts again after the app was closed), so a lower
		//value than the one before is a new stretch and counts from zero
		long activeTotal = 0, activePrev = -1;
		boolean haveActive = false;
		for (int i = 0; i < events.size(); i++){
			ApdEvent e = events.get(i);
			lastClock = Math.max(lastClock, e.clock);
			try {
				switch (e.type){
					case EventTypes.LEVEL_ENTER: {
						if (cur != null) floorRows.add(closeVisit(fl, meta, v, cur, e.clock, 2, countProv, true, activeTotal, haveActive));
						LifecycleEvents.LevelEnter l = LifecycleEvents.decodeLevelEnter(e, r);
						cur = new Visit();
						cur.n = visits++;
						cur.depth = l.depth;
						cur.branch = l.branch;
						cur.entry = l.entryMode;
						cur.levelClass = l.levelClass;
						cur.feeling = l.feeling;
						cur.enter = e.clock;
						cur.activeStart = activeTotal;
						cur.activeStartKnown = haveActive;
						break;
					}
					case EventTypes.LEVEL_EXIT: {
						LifecycleEvents.LevelExit x = LifecycleEvents.decodeLevelExit(e);
						if (cur != null){
							floorRows.add(closeVisit(fl, meta, v, cur, e.clock, x.reason == LifecycleEvents.EXIT_RUN_ENDED ? 1 : 0, countProv, false, activeTotal, haveActive));
							cur = null;
						}
						break;
					}
					case EventTypes.ATTACK: {
						if (!combat) break;
						CombatEvents.Attack a = CombatEvents.decodeAttack(e, r);
						boolean byHero = a.attacker.kind == CombatEvents.KIND_HERO, onHero = a.defender.kind == CombatEvents.KIND_HERO;
						if (cur != null){
							if (byHero){ cur.sums[A_BY]++; if (a.hit) cur.sums[H_BY]++; }
							if (onHero){ cur.sums[A_ON]++; if (a.hit) cur.sums[H_ON]++; }
						}
						Row row = newRow(at, meta, "attack", nAt++);
						base(row, at, "attack", meta, e.clock, a.where.depth, a.where.branch);
						num(row, at, "attack.byKind", a.attacker.kind, Fact.PROV_RECORDED);
						text(row, at, "attack.attacker", a.attacker.name, Fact.PROV_RECORDED);
						text(row, at, "attack.defender", a.defender.name, Fact.PROV_RECORDED);
						num(row, at, "attack.hit", a.hit ? 1 : 0, Fact.PROV_RECORDED);
						num(row, at, "attack.magic", a.magic ? 1 : 0, Fact.PROV_RECORDED);
						num(row, at, "attack.thrown", a.thrown ? 1 : 0, Fact.PROV_RECORDED);
						if (byHero) text(row, at, "attack.weapon", a.weaponName, Fact.PROV_RECORDED);
						ApdEvent de = detailAfter(events, i, EventTypes.ATTACK_DETAIL);
						CombatEvents.AttackDetail detail = null;
						try {
							if (de != null) detail = CombatEvents.decodeAttackDetail(de, r);
						} catch (RuntimeException badDetail){
							//The public attack remains usable; only optional hidden detail is unknown.
						}
						if (detail != null){
							num(row, at, "attack.rolled", detail.rolled, Fact.PROV_RECORDED);
							num(row, at, "attack.armor", detail.armorReduction, Fact.PROV_RECORDED);
							num(row, at, "attack.effective", detail.effective, Fact.PROV_RECORDED);
							text(row, at, "attack.attackerClass", detail.attackerClass, Fact.PROV_RECORDED);
							text(row, at, "attack.defenderClass", detail.defenderClass, Fact.PROV_RECORDED);
						}
						atRows.add(row);
						break;
					}
					case EventTypes.DAMAGE: {
						if (!combat) break;
						CombatEvents.Damage d = CombatEvents.decodeDamage(e, r);
						if (cur != null){
							if (d.target.kind == CombatEvents.KIND_HERO) cur.sums[DMG_IN] += d.shown;
							else if (d.target.kind == CombatEvents.KIND_ENEMY && d.source.kind == CombatEvents.SRC_CHAR && d.source.fighterKind == CombatEvents.KIND_HERO)
								cur.sums[DMG_OUT] += d.shown;
						}
						Row row = newRow(dm, meta, "damage", nDm++);
						base(row, dm, "damage", meta, e.clock, d.where.depth, d.where.branch);
						num(row, dm, "damage.targetKind", d.target.kind, Fact.PROV_RECORDED);
						text(row, dm, "damage.target", d.target.name, Fact.PROV_RECORDED);
						text(row, dm, "damage.source", d.source.label, Fact.PROV_RECORDED);
						num(row, dm, "damage.sourceKind", d.source.kind, Fact.PROV_RECORDED);
						num(row, dm, "damage.amount", d.shown, Fact.PROV_RECORDED);
						num(row, dm, "damage.killed", d.killed ? 1 : 0, Fact.PROV_RECORDED);
						if (d.heroHp >= 0) num(row, dm, "damage.heroHp", d.heroHp, Fact.PROV_RECORDED);
						if (d.heroHpMax >= 0) num(row, dm, "damage.heroHpMax", d.heroHpMax, Fact.PROV_RECORDED);
						if (d.shieldAbsorbed >= 0) num(row, dm, "damage.shieldAbsorbed", d.shieldAbsorbed, Fact.PROV_RECORDED);
						dmRows.add(row);
						break;
					}
					case EventTypes.HEAL: {
						if (!combat) break;
						CombatEvents.Heal h = CombatEvents.decodeHeal(e, r);
						if (cur != null) cur.sums[HEALED] += h.amount;
						Row row = newRow(he, meta, "heal", nHe++);
						base(row, he, "heal", meta, e.clock, h.where.depth, h.where.branch);
						num(row, he, "heal.amount", h.amount, Fact.PROV_RECORDED);
						num(row, he, "heal.hpAfter", h.hpAfter, Fact.PROV_RECORDED);
						num(row, he, "heal.hpMax", h.hpMax, Fact.PROV_RECORDED);
						num(row, he, "heal.inferred", (e.flags & CombatEvents.FLAG_INFERRED) != 0 ? 1 : 0, Fact.PROV_RECORDED);
						ApdEvent de = detailAfter(events, i, EventTypes.HEAL_DETAIL);
						try {
							if (de != null) text(row, he, "heal.source", CombatEvents.decodeHealDetail(de, r).source, Fact.PROV_RECORDED);
						} catch (RuntimeException badDetail){
							//Keep the readable public heal; its exact source remains unknown.
						}
						heRows.add(row);
						break;
					}
					case EventTypes.SHIELD_GAIN: {
						if (!combat) break;
						CombatEvents.ShieldGain s = CombatEvents.decodeShieldGain(e, r);
						if (cur != null) cur.sums[SHIELDED] += s.amount;
						Row row = newRow(sh, meta, "shield", nSh++);
						base(row, sh, "shield", meta, e.clock, s.where.depth, s.where.branch);
						num(row, sh, "shield.amount", s.amount, Fact.PROV_RECORDED);
						num(row, sh, "shield.after", s.shieldAfter, Fact.PROV_RECORDED);
						text(row, sh, "shield.source", s.source, Fact.PROV_RECORDED);
						shRows.add(row);
						break;
					}
					case EventTypes.XP_GAIN: {
						if (!combat) break;
						CombatEvents.XpGain x = CombatEvents.decodeXpGain(e, r);
						if (cur != null) cur.sums[XP_I] += x.amount;
						Row row = newRow(xpD, meta, "xp", nXp++);
						base(row, xpD, "xp", meta, e.clock, x.where.depth, x.where.branch);
						num(row, xpD, "xp.amount", x.amount, Fact.PROV_RECORDED);
						num(row, xpD, "xp.levelBefore", x.levelBefore, Fact.PROV_RECORDED);
						num(row, xpD, "xp.category", x.category, Fact.PROV_RECORDED);
						xpRows.add(row);
						break;
					}
					case EventTypes.LEVEL_UP:
						if (combat && cur != null) cur.sums[LEVEL_UPS]++;
						break;
					case EventTypes.HERO_DEATH:
						if (combat && cur != null) cur.sums[DEATHS]++;
						break;
					case EventTypes.KILL: {
						if (!combat) break;
						CombatEvents.Kill k = CombatEvents.decodeKill(e, r);
						if (cur != null && k.victim.kind == CombatEvents.KIND_ENEMY) cur.sums[KILLS_I]++;
						Row row = newRow(ki, meta, "kill", nKi++);
						base(row, ki, "kill", meta, e.clock, k.where.depth, k.where.branch);
						num(row, ki, "kill.victimKind", k.victim.kind, Fact.PROV_RECORDED);
						text(row, ki, "kill.victim", k.victim.name, Fact.PROV_RECORDED);
						text(row, ki, "kill.killer", k.killer.label, Fact.PROV_RECORDED);
						num(row, ki, "kill.killerKind", k.killer.kind, Fact.PROV_RECORDED);
						ApdEvent de = detailAfter(events, i, EventTypes.KILL_DETAIL);
						CombatEvents.KillDetail detail = null;
						try {
							if (de != null) detail = CombatEvents.decodeKillDetail(de, r);
						} catch (RuntimeException badDetail){
							//The public kill remains usable; only optional hidden detail is unknown.
						}
						if (detail != null){
							text(row, ki, "kill.victimClass", detail.victimClass, Fact.PROV_RECORDED);
							num(row, ki, "kill.expValue", detail.expValue, Fact.PROV_RECORDED);
						}
						kiRows.add(row);
						break;
					}
					case EventTypes.ITEM_EQUIPPED: {
						if (!items) break;
						ItemEvents.Equip q = ItemEvents.decodeEquip(e, r);
						if (worn.containsKey(q.uid)) break;   //already worn: nothing new
						Row row = newRow(eq, meta, "equip", nEq++);
						text(row, eq, "equip.run", meta.runId, Fact.PROV_RECORDED);
						text(row, eq, "equip.item", q.state.name, Fact.PROV_RECORDED);
						num(row, eq, "equip.category", q.state.category, Fact.PROV_RECORDED);
						num(row, eq, "equip.slot", q.slot, Fact.PROV_RECORDED);
						if (q.state.levelKnown) num(row, eq, "equip.level", q.state.visibleLevel, Fact.PROV_RECORDED);
						num(row, eq, "equip.cursedKnown", q.state.cursedKnown ? 1 : 0, Fact.PROV_RECORDED);
						num(row, eq, "equip.identified", q.state.identified ? 1 : 0, Fact.PROV_RECORDED);
						text(row, eq, "equip.uid", q.uid, Fact.PROV_RECORDED);
						num(row, eq, "equip.depth", q.where.depth, Fact.PROV_RECORDED);
						num(row, eq, "equip.start", e.clock, Fact.PROV_RECORDED);
						num(row, eq, "equip.stillWorn", 1, Fact.PROV_RECORDED);
						worn.put(q.uid, row);
						eqRows.add(row);
						break;
					}
					case EventTypes.ITEM_UNEQUIPPED: {
						if (!items) break;
						ItemEvents.Equip q = ItemEvents.decodeEquip(e, r);
						Row row = worn.remove(q.uid);
						if (row == null) break;   //taken off something the log never saw put on
						num(row, eq, "equip.end", e.clock, Fact.PROV_RECORDED);
						num(row, eq, "equip.turns", Math.max(0, e.clock - row.num(eq.indexOf("equip.start"))), Fact.PROV_RECORDED);
						num(row, eq, "equip.stillWorn", 0, Fact.PROV_RECORDED);
						break;
					}
					case EventTypes.ITEM_SEEN:
						if (items && cur != null) cur.sums[SEEN]++;
						break;
					case EventTypes.ITEM_ACQUIRED:
						if (items && cur != null) cur.sums[ACQ]++;
						break;
					default:
						if (cur != null && FactEvents.isFactEvent(e.type)){
							for (FactEvents.Observation o : FactEvents.decode(e, r)){
								Integer c = cur.facts.get(o.key);
								cur.facts.put(o.key, c == null ? 1 : c + 1);
							}
						}
						if (FactEvents.isFactEvent(e.type)){
							for (FactEvents.Observation o : FactEvents.decode(e, r)){
								if (o.key.equals("time.active") && o.values.length > 0){
									long v2 = o.values[0];
									activeTotal += activePrev >= 0 && v2 >= activePrev ? v2 - activePrev : v2;
									activePrev = v2;
									haveActive = true;
								}
							}
						}
				}
			} catch (RuntimeException bad){
				//an event this reader cannot decode contributes nothing
			}
		}
		//still worn when the log ends: how long so far is only a lower bound
		for (Row row : worn.values()) num(row, eq, "equip.turns", Math.max(0, lastClock - row.num(eq.indexOf("equip.start"))), Fact.PROV_INCOMPLETE);
		//a floor the hero was still on when the log ends: how long so far is only a lower bound
		if (cur != null) floorRows.add(closeVisit(fl, meta, v, cur, lastClock, 2, countProv, true, activeTotal, haveActive));
		if (atLeast(v, V11)) out.rows.put(FLOORS, floorRows);
		if (items) out.rows.put(EQUIPMENT, eqRows);
		if (combat){
			out.rows.put(ATTACKS, atRows);
			out.rows.put(DAMAGE, dmRows);
			out.rows.put(HEALS, heRows);
			out.rows.put(SHIELDS, shRows);
			out.rows.put(XP, xpRows);
			out.rows.put(KILLS, kiRows);
		}
	}

	private static Row closeVisit( DatasetDef d, LogMeta meta, TelemetryVersion v, Visit c, long exitClock, int exit, int countProv, boolean open,
	                              long activeTotal, boolean haveActive ){
		Row row = newRow(d, meta, "floor", c.n);
		text(row, d, "floor.run", meta.runId, Fact.PROV_RECORDED);
		num(row, d, "floor.visit", c.n + 1, Fact.PROV_RECORDED);
		num(row, d, "floor.depth", c.depth, Fact.PROV_RECORDED);
		num(row, d, "floor.branch", c.branch, Fact.PROV_RECORDED);
		text(row, d, "floor.entry", c.entry, Fact.PROV_RECORDED);
		text(row, d, "floor.levelClass", c.levelClass, Fact.PROV_RECORDED);
		text(row, d, "floor.feeling", c.feeling, Fact.PROV_RECORDED);
		num(row, d, "floor.enterTurn", c.enter, Fact.PROV_RECORDED);
		int tprov = open ? Fact.PROV_INCOMPLETE : Fact.PROV_RECORDED;
		if (!open) num(row, d, "floor.exitTurn", exitClock, Fact.PROV_RECORDED);
		num(row, d, "floor.turns", Math.max(0, exitClock - c.enter), tprov);
		num(row, d, "floor.exit", exit, Fact.PROV_RECORDED);
		if (c.activeStartKnown && haveActive) num(row, d, "floor.activeMillis", Math.max(0, activeTotal - c.activeStart), tprov);
		boolean combat = atLeast(v, V13), items = atLeast(v, V12);
		for (int i = 0; i < SUM_COLUMNS.length; i++){
			boolean known = i >= SEEN ? items : combat;
			if (known) num(row, d, "floor." + SUM_COLUMNS[i], c.sums[i], countProv);
		}
		for (FactKind k : FactKinds.all()){
			if (!atLeast(v, k.since)) continue;
			Integer n = c.facts.get(k.key);
			num(row, d, factCountColumn(k), n == null ? 0 : n, countProv);
		}
		return row;
	}
}
