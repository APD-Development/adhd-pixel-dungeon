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


package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellFormatter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Filter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Op;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.StatusText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;

import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Entry;

import java.util.ArrayList;
import java.util.List;

//APD: adding and editing a UIMSD filter in three clear steps: what to filter on (columns grouped by what they describe), how to compare (in plain
//words), and the value (chosen from what the data really holds wherever it can be: the classes you played, Won/Died/Abandoned, the difficulty levels;
//typed only where any text or number makes sense). Every step says where you are, what it means and what is on now. Every step is built from the
//column's own definition (its type, its words), so it works for any dataset. Nothing is removed: every operator is still there.
public final class WndUimsdFilter {

	private WndUimsdFilter(){}

	private static final int MAX_CHOICES = 60;

	private static void show( com.shatteredpixel.shatteredpixeldungeon.ui.Window w ){
		//a step stays open under the next, so Back returns to the step before
		if (w instanceof WndUimsdList) ((WndUimsdList) w).keepOpen();
		ShatteredPixelDungeon.scene().addToFront(w);
	}

	private static String shorten( String s, int n ){
		return s.length() <= n ? s : s.substring(0, n - 2) + "..";
	}

	//what kind of value a column holds, in a few words
	static String kindHint( UimsdModel m, ColumnDef c ){
		switch (c.type){
			case ENUM: {
				StringBuilder b = new StringBuilder("choose: ");
				int n = 0;
				for (String w : c.labels.values()){
					if (n == 3){
						b.append(", ...");
						break;
					}
					b.append(n++ == 0 ? "" : ", ").append(w);
				}
				return c.labels.isEmpty() ? "a choice" : b.toString();
			}
			case TEXT: {
				List<UimsdModel.Choice> ch = m.choicesFor(c, 3);
				if (ch.isEmpty()) return "text";
				StringBuilder b = new StringBuilder("choose or type, like ");
				for (int i = 0; i < ch.size(); i++) b.append(i > 0 ? ", " : "").append(ch.get(i).label);
				return shorten(b.toString(), 60);
			}
			default:
				return c.format == ColumnDef.Format.DATE_MILLIS ? "a date" : "a number";
		}
	}

	private static String groupName( ColumnDef c ){
		if (c.group.isEmpty()) return "Details";
		if (c.group.equals("Hidden")) return "Hidden information (finished runs only)";
		return c.group;
	}

	//step 1: a new filter
	public static void add( final UimsdModel m ){
		final List<ColumnDef> cols = m.offeredColumns();
		List<Entry> entries = new ArrayList<>();
		String last = null;
		for (ColumnDef c : cols){
			String g = groupName(c);
			if (!g.equals(last)){
				entries.add(Entry.heading(g));
				last = g;
			}
			String detail = kindHint(m, c) + (c.description.isEmpty() ? "" : "  -  " + shorten(c.description, 70));
			entries.add(Entry.item(c.label, detail));
		}
		show(WndUimsdList.menu("Add a filter",
				"Step 1 of 3: what do you want to filter on? Entries where this was never recorded are left out, unless you choose \"is unknown\".",
				entries, new WndUimsdList.Picker(){
					@Override
					public void onPick( int index, boolean checked ){
						pickOp(m, -1, null, cols.get(index));
					}
				}).restoreWith(() -> add(m)));
	}

