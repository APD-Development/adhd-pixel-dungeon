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
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadStatus;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFactsBuilder;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Turns one raw log into the rows of every dataset. Pure and read-only: the log is the source of truth, rows are a view of it that
//can always be rebuilt (and are cached per log, see RowCache). Only the canonical timeline counts: rolled-back play is left out.
//
//What a PUBLIC cell and a public row may contain is decided here: they are made only from PUBLIC events. Anything that comes
//from INTERNAL events goes into INTERNAL columns, and a row that exists only because of INTERNAL events (an item or an enemy
//the player never met) is not a public row.
public final class RowBuilders {

	private RowBuilders(){}

	public static final int BUILDER_VERSION = 18;

	private static final DatasetDef RUNS = Datasets.runs();
	private static final DatasetDef ITEMS = Datasets.items();
	private static final DatasetDef ENEMIES = Datasets.enemies();
	private static final DatasetDef DEATHS = Datasets.deaths();

	public static DatasetDef def( String dataset ){
		switch (dataset){
			case Datasets.RUNS: return RUNS;
			case Datasets.ITEMS: return ITEMS;
			case Datasets.ENEMIES: return ENEMIES;
			case Datasets.DEATHS: return DEATHS;
			default: {
				DatasetDef d = EventDatasets.def(dataset);
				if (d == null) d = FactDatasets.def(dataset);
				return d != null ? d : DEATHS;
			}
		}
	}

	private static boolean atLeast( TelemetryVersion v, int major, int minor ){
		return v.compareTo(new TelemetryVersion(major, minor)) >= 0;
	}

	//---- one APD run log ----

	public static LogRows fromLog( ReadResult r, String logId ){
		LogRows out = new LogRows();
		LogMeta meta = out.meta;
		meta.logId = logId;
		meta.runId = logId;
		RunFacts facts = RunFactsBuilder.fromLog(r, logId);
		boolean readable = r != null && r.header != null && !r.newerThanReader();
		if (!readable){
			meta.unreadable = true;
			out.of(Datasets.RUNS).add(runRow(facts, meta, null));
			return out;
		}
		TelemetryVersion v = r.header.telemetryVersion;
		meta.telemetry = v.toString();
		meta.createdAtMillis = r.header.createdAtMillis;
		if (facts.isKnown(FactKeys.RUN_ID) && !facts.textOr(FactKeys.RUN_ID, "").isEmpty()) meta.runId = facts.textOr(FactKeys.RUN_ID, logId);
		meta.segment = (int) facts.numOr(FactKeys.SEGMENT, 0);
		meta.rankingId = facts.textOr(FactKeys.RANKING_ID, "");

		List<ApdEvent> events = RunTimeline.of(r.events).canonical();
		String startRuleset = facts.isKnown(FactKeys.RULESET) && !facts.textOr(FactKeys.RULESET, "").isEmpty() ? facts.textOr(FactKeys.RULESET, "") : null;
		int rulesetChanges = 0;
		long endClock = -1;
		long lastClock = 0;
		for (ApdEvent e : events){
			//the moment a run "was last seen" comes only from events the player could see: a hidden event must not move a public number
			if (EventTypes.visibility(e.type) == EventTypes.Visibility.PUBLIC) lastClock = Math.max(lastClock, e.clock);
			if (e.type == EventTypes.RUN_END){
				try {
					LifecycleEvents.decodeRunEnd(e, r);
					meta.hasRunEnd = true;
					endClock = e.clock;
				} catch (RuntimeException ignored){
					//A frame can be checksum-valid while its typed payload is damaged.
					//Do not expose hidden run details or label the run finished without a readable end.
				}
			} else if (e.type == EventTypes.RULESET_CHANGE){
				rulesetChanges++;
			}
		}
		meta.ruleset = rulesetChanges > 0 ? Row.RULESET_MIXED : startRuleset;
		boolean gaps = r.gapEvents > 0 || facts.isKnown(FactKeys.PARTIAL) || !r.status.isClean();
		// Valid framing does not guarantee readable typed payloads. RunFactsBuilder
		// already marks surviving counts incomplete when decoding loses an event.
		for (Fact fact : facts.all().values()){
			if (fact.prov == Fact.PROV_INCOMPLETE){
				gaps = true;
				break;
			}
		}
		int countProv = gaps ? Fact.PROV_INCOMPLETE : Fact.PROV_RECORDED;

		out.of(Datasets.RUNS).add(runRow(facts, meta, v));
		if (atLeast(v, 1, 2)) out.rows.put(Datasets.ITEMS, itemRows(events, r, meta, endClock >= 0 ? endClock : lastClock, endClock >= 0, countProv));
		if (atLeast(v, 1, 3)) out.rows.put(Datasets.ENEMIES, enemyRows(events, r, meta, v, countProv));
		if (atLeast(v, 1, 1)) out.rows.put(Datasets.DEATHS, deathRows(events, r, meta, v));
		if (atLeast(v, 1, 1)) EventDatasets.addRows(out, events, r, meta, v, countProv);
		if (atLeast(v, 1, 8)) FactDatasets.addRows(out, events, r, meta);
		return out;
	}

