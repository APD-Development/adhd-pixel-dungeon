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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GameFacts;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

import java.util.LinkedHashMap;
import java.util.List;

//Turns what the hero does and what happens around them into generic fact observations (see FactKinds, GameFacts): steps, rests,
//searches, item actions, status effects, talents, spells, abilities, gold, trades, traps, and the time actually spent playing.
//
//Reads the game, never changes it, never consumes a random number. Every method is called from a one-line hook in the game code (see
//ApdPlay) and swallows its own errors: a statistics problem must never reach gameplay.
final class PlayTracker {

	private final FactTracker facts;

	private int lastGold;
	private long lastNano;
	private long activeNanos;
	//idle gaps (the app in the background, a long pause) are not play time
	private static final long MAX_GAP_NANOS = 30_000_000_000L;
	private static final long NANOS_PER_MILLI = 1_000_000L;

	PlayTracker( FactTracker facts ){
		this.facts = facts;
		baseline();
	}

	//the state "as it is now" is the starting point: only changes after this are observations
	void baseline(){
		lastGold = Dungeon.gold;
		lastNano = System.nanoTime();
	}

	private static String name( Class<?> c ){
		return GameSnapshot.shortName(c);
	}

	// ---- the hero's turn ----

	void onHeroTurn(){
		long now = System.nanoTime();
		long gap = now - lastNano;
		lastNano = now;
		if (gap > 0 && gap <= MAX_GAP_NANOS) activeNanos += gap;
		pollGold("other");
		Hero h = Dungeon.hero;
		if (h != null){
			Hunger hunger = h.buff(Hunger.class);
			facts.record(GameFacts.HERO_STATE, GameSnapshot.turn(), "", "", new long[]{ h.HP, h.HT, h.shielding(), hunger == null ? 0 : hunger.hunger() }, null);
		}
	}

	void recordTime(){
		facts.record(GameFacts.TIME_ACTIVE, GameSnapshot.turn(), "", "", new long[]{ activeNanos / NANOS_PER_MILLI }, null);
	}

	// ---- movement and exploring ----

	void onHeroMoved( Hero hero, int to, boolean travelling ){
		facts.record(GameFacts.MOVE_STEP, GameSnapshot.turn(), "", "", new long[]{ to, Dungeon.level.width(), travelling ? 1 : 0 }, null);
	}

	void onMoveCommand( int destination, String mode ){
		facts.record(GameFacts.MOVE_COMMAND, GameSnapshot.turn(), "", "",
				new long[]{ destination, Dungeon.level.width() }, new String[]{ mode == null ? "" : mode });
	}

	void onBossPhase( String boss, int phase, String state ){
		if (boss == null || boss.isEmpty() || phase < 2) return;
		facts.record(GameFacts.BOSS_PHASE, GameSnapshot.turn(), boss, "",
				new long[]{ phase, Dungeon.depth }, new String[]{ state == null ? "" : state });
	}

	void onHeroRested( boolean full ){
		facts.record(GameFacts.REST, GameSnapshot.turn(), "", "", new long[]{ full ? 1 : 0 }, null);
	}

	void onHeroSearched( boolean intentional, boolean found ){
		facts.record(GameFacts.SEARCH, GameSnapshot.turn(), "", "", new long[]{ intentional ? 1 : 0, found ? 1 : 0 }, null);
	}

	void onHeapOpened( com.shatteredpixel.shatteredpixeldungeon.items.Heap heap ){
		if (heap == null) return;
		facts.record(GameFacts.OPEN, GameSnapshot.turn(), String.valueOf(heap.type), "", new long[]{ heap.items.size() }, null);
	}

	void onTrapTriggered( Trap trap, Char who ){
		if (trap == null) return;
		boolean byHero = who == Dungeon.hero;
		if (!byHero && !CombatTracker.visible(who) && !(Dungeon.level.heroFOV != null && trap.pos >= 0 && trap.pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[trap.pos])) return;
		facts.record(GameFacts.TRAP, GameSnapshot.turn(), name(trap.getClass()), byHero || who == null ? "" : CombatTracker.nameOf(who), new long[]{ byHero ? 1 : 0 }, null);
	}

	void onPage( String document, String page, boolean found ){
		facts.record(GameFacts.PAGE, GameSnapshot.turn(), document, "", new long[0], new String[]{ found ? "found" : "read", page });
	}

	// ---- secrets and wells ----

	void onSecretRevealed( int terrainBefore, int cell ){
		String what = terrainBefore == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR ? "Secret door"
				: terrainBefore == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_TRAP ? "Secret trap" : "";
		if (what.isEmpty()) return;
		facts.record(GameFacts.DISCOVER, GameSnapshot.turn(), what, "", new long[]{ cell }, null);
	}

	void onWellUsed( Object well ){
		if (well == null) return;
		facts.record(GameFacts.WELL, GameSnapshot.turn(), name(well.getClass()), "", new long[0], null);
	}

	// ---- journal ----

	void onLandmark( String landmark, int depth, boolean added ){
		facts.record(GameFacts.LANDMARK, GameSnapshot.turn(), landmark, "", new long[]{ depth }, new String[]{ added ? "added" : "removed" });
	}

	void onNpcInteraction( NPC npc ){
		if (npc == null) return;
		String shown;
		try {
			shown = npc.name();
		} catch (Throwable t){
			shown = "";
		}
		facts.record(GameFacts.NPC_INTERACT, GameSnapshot.turn(), shown, "", new long[]{ Dungeon.depth }, null);
	}

