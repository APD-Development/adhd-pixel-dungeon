package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;

/** APD: the hunger text shown top-right and in the hero status tab. Pure, so it can be tested. */
public final class HungerDisplay {
	private HungerDisplay(){}

	public static final int NORMAL_COLOR = 0xCACFC2, HUNGRY_COLOR = 0xFFFF44, STARVING_COLOR = 0xFF4444;

	/** Top-right text, e.g. "Hunger 180/450"; empty when there is no hunger buff (value < 0). */
	public static String corner(int value){
		return value < 0 ? "" : "Hunger " + value + "/" + (int) Hunger.STARVING;
	}

	public static int color(int value){
		if (value >= Hunger.STARVING) return STARVING_COLOR;
		if (value >= Hunger.HUNGRY) return HUNGRY_COLOR;
		return NORMAL_COLOR;
	}

	/** Status tab line, e.g. "Hunger: _180/450_. Hungry in _120 turns_." */
	public static String status(int value, boolean starving, int turnsUntilHungry){
		String when = starving ? "You are _Starving_."
				: turnsUntilHungry == 0 ? "You are _Hungry_."
				: turnsUntilHungry < 0 ? "Your hunger isn't rising."
				: "Hungry in _" + turnsUntilHungry + (turnsUntilHungry == 1 ? " turn_." : " turns_.");
		return "Hunger: _" + value + "/" + (int) Hunger.STARVING + "_. " + when;
	}
}