	private static Row runRow( RunFacts facts, LogMeta meta, TelemetryVersion v ){
		Row row = new Row(meta.logId, meta.runId, meta.runId, RUNS.size());
		row.finished = meta.hasRunEnd;
		row.ruleset = meta.ruleset;
		row.telemetry = meta.telemetry;
		for (Map.Entry<String, Fact> e : facts.all().entrySet()){
			int i = RUNS.indexOf(e.getKey());
			if (i >= 0) row.setFact(i, e.getValue());
		}
		row.setNum(RUNS.indexOf("run.segments"), 1, Fact.PROV_RECORDED);
		row.setNum(RUNS.indexOf("run.source"), 1, Fact.PROV_RECORDED);
		if (!meta.telemetry.isEmpty()) row.setText(RUNS.indexOf("run.telemetry"), meta.telemetry, Fact.PROV_RECORDED);
		//the start of the run has an unknown ruleset, a changed one is mixed (see LogMeta)
		return row;
	}

	//---- items ----

	private static final class ItemAcc {
		final String uid;
		ItemEvents.VisibleState last;
		boolean seen, acquired, equipped;
		int firstSeenDepth = -1, acquiredDepth = -1;
		long acquiredTurn = -1;
		int equipCount;
		long turnsEquipped;
		long openSince = -1;
		final Set<Integer> floors = new HashSet<>();
		//index of the first event the player could see about this item (-1: none yet): rows are listed in that order, so the
		//order never depends on hidden events
		long firstPublic = -1;
		boolean hasTruth;
		//the state the item was registered in when it was GENERATED (before play could change it): only then is it the as-generated state
		boolean genState;
		int genLevel;
		boolean genCursed;
		String genEnchant = "";
		int origin;
		String trueClass = "";
		int trueLevel;
		boolean cursed;
		String enchant = "";
		boolean hasGen;
		int genDepth;
		int container;
		String room = "";
		int drawKind;
		String requestedCategory = "";

		ItemAcc( String uid ){
			this.uid = uid;
		}
	}

	private static final String[] CONTAINERS = { "On the floor", "Chest", "Locked chest", "Crystal chest", "Tomb", "Skeleton", "Remains",
			"For sale", "Mimic", "Statue", "Quest reward", "Enemy loot", "Bonus drop" };
	private static final String[] DRAWS = { "Any item", "From a category", "From defaults", "From defaults, category", "By class", "Artifact" };

	//Kinds of item whose class is plain to see in the game: a weapon, armor, wand, artifact or trinket always shows what it is. Rings,
	//potions and scrolls (unlearned colours, gems and runes) are not here, and neither is "other", which is not understood well enough.
	public static boolean kindIsVisible( int category ){
		switch (category){
			case ItemEvents.CAT_WEAPON: case ItemEvents.CAT_MISSILE: case ItemEvents.CAT_ARMOR: case ItemEvents.CAT_ARTIFACT:
			case ItemEvents.CAT_TRINKET: case ItemEvents.CAT_WAND: case ItemEvents.CAT_FOOD: case ItemEvents.CAT_BAG:
			case ItemEvents.CAT_STONE: case ItemEvents.CAT_SPELL: case ItemEvents.CAT_BOMB: case ItemEvents.CAT_TOOL: case ItemEvents.CAT_SEED:
				return true;
			default:
				return false;
		}
	}

	private static ItemAcc acc( Map<String, ItemAcc> m, String uid ){
		ItemAcc a = m.get(uid);
		if (a == null){
			a = new ItemAcc(uid);
			m.put(uid, a);
		}
		return a;
	}

	private static void seenAt( ItemAcc a, int depth ){
		if (a.firstSeenDepth < 0 || depth < a.firstSeenDepth) a.firstSeenDepth = depth;
	}

