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

import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.List;

//The game's entry points for item identity. Every method returns immediately (one volatile read) unless a run is being
//recorded, never throws, and never changes the game: see ItemTracker for what is done with the calls.
public final class ApdItems {

	private ApdItems(){}

	//the actions the game does not name itself
	public static final String ACTION_DROP = "DROP";
	public static final String ACTION_THROW = "THROW";

	//true while a run is being recorded with item tracking. Written by ApdRecorder only.
	static volatile boolean active;

	//The player (or the game on their behalf) is doing 'action' with this item: used to say why an item later leaves.
	public static void noteAction( Item item, String action ){
		if (active) ApdRecorder.itemNoteAction(item, action);
	}

	//the next items to arrive in the hero's hands come from this source (ItemEvents.ACQ_*)
	public static void noteAcquireContext( int context ){
		if (active) ApdRecorder.itemNoteAcquire(context);
	}

	//'child' was just split off 'parent' (child already has no identity of its own yet)
	public static void onSplit( Item parent, Item child, int amount ){
		if (active) ApdRecorder.itemSplit(parent, child, amount);
	}

	//'absorbed' is about to be added into 'survivor' and will cease to exist
	public static void onMerge( Item survivor, Item absorbed, int amount ){
		if (active) ApdRecorder.itemMerge(survivor, absorbed, amount);
	}

	//'piece' was just taken out of the hero's bag; it is the same object as 'original' unless a stack was split
	public static void onDetached( Item original, Item piece ){
		if (active) ApdRecorder.itemDetached(original, piece);
	}

	//'newItem' replaced 'oldItem' (transmutation, class armor upgrade, ...)
	public static void onTransformed( Item oldItem, Item newItem, String cause ){
		if (oldItem != null) {
			com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.record(newItem, oldItem.apdFoundOrigin);
			if (newItem != null) newItem.apdGenerationRange = oldItem.apdGenerationRange;
		}
		if (active) ApdRecorder.itemTransformed(oldItem, newItem, cause);
	}

	//alchemy made 'result' out of 'ingredients'
	public static void onCrafted( List<Item> ingredients, Item result ){
		if (result != null && (ingredients == null || !ingredients.contains(result))) result.apdGenerationRange = null;
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null) com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.recordSource(result, com.shatteredpixel.shatteredpixeldungeon.Dungeon.level, com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.pos, com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.CRAFTED);
		if (active) ApdRecorder.itemCrafted(ingredients, result);
	}

	//the player learned what a whole kind of potion, scroll or ring is
	public static void onClassKnown( Class<?> cls, String labelBefore, Item sample ){
		if (active) ApdRecorder.itemClassKnown(cls, labelBefore, sample);
	}

	//the hero is about to take a turn (Hero.act)
	public static void onHeroTurn(){
		if (active) ApdRecorder.itemHeroTurn();
	}
}
