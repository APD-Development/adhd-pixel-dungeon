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


package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

//Generation provenance for one run. Package-private; the game reaches it through ApdGen.
//
//What it does: (1) the generator hooks note HOW each item was asked for (which category, which kind of draw) and remember it
//against the item object; (2) once a fresh level is completely built, the final state is read: every item placed (heaps,
//chests, mimics, statues, quest rewards) gets its apd_uid and an ITEM_GENERATED event, every creature a MOB_GENERATED, and
//the level a LEVEL_GENERATED with the context the generation depended on; (3) creature loot and bonus drops are noted when
//they are created at play time.
//
//It only reads. It never calls the generator, never uses the game's random numbers, never changes a level, a heap or a
//creature (the only write is the item id, which is not part of how the game plays).
final class GenerationTracker {

	private final RunLogWriter w;
	private final ItemTracker items;

	//nesting of generator calls: only the outermost is a draw
	private int depth;
	private Generator.Category firstInnerCategory;

	private int stage = GenerationEvents.STAGE_NONE;
	private int attempts;
	private int draws;

	private static final class Draw {
		int kind;
		String requested = "";
		String actual = "";
		int dropped = -1;
		int stage;
	}

	//how each item object was asked for; weak so items that are discarded do not pile up (Item has no equals/hashCode, so
	//this is identity based)
	private final Map<Item, Draw> drawn = new WeakHashMap<>();

	//creatures reported by MOB_GENERATED that have not had their game id reported yet
	private final Map<Mob, Boolean> awaitingId = new WeakHashMap<>();

	GenerationTracker( RunLogWriter w, ItemTracker items ){
		this.w = w;
		this.items = items;
	}

	//---- generator observation -----------------------------------------------------------------------------------

	int begin(){
		return depth++;
	}

	void end( int d, int kind, Generator.Category requested, Item result ){
		depth = Math.max(0, d);
		if (d > 0){
			//a call made by another generator call: remember the first category it picked, for the outer one to report
			if (requested != null && firstInnerCategory == null) firstInnerCategory = requested;
			return;
		}
		Generator.Category actual = requested != null ? requested : firstInnerCategory;
		firstInnerCategory = null;
		if (result == null) return;
		draws++;
		Draw dr = new Draw();
		dr.kind = kindOf(kind);
		dr.requested = requested == null ? "" : requested.name();
		dr.actual = actual == null ? "" : actual.name();
		dr.dropped = actual == null ? -1 : actual.dropped;
		dr.stage = stage;
		drawn.put(result, dr);
	}

	private static int kindOf( int kind ){
		switch (kind){
			case ApdGen.KIND_RANDOM: return GenerationEvents.DRAW_RANDOM;
			case ApdGen.KIND_CATEGORY: return GenerationEvents.DRAW_CATEGORY;
			case ApdGen.KIND_DEFAULTS: return GenerationEvents.DRAW_DEFAULTS;
			case ApdGen.KIND_DEFAULTS_CATEGORY: return GenerationEvents.DRAW_DEFAULTS_CATEGORY;
			case ApdGen.KIND_CLASS: return GenerationEvents.DRAW_CLASS;
			case ApdGen.KIND_ARTIFACT: return GenerationEvents.DRAW_ARTIFACT;
			default: return GenerationEvents.DRAW_NONE;
		}
	}

	void levelStart(){
		attempts = 0;
		draws = 0;
		stage = GenerationEvents.STAGE_NONE;
	}

	void buildAttempt(){
		attempts++;
		stage = GenerationEvents.STAGE_BUILD;
	}

	void setStage( int s ){
		stage = s;
	}

	//---- the finished level --------------------------------------------------------------------------------------

	private static GenerationEvents.Where where( Level level ){
		GenerationEvents.Where x = new GenerationEvents.Where();
		x.depth = Dungeon.depth;
		x.branch = Dungeon.branch;
		return x;
	}

	private static String shortName( Class<?> c ){
		return GameSnapshot.shortName(c);
	}

	private String roomAt( Level level, int pos ){
		if (!(level instanceof RegularLevel)) return "";
		try {
			Room r = ((RegularLevel) level).room(pos);
			return r == null ? "" : r.getClass().getSimpleName();
		} catch (Throwable t){
			return "";
		}
	}