	private static List<Row> itemRows( List<ApdEvent> events, ReadResult r, LogMeta meta, long endClock, boolean ended, int countProv ){
		Map<String, ItemAcc> items = new LinkedHashMap<>();
		Set<ItemAcc> open = new HashSet<>();
		//kinds of unidentified items (ring gems, potion colours, scroll runes) the player has learned so far, from ITEM_CLASS_KNOWN
		Set<String> knownKinds = new HashSet<>();
		int curDepth = 0;
		for (ApdEvent e : events){
			try {
				if (EventTypes.visibility(e.type) == EventTypes.Visibility.PUBLIC && isItemEvent(e.type)){
					String uid = uidOf(e, r);
					if (uid != null){
						ItemAcc a = acc(items, uid);
						if (a.firstPublic < 0) a.firstPublic = e.index;
					}
				}
				switch (e.type){
					case EventTypes.LEVEL_ENTER: {
						curDepth = LifecycleEvents.decodeLevelEnter(e, r).depth;
						for (ItemAcc a : open) a.floors.add(curDepth);
						break;
					}
					case EventTypes.ITEM_CLASS_KNOWN: {
						knownKinds.add(ItemEvents.decodeClassKnown(e, r).className);
						break;
					}
					case EventTypes.ITEM_REGISTERED: {
						ItemEvents.Registered g = ItemEvents.decodeRegistered(e, r);
						ItemAcc a = acc(items, g.uid);
						a.hasTruth = true;
						a.origin = g.origin;
						a.trueClass = g.trueClass;
						a.trueLevel = g.trueLevel;
						a.cursed = g.cursed;
						a.enchant = g.enchant;
						if (g.origin == ItemEvents.ORIGIN_GENERATED){
							a.genState = true;
							a.genLevel = g.trueLevel;
							a.genCursed = g.cursed;
							a.genEnchant = g.enchant;
						}
						break;
					}
					case EventTypes.ITEM_TRUTH_CHANGED: {
						ItemEvents.TruthChanged t = ItemEvents.decodeTruthChanged(e, r);
						ItemAcc a = acc(items, t.uid);
						a.trueLevel = t.newLevel;
						a.cursed = t.newCursed;
						a.enchant = t.newEnchant;
						break;
					}
					case EventTypes.ITEM_GENERATED: {
						GenerationEvents.ItemGenerated g = GenerationEvents.decodeItemGenerated(e, r);
						ItemAcc a = acc(items, g.uid);
						a.hasGen = true;
						a.genDepth = g.where.depth;
						a.container = g.container;
						a.room = g.room;
						a.drawKind = g.drawKind;
						a.requestedCategory = g.requestedCategory;
						break;
					}
					case EventTypes.ITEM_SEEN: {
						ItemEvents.Seen s = ItemEvents.decodeSeen(e, r);
						ItemAcc a = acc(items, s.uid);
						a.seen = true;
						a.last = s.state;
						seenAt(a, s.where.depth);
						break;
					}
					case EventTypes.ITEM_ACQUIRED: {
						ItemEvents.Acquired q = ItemEvents.decodeAcquired(e, r);
						ItemAcc a = acc(items, q.uid);
						if (!a.acquired){
							a.acquiredDepth = q.where.depth;
							a.acquiredTurn = e.clock;
						}
						a.acquired = true;
						a.last = q.state;
						seenAt(a, q.where.depth);
						break;
					}
					case EventTypes.ITEM_LEFT: {
						ItemEvents.Left l = ItemEvents.decodeLeft(e, r);
						acc(items, l.uid).last = l.state;
						break;
					}
					case EventTypes.ITEM_KNOWLEDGE: {
						ItemEvents.Knowledge k = ItemEvents.decodeKnowledge(e, r);
						acc(items, k.uid).last = k.state;
						break;
					}
					case EventTypes.ITEM_EQUIPPED: {
						ItemEvents.Equip q = ItemEvents.decodeEquip(e, r);
						ItemAcc a = acc(items, q.uid);
						a.equipped = true;
						a.equipCount++;
						a.last = q.state;
						if (a.openSince < 0){
							a.openSince = e.clock;
							a.floors.add(q.where.depth);
							open.add(a);
						}
						break;
					}
					case EventTypes.ITEM_UNEQUIPPED: {
						ItemEvents.Equip q = ItemEvents.decodeEquip(e, r);
						ItemAcc a = acc(items, q.uid);
						a.last = q.state;
						if (a.openSince >= 0){
							a.turnsEquipped += Math.max(0, e.clock - a.openSince);
							a.openSince = -1;
							open.remove(a);
						}
						break;
					}
					default:
				}
			} catch (RuntimeException bad){
				//an event this reader cannot decode contributes nothing
			}
		}
		for (ItemAcc a : open) a.turnsEquipped += Math.max(0, endClock - a.openSince);

		List<ItemAcc> ordered = new ArrayList<>(items.values());
		java.util.Collections.sort(ordered, new java.util.Comparator<ItemAcc>(){
			@Override
			public int compare( ItemAcc a, ItemAcc b ){
				boolean pa = a.firstPublic >= 0, pb = b.firstPublic >= 0;
				if (pa != pb) return pa ? -1 : 1;
				if (pa) return a.firstPublic < b.firstPublic ? -1 : a.firstPublic > b.firstPublic ? 1 : 0;
				return a.uid.compareTo(b.uid);
			}
		});
		List<Row> rows = new ArrayList<>();
		for (ItemAcc a : ordered){
			Row row = new Row(meta.runId + "/" + a.uid, meta.runId + "/" + a.uid, meta.runId, ITEMS.size());
			row.finished = meta.hasRunEnd;
			row.ruleset = meta.ruleset;
			row.telemetry = meta.telemetry;
			boolean player = a.seen || a.acquired || a.last != null;
			row.publicRow = player;
			put(row, "item.run", meta.runId, Fact.PROV_RECORDED);
			boolean openAtEnd = !ended && a.openSince >= 0;
			int timeProv = countProv == Fact.PROV_INCOMPLETE || openAtEnd ? Fact.PROV_INCOMPLETE : Fact.PROV_RECORDED;
			if (player && a.last != null){
				put(row, "item.name", a.last.name, Fact.PROV_RECORDED);
				put(row, "item.category", a.last.category, Fact.PROV_RECORDED);
				//The stable kind is built from hidden truth (the true class), so it is only used where the player knows it: always for
				//kinds that show what they are (a weapon is a Glaive however it is enchanted), and for a ring, potion or scroll only once
				//its kind was learned. Otherwise the name the player saw stands in, and is marked as such.
				if (a.hasTruth && !a.trueClass.isEmpty() && (kindIsVisible(a.last.category) || knownKinds.contains(a.trueClass))){
					put(row, "item.type", a.trueClass, Fact.PROV_RECORDED);
				} else if (!a.last.name.isEmpty()){
					put(row, "item.type", CellFormatter.NAME_KEY_PREFIX + a.last.name, Fact.PROV_INFERRED);
				}
				put(row, "item.identified", a.last.identified ? 1 : 0, Fact.PROV_RECORDED);
				if (a.last.levelKnown) put(row, "item.visibleLevel", a.last.visibleLevel, Fact.PROV_RECORDED);
			}
			int fate = a.equipped ? Datasets.FATE_EQUIPPED : a.acquired ? Datasets.FATE_NEVER_EQUIPPED : a.seen ? Datasets.FATE_LEFT_BEHIND : Datasets.FATE_NEVER_SEEN;
			put(row, "item.fate", fate, countProv);
			put(row, "item.seen", a.seen || a.acquired ? 1 : 0, countProv);
			put(row, "item.acquired", a.acquired ? 1 : 0, countProv);
			put(row, "item.equipped", a.equipped ? 1 : 0, countProv);
			if (a.firstSeenDepth >= 0) put(row, "item.firstSeenDepth", a.firstSeenDepth, Fact.PROV_RECORDED);
			if (a.acquired){
				put(row, "item.acquiredDepth", a.acquiredDepth, Fact.PROV_RECORDED);
				put(row, "item.acquiredTurn", a.acquiredTurn, Fact.PROV_RECORDED);
			}
			put(row, "item.equipCount", a.equipCount, countProv);
			put(row, "item.turnsEquipped", a.turnsEquipped, timeProv);
			put(row, "item.floorsEquipped", a.floors.size(), timeProv);
			if (a.hasTruth){
				put(row, "item.trueClass", a.trueClass, Fact.PROV_RECORDED);
				put(row, "item.trueLevel", a.trueLevel, Fact.PROV_RECORDED);
				put(row, "item.cursed", a.cursed ? 1 : 0, Fact.PROV_RECORDED);
				put(row, "item.enchant", a.enchant, Fact.PROV_RECORDED);
				put(row, "item.origin", a.origin, Fact.PROV_RECORDED);
				//the state at generation is recorded only for an item whose first record was its generation; for any other the later state says nothing about it
				if (a.genState){
					put(row, "item.genLevel", a.genLevel, Fact.PROV_RECORDED);
					put(row, "item.genCursed", a.genCursed ? 1 : 0, Fact.PROV_RECORDED);
					put(row, "item.genEnchant", a.genEnchant, Fact.PROV_RECORDED);
				}
			}
			if (a.hasGen){
				put(row, "item.genDepth", a.genDepth, Fact.PROV_RECORDED);
				put(row, "item.container", a.container >= 0 && a.container < CONTAINERS.length ? CONTAINERS[a.container] : "Other", Fact.PROV_RECORDED);
				put(row, "item.room", a.room, Fact.PROV_RECORDED);
				put(row, "item.drawKind", a.drawKind >= 0 && a.drawKind < DRAWS.length ? DRAWS[a.drawKind] : "Not drawn", Fact.PROV_RECORDED);
				put(row, "item.requestedCategory", a.requestedCategory, Fact.PROV_RECORDED);
			}
			rows.add(row);
		}
		return rows;
	}

