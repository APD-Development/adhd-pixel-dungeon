package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import java.util.ArrayList;
import java.util.List;

//A report: a readable document built from UIMSD data (never a picture of a table). It is plain data: headings, notes, labelled values and
//tables, in which every value keeps its CellText state (exact, >= lower bound, ~ inferred, ? unknown, hidden, legacy) and every table says
//where its rows come from (RECORDED history, CANONICAL seed replay, or both with a Basis column). The renderers (ReportRenderer) turn it into
//Markdown, plain text, a web page or CSV, and print the legend for exactly the marks that appear.
public final class Report {

	//where the numbers of a block come from
	public enum Provenance { RECORDED, CANONICAL, MIXED }

	public enum NoteKind { INFO, NOTE, WARN }

	//a value with the kind of knowledge it is (CellText.EXACT ... CellText.HIDDEN)
	public static final class Cell {
		public final String text;
		public final int state;
		public final boolean small;

		public Cell( String text, int state, boolean small ){
			this.text = text == null ? "" : text;
			this.state = state;
			this.small = small;
		}

		public Cell( String text, int state ){
			this(text, state, false);
		}
	}

	public static abstract class Block {
	}

	public static final class Heading extends Block {
		public final int level;
		public final String text;

		public Heading( int level, String text ){
			this.level = Math.max(1, Math.min(3, level));
			this.text = text;
		}
	}

	public static final class Note extends Block {
		public final NoteKind kind;
		public final String text;

		public Note( NoteKind kind, String text ){
			this.kind = kind;
			this.text = text;
		}
	}

	public static final class Fact {
		public final String label;
		public final Cell value;
		public final String detail;

		public Fact( String label, Cell value, String detail ){
			this.label = label;
			this.value = value;
			this.detail = detail == null ? "" : detail;
		}
	}

	//labelled values, one per line ("Won: 12 (40%)"). A fact with a null value is a sub-heading inside the list.
	public static final class Facts extends Block {
		public final Provenance provenance;
		public final List<Fact> facts = new ArrayList<>();

		public Facts( Provenance provenance ){
			this.provenance = provenance;
		}

		public Facts add( String label, Cell value, String detail ){
			facts.add(new Fact(label, value, detail));
			return this;
		}

		public Facts section( String label ){
			facts.add(new Fact(label, null, ""));
			return this;
		}
	}

	public static final class Table extends Block {
		public final String title;
		public final Provenance provenance;
		public final List<String> headers = new ArrayList<>();
		//"17/28" known out of cells that could be looked at, "" when it does not apply (parallel to headers)
		public final List<String> coverage = new ArrayList<>();
		public final List<Cell[]> rows = new ArrayList<>();
		//rows that exist but are not listed (a cap on very large tables)
		public int omitted;

		public Table( String title, Provenance provenance ){
			this.title = title;
			this.provenance = provenance;
		}
	}

	public final String title;
	//what the report is about, in a line: scope, rules, when
	public final List<String> about = new ArrayList<>();
	public final List<Block> blocks = new ArrayList<>();
	public final long generatedAt;
	public final String appVersion;

	public Report( String title, long generatedAt, String appVersion ){
		this.title = title;
		this.generatedAt = generatedAt;
		this.appVersion = appVersion == null ? "" : appVersion;
	}

	public Report add( Block b ){
		blocks.add(b);
		return this;
	}

	//built from synthetic demo/sample data: every format says so, so it can never be mistaken for real play history
	public boolean synthetic;

	//true when anything in the report is canonical (a replay of a seed) rather than recorded history
	public boolean hasCanonical(){
		for (Block b : blocks){
			if (b instanceof Facts && ((Facts) b).provenance != Provenance.RECORDED) return true;
			if (b instanceof Table && ((Table) b).provenance != Provenance.RECORDED) return true;
		}
		return false;
	}

	//the first table (what CSV exports), null when there is none
	public Table firstTable(){
		for (Block b : blocks) if (b instanceof Table) return (Table) b;
		return null;
	}

	//which cell states appear anywhere, for the legend
	public boolean[] statesUsed(){
		boolean[] used = new boolean[6];
		for (Block b : blocks){
			if (b instanceof Facts){
				for (Fact f : ((Facts) b).facts) if (f.value != null) mark(used, f.value);
			} else if (b instanceof Table){
				for (Cell[] row : ((Table) b).rows) for (Cell c : row) mark(used, c);
			}
		}
		return used;
	}

	private static void mark( boolean[] used, Cell c ){
		if (c != null && c.state >= 0 && c.state < used.length) used[c.state] = true;
	}
}
