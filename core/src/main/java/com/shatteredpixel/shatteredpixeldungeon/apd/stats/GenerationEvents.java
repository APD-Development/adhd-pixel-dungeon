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

import java.util.ArrayList;
import java.util.List;

//Encoders and decoders for the generation provenance events of telemetry 1.4. Pure data, no game classes.
//
//These events say what the game PLACED in a level and where each piece came from. They are all INTERNAL: they include
//everything the player has not found (chest and mimic contents, secret rooms, hidden traps) and the truth about items the
//player cannot identify. Player-facing views and exports must not show them. What the player later sees of these items is in
//the PUBLIC item events, joined to these by the item's apd_uid.
//
//Field order inside a payload is part of the format. A telemetry 1.x version may only ADD fields at the END of a payload.
public final class GenerationEvents {

	private GenerationEvents(){}

	//where an item was when the level was built (ITEM_GENERATED.container)
	public static final int C_HEAP = 0, C_CHEST = 1, C_LOCKED_CHEST = 2, C_CRYSTAL_CHEST = 3, C_TOMB = 4, C_SKELETON = 5,
			C_REMAINS = 6, C_FOR_SALE = 7, C_MIMIC = 8, C_STATUE = 9, C_QUEST_REWARD = 10, C_MOB_LOOT = 11, C_BONUS_DROP = 12;

	//what part of level creation was running when the item was drawn (ITEM_GENERATED.stage)
	public static final int STAGE_NONE = 0, STAGE_BUILD = 1, STAGE_MOBS = 2, STAGE_ITEMS = 3;

	//how the item was asked for from the generator (ITEM_GENERATED.drawKind), -1 when it was not drawn from it at all
	public static final int DRAW_NONE = -1, DRAW_RANDOM = 0, DRAW_CATEGORY = 1, DRAW_DEFAULTS = 2, DRAW_DEFAULTS_CATEGORY = 3,
			DRAW_CLASS = 4, DRAW_ARTIFACT = 5;

	//MOB_GENERATED.state
	public static final int STATE_SLEEPING = 0, STATE_WANDERING = 1, STATE_HUNTING = 2, STATE_FLEEING = 3, STATE_PASSIVE = 4,
			STATE_OTHER = 5;

	public static final class Where {
		public int depth;
		public int branch;
	}

	private static void putWhere( EventBuilder b, Where w ){
		b.unsigned(Math.max(0, w.depth)).unsigned(Math.max(0, w.branch));
	}

	private static Where readWhere( PayloadReader p ){
		Where w = new Where();
		w.depth = p.unsignedInt();
		w.branch = p.unsignedInt();
		return w;
	}

	private static void putStrings( EventBuilder b, List<String> list ){
		b.unsigned(list.size());
		for (String s : list) b.string(s);
	}

	private static List<String> readStrings( PayloadReader p ){
		int n = p.unsignedInt();
		List<String> out = new ArrayList<>();
		for (int i = 0; i < n; i++) out.add(str(p.string()));
		return out;
	}

	//---- LEVEL_GENERATED ---------------------------------------------------------------------------------------------

	//A level was built for the first time (not loaded from a save). Next to the seed it keeps the context the generation
	//depended on, because the seed alone does not decide a level (see APD-STATS-PHASE0.md).
	public static final class LevelGenerated {
		public Where where = new Where();
		public String levelClass = "";
		public String feeling = "";
		public int buildAttempts;       //painting can fail and start over; draws made in failed attempts are not in the level
		public int draws;               //item draws from the generator during creation
		public int heaps, itemsFound, mobs, trapsVisible, trapsHidden;
		//context
		public boolean intro;
		public int exoticChance;
		public int challenges;
		public int adventurersPagesFound, alchemyPagesFound;
		public List<String> heroItems = new ArrayList<>();      //what the hero carried (shops and some drops depend on it)
		public List<String> rooms = new ArrayList<>();          //every room's class, secret and special ones included
		public List<String> limitedNames = new ArrayList<>();   //guaranteed-drop bookkeeping after this level was built
		public List<Integer> limitedCounts = new ArrayList<>();
		public List<String> categoryNames = new ArrayList<>();  //how many cards each generator category had given out
		public List<Integer> categoryDropped = new ArrayList<>();
	}

	public static boolean emitLevelGenerated( RunLogWriter w, long clock, LevelGenerated g ){
		EventBuilder b = w.event(EventTypes.LEVEL_GENERATED).clock(clock);
		putWhere(b, g.where);
		b.string(g.levelClass).string(g.feeling).unsigned(g.buildAttempts).unsigned(g.draws).unsigned(g.heaps)
				.unsigned(g.itemsFound).unsigned(g.mobs).unsigned(g.trapsVisible).unsigned(g.trapsHidden)
				.bool(g.intro).unsigned(Math.max(0, g.exoticChance)).unsigned(Math.max(0, g.challenges))
				.unsigned(g.adventurersPagesFound).unsigned(g.alchemyPagesFound);
		putStrings(b, g.heroItems);
		putStrings(b, g.rooms);
		putStrings(b, g.limitedNames);
		b.unsigned(g.limitedCounts.size());
		for (int c : g.limitedCounts) b.signed(c);
		putStrings(b, g.categoryNames);
		b.unsigned(g.categoryDropped.size());
		for (int c : g.categoryDropped) b.signed(c);
		return b.emit();
	}

