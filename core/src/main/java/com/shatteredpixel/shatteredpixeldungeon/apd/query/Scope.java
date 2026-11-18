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

//Which rows are in play before any filter. The defaults are the safe ones: only the current ruleset, only finished runs,
//and no imported record that is the same run as a recorded one. Everything left out is counted and can be included on purpose.
//A row whose ruleset is unknown is never treated as being in the current ruleset.
public final class Scope {

	public enum Rules { CURRENT, ALL, SPECIFIC }

	public final Rules rules;
	public final String ruleset;
	public final boolean includeUnknownRuleset;
	public final boolean includeMixedRuleset;
	public final boolean includeUnfinished;
	public final boolean includeDuplicates;
	public final boolean finishedOnly;

	public Scope( Rules rules, String ruleset, boolean includeUnknownRuleset, boolean includeMixedRuleset,
	              boolean includeUnfinished, boolean includeDuplicates ){
		this(rules, ruleset, includeUnknownRuleset, includeMixedRuleset, includeUnfinished, includeDuplicates, false);
	}

	public Scope( Rules rules, String ruleset, boolean includeUnknownRuleset, boolean includeMixedRuleset,
	              boolean includeUnfinished, boolean includeDuplicates, boolean finishedOnly ){
		this.rules = rules;
		this.ruleset = ruleset == null ? "" : ruleset;
		this.includeUnknownRuleset = includeUnknownRuleset;
		this.includeMixedRuleset = includeMixedRuleset;
		this.includeUnfinished = includeUnfinished && !finishedOnly;
		this.includeDuplicates = includeDuplicates;
		this.finishedOnly = finishedOnly;
	}

	public static final Scope DEFAULT = new Scope(Rules.CURRENT, "", false, false, false, false);

	public Scope withRules( Rules r, String ruleset ){
		return new Scope(r, ruleset, includeUnknownRuleset, includeMixedRuleset, includeUnfinished, includeDuplicates, finishedOnly);
	}

	public Scope withUnknownRuleset( boolean v ){
		return new Scope(rules, ruleset, v, includeMixedRuleset, includeUnfinished, includeDuplicates, finishedOnly);
	}

	public Scope withMixedRuleset( boolean v ){
		return new Scope(rules, ruleset, includeUnknownRuleset, v, includeUnfinished, includeDuplicates, finishedOnly);
	}

	public Scope withUnfinished( boolean v ){
		return new Scope(rules, ruleset, includeUnknownRuleset, includeMixedRuleset, v, includeDuplicates, finishedOnly);
	}

	public Scope withDuplicates( boolean v ){
		return new Scope(rules, ruleset, includeUnknownRuleset, includeMixedRuleset, includeUnfinished, v, finishedOnly);
	}

	public Scope withFinishedOnly(boolean value){
		return new Scope(rules, ruleset, includeUnknownRuleset, includeMixedRuleset, includeUnfinished, includeDuplicates, value);
	}

	@Override
	public boolean equals( Object o ){
		if (!(o instanceof Scope)) return false;
		Scope s = (Scope) o;
		return rules == s.rules && ruleset.equals(s.ruleset) && includeUnknownRuleset == s.includeUnknownRuleset
				&& includeMixedRuleset == s.includeMixedRuleset && includeUnfinished == s.includeUnfinished
				&& includeDuplicates == s.includeDuplicates && finishedOnly == s.finishedOnly;
	}

	@Override
	public int hashCode(){
		return rules.hashCode() * 31 + ruleset.hashCode() + (includeUnknownRuleset ? 1 : 0) + (includeUnfinished ? 2 : 0)
				+ (includeMixedRuleset ? 4 : 0) + (includeDuplicates ? 8 : 0) + (finishedOnly ? 16 : 0);
	}
}