	private static boolean isItemEvent( int type ){
		return type == EventTypes.ITEM_SEEN || type == EventTypes.ITEM_ACQUIRED || type == EventTypes.ITEM_LEFT
				|| type == EventTypes.ITEM_EQUIPPED || type == EventTypes.ITEM_UNEQUIPPED || type == EventTypes.ITEM_KNOWLEDGE;
	}

	private static String uidOf( ApdEvent e, ReadResult r ){
		switch (e.type){
			case EventTypes.ITEM_SEEN: return ItemEvents.decodeSeen(e, r).uid;
			case EventTypes.ITEM_ACQUIRED: return ItemEvents.decodeAcquired(e, r).uid;
			case EventTypes.ITEM_LEFT: return ItemEvents.decodeLeft(e, r).uid;
			case EventTypes.ITEM_EQUIPPED:
			case EventTypes.ITEM_UNEQUIPPED: return ItemEvents.decodeEquip(e, r).uid;
			case EventTypes.ITEM_KNOWLEDGE: return ItemEvents.decodeKnowledge(e, r).uid;
			default: return null;
		}
	}

	private static void put( Row row, String col, long v, int prov ){
		DatasetDef d = defOf(row);
		int i = d.indexOf(col);
		if (i >= 0) row.setNum(i, v, prov);
	}

