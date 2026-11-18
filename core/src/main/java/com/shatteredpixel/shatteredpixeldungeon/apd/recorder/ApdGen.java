/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
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

import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

//The game's entry points for generation provenance. Every method returns at once (one volatile read) unless a run is being
//recorded, never throws, never calls the item generator or the game's random numbers, and never changes what is generated:
//see GenerationTracker. The generator wrappers only OBSERVE the item that was returned.
public final class ApdGen {

	private ApdGen(){}

	public static final int KIND_RANDOM = 0, KIND_CATEGORY = 1, KIND_DEFAULTS = 2, KIND_DEFAULTS_CATEGORY = 3, KIND_CLASS = 4,
			KIND_ARTIFACT = 5;

	public static final int STAGE_NONE = 0, STAGE_BUILD = 1, STAGE_MOBS = 2, STAGE_ITEMS = 3;

	//true while a run is being recorded. Written by ApdRecorder only.
	static volatile boolean active;

	//Generator entry point starting: returns how deeply nested this call is (-1 if not recording)
	public static int begin(){
		return active ? ApdRecorder.genBegin() : -1;
	}

	//Generator entry point finished with 'result'. Only the outermost call is recorded as a draw.
	public static void end( int depth, int kind, Generator.Category requested, Item result ){
		if (depth >= 0) ApdRecorder.genEnd(depth, kind, requested, result);
	}

	//Level.create is starting a fresh level
	public static void onLevelCreateStart(){
		if (active) ApdRecorder.genLevelStart();
	}

	//Level.create is about to try building the layout (once more after every failed attempt)
	public static void onBuildAttempt(){
		if (active) ApdRecorder.genBuildAttempt();
	}

	public static void setStage( int stage ){
		if (active) ApdRecorder.genStage(stage);
	}

	//Dungeon.newLevel: a fresh level is fully built (never called for a level loaded from a save)
	public static void onLevelGenerated( Level level ){
		com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.captureLevel(level);
		if (active) ApdRecorder.genLevelGenerated(level);
	}

	//Mob.rollToDropLoot: a creature's death produced this item
	public static void onLootCreated( Mob mob, Item item ){
		com.shatteredpixel.shatteredpixeldungeon.apd.qol.GenerationUpgradeRanges.finalMobSupport(item, mob);
		if (mob != null) com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.recordSource(item, com.shatteredpixel.shatteredpixeldungeon.Dungeon.level, mob.pos, com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents.C_MOB_LOOT);
		if (active) ApdRecorder.genLoot(mob, item);
	}

	//Heap.open: bonus items (Ring of Wealth) were added to an opened heap
	public static void onBonusDrop( Item item, int pos ){
		com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.recordSource(item, com.shatteredpixel.shatteredpixeldungeon.Dungeon.level, pos, com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents.C_BONUS_DROP);
		if (active) ApdRecorder.genBonus(item, pos);
	}
}
