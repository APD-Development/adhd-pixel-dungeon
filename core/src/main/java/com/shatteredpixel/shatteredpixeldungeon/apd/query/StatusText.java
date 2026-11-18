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

import java.util.ArrayList;
import java.util.List;

//The lines of the status strip above a UIMSD table: how many rows there are, what the scope and the filters left out, and how
//much of the data is actually known. Plain sentences, so a screen only has to draw them. A line may carry an action (what tapping
//it does) so "4 unfinished runs hidden" can be turned into "include them".
public final class StatusText {

	private StatusText(){}

	public static final int INFO = 0, NOTE = 1, WARN = 2;

	public static final String ACT_NONE = "", ACT_UNFINISHED = "unfinished", ACT_UNKNOWN_RULESET = "unknownRuleset",
			ACT_MIXED_RULESET = "mixedRuleset", ACT_OTHER_RULESETS = "otherRulesets", ACT_DUPLICATES = "duplicates",
			ACT_SMALL_GROUPS = "smallGroups", ACT_HIDDEN_INFO = "hiddenInfo";

	public static final class Line {
		public final String text;
		public final int kind;
		public final String action;

		Line( String text, int kind, String action ){
			this.text = text;
			this.kind = kind;
			this.action = action;
		}
	}

	private static String plural( int n, String one, String many ){
		return n + " " + (n == 1 ? one : many);
	}