	private static void put( Row row, String col, String v, int prov ){
		DatasetDef d = defOf(row);
		int i = d.indexOf(col);
		if (i >= 0) row.setText(i, v, prov);
	}

	//rows only know their width, and each dataset has a different one
	private static DatasetDef defOf( Row row ){
		int n = row.size();
		if (n == ITEMS.size()) return ITEMS;
		if (n == ENEMIES.size()) return ENEMIES;
		if (n == DEATHS.size()) return DEATHS;
		return RUNS;
	}

	//---- enemies ----

	//One row per enemy NAME the player met (what the game called it): the identity of a public row is never a hidden class. The
	//true classes behind a name are in a hidden column. Classes that were generated but never met in a fight get hidden rows.
	private static final class EnemyAcc {
		String name = "";
		final Set<String> classes = new java.util.LinkedHashSet<>();
		long firstPublic = -1;
		long attacksOnHero, hitsOnHero, attacksByHero, hitsByHero, damageToHero, damageFromHero, kills, deaths, heroDeaths;
		long expValue = -1;
	}

	private static String shortClass( String c ){
		int dot = c.lastIndexOf('.');
		return dot >= 0 ? c.substring(dot + 1) : c;
	}

	private static EnemyAcc enemy( Map<String, EnemyAcc> m, String name, String cls, long index ){
		EnemyAcc a = m.get(name);
		if (a == null){
			a = new EnemyAcc();
			a.name = name;
			m.put(name, a);
		}
		if (a.firstPublic < 0) a.firstPublic = index;
		if (cls != null && !cls.isEmpty()) a.classes.add(cls);
		return a;
	}

	//the detail event that goes with a public combat event is written right after it (a few events at most)
	private static ApdEvent detailAfter( List<ApdEvent> events, int i, int type ){
		for (int k = i + 1; k < events.size() && k <= i + 3; k++){
			if (events.get(k).type == type) return events.get(k);
			int next = events.get(k).type;
			if (next == EventTypes.GAP || next == EventTypes.ATTACK || next == EventTypes.DAMAGE
					|| next == EventTypes.KILL || next == EventTypes.HEAL) break;
		}
		return null;
	}