	public static LevelGenerated decodeLevelGenerated( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		LevelGenerated g = new LevelGenerated();
		g.where = readWhere(p);
		g.levelClass = str(p.string());
		g.feeling = str(p.string());
		g.buildAttempts = p.unsignedInt();
		g.draws = p.unsignedInt();
		g.heaps = p.unsignedInt();
		g.itemsFound = p.unsignedInt();
		g.mobs = p.unsignedInt();
		g.trapsVisible = p.unsignedInt();
		g.trapsHidden = p.unsignedInt();
		g.intro = p.bool();
		g.exoticChance = p.unsignedInt();
		g.challenges = p.unsignedInt();
		g.adventurersPagesFound = p.unsignedInt();
		g.alchemyPagesFound = p.unsignedInt();
		g.heroItems = readStrings(p);
		g.rooms = readStrings(p);
		g.limitedNames = readStrings(p);
		int n = p.unsignedInt();
		for (int i = 0; i < n; i++) g.limitedCounts.add((int) p.signed());
		g.categoryNames = readStrings(p);
		n = p.unsignedInt();
		for (int i = 0; i < n; i++) g.categoryDropped.add((int) p.signed());
		return g;
	}

	//---- ITEM_GENERATED ----------------------------------------------------------------------------------------------

	//An item came into existence in the game: placed when a level was built, dropped by a creature, or a bonus drop.
	//The item's apd_uid is assigned here, so it can be followed from before anyone has seen it.
	public static final class ItemGenerated {
		public String uid = "";
		public Where where = new Where();
		public int container;
		public int pos = -1;
		public int heapType = -1;
		public boolean hidden;
		public String room = "";         //the room it lies in ("" when unknown)
		public String owner = "";        //the creature or quest giver that holds it (mimic, statue, quest rewards, loot)
		public int stage;
		public int drawKind = DRAW_NONE;
		public String requestedCategory = "";
		public String actualCategory = "";
		public int categoryDropped = -1;
		public String trueClass = "";
		public int trueLevel;
		public boolean cursed;
		public String enchant = "";
		public int quantity = 1;
	}

	public static boolean emitItemGenerated( RunLogWriter w, long clock, ItemGenerated g ){
		EventBuilder b = w.event(EventTypes.ITEM_GENERATED).clock(clock).string(g.uid);
		putWhere(b, g.where);
		b.unsigned(g.container).signed(g.pos).signed(g.heapType).bool(g.hidden).string(g.room).string(g.owner).unsigned(g.stage)
				.signed(g.drawKind).string(g.requestedCategory).string(g.actualCategory).signed(g.categoryDropped)
				.string(g.trueClass).signed(g.trueLevel).bool(g.cursed).string(g.enchant).unsigned(Math.max(0, g.quantity));
		return b.emit();
	}

	public static ItemGenerated decodeItemGenerated( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		ItemGenerated g = new ItemGenerated();
		g.uid = str(p.string());
		g.where = readWhere(p);
		g.container = p.unsignedInt();
		g.pos = (int) p.signed();
		g.heapType = (int) p.signed();
		g.hidden = p.bool();
		g.room = str(p.string());
		g.owner = str(p.string());
		g.stage = p.unsignedInt();
		g.drawKind = (int) p.signed();
		g.requestedCategory = str(p.string());
		g.actualCategory = str(p.string());
		g.categoryDropped = (int) p.signed();
		g.trueClass = str(p.string());
		g.trueLevel = (int) p.signed();
		g.cursed = p.bool();
		g.enchant = str(p.string());
		g.quantity = p.unsignedInt();
		return g;
	}

	//---- MOB_GENERATED -----------------------------------------------------------------------------------------------

	public static final class MobGenerated {
		public Where where = new Where();
		public String mobClass = "";
		public long id;
		public int pos = -1;
		public int hp, hpMax;
		public int state;
		public int alignment;            //0 enemy, 1 neutral, 2 ally
		public List<String> champion = new ArrayList<>();
		public String room = "";
		public int carries;              //items it holds (mimics, statues)
	}

	public static boolean emitMobGenerated( RunLogWriter w, long clock, MobGenerated m ){
		EventBuilder b = w.event(EventTypes.MOB_GENERATED).clock(clock);
		putWhere(b, m.where);
		b.string(m.mobClass).signed(m.id).signed(m.pos).signed(m.hp).signed(m.hpMax).unsigned(m.state).unsigned(m.alignment);
		putStrings(b, m.champion);
		b.string(m.room).unsigned(Math.max(0, m.carries));
		return b.emit();
	}

	public static MobGenerated decodeMobGenerated( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		MobGenerated m = new MobGenerated();
		m.where = readWhere(p);
		m.mobClass = str(p.string());
		m.id = p.signed();
		m.pos = (int) p.signed();
		m.hp = (int) p.signed();
		m.hpMax = (int) p.signed();
		m.state = p.unsignedInt();
		m.alignment = p.unsignedInt();
		m.champion = readStrings(p);
		m.room = str(p.string());
		m.carries = p.unsignedInt();
		return m;
	}

	//---- MOB_ID_ASSIGNED ---------------------------------------------------------------------------------------------

	//A level is built before the game gives its creatures ids (that happens when the hero arrives, and the recorder must not
	//cause it earlier). This event joins a MOB_GENERATED (same level, class and position) to the id that later combat events use.
	public static final class MobIdAssigned {
		public Where where = new Where();
		public String mobClass = "";
		public int pos = -1;
		public long id;
	}

	public static boolean emitMobIdAssigned( RunLogWriter w, long clock, MobIdAssigned m ){
		EventBuilder b = w.event(EventTypes.MOB_ID_ASSIGNED).clock(clock);
		putWhere(b, m.where);
		b.string(m.mobClass).signed(m.pos).signed(m.id);
		return b.emit();
	}

	public static MobIdAssigned decodeMobIdAssigned( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		MobIdAssigned m = new MobIdAssigned();
		m.where = readWhere(p);
		m.mobClass = str(p.string());
		m.pos = (int) p.signed();
		m.id = p.signed();
		return m;
	}

	private static String str( String s ){
		return s == null ? "" : s;
	}
}
