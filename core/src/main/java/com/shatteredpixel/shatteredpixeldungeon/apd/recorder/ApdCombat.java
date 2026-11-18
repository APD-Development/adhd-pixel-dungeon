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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

//The game's entry points for combat and health recording. Every method returns at once (one volatile read) unless a run is
//being recorded, never throws, and never changes the game or consumes its random numbers: see CombatTracker.
public final class ApdCombat {

	private ApdCombat(){}

	public static final int REVIVE_BLESSED_ANKH = 0, REVIVE_RESURRECT = 1;

	//true while a run is being recorded. Written by ApdRecorder only.
	static volatile boolean active;

	//Char.attack: the roll to hit came out 'hit' or not. Called before anything the hit sets off, so names are as the
	//player saw them when they struck.
	public static void onAttackRolled( Char attacker, Char defender, boolean hit ){
		if (active) ApdRecorder.combatAttackRolled(attacker, defender, hit);
	}

	//Char.attack: a hit is about to deal 'effective' damage (after armor 'armorReduction' was taken off): the numbers behind it
	public static void onAttackDetail( Char attacker, Char defender, int rolled, int armorReduction, int effective ){
		if (active) ApdRecorder.combatAttackDetail(attacker, defender, rolled, armorReduction, effective);
	}

	//Char.hit (the form spells and bolts use): the roll came out 'hit' or not
	public static void onMagicRoll( Char attacker, Char defender, boolean hit ){
		if (active) ApdRecorder.combatMagicRoll(attacker, defender, hit);
	}

	//Char.damage: damage was applied
	public static void onDamage( Char target, Object src, int requested, int hpBefore, int shieldBefore, int absorbed, int hpLost ){
		if (active) ApdRecorder.combatDamage(target, src, requested, hpBefore, shieldBefore, absorbed, hpLost);
	}

	//Char.die: something died
	public static void onCharDied( Char ch, Object src ){
		if (active) ApdRecorder.combatCharDied(ch, src);
	}

	//Hero.die: the hero's HP reached zero (an ankh may still bring them back)
	public static void onHeroDying( Object cause ){
		if (active) ApdRecorder.combatHeroDying(cause);
	}

	public static void onHeroRevived( int kind ){
		if (active) ApdRecorder.combatHeroRevived(kind);
	}

	//Hero.earnExp
	public static void onXp( Hero hero, int amount, Class<?> source ){
		if (active) ApdRecorder.combatXp(hero, amount, source);
	}

	//The keys stored in the log for each heal source (HEAL_DETAIL), so they never change meaning. The part before the first dot is the
	//category UIMSD sums them into (CombatEvents.healCategory). Heals of other creatures than the hero are not recorded, so Bless, Lay on
	//Hands and Hallowed Ground (which only ever heal allies; the hero gets a shield) have no key.
	public static final String
			HEAL_REGEN = "regen",                                       //natural regeneration (the Chalice of Blood speeds it up)
			HEAL_POTION_HEALING = "potion.healing",                     //the Healing effect: Potion of Healing, and Vial of Blood dewdrop bursts
			HEAL_POTION_AQUATIC = "potion.aquaticElixir",
			HEAL_FOOD_WELL_FED = "food.wellFed",
			HEAL_FOOD_PASTY = "food.pasty",
			HEAL_FOOD_SUPPLY_RATION = "food.supplyRation",
			HEAL_FOOD_PHANTOM_MEAT = "food.phantomMeat",
			HEAL_FOOD_FROZEN_CARPACCIO = "food.frozenCarpaccio",
			HEAL_PLANT_SUNGRASS = "plant.sungrass",
			HEAL_PLANT_DEWDROP = "plant.dewdrop",
			HEAL_GEAR_CHALICE = "gear.chalice",
			HEAL_GEAR_VAMPIRIC = "gear.vampiric",
			HEAL_GEAR_METABOLISM = "gear.metabolism",
			HEAL_GEAR_TORN_PAGE = "gear.tornPage",
			HEAL_SKILL_HEARTY_MEAL = "skill.heartyMeal",
			HEAL_SKILL_SOUL_SIPHON = "skill.soulSiphon",
			HEAL_SKILL_INVIGORATING_VICTORY = "skill.invigoratingVictory",
			HEAL_SKILL_ELEMENTAL_STRIKE = "skill.elementalStrike",
			HEAL_WAND_TRANSFUSION = "wand.transfusion",
			HEAL_WAND_CURSED = "wand.cursed",
			HEAL_OTHER_SLEEP = "other.sleep",
			HEAL_OTHER_FULL_HEAL = "other.fullHeal",                   //the vault exits (City level, Escape Crystal)
			HEAL_OTHER_WELL_OF_HEALTH = "other.wellOfHealth";

	//The hero was healed by 'source' (one of the HEAL_* keys below): called right AFTER the game applied the heal, with the HP from just
	//BEFORE it. The recorder reads the HP now, so the amount is exactly what the game applied (the maximum HP cap included).
	//'requested' is what the source asked for before that cap, or -1.
	public static void onHeal( Char target, int hpBefore, String source, int requested ){
		if (active) ApdRecorder.combatHeal(target, hpBefore, source, requested);
	}

	//Hunger.affectHunger: hunger was changed by eating or an effect
	public static void onHungerChanged( float energy, int before, int after ){
		if (active) ApdRecorder.combatHunger(energy, before, after);
	}
}