	private static List<Row> enemyRows( List<ApdEvent> events, ReadResult r, LogMeta meta, TelemetryVersion v, int countProv ){
		Map<String, EnemyAcc> enemies = new LinkedHashMap<>();
		Map<String, Long> generated = new LinkedHashMap<>();
		Map<String, String> classOfName = new LinkedHashMap<>();
		for (int i = 0; i < events.size(); i++){
			ApdEvent e = events.get(i);
			try {
				switch (e.type){
					case EventTypes.ATTACK: {
						CombatEvents.Attack a = CombatEvents.decodeAttack(e, r);
						ApdEvent de = detailAfter(events, i, EventTypes.ATTACK_DETAIL);
						CombatEvents.AttackDetail d = null;
						try {
							if (de != null) d = CombatEvents.decodeAttackDetail(de, r);
						} catch (RuntimeException badDetail){
							//The public attack is still readable; only its hidden identity detail was lost.
						}
						if (a.attacker.kind == CombatEvents.KIND_HERO && a.defender.kind == CombatEvents.KIND_ENEMY){
							EnemyAcc x = enemy(enemies, a.defender.name, d == null ? "" : d.defenderClass, e.index);
							x.attacksByHero++;
							if (a.hit) x.hitsByHero++;
							if (d != null && !d.defenderClass.isEmpty()) classOfName.put(a.defender.name, d.defenderClass);
						} else if (a.defender.kind == CombatEvents.KIND_HERO && a.attacker.kind == CombatEvents.KIND_ENEMY){
							EnemyAcc x = enemy(enemies, a.attacker.name, d == null ? "" : d.attackerClass, e.index);
							x.attacksOnHero++;
							if (a.hit) x.hitsOnHero++;
							if (d != null && !d.attackerClass.isEmpty()) classOfName.put(a.attacker.name, d.attackerClass);
						}
						break;
					}
					case EventTypes.DAMAGE: {
						CombatEvents.Damage dm = CombatEvents.decodeDamage(e, r);
						ApdEvent de = detailAfter(events, i, EventTypes.DAMAGE_DETAIL);
						CombatEvents.DamageDetail d = null;
						try {
							if (de != null) d = CombatEvents.decodeDamageDetail(de, r);
						} catch (RuntimeException badDetail){
							//Keep the readable public damage even when its hidden detail is malformed.
						}
						if (dm.target.kind == CombatEvents.KIND_HERO && dm.source.kind == CombatEvents.SRC_CHAR
								&& dm.source.fighterKind == CombatEvents.KIND_ENEMY){
							enemy(enemies, dm.source.label, d == null ? "" : d.srcClass, e.index).damageToHero += dm.shown;
						} else if (dm.target.kind == CombatEvents.KIND_ENEMY && dm.source.kind == CombatEvents.SRC_CHAR
								&& dm.source.fighterKind == CombatEvents.KIND_HERO){
							enemy(enemies, dm.target.name, d == null ? "" : d.targetClass, e.index).damageFromHero += dm.shown;
						}
						break;
					}
					case EventTypes.KILL: {
						CombatEvents.Kill k = CombatEvents.decodeKill(e, r);
						ApdEvent de = detailAfter(events, i, EventTypes.KILL_DETAIL);
						CombatEvents.KillDetail d = null;
						try {
							if (de != null) d = CombatEvents.decodeKillDetail(de, r);
						} catch (RuntimeException badDetail){
							//Keep the readable public kill even when its hidden detail is malformed.
						}
						if (k.victim.kind == CombatEvents.KIND_ENEMY){
							EnemyAcc x = enemy(enemies, k.victim.name, d == null ? "" : d.victimClass, e.index);
							x.deaths++;
							if (k.killer.kind == CombatEvents.SRC_CHAR && k.killer.fighterKind == CombatEvents.KIND_HERO) x.kills++;
							if (d != null) x.expValue = d.expValue;
						}
						break;
					}
					case EventTypes.HERO_DEATH: {
						CombatEvents.HeroDeath hd = CombatEvents.decodeHeroDeath(e, r);
						if (hd.cause.kind == CombatEvents.SRC_CHAR && hd.cause.fighterKind == CombatEvents.KIND_ENEMY){
							enemy(enemies, hd.cause.label, classOfName.get(hd.cause.label), e.index).heroDeaths++;
						}
						break;
					}
					case EventTypes.MOB_GENERATED: {
						GenerationEvents.MobGenerated g = GenerationEvents.decodeMobGenerated(e, r);
						//allies and neutral creatures are not enemies
						if (g.alignment == 0){
							Long n = generated.get(g.mobClass);
							generated.put(g.mobClass, n == null ? 1 : n + 1);
						}
						break;
					}
					default:
				}
			} catch (RuntimeException bad){
				//skipped
			}
		}
		boolean gen = atLeast(v, 1, 4);
		List<EnemyAcc> met = new ArrayList<>(enemies.values());
		java.util.Collections.sort(met, new java.util.Comparator<EnemyAcc>(){
			@Override
			public int compare( EnemyAcc a, EnemyAcc b ){
				return a.firstPublic < b.firstPublic ? -1 : a.firstPublic > b.firstPublic ? 1 : 0;
			}
		});
		Set<String> metClasses = new HashSet<>();
		List<Row> rows = new ArrayList<>();
		for (EnemyAcc a : met){
			metClasses.addAll(a.classes);
			Row row = new Row(meta.runId + "|" + a.name, meta.runId + "|" + a.name, meta.runId, ENEMIES.size());
			row.finished = meta.hasRunEnd;
			row.ruleset = meta.ruleset;
			row.telemetry = meta.telemetry;
			row.publicRow = true;
			put(row, "enemy.run", meta.runId, Fact.PROV_RECORDED);
			put(row, "enemy.name", a.name, Fact.PROV_RECORDED);
			fightCells(row, a.attacksOnHero, a.hitsOnHero, a.attacksByHero, a.hitsByHero, a.damageToHero, a.damageFromHero, a.kills, a.deaths,
					a.heroDeaths, countProv);
			//Derived from the true class, which is hidden until the run is over (exports also leave it out of unfinished runs), so unfinished runs show "?"
			if (meta.hasRunEnd && !a.classes.isEmpty()){
				boolean boss = false;
				for (String c : a.classes) boss |= BossClasses.isBoss(c);
				put(row, "enemy.boss", boss ? 1 : 0, Fact.PROV_RECORDED);
			}
			if (a.classes.size() == 1) put(row, "enemy.class", a.classes.iterator().next(), Fact.PROV_RECORDED);
			else if (a.classes.size() > 1) put(row, "enemy.class", "(several)", Fact.PROV_RECORDED);
			if (gen && !a.classes.isEmpty()){
				long n = 0;
				for (String c : a.classes){
					Long g = generated.get(c);
					if (g != null) n += g;
				}
				put(row, "enemy.spawned", n, countProv);
			}
			if (a.expValue >= 0) put(row, "enemy.expValue", a.expValue, Fact.PROV_RECORDED);
			rows.add(row);
		}
		//kinds of enemy that were generated but never met in a fight: hidden rows
		List<String> unmet = new ArrayList<>();
		for (String c : generated.keySet()) if (!metClasses.contains(c)) unmet.add(c);
		java.util.Collections.sort(unmet);
		for (String c : unmet){
			Row row = new Row(meta.runId + "|class:" + c, meta.runId + "|class:" + c, meta.runId, ENEMIES.size());
			row.finished = meta.hasRunEnd;
			row.ruleset = meta.ruleset;
			row.telemetry = meta.telemetry;
			row.publicRow = false;
			put(row, "enemy.run", meta.runId, Fact.PROV_RECORDED);
			put(row, "enemy.name", shortClass(c), Fact.PROV_RECORDED);
			fightCells(row, 0, 0, 0, 0, 0, 0, 0, 0, 0, countProv);
			put(row, "enemy.class", c, Fact.PROV_RECORDED);
			if (gen) put(row, "enemy.spawned", generated.get(c), countProv);
			rows.add(row);
		}
		return rows;
	}