	void onAllySummoned( Char summoned, Char creator ){
		if (summoned == null || creator == null
				|| summoned.apdCreatorOffensiveOrigin() != com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin.PLAYER
				|| !CombatTracker.visible(creator)) return;
		facts.record(GameFacts.ALLY_SUMMON, GameSnapshot.turn(), CombatTracker.nameOf(summoned),
				CombatTracker.nameOf(creator), new long[]{ Dungeon.depth }, null);
	}

	void onQuest( String npc, String step ){
		facts.record(GameFacts.QUEST, GameSnapshot.turn(), npc, "", new long[]{ Dungeon.depth }, new String[]{ step });
	}

	void onKey( com.shatteredpixel.shatteredpixeldungeon.items.Item key, int depth, int quantity, boolean added ){
		if (key == null) return;
		facts.record(GameFacts.KEY, GameSnapshot.turn(), name(key.getClass()), "", new long[]{ depth, quantity }, new String[]{ added ? "added" : "used" });
	}

	// ---- items ----

	void onItemAction( Item item, String action ){
		if (item == null) return;
		String shown;
		try {
			shown = item.name();
		} catch (Throwable t){
			shown = "";
		}
		long clock = GameSnapshot.turn();
		facts.record(GameFacts.ITEM_USE, clock, shown, "", new long[]{ item.quantity() }, new String[]{ action == null ? "" : action });
		long charges = -1;
		if (item instanceof Wand) charges = ((Wand) item).curCharges;
		facts.record(GameFacts.ITEM_USE_TRUTH, clock, name(item.getClass()), "", new long[]{ item.level(), charges }, new String[]{ action == null ? "" : action });
	}

	void onAlchemyCrafted( List<Item> ingredients, Item result, int totalEnergy, int carriedEnergy, int toolkitEnergy ){
		if (result == null) return;
		StringBuilder shownIngredients = new StringBuilder();
		if (ingredients != null){
			for (Item ingredient : ingredients){
				if (ingredient == null) continue;
				if (shownIngredients.length() > 0) shownIngredients.append(" + ");
				shownIngredients.append(ingredient.quantity()).append("x ").append(shownName(ingredient));
			}
		}
		facts.record(GameFacts.ALCHEMY_CRAFT, GameSnapshot.turn(), shownName(result), "",
				new long[]{ Dungeon.depth, result.quantity(), totalEnergy, carriedEnergy, toolkitEnergy },
				new String[]{ shownIngredients.toString() });
	}

	private static String shownName( Item item ){
		try {
			return item == null ? "" : item.name();
		} catch (Throwable t){
			return "";
		}
	}

	// ---- status effects ----

	void onBuff( Buff buff, Char target, boolean began ){
		if (buff == null || target == null) return;
		if (target != Dungeon.hero && !CombatTracker.visible(target)) return;
		float turns = -1;
		if (buff instanceof FlavourBuff){
			try {
				turns = ((FlavourBuff) buff).cooldown();
			} catch (Throwable ignored){
			}
		}
		boolean shownToPlayer = buff.icon() != BuffIndicator.NONE;
		String who = target == Dungeon.hero ? "" : CombatTracker.nameOf(target);
		long[] v = new long[]{ Math.round(turns) };
		long clock = GameSnapshot.turn();
		//what the player saw: the effect's shown name, and only for an effect with an icon. The class says more (an armor glyph, a ring), so it
		//goes only in the hidden record, which has every effect.
		if (shownToPlayer){
			String shown;
			try {
				shown = buff.toString();
			} catch (Throwable t){
				shown = "";
			}
			facts.record(began ? GameFacts.STATUS_START : GameFacts.STATUS_END, clock, shown, who, v, null);
		}
		facts.record(began ? GameFacts.STATUS_START_TRUTH : GameFacts.STATUS_END_TRUTH, clock, name(buff.getClass()), who, v, null);
	}

	// ---- character ----

	void onTalentUpgraded( Hero hero, Talent talent ){
		int tierNo = 0, points = 0;
		for (int t = 0; t < hero.talents.size(); t++){
			LinkedHashMap<Talent, Integer> tier = hero.talents.get(t);
			if (tier.containsKey(talent)){
				tierNo = t + 1;
				points = tier.get(talent);
			}
		}
		facts.record(GameFacts.TALENT, GameSnapshot.turn(), talent.name(), "", new long[]{ points, tierNo }, null);
	}

	void onSubclass( Object subclass ){
		facts.record(GameFacts.SUBCLASS, GameSnapshot.turn(), String.valueOf(subclass), "", null, null);
	}

	void onAbilityChosen( Object ability ){
		facts.record(GameFacts.ABILITY_CHOOSE, GameSnapshot.turn(), ability == null ? "" : name(ability.getClass()), "", null, null);
	}

	void onSpell( Object spell, float cost ){
		facts.record(GameFacts.SPELL_CAST, GameSnapshot.turn(), name(spell.getClass()), "", new long[]{ Math.round(cost) }, null);
	}

	void onAbilityUsed( Object ability, float cost ){
		facts.record(GameFacts.ABILITY_USE, GameSnapshot.turn(), name(ability.getClass()), "", new long[]{ Math.round(cost) }, null);
	}

	// ---- economy ----

	void pollGold( String cause ){
		int now = Dungeon.gold;
		if (now == lastGold) return;
		facts.record(GameFacts.GOLD, GameSnapshot.turn(), "", "", new long[]{ now - lastGold, now }, new String[]{ cause });
		lastGold = now;
	}

	void onTrade( String side, Item item, int price, int quantity ){
		String shown;
		try {
			shown = item == null ? "" : item.name();
		} catch (Throwable t){
			shown = "";
		}
		facts.record(GameFacts.TRADE, GameSnapshot.turn(), shown, "", new long[]{ price, quantity }, new String[]{ side });
	}
}