	//rulesetLabel: how the player's current rules read ("challenges 0, exotic 30%, difficulty 3"), may be empty
	public static List<Line> lines( QueryResult r, String viewTitle, String rulesetLabel ){
		List<Line> out = new ArrayList<>();
		QueryResult.Stats s = r.stats;
		Query q = r.query;
		String one = r.def.unitSingular(), many = r.def.unitPlural();
		if (r.isGrouped()){
			out.add(new Line(viewTitle + ": " + plural(r.groups.size(), "group", "groups") + ", counting " + s.matching + " of " + s.total + " " + many, INFO, ACT_NONE));
		} else {
			out.add(new Line(viewTitle + ": " + s.matching + " of " + s.total + " " + many
					+ (s.matching == s.inScope ? "" : " (your filters removed " + (s.inScope - s.matching) + ")"), INFO, ACT_NONE));
		}
		String rules;
		switch (q.scope.rules){
			case ALL: rules = "all rules, mixed together"; break;
			case SPECIFIC: rules = "one set of rules"; break;
			default: rules = "your current rules" + (rulesetLabel == null || rulesetLabel.isEmpty() ? "" : " (" + rulesetLabel + ")");
		}
		out.add(new Line("Showing " + rules + (q.scope.includeUnfinished ? ", runs still going included" : ", finished runs only"), INFO, ACT_NONE));

		if (s.rulesetsInScope > 1){
			out.add(new Line("MIXED RULES: these " + many + " were played under " + s.rulesetsInScope + " different sets of rules (difficulty, challenges, exotic-item setting), "
					+ "so they are not directly comparable. Narrow the rules in Options, or group or filter by Difficulty, to compare like with like.", WARN, ACT_NONE));
		}
		if (s.otherRulesets > 0){
			out.add(new Line(plural(s.otherRulesets, one + " was", many + " were") + " played under other rules (another difficulty, challenges or exotic-item setting) and "
					+ (s.otherRulesets == 1 ? "is" : "are") + " left out. Tap to include " + (s.otherRulesets == 1 ? "it" : "them") + ".", NOTE, ACT_OTHER_RULESETS));
		}
		if (s.unknownRuleset > 0){
			out.add(new Line(plural(s.unknownRuleset, one, many) + (s.unknownRuleset == 1 ? " is" : " are") + " from runs that never recorded their rules, so "
					+ (s.unknownRuleset == 1 ? "it is" : "they are") + " left out. Tap to include " + (s.unknownRuleset == 1 ? "it" : "them") + ".", NOTE, ACT_UNKNOWN_RULESET));
		}
		if (s.mixedRuleset > 0){
			out.add(new Line(plural(s.mixedRuleset, one, many)
					+ (one.equals("run") ? (s.mixedRuleset == 1 ? " had its" : " had their") + " rules changed while being played"
					: (s.mixedRuleset == 1 ? " belongs" : " belong") + " to runs whose rules changed while being played") + ", so "
					+ (s.mixedRuleset == 1 ? "it is" : "they are") + " left out. Tap to include " + (s.mixedRuleset == 1 ? "it" : "them") + ".", NOTE, ACT_MIXED_RULESET));
		}
		if (s.unfinished > 0){
			if (r.query.scope.finishedOnly){
				out.add(new Line(plural(s.unfinished, one, many) + " left out: this view includes finished runs only.", NOTE, ACT_NONE));
			} else {
			out.add(new Line(plural(s.unfinished, one, many) + (s.unfinished == 1 ? " belongs" : " belong") + " to a run that is still going, so "
					+ (s.unfinished == 1 ? "it is" : "they are") + " left out. Tap to include " + (s.unfinished == 1 ? "it" : "them") + ".", NOTE, ACT_UNFINISHED));
			}
		}
		if (s.duplicates > 0){
			out.add(new Line(plural(s.duplicates, "imported ranking record is", "imported ranking records are") + " the same run as one APD recorded itself, so "
					+ (s.duplicates == 1 ? "it is" : "they are") + " counted once. Tap to show " + (s.duplicates == 1 ? "it" : "them") + " too.", NOTE, ACT_DUPLICATES));
		}
		if (s.hiddenRows > 0){
			out.add(new Line(plural(s.hiddenRows, one + " exists", many + " exist") + " only as hidden information of finished runs. "
					+ "Turn on \"Show hidden information\" in Options to see " + (s.hiddenRows == 1 ? "it" : "them") + ".", NOTE, ACT_HIDDEN_INFO));
		}
		for (QueryResult.FilterReport f : s.filters){
			int lost = f.unknown + f.uncertain + f.hidden;
			if (lost == 0) continue;
			StringBuilder b = new StringBuilder(describe(r.def, f.filter)).append(": ");
			boolean first = true;
			if (f.unknown > 0){
				b.append(f.unknown).append(" never recorded");
				first = false;
			}
			if (f.uncertain > 0){
				b.append(first ? "" : ", ").append(f.uncertain).append(" cannot be decided (only an \"at least\" value)");
				first = false;
			}
			if (f.hidden > 0) b.append(first ? "" : ", ").append(f.hidden).append(" hidden");
			b.append(". Left out, not counted as a mismatch.");
			out.add(new Line(b.toString(), NOTE, ACT_NONE));
		}
		if (r.isGrouped() && s.hiddenSmallGroups > 0){
			out.add(new Line(plural(s.hiddenSmallGroups, "group is", "groups are") + " hidden because " + (s.hiddenSmallGroups == 1 ? "it has" : "they have") + " fewer than " + q.minSample
					+ " " + many + " (" + s.hiddenSmallGroupRows + " in all). Tap to show all groups.", NOTE, ACT_SMALL_GROUPS));
		}
		if (s.runsInScope > 0 && s.runsCovered < s.runsInScope){
			out.add(new Line("This table has " + many + " from " + s.runsCovered + " of " + plural(s.runsInScope, "run", "runs") + " in scope; the others were recorded before this kind of detail existed.", WARN, ACT_NONE));
		}
		if (q.showInternal) out.add(new Line("Hidden information is on. It is shown for finished runs only, never for a run that is still going.", INFO, ACT_NONE));
		for (String p : r.problems) out.add(new Line(p, WARN, ACT_NONE));
		return out;
	}

	public static String describe( DatasetDef def, Filter f ){
		ColumnDef c = def.column(f.column);
		String col = c == null ? f.column : c.label;
		StringBuilder b = new StringBuilder(col).append(' ').append(f.op.word);
		if (f.op.takesValue()){
			if (c != null && c.type == ColumnDef.Type.TEXT){
				if (f.texts.length > 3){
					b.append(' ').append(f.texts.length).append(" values");   //"runs containing it" can name hundreds
				} else {
					for (int i = 0; i < f.texts.length; i++){
						String t = c.format == ColumnDef.Format.CLASS_NAME ? CellFormatter.className(f.texts[i]) : f.texts[i];
						b.append(i > 0 ? ", " : " ").append('"').append(t).append('"');
					}
				}
			} else {
				for (int i = 0; i < f.nums.length; i++){
					String v = c == null ? Long.toString(f.nums[i]) : CellFormatter.value(c, f.nums[i]);
					b.append(i > 0 ? (f.op == Op.BETWEEN ? " and " : ", ") : " ").append(v);
				}
			}
		}
		return b.toString();
	}
}