	private static void fightCells( Row row, long onHero, long hitsOnHero, long byHero, long hitsByHero, long toHero, long fromHero,
	                                long kills, long deaths, long heroDeaths, int prov ){
		put(row, "enemy.attacksOnHero", onHero, prov);
		put(row, "enemy.hitsOnHero", hitsOnHero, prov);
		put(row, "enemy.attacksByHero", byHero, prov);
		put(row, "enemy.hitsByHero", hitsByHero, prov);
		put(row, "enemy.damageToHero", toHero, prov);
		put(row, "enemy.damageFromHero", fromHero, prov);
		put(row, "enemy.kills", kills, prov);
		put(row, "enemy.deaths", deaths, prov);
		put(row, "enemy.heroDeaths", heroDeaths, prov);
	}

	//---- deaths ----

	private static List<Row> deathRows( List<ApdEvent> events, ReadResult r, LogMeta meta, TelemetryVersion v ){
		List<Row> rows = new ArrayList<>();
		LifecycleEvents.RunEnd end = null;
		boolean endDetails = false;
		for (ApdEvent e : events){
			if (e.type == EventTypes.RUN_END){
				try {
					end = LifecycleEvents.decodeRunEnd(e, r);
					endDetails = (e.flags & LifecycleEvents.FLAG_DETAILS_UNKNOWN) == 0;
				} catch (RuntimeException bad){
					end = null;
				}
			}
		}
		boolean ranEnded = end != null && end.outcome == LifecycleEvents.OUTCOME_DIED;
		int count = 0;
		List<Row> fromEvents = new ArrayList<>();
		if (atLeast(v, 1, 3)){
			for (int i = 0; i < events.size(); i++){
				ApdEvent e = events.get(i);
				if (e.type != EventTypes.HERO_DEATH) continue;
				try {
					CombatEvents.HeroDeath hd = CombatEvents.decodeHeroDeath(e, r);
					boolean revived = false;
					for (int k = i + 1; k < events.size(); k++){
						int t = events.get(k).type;
						if (t == EventTypes.HERO_REVIVED){ revived = true; break; }
						if (t == EventTypes.HERO_DEATH || t == EventTypes.RUN_END) break;
					}
					Row row = new Row(meta.runId + "/death/" + (count++) + "@" + meta.logId, null, meta.runId, DEATHS.size());
					row.finished = meta.hasRunEnd;
					row.ruleset = meta.ruleset;
					row.telemetry = meta.telemetry;
					put(row, "death.run", meta.runId, Fact.PROV_RECORDED);
					put(row, "death.depth", hd.where.depth, Fact.PROV_RECORDED);
					put(row, "death.turn", e.clock, Fact.PROV_RECORDED);
					put(row, "death.cause", hd.cause.label.isEmpty() ? "(not apparent)" : hd.cause.label, Fact.PROV_RECORDED);
					put(row, "death.causeKind", hd.cause.kind, Fact.PROV_RECORDED);
					put(row, "death.revived", revived ? 1 : 0, Fact.PROV_RECORDED);
					fromEvents.add(row);
				} catch (RuntimeException bad){
					//skipped
				}
			}
			if (!fromEvents.isEmpty() && ranEnded){
				//the last death that was not undone is the one that ended the run
				for (int i = fromEvents.size() - 1; i >= 0; i--){
					Row row = fromEvents.get(i);
					boolean revived = row.num(DEATHS.indexOf("death.revived")) == 1;
					if (!revived){
						row.setNum(DEATHS.indexOf("death.final"), 1, Fact.PROV_RECORDED);
						if (endDetails) row.setNum(DEATHS.indexOf("death.heroLevel"), end.heroLevel, Fact.PROV_RECORDED);
						break;
					}
				}
			}
			for (Row row : fromEvents) if (!row.isKnown(DEATHS.indexOf("death.final"))) row.setNum(DEATHS.indexOf("death.final"), 0, Fact.PROV_RECORDED);
			rows.addAll(fromEvents);
		}
		if (fromEvents.isEmpty() && ranEnded){
			//older telemetry (or no death event): the end of the run is all there is
			Row row = new Row(meta.runId + "/death/end@" + meta.logId, null, meta.runId, DEATHS.size());
			row.finished = meta.hasRunEnd;
			row.ruleset = meta.ruleset;
			row.telemetry = meta.telemetry;
			put(row, "death.run", meta.runId, Fact.PROV_RECORDED);
			put(row, "death.final", 1, Fact.PROV_RECORDED);
			if (endDetails){
				put(row, "death.depth", end.depth, Fact.PROV_RECORDED);
				put(row, "death.turn", end.turns, Fact.PROV_RECORDED);
				put(row, "death.cause", end.cause.isEmpty() ? "(not apparent)" : end.cause, Fact.PROV_RECORDED);
				put(row, "death.heroLevel", end.heroLevel, Fact.PROV_RECORDED);
			}
			rows.add(row);
		}
		return rows;
	}