	void levelGenerated( Level level ){
		if (level == null) return;
		long clock = GameSnapshot.turn();
		GenerationEvents.Where where = where(level);

		List<Heap> heaps = new ArrayList<>(level.heaps.valueList());
		Collections.sort(heaps, new Comparator<Heap>(){
			@Override
			public int compare( Heap a, Heap b ){
				return a.pos < b.pos ? -1 : (a.pos > b.pos ? 1 : 0);
			}
		});
		List<Mob> mobs = new ArrayList<>(level.mobs);
		Collections.sort(mobs, new Comparator<Mob>(){
			@Override
			public int compare( Mob a, Mob b ){
				if (a.pos != b.pos) return a.pos < b.pos ? -1 : 1;
				return a.getClass().getName().compareTo(b.getClass().getName());
			}
		});

		int found = 0;
		for (Heap h : heaps){
			for (Item it : new ArrayList<>(h.items)){
				if (itemGenerated(level, it, containerOf(h.type), h.pos, h.type.ordinal(), h.hidden, "", stage)) found++;
			}
		}
		for (Mob m : mobs){
			int carried = 0;
			try {
				if (m instanceof Mimic && ((Mimic) m).items != null){
					for (Item it : new ArrayList<>(((Mimic) m).items)){
						if (itemGenerated(level, it, GenerationEvents.C_MIMIC, m.pos, -1, false, shortName(m.getClass()), stage)){
							found++;
							carried++;
						}
					}
				}
				if (m instanceof Statue){
					Item wep = ((Statue) m).weapon();
					if (wep != null && itemGenerated(level, wep, GenerationEvents.C_STATUE, m.pos, -1, false, shortName(m.getClass()), stage)){
						found++;
						carried++;
					}
					if (m instanceof ArmoredStatue){
						Item arm = ((ArmoredStatue) m).armor();
						if (arm != null && itemGenerated(level, arm, GenerationEvents.C_STATUE, m.pos, -1, false, shortName(m.getClass()), stage)){
							found++;
							carried++;
						}
					}
				}
			} catch (Throwable t){
				//a creature whose held items cannot be read is still reported as a creature
			}
			mobGenerated(level, m, carried);
			awaitingId.put(m, Boolean.TRUE);
		}
		found += questRewards(level, mobs);

		GenerationEvents.LevelGenerated g = new GenerationEvents.LevelGenerated();
		g.where = where;
		g.levelClass = shortName(level.getClass());
		g.feeling = level.feeling == null ? "" : level.feeling.name();
		g.buildAttempts = attempts;
		g.draws = draws;
		g.heaps = heaps.size();
		g.itemsFound = found;
		g.mobs = mobs.size();
		for (Trap t : level.traps.valueList()){
			if (t.visible) g.trapsVisible++; else g.trapsHidden++;
		}
		g.intro = SPDSettings.intro();
		g.exoticChance = SPDSettings.apdExoticChance();
		g.challenges = Dungeon.challenges;
		g.adventurersPagesFound = pagesFound(Document.ADVENTURERS_GUIDE);
		g.alchemyPagesFound = pagesFound(Document.ALCHEMY_GUIDE);
		Hero hero = Dungeon.hero;
		if (hero != null && hero.belongings != null){
			for (Item it : hero.belongings){
				if (g.heroItems.size() >= 60) break;
				g.heroItems.add(shortName(it.getClass()));
			}
		}
		if (level instanceof RegularLevel){
			for (Room r : ((RegularLevel) level).rooms()) g.rooms.add(r.getClass().getSimpleName());
		}
		for (Dungeon.LimitedDrops d : Dungeon.LimitedDrops.values()){
			g.limitedNames.add(d.name());
			g.limitedCounts.add(d.count);
		}
		for (Generator.Category c : Generator.Category.values()){
			g.categoryNames.add(c.name());
			g.categoryDropped.add(c.dropped);
		}
		GenerationEvents.emitLevelGenerated(w, clock, g);

		stage = GenerationEvents.STAGE_NONE;
		draws = 0;
		attempts = 0;
	}

	private static int pagesFound( Document doc ){
		int n = 0;
		try {
			for (String page : doc.pageNames()) if (doc.isPageFound(page)) n++;
		} catch (Throwable t){
			return 0;
		}
		return n;
	}

	private static int containerOf( Heap.Type t ){
		switch (t){
			case CHEST: return GenerationEvents.C_CHEST;
			case LOCKED_CHEST: return GenerationEvents.C_LOCKED_CHEST;
			case CRYSTAL_CHEST: return GenerationEvents.C_CRYSTAL_CHEST;
			case TOMB: return GenerationEvents.C_TOMB;
			case SKELETON: return GenerationEvents.C_SKELETON;
			case REMAINS: return GenerationEvents.C_REMAINS;
			case FOR_SALE: return GenerationEvents.C_FOR_SALE;
			default: return GenerationEvents.C_HEAP;
		}
	}

	//the item gets its identity and its provenance event. False for items that have no identity (gold, keys, dewdrops).
	private boolean itemGenerated( Level level, Item item, int container, int pos, int heapType, boolean hidden, String owner, int stageAtBuild ){
		if (item == null || items == null) return false;
		String uid = items.ensureUid(item, ItemEvents.ORIGIN_GENERATED);
		if (uid == null) return false;
		ItemFacts.Truth t = ItemFacts.truth(item);
		GenerationEvents.ItemGenerated g = new GenerationEvents.ItemGenerated();
		g.uid = uid;
		g.where = where(level);
		g.container = container;
		g.pos = pos;
		g.heapType = heapType;
		g.hidden = hidden;
		g.room = pos >= 0 ? roomAt(level, pos) : "";
		g.owner = owner;
		Draw d = drawn.get(item);
		if (d != null){
			g.stage = d.stage;
			g.drawKind = d.kind;
			g.requestedCategory = d.requested;
			g.actualCategory = d.actual;
			g.categoryDropped = d.dropped;
		} else {
			g.stage = stageAtBuild;
			g.drawKind = GenerationEvents.DRAW_NONE;
		}
		g.trueClass = t.trueClass;
		g.trueLevel = t.level;
		g.cursed = t.cursed;
		g.enchant = t.enchant;
		g.quantity = t.quantity;
		GenerationEvents.emitItemGenerated(w, GameSnapshot.turn(), g);
		return true;
	}

