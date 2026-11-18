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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty;

import java.security.MessageDigest;

//The gameplay-affecting rules in force, as an identity. Two runs with the same ruleset id were played under the same
//rules, runs with different ids must not be silently mixed in statistics.
//
//What counts (decided with the statistics architecture): APD-0001 (starvation consumes shielding), Cleric blocked,
//the exotic crystal conversion chance, the challenge configuration, and future gameplay-affecting APD settings and
//mods. Changing an OFFICIAL APD setting changes the ruleset id but is not "modded": externalModsPresent is a separate
//flag (always false until a mod system exists).
//
//The id is "r1-" plus the first 12 hex digits of the SHA-256 of the rule text. "r1" is the version of this rule list:
//if a rule is ever added to the text, bump it so old and new ids can never collide.
public final class Ruleset {

	public static final String BASE_GAME = "Shattered Pixel Dungeon 4.0.0";
	private static final String ID_VERSION = "r1";

	public final String id;
	public final String text;
	public final int exoticChance;

	private Ruleset( String id, String text, int exoticChance ){
		this.id = id;
		this.text = text;
		this.exoticChance = exoticChance;
	}

	//reads the rules from the game right now (the run being played: its own difficulty, not the setting)
	public static Ruleset current(){
		//a run that never recorded its exotic setting (saved by an older version) has no known ruleset: it is not given today's setting
		if (!Dungeon.apdExoticKnown) return unknown();
		boolean clericRun = Dungeon.hero != null && Dungeon.hero.heroClass == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.CLERIC;
		return withHealingOnlyConversion(withCustomRules(of(Dungeon.apdExotic, Dungeon.challenges, Dungeon.difficulty, Dungeon.difficultyBalance, clericRun), Dungeon.customRules()));
	}

	//the rules a NEW run would be played under, from the settings
	public static Ruleset forNewRun(){
		return withHealingOnlyConversion(withCustomRules(of(SPDSettings.apdExoticSetting(), SPDSettings.challenges(), SPDSettings.apdDifficulty()), SPDSettings.apdCustomRules()));
	}

	//stored in place of the exotic chance when it was not recorded (a real chance is 0-80)
	public static final int EXOTIC_NOT_RECORDED = com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.EXOTIC_NOT_RECORDED;

	//rules that are not known: empty id (a run recorded with it has an unknown ruleset, which no scope of "my rules" includes)
	public static Ruleset unknown(){
		return new Ruleset("", "unknown", EXOTIC_NOT_RECORDED);
	}

	public boolean isUnknown(){
		return id.isEmpty();
	}

	//difficulty 3 (the game as it is)
	public static Ruleset of( int exoticChance, int challenges ){
		return of(exoticChance, challenges, Difficulty.DEFAULT);
	}

	//Difficulty is part of the identity. Difficulty 3 adds nothing to the rule text, so its id is exactly the id the same rules had before
	//difficulty existed (older runs stay in the same ruleset as new difficulty-3 runs: same balance). Any other level adds its number and its
	//balance revision (0 = placeholder), so runs of different difficulty or different balance can never share an id.
	public static Ruleset of( int exoticChance, int challenges, int difficulty ){
		int level = Difficulty.isValid(difficulty) ? difficulty : Difficulty.DEFAULT;
		return of(exoticChance, challenges, level, Difficulty.balanceRevision(level));
	}

	//a run's own rules: its difficulty and the balance revision it was started with (which a later build's newer balance never changes)
	public static Ruleset of( int exoticChance, int challenges, int difficulty, int balance ){
		return of(exoticChance, challenges, difficulty, balance, false);
	}

	//A run played as Cleric (possible only when the test switch un-bans Cleric) says "cleric=playable" instead of "cleric=blocked".
	//Every other run keeps exactly the text, and so the id, it always had: nothing already recorded changes identity, and the Cleric
	//runs, whose rules were never played before, can never be mixed into the history of the others.
	public static Ruleset of( int exoticChance, int challenges, int difficulty, int balance, boolean clericRun ){
		String text = "base=spd-4.0.0;apd0001=on;cleric=" + (clericRun ? "playable" : "blocked") + ";exotic=" + exoticChance + ";challenges=" + challenges;
		int level = Difficulty.isValid(difficulty) ? difficulty : Difficulty.DEFAULT;
		if (level != Difficulty.DEFAULT) text += ";difficulty=" + level + ";balance=" + Math.max(0, balance);
		return new Ruleset(ID_VERSION + "-" + hash12(text), text, exoticChance);
	}

    /** Historical/neutral identities remain intact; custom configurations cannot silently share a scope. */
    public static Ruleset withCustomRules(Ruleset base,com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules rules){
        if(base.isUnknown() || rules==null || rules.isNeutral())return base;
        String text=base.text+";"+rules.identityText();
        return new Ruleset("r2-"+hash12(text),text,base.exoticChance);
    }

    /** Distinguish the corrected healing-only override without rewriting stored historical identities. */
    public static Ruleset withHealingOnlyConversion(Ruleset base){
        if(base.isUnknown())return base;
        String text=base.text+";exotic-scope=healing-absolute-v2";
        return new Ruleset("r4-"+hash12(text),text,base.exoticChance);
    }

	private static String hash12( String text ){
		try {
			byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 6; i++){
				sb.append(Character.forDigit((d[i] >> 4) & 0xF, 16)).append(Character.forDigit(d[i] & 0xF, 16));
			}
			return sb.toString();
		} catch (Exception e){
			//SHA-256 and UTF-8 always exist, but never let a recorder problem escape
			return "unknown00000";
		}
	}
}