	//---- imported ranking records ----

	public static LogRows fromLegacyLog( ReadResult r, String logId ){
		LogRows out = new LogRows();
		out.meta.logId = logId;
		out.meta.imported = true;
		out.meta.runId = "";
		if (r == null || r.header == null || r.newerThanReader()){
			out.meta.unreadable = true;
			return out;
		}
		out.meta.telemetry = r.header.telemetryVersion.toString();
		out.meta.createdAtMillis = r.header.createdAtMillis;
		List<Row> rows = out.of(Datasets.RUNS);
		for (ApdEvent e : r.events){
			if (e.type != EventTypes.LEGACY_RECORD) continue;
			try {
				LegacyEvents.Record rec = LegacyEvents.decodeRecord(e, r);
				String id = "legacy:" + rec.gameId;
				Row row = new Row(id, null, id, RUNS.size());
				row.imported = true;
				row.finished = true;
				row.ruleset = null;
				row.telemetry = out.meta.telemetry;
				put(row, FactKeys.RUN_ID, id, Fact.PROV_LEGACY);
				put(row, FactKeys.STATUS, "imported", Fact.PROV_LEGACY);
				put(row, "run.source", 2, Fact.PROV_LEGACY);
				put(row, "run.segments", 1, Fact.PROV_LEGACY);
				for (Map.Entry<String, LegacyEvents.Value> f : rec.fields.entrySet()){
					LegacyEvents.Value val = f.getValue();
					if (!val.isKnown()) continue;
					String key = f.getKey();
					if (key.equals(FactKeys.DATE)){
						long ms = parseDate(val.text);
						if (ms != Long.MIN_VALUE) put(row, FactKeys.START_MILLIS, ms, Fact.PROV_LEGACY);
						continue;
					}
					int i = RUNS.indexOf(key);
					if (i < 0) continue;
					if (val.kind == LegacyEvents.KIND_NUM){row.setNum(i,val.num,Fact.PROV_LEGACY);if(FactKeys.SCORE.equals(key))put(row,com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.ORIGINAL_SCORE,val.num,Fact.PROV_LEGACY);}
					else row.setText(i, val.text, Fact.PROV_LEGACY);
				}
				rows.add(row);
			} catch (RuntimeException bad){
				//skipped
			}
		}
		return out;
	}

	//yyyy-MM-dd as milliseconds at 00:00 UTC, Long.MIN_VALUE if it does not parse
	static long parseDate( String s ){
		try {
			if (s == null || s.length() != 10 || s.charAt(4) != '-' || s.charAt(7) != '-') return Long.MIN_VALUE;
			long y = Long.parseLong(s.substring(0, 4)), m = Long.parseLong(s.substring(5, 7)), d = Long.parseLong(s.substring(8, 10));
			if (m < 1 || m > 12 || d < 1 || d > 31) return Long.MIN_VALUE;
			boolean leap = y % 4 == 0 && (y % 100 != 0 || y % 400 == 0);
			int maxDay;
			switch ((int) m){
				case 2: maxDay = leap ? 29 : 28; break;
				case 4: case 6: case 9: case 11: maxDay = 30; break;
				default: maxDay = 31;
			}
			if (d > maxDay) return Long.MIN_VALUE;
			y -= m <= 2 ? 1 : 0;
			long era = (y >= 0 ? y : y - 399) / 400;
			long yoe = y - era * 400;
			long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
			long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
			long days = era * 146097 + doe - 719468;
			return days * 86400000L;
		} catch (RuntimeException e){
			return Long.MIN_VALUE;
		}
	}
}