	private void mobGenerated( Level level, Mob m, int carried ){
		GenerationEvents.MobGenerated e = new GenerationEvents.MobGenerated();
		e.where = where(level);
		e.mobClass = shortName(m.getClass());
		e.id = m.apdPeekId();
		e.pos = m.pos;
		e.hp = m.HP;
		e.hpMax = m.HT;
		e.state = m.state == m.SLEEPING ? GenerationEvents.STATE_SLEEPING
				: m.state == m.WANDERING ? GenerationEvents.STATE_WANDERING
				: m.state == m.HUNTING ? GenerationEvents.STATE_HUNTING
				: m.state == m.FLEEING ? GenerationEvents.STATE_FLEEING
				: m.state == m.PASSIVE ? GenerationEvents.STATE_PASSIVE : GenerationEvents.STATE_OTHER;
		e.alignment = m.alignment == Char.Alignment.ENEMY ? 0 : (m.alignment == Char.Alignment.NEUTRAL ? 1 : 2);
		for (Buff b : new ArrayList<>(m.buffs())){
			if (b instanceof ChampionEnemy) e.champion.add(b.getClass().getSimpleName());
		}
		e.room = roomAt(level, m.pos);
		e.carries = carried;
		GenerationEvents.emitMobGenerated(w, GameSnapshot.turn(), e);
	}

	//rewards held by quest givers live in static quest fields, not in the level
	private int questRewards( Level level, List<Mob> mobs ){
		int n = 0;
		for (Mob m : mobs){
			String owner = shortName(m.getClass());
			try {
				if (m instanceof Ghost){
					n += reward(level, Ghost.Quest.weapon, m, owner);
					n += reward(level, Ghost.Quest.armor, m, owner);
				} else if (m instanceof Wandmaker){
					n += reward(level, Wandmaker.Quest.wand1, m, owner);
					n += reward(level, Wandmaker.Quest.wand2, m, owner);
				} else if (m instanceof Blacksmith && Blacksmith.Quest.smithRewards != null){
					for (Item it : new ArrayList<>(Blacksmith.Quest.smithRewards)) n += reward(level, it, m, owner);
				} else if (m instanceof Imp && Imp.Quest.rewardOptions != null){
					for (Item it : new ArrayList<>(Imp.Quest.rewardOptions)) n += reward(level, it, m, owner);
				}
			} catch (Throwable t){
				//quest state that cannot be read is skipped
			}
		}
		return n;
	}

	private int reward( Level level, Item it, Mob giver, String owner ){
		return itemGenerated(level, it, GenerationEvents.C_QUEST_REWARD, giver.pos, -1, false, owner, stage) ? 1 : 0;
	}

	//The hero arrived on a level: creatures built earlier now have the ids the game gave them (assigned by the game itself when
	//they joined the level; reading them never assigns one).
	void levelEntered( Level level ){
		if (level == null || awaitingId.isEmpty()) return;
		List<Mob> mobs = new ArrayList<>(level.mobs);
		Collections.sort(mobs, new Comparator<Mob>(){
			@Override
			public int compare( Mob a, Mob b ){
				return a.pos < b.pos ? -1 : (a.pos > b.pos ? 1 : 0);
			}
		});
		for (Mob m : mobs){
			if (!awaitingId.containsKey(m) || m.apdPeekId() <= 0) continue;
			awaitingId.remove(m);
			GenerationEvents.MobIdAssigned e = new GenerationEvents.MobIdAssigned();
			e.where = where(level);
			e.mobClass = shortName(m.getClass());
			e.pos = m.pos;
			e.id = m.apdPeekId();
			GenerationEvents.emitMobIdAssigned(w, GameSnapshot.turn(), e);
		}
	}

	//---- play-time sources ---------------------------------------------------------------------------------------

	void loot( Mob mob, Item item ){
		if (mob == null || item == null) return;
		itemGenerated(Dungeon.level, item, GenerationEvents.C_MOB_LOOT, mob.pos, -1, false, shortName(mob.getClass()), GenerationEvents.STAGE_NONE);
	}

	void bonus( Item item, int pos ){
		if (item == null) return;
		itemGenerated(Dungeon.level, item, GenerationEvents.C_BONUS_DROP, pos, -1, false, "", GenerationEvents.STAGE_NONE);
	}
}
