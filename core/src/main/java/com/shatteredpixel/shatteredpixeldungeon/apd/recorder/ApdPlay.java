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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;

import java.util.List;

//The game's one-line entry points for recording what the hero does and what happens around them (movement, items, status effects, talents,
//spells, gold, shops, traps). Each returns at once (one volatile read) unless a run is being recorded, never throws, and never changes the
//game or consumes its random numbers. To record a new kind of thing, declare a FactKind (GameFacts) and call ApdFacts.record where it
//happens; the calls below are the ones the game itself makes.
public final class ApdPlay {

	private ApdPlay(){}

	//true while the game is restoring creatures from a save: effects put back on a creature did not just begin
	private static int restoring;

	public static void restoreBegin(){
		restoring++;
	}

	public static void restoreEnd(){
		if (restoring > 0) restoring--;
	}

	//a cause for the gold change that Gold.doPickUp is about to make, when the pickup is really something else (selling)
	private static String goldCause;

	public static void setGoldCause( String cause ){
		goldCause = cause;
	}

	public static void clearGoldCause(){
		goldCause = null;
	}

	public static String goldCauseOr( String fallback ){
		return goldCause != null ? goldCause : fallback;
	}

	private static PlayTracker t(){
		return ApdFacts.active ? ApdFacts.play : null;
	}

	//Hero.act: the hero is about to take a turn
	public static void onHeroTurn(){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onHeroTurn(); } catch (Throwable ignored){}
	}

	//Hero.move: the hero is now on cell 'to'
	public static void onHeroMoved( Hero hero, int to, boolean travelling ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onHeroMoved(hero, to, travelling); } catch (Throwable ignored){}
	}

	//Hero.actMove: one observation when a manual step, point-to-point travel or stair walk command begins
	public static void onMoveCommand( int destination, String mode ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onMoveCommand(destination, mode); } catch (Throwable ignored){}
	}

	//a visible boss crossed one of its explicit gameplay phase boundaries
	public static void onBossPhase( String boss, int phase, String state ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onBossPhase(boss, phase, state); } catch (Throwable ignored){}
	}

	public static void onHeroRested( boolean full ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onHeroRested(full); } catch (Throwable ignored){}
	}

	public static void onHeroSearched( boolean intentional, boolean found ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onHeroSearched(intentional, found); } catch (Throwable ignored){}
	}

	//Heap.open: the hero opened a container
	public static void onHeapOpened( com.shatteredpixel.shatteredpixeldungeon.items.Heap heap ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onHeapOpened(heap); } catch (Throwable ignored){}
	}

	public static void onTrapTriggered( Trap trap, Char who ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onTrapTriggered(trap, who); } catch (Throwable ignored){}
	}

	//Item.execute: the hero chose an action for an item
	public static void onItemAction( Item item, String action ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onItemAction(item, action); } catch (Throwable ignored){}
	}

	//Document.findPage / readPage: a page was found, or read for the first time
	public static void onPage( String document, String page, boolean found ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onPage(document, page, found); } catch (Throwable ignored){}
	}

	//Level.discover: a secret cell is about to be revealed
	public static void onSecretRevealed( int terrainBefore, int cell ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onSecretRevealed(terrainBefore, cell); } catch (Throwable ignored){}
	}

	//WellWater.affect: the hero used the well
	public static void onWellUsed( Object well ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onWellUsed(well); } catch (Throwable ignored){}
	}

	//Hero.actInteract: a visible NPC accepted the hero's interaction; quest progress is recorded separately
	public static void onNpcInteraction( NPC npc ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onNpcInteraction(npc); } catch (Throwable ignored){}
	}

	//a creature explicitly inherited player ownership from the visible character that created it
	public static void onAllySummoned( Char summoned, Char creator ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onAllySummoned(summoned, creator); } catch (Throwable ignored){}
	}

	//AlchemyScene.combine: a recipe completed; the ingredient list is the player-visible pre-brew snapshot
	public static void onAlchemyCrafted( List<Item> ingredients, Item result, int totalEnergy, int carriedEnergy, int toolkitEnergy ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onAlchemyCrafted(ingredients, result, totalEnergy, carriedEnergy, toolkitEnergy); } catch (Throwable ignored){}
	}

	//an NPC quest moved on: "given", "ready" (the thing asked for was found or beaten) or "done" (the reward stage was reached)
	public static void onQuest( String npc, String step ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onQuest(npc, step); } catch (Throwable ignored){}
	}

	//Notes.add / Notes.remove for a landmark, after the journal changed
	public static void onLandmark( String landmark, int depth, boolean added ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onLandmark(landmark, depth, added); } catch (Throwable ignored){}
	}

	//Notes.add / Notes.remove for a key, after the journal changed
	public static void onKey( Item key, int depth, int quantity, boolean added ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onKey(key, depth, quantity, added); } catch (Throwable ignored){}
	}

	//Buff.attachTo (after it worked) and Buff.detach (before it is removed)
	public static void onBuffAttached( Buff buff, Char target ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onBuff(buff, target, true); } catch (Throwable ignored){}
	}

	public static void onBuffDetached( Buff buff, Char target ){
		PlayTracker p = t();
		if (p == null || restoring > 0) return;
		try { p.onBuff(buff, target, false); } catch (Throwable ignored){}
	}

	public static void onTalentUpgraded( Hero hero, Talent talent ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onTalentUpgraded(hero, talent); } catch (Throwable ignored){}
	}

	public static void onSubclassChosen( Object subclass ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onSubclass(subclass); } catch (Throwable ignored){}
	}

	public static void onAbilityChosen( Object ability ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onAbilityChosen(ability); } catch (Throwable ignored){}
	}

	//a spell is about to be cast / an armor ability is about to be used
	public static void onSpellCast( Object spell, float chargeCost ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onSpell(spell, chargeCost); } catch (Throwable ignored){}
	}

	public static void onAbilityUsed( Object ability, float chargeCost ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onAbilityUsed(ability, chargeCost); } catch (Throwable ignored){}
	}

	//the hero's gold has just changed because of 'cause' ("pickup", "shop.buy", "shop.sell", "shop.buyback")
	public static void onGoldChanged( String cause ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.pollGold(cause); } catch (Throwable ignored){}
	}

	//an item was bought from or sold to a shopkeeper ("buy", "sell", "buyback")
	public static void onTrade( String side, Item item, int price, int quantity ){
		PlayTracker p = t();
		if (p == null) return;
		try { p.onTrade(side, item, price, quantity); } catch (Throwable ignored){}
	}
}