	//what to do with a filter that is already there
	public static void chip( final UimsdModel m, final int index ){
		if (index < 0 || index >= m.query().filters.size()) return;
		final Filter f = m.query().filters.get(index);
		List<Entry> entries = new ArrayList<>();
		entries.add(Entry.item("Change it", "pick another comparison or value for the same column"));
		entries.add(Entry.item("Remove it", "show everything this filter was leaving out again"));
		show(WndUimsdList.menu("Filter", StatusText.describe(m.dataset().def, f), entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				if (i == 1){
					WndUimsdList.closeAll();
					m.removeFilter(index);
				} else {
					ColumnDef c = m.dataset().def.column(f.column);
					if (c != null) pickOp(m, index, f, c);
				}
			}
		}).restoreWith(() -> chip(m, index)));
	}

	//step 2: how to compare
	//(public so a device test can build every step of the workflow for every column)
	public static void pickOp( final UimsdModel m, final int index, final Filter existing, final ColumnDef col ){
		final Op[] ops = Op.ops(col.type);
		List<Entry> entries = new ArrayList<>();
		for (Op o : ops){
			entries.add(Entry.item(col.label + " " + o.word, o.hint).current(existing != null && existing.op == o));
		}
		show(WndUimsdList.menu(col.label, "Step 2 of 3: how should " + col.label + " be compared?", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				Op op = ops[i];
				if (!op.takesValue()) finish(m, index, new Filter(col.id, op, null, null));
				else pickValue(m, index, existing, col, op);
			}
		}).restoreWith(() -> pickOp(m, index, existing, col)));
	}

	private static boolean hasNum( Filter f, ColumnDef col, long v ){
		if (f == null || !f.column.equals(col.id)) return false;
		for (long n : f.nums) if (n == v) return true;
		return false;
	}

	private static boolean hasText( Filter f, ColumnDef col, String v ){
		if (f == null || !f.column.equals(col.id)) return false;
		for (String t : f.texts) if (t.equals(v)) return true;
		return false;
	}

	//step 3: the value
	public static void pickValue( final UimsdModel m, final int index, final Filter existing, final ColumnDef col, final Op op ){
		final String sentence = col.label + " " + op.word;
		if (col.type == ColumnDef.Type.ENUM && !col.labels.isEmpty()){
			final List<Long> values = new ArrayList<>(col.labels.keySet());
			List<Entry> entries = new ArrayList<>();
			for (Long v : values) entries.add(Entry.item(col.labels.get(v)).current(hasNum(existing, col, v)));
			choose(m, index, col, op, sentence, entries, new Chosen(){
				@Override
				public Filter filter( List<Integer> picked ){
					if (op == Op.IN){
						long[] nums = new long[picked.size()];
						for (int i = 0; i < nums.length; i++) nums[i] = values.get(picked.get(i));
						return Filter.num(col.id, Op.IN, nums);
					}
					return Filter.num(col.id, op, values.get(picked.get(0)));
				}
			}, null);
			return;
		}
		if (col.type == ColumnDef.Type.TEXT){
			final List<UimsdModel.Choice> choices = op == Op.CONTAINS ? new ArrayList<UimsdModel.Choice>() : m.choicesFor(col, MAX_CHOICES);
			if (choices.isEmpty()){
				typeText(m, index, col, op, sentence);
				return;
			}
			List<Entry> entries = new ArrayList<>();
			for (UimsdModel.Choice c : choices){
				entries.add(Entry.item(c.label, c.count + (c.count == 1 ? " entry" : " entries")).current(hasText(existing, col, c.value)));
			}
			choose(m, index, col, op, sentence, entries, new Chosen(){
				@Override
				public Filter filter( List<Integer> picked ){
					if (op == Op.IN){
						String[] t = new String[picked.size()];
						for (int i = 0; i < t.length; i++) t[i] = choices.get(picked.get(i)).value;
						return Filter.text(col.id, Op.IN, t);
					}
					return Filter.text(col.id, op, choices.get(picked.get(0)).value);
				}
			}, new Runnable(){
				@Override
				public void run(){
					typeText(m, index, col, op, sentence);
				}
			});
			return;
		}
		//numbers (dates are typed as yyyy-MM-dd)
		final String hint = col.format == ColumnDef.Format.DATE_MILLIS ? "A date, like 2026-10-07" : "A whole number, like 250";
		if (op == Op.BETWEEN){
			ask(sentence, "The lower value. " + hint, new Answer(){
				@Override
				public void got( final String lo ){
					ask(sentence, "The upper value. " + hint, new Answer(){
						@Override
						public void got( String hi ){
							Long a = CellFormatter.parse(col, lo), b = CellFormatter.parse(col, hi);
							if (a == null || b == null){
								ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", "That is not a valid value. " + hint + ".", false));
								return;
							}
							finish(m, index, Filter.num(col.id, Op.BETWEEN, a, b));
						}
					});
				}
			});
		} else {
			ask(sentence, hint, new Answer(){
				@Override
				public void got( String s ){
					Long a = CellFormatter.parse(col, s);
					if (a == null){
						ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", "That is not a valid value. " + hint + ".", false));
						return;
					}
					finish(m, index, Filter.num(col.id, op, a));
				}
			});
		}
	}

	private interface Chosen {
		Filter filter( List<Integer> picked );
	}

	//a list of explicit choices: one tap for a single value, ticks for "is one of". 'typeInstead' adds a last entry that falls back to typing.
	private static void choose( final UimsdModel m, final int index, ColumnDef col, final Op op, String sentence, List<Entry> entries,
	                            final Chosen chosen, final Runnable typeInstead ){
		if (op == Op.IN){
			List<Integer> picked = new ArrayList<>();
			for (int i = 0; i < entries.size(); i++) if (entries.get(i).current) picked.add(i);
			chooseMany(m, index, sentence, entries, chosen, picked, typeInstead);
			return;
		}
		final List<Entry> originalEntries = new ArrayList<>(entries);
		entries = new ArrayList<>(entries);
		final int typeAt = typeInstead == null ? -1 : entries.size();
		if (typeInstead != null) entries.add(Entry.item("Something else...", "type a value yourself"));
		show(WndUimsdList.menu(sentence, "Step 3 of 3: choose a value.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				if (i == typeAt){
					typeInstead.run();
					return;
				}
				List<Integer> one = new ArrayList<>();
				one.add(i);
				finish(m, index, chosen.filter(one));
			}
		}).restoreWith(() -> choose(m, index, col, op, sentence, originalEntries, chosen, typeInstead)));
	}

	private static void chooseMany(final UimsdModel m, final int index, String sentence,
								   List<Entry> entries, final Chosen chosen, final List<Integer> picked, final Runnable typeInstead){
		for (int i = 0; i < entries.size(); i++) entries.get(i).checked(picked.contains(i));
        final int typeAt = typeInstead == null ? -1 : entries.size();
        List<Entry> displayed = new ArrayList<>(entries);
        if (typeInstead != null) displayed.add(Entry.action("Enter a full text list..."));
			show(new WndUimsdList(sentence, "Step 3 of 3: tick every value you want, then Done.", true, displayed, new WndUimsdList.Picker(){
				@Override
				public void onPick( int i, boolean checked ){
                    if (i == typeAt){ typeInstead.run(); return; }
					if (checked){
						if (!picked.contains(i)) picked.add(i);
					} else {
						picked.remove(Integer.valueOf(i));
					}
				}
			}){
				@Override
				protected void onDone(){
					hide();
					if (!picked.isEmpty()) finish(m, index, chosen.filter(picked));
				}
			}.restoreWith(() -> chooseMany(m, index, sentence, entries, chosen, new ArrayList<>(picked), typeInstead)));
	}

	private static void typeText( final UimsdModel m, final int index, final ColumnDef col, final Op op, String sentence ){
		ask(sentence, op == Op.IN ? "Enter the full list of values, separated by commas" : op == Op.CONTAINS ? "Some text that appears in it" : "The text to match", op == Op.IN ? Integer.MAX_VALUE : 40, new Answer(){
			@Override
			public void got( String s ){
				if (op == Op.IN){
					String[] parts = s.split(",");
					List<String> t = new ArrayList<>();
					for (String p : parts) if (!p.trim().isEmpty()) t.add(p.trim());
					if (t.isEmpty()) return;
					finish(m, index, Filter.text(col.id, Op.IN, t.toArray(new String[0])));
				} else {
					if (s.trim().isEmpty()) return;
					finish(m, index, Filter.text(col.id, op, s.trim()));
				}
			}
		});
	}

	private interface Answer {
		void got( String text );
	}

	private static void ask( String title, String body, final Answer answer ){
        ask(title, body, 40, answer);
    }

    private static void ask(String title, String body, int limit, final Answer answer){
		show(new WndUimsdTextInput(title, body, "", limit, false, "OK", "Cancel", (positive, text) -> {
				if (positive) answer.got(text);
			}));
	}

	private static void finish( UimsdModel m, int index, Filter f ){
		WndUimsdList.closeAll();
		if (index < 0) m.addFilter(f); else m.replaceFilter(index, f);
	}
}

