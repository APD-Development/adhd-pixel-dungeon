package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;

import java.util.List;
import java.util.Locale;

//Turns a Report into text: Markdown, plain text, a self-contained web page, or CSV. Pure and deterministic (the same Report gives the same
//bytes). The marks stay in the values themselves (?, >=, ~, hidden); each renderer adds the legend for the marks that appear, and a
//prominent canonical notice whenever any part of the report is a replay of a seed rather than recorded history.
public final class ReportRenderer {

	private ReportRenderer(){}

	public static final String SYNTHETIC_NOTICE = "SYNTHETIC DEMO DATA: made up for testing. This is NOT real play history.";

	public static final String CANONICAL_NOTICE = "CANONICAL DATA: entries marked canonical are what the run's seed gives in an in-order replay under APD's "
			+ "reference rules. They are NOT recorded history and NOT what this run would have met: play changes what later floors hold. "
			+ "Rows marked recorded come from the run's own log and are authoritative.";

	private static String when( long millis ){
		//yyyy-MM-dd HH:mm UTC without java.time
		long days = Math.floorDiv(millis, 86400000L);
		long rem = Math.floorMod(millis, 86400000L);
		long z = days + 719468;
		long era = Math.floorDiv(z, 146097);
		long doe = z - era * 146097;
		long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
		long y = yoe + era * 400;
		long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
		long mp = (5 * doy + 2) / 153;
		long d = doy - (153 * mp + 2) / 5 + 1;
		long m = mp < 10 ? mp + 3 : mp - 9;
		if (m <= 2) y++;
		long h = rem / 3600000L, min = (rem / 60000L) % 60;
		return y + "-" + two(m) + "-" + two(d) + " " + two(h) + ":" + two(min) + " UTC";
	}

	private static String two( long n ){
		return n < 10 ? "0" + n : Long.toString(n);
	}

	private static String provLabel( Report.Provenance p ){
		switch (p){
			case CANONICAL: return "CANONICAL (seed replay, not recorded history)";
			case MIXED: return "both RECORDED and CANONICAL entries: see the Basis column";
			default: return "RECORDED";
		}
	}

	//the marks that appear, as (mark, meaning) pairs
	static String[][] legend( boolean[] used ){
		String[][] all = {
				{ "", "recorded exactly" },
				{ ">=", "at least this much: the log has gaps or an older recording could not count all of it" },
				{ "~", "inferred or canonical: worked out, not recorded exactly" },
				{ "legacy", "taken from an imported ranking record" },
				{ "?", "unknown: never recorded (not zero)" },
				{ "hidden", "hidden information of a run that is still going" } };
		java.util.List<String[]> out = new java.util.ArrayList<>();
		for (int s = 1; s < all.length; s++){
			int state = s == 3 ? CellText.LEGACY : s == 4 ? CellText.UNKNOWN : s == 5 ? CellText.HIDDEN : s;
			if (state < used.length && used[state]) out.add(all[s]);
		}
		return out.toArray(new String[0][]);
	}

	private static String footer( Report r ){
		return "Made by ADHD Pixel Dungeon UIMSD" + (r.appVersion.isEmpty() ? "" : " " + r.appVersion) + " on " + when(r.generatedAt)
				+ ". Figures are what this device recorded; they are not verified and not a ranking.";
	}

	// A row may also omit trailing positions. The export still needs a field for each named column.
	// A sparse report table may contain a null cell; render it as blank without inventing an unknown statistic.
	private static String displayText( Report.Cell c ){
		return c == null ? "" : c.text;
	}

	//---- Markdown ----

	private static String md( String s ){
		// Encode paired emphasis markers from data, but keep ordinary uncertainty
		// marks (~6), single asterisks and underscores intact for older reports.
		String escaped = s.replace("\\", "\\\\").replace("|", "\\|").replace("<", "&lt;")
				.replace("`", "&#96;").replace("[", "&#91;").replace("]", "&#93;");
		if (pairedMarker(s, '*')) escaped = escaped.replace("*", "&#42;");
		if (pairedMarker(s, '_')) escaped = escaped.replace("_", "&#95;");
		if (s.contains("~~")) escaped = escaped.replace("~", "&#126;");
		return escaped.replace("\r\n", " ").replace("\r", " ").replace("\n", " ");
	}

	private static boolean pairedMarker( String s, char mark ){
		int first = s.indexOf(mark);
		return first >= 0 && s.indexOf(mark, first + 1) >= 0;
	}

	// Notes intentionally retain existing inline Markdown emphasis (see prior tests).
	// They remain one blockquote; literal HTML, links and code ticks stay inert.
	private static String quotedNoteLines( String s ){
		return String.valueOf(s).replace("<", "&lt;").replace("`", "&#96;").replace("[", "&#91;").replace("]", "&#93;")
				.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\n> ");
	}

		public static String markdown( Report r ){
		StringBuilder sb = new StringBuilder();
		sb.append("# ").append(md(r.title)).append("\n\n");
		if (r.synthetic) sb.append("> **").append(SYNTHETIC_NOTICE).append("**\n\n");
		for (String a : r.about) sb.append(md(a)).append("  \n");
		if (!r.about.isEmpty()) sb.append("\n");
		if (r.hasCanonical()) sb.append("> **").append(CANONICAL_NOTICE).append("**\n\n");
		for (Report.Block b : r.blocks){
			if (b instanceof Report.Heading){
				Report.Heading h = (Report.Heading) b;
				sb.append("\n");
				for (int i = 0; i < h.level + 1; i++) sb.append('#');
				sb.append(' ').append(md(h.text)).append("\n\n");
			} else if (b instanceof Report.Note){
				Report.Note n = (Report.Note) b;
				sb.append("> ").append(n.kind == Report.NoteKind.WARN ? "**Warning:** " : n.kind == Report.NoteKind.NOTE ? "Note: " : "").append(quotedNoteLines(n.text)).append("\n\n");
			} else if (b instanceof Report.Facts){
				Report.Facts f = (Report.Facts) b;
				if (f.provenance != Report.Provenance.RECORDED) sb.append("_").append(provLabel(f.provenance)).append("_\n\n");
				for (Report.Fact x : f.facts){
					if (x.value == null){
						sb.append("\n**").append(md(x.label)).append("**\n\n");
						continue;
					}
					sb.append("- ").append(md(x.label)).append(": ").append(md(x.value.text));
					if (!x.detail.isEmpty()) sb.append(" (").append(md(x.detail)).append(")");
					sb.append("\n");
				}
				sb.append("\n");
			} else if (b instanceof Report.Table){
				Report.Table t = (Report.Table) b;
				if (!t.title.isEmpty()) sb.append("**").append(md(t.title)).append("**  \n");
				sb.append("_").append(provLabel(t.provenance)).append("_\n\n");
				sb.append("|");
				for (String h : t.headers) sb.append(' ').append(md(h)).append(" |");
				sb.append("\n|");
				for (int i = 0; i < t.headers.size(); i++) sb.append(" --- |");
				sb.append("\n");
				for (Report.Cell[] row : t.rows){
					sb.append("|");
					for (int i = 0; i < Math.max(row.length, t.headers.size()); i++){
						Report.Cell c = i < row.length ? row[i] : null;
						sb.append(' ').append(md(displayText(c))).append(c != null && c.small ? " (small sample)" : "").append(" |");
					}
					sb.append("\n");
				}
				if (t.omitted > 0) sb.append("\n_").append(t.omitted).append(" more entries are not listed._\n");
				StringBuilder cov = new StringBuilder();
				// Orphan coverage values have no column label; avoid failing the whole export.
				for (int i = 0; i < t.coverage.size() && i < t.headers.size(); i++){
					if (!t.coverage.get(i).isEmpty()) cov.append(cov.length() > 0 ? "; " : "").append(md(t.headers.get(i))).append(" ").append(md(t.coverage.get(i)));
				}
				if (cov.length() > 0) sb.append("\n_Known out of listed (per column): ").append(cov).append("._\n");
				sb.append("\n");
			}
		}
		appendLegend(sb, r, "\n---\n\n**Marks:**\n\n", "- ", ": ", "\n");
		sb.append("\n---\n").append(md(footer(r))).append("\n");
		return sb.toString();
	}

	private static void appendLegend( StringBuilder sb, Report r, String head, String itemStart, String sep, String itemEnd ){
		String[][] legend = legend(r.statesUsed());
		if (legend.length == 0) return;
		sb.append(head);
		for (String[] l : legend) sb.append(itemStart).append(l[0]).append(sep).append(l[1]).append(itemEnd);
	}

	//---- plain text ----

	// Version metadata belongs to the footer, never to an extra report line.
	private static String textFooter( Report r ){
		return footer(r).replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
	}

	public static String text( Report r ){
		StringBuilder sb = new StringBuilder();
		if (r.synthetic) sb.append(SYNTHETIC_NOTICE).append("\n\n");
		sb.append(r.title.toUpperCase(Locale.ROOT)).append("\n");
		for (int i = 0; i < r.title.length(); i++) sb.append('=');
		sb.append("\n");
		for (String a : r.about) sb.append(a).append("\n");
		sb.append("\n");
		if (r.hasCanonical()) sb.append(CANONICAL_NOTICE).append("\n\n");
		for (Report.Block b : r.blocks){
			if (b instanceof Report.Heading){
				Report.Heading h = (Report.Heading) b;
				sb.append("\n").append(h.level == 1 ? h.text.toUpperCase(Locale.ROOT) : h.text).append("\n");
			} else if (b instanceof Report.Note){
				Report.Note n = (Report.Note) b;
				sb.append(n.kind == Report.NoteKind.WARN ? "WARNING: " : n.kind == Report.NoteKind.NOTE ? "Note: " : "").append(n.text).append("\n");
			} else if (b instanceof Report.Facts){
				Report.Facts f = (Report.Facts) b;
				if (f.provenance != Report.Provenance.RECORDED) sb.append("[").append(provLabel(f.provenance)).append("]\n");
				for (Report.Fact x : f.facts){
					if (x.value == null) sb.append("\n").append(x.label).append(":\n");
					else sb.append("  ").append(x.label).append(": ").append(x.value.text).append(x.detail.isEmpty() ? "" : " (" + x.detail + ")").append("\n");
				}
			} else if (b instanceof Report.Table){
				Report.Table t = (Report.Table) b;
				sb.append("\n").append(t.title.isEmpty() ? "Table" : t.title).append("  [").append(provLabel(t.provenance)).append("]\n");
				for (Report.Cell[] row : t.rows){
					sb.append("  ");
					for (int i = 0; i < Math.max(row.length, t.headers.size()); i++){
						if (i > 0) sb.append(" | ");
						if (i < t.headers.size() && !t.headers.get(i).isEmpty()) sb.append(t.headers.get(i)).append(' ');
						sb.append(displayText(i < row.length ? row[i] : null));
					}
					sb.append("\n");
				}
				if (t.omitted > 0) sb.append("  (").append(t.omitted).append(" more entries are not listed)\n");
			}
		}
		appendLegend(sb, r, "\nMARKS\n", "  ", " = ", "\n");
		sb.append("\n").append(textFooter(r)).append("\n");
		return sb.toString();
	}

	//---- HTML ----

	static String esc( String s ){
		StringBuilder sb = new StringBuilder(s.length() + 8);
		for (int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			switch (c){
				case '&': sb.append("&amp;"); break;
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '"': sb.append("&quot;"); break;
				default: sb.append(c);
			}
		}
		return sb.toString();
	}

	private static String cls( Report.Cell c ){
		if (c == null) return "exact";
		String k;
		switch (c.state){
			case CellText.LOWER: k = "lower"; break;
			case CellText.INFERRED: k = "inferred"; break;
			case CellText.LEGACY: k = "legacy"; break;
			case CellText.UNKNOWN: k = "unknown"; break;
			case CellText.HIDDEN: k = "hidden"; break;
			default: k = "exact";
		}
		return k + (c.small ? " small" : "");
	}

	public static String html( Report r ){
		StringBuilder sb = new StringBuilder();
		sb.append("<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		sb.append("<title>").append(esc(r.title)).append("</title>\n<style>\n")
				.append("body{font:15px/1.45 system-ui,sans-serif;max-width:60em;margin:1em auto;padding:0 1em;color:#1b1b1b;background:#fff}\n")
				.append("h1{font-size:1.5em}h2{font-size:1.25em;margin-top:1.5em}h3{font-size:1.1em}\n")
				.append("table{border-collapse:collapse;margin:.5em 0;width:100%}th,td{border:1px solid #bbb;padding:.25em .5em;text-align:left;vertical-align:top}th{background:#eee}\n")
				.append(".canonical{border:3px solid #b35900;background:#fff3e0;padding:.6em;font-weight:bold}\n")
				.append(".prov{font-style:italic;color:#555}.prov.canon{color:#b35900;font-weight:bold}\n")
				.append(".note{background:#f1f5f9;border-left:4px solid #64748b;padding:.4em .6em;margin:.5em 0}.warn{background:#fff7ed;border-left-color:#c2410c}\n")
				.append(".lower{font-weight:bold}.inferred{font-style:italic;color:#8a4b00}.unknown{color:#777}.hidden{color:#777;background:#eee}.legacy{color:#2563eb}.small{opacity:.65}\n")
				.append("footer{margin-top:2em;color:#555;font-size:.9em}\n@media print{body{max-width:none}}\n</style></head><body>\n");
		sb.append("<h1>").append(esc(r.title)).append("</h1>\n");
		if (r.synthetic) sb.append("<p class=\"warn\"><strong>").append(esc(SYNTHETIC_NOTICE)).append("</strong></p>\n");
		for (String a : r.about) sb.append("<div>").append(esc(a)).append("</div>\n");
		if (r.hasCanonical()) sb.append("<p class=\"canonical\">").append(esc(CANONICAL_NOTICE)).append("</p>\n");
		for (Report.Block b : r.blocks){
			if (b instanceof Report.Heading){
				Report.Heading h = (Report.Heading) b;
				sb.append("<h").append(h.level + 1).append(">").append(esc(h.text)).append("</h").append(h.level + 1).append(">\n");
			} else if (b instanceof Report.Note){
				Report.Note n = (Report.Note) b;
				sb.append("<p class=\"note").append(n.kind == Report.NoteKind.WARN ? " warn" : "").append("\">").append(esc(n.text)).append("</p>\n");
			} else if (b instanceof Report.Facts){
				Report.Facts f = (Report.Facts) b;
				if (f.provenance != Report.Provenance.RECORDED) sb.append("<p class=\"prov canon\">").append(esc(provLabel(f.provenance))).append("</p>\n");
				sb.append("<table>\n");
				for (Report.Fact x : f.facts){
					if (x.value == null) sb.append("<tr><th colspan=\"2\">").append(esc(x.label)).append("</th></tr>\n");
					else sb.append("<tr><td>").append(esc(x.label)).append("</td><td class=\"").append(cls(x.value)).append("\">").append(esc(x.value.text))
							.append(x.detail.isEmpty() ? "" : " <span class=\"prov\">(" + esc(x.detail) + ")</span>").append("</td></tr>\n");
				}
				sb.append("</table>\n");
			} else if (b instanceof Report.Table){
				Report.Table t = (Report.Table) b;
				if (!t.title.isEmpty()) sb.append("<h3>").append(esc(t.title)).append("</h3>\n");
				sb.append("<p class=\"prov").append(t.provenance == Report.Provenance.RECORDED ? "" : " canon").append("\">").append(esc(provLabel(t.provenance))).append("</p>\n");
				sb.append("<table>\n<tr>");
				for (int i = 0; i < t.headers.size(); i++){
					sb.append("<th>").append(esc(t.headers.get(i)));
					if (i < t.coverage.size() && !t.coverage.get(i).isEmpty()) sb.append("<br><small>known ").append(esc(t.coverage.get(i))).append("</small>");
					sb.append("</th>");
				}
				sb.append("</tr>\n");
				for (Report.Cell[] row : t.rows){
					sb.append("<tr>");
					for (int i = 0; i < Math.max(row.length, t.headers.size()); i++){
						Report.Cell c = i < row.length ? row[i] : null;
						sb.append("<td class=\"").append(cls(c)).append("\">").append(esc(displayText(c))).append("</td>");
					}
					sb.append("</tr>\n");
				}
				sb.append("</table>\n");
				if (t.omitted > 0) sb.append("<p class=\"prov\">").append(t.omitted).append(" more entries are not listed.</p>\n");
			}
		}
		String[][] legend = legend(r.statesUsed());
		if (legend.length > 0){
			sb.append("<h2>Marks</h2>\n<ul>\n");
			for (String[] l : legend) sb.append("<li><b>").append(esc(l[0])).append("</b> ").append(esc(l[1])).append("</li>\n");
			sb.append("</ul>\n");
		}
		sb.append("<footer>").append(esc(footer(r))).append("</footer>\n</body></html>\n");
		return sb.toString();
	}

	//---- CSV (tables only) ----

	static String csv( String s ){
		boolean quote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
		//a cell must never be read by a spreadsheet as a formula, including when a spreadsheet trims leading whitespace
		if (startsFormula(s)){
			s = "'" + s;
		}
		return quote ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
	}

	private static boolean startsFormula( String s ){
		int first = 0;
		while (first < s.length() && Character.isWhitespace(s.charAt(first))) first++;
		return first < s.length() && "=+-@".indexOf(s.charAt(first)) >= 0 && !looksNumeric(s.trim());
	}

	private static boolean looksNumeric( String s ){
		try {
			Double.parseDouble(s.replace(",", ""));
			return true;
		} catch (NumberFormatException e){
			return false;
		}
	}

	//Every table in turn: a "# title [provenance]" line, the header row, the rows. Marks stay in the cell text, and a column after each value
	//column would double the file, so the state words are in the legend of the other formats and the marks are in the data.
	//CSV comments are not quoted fields. Keep metadata on one physical line even when a
	//saved report title or version contains line breaks; ordinary data cells stay untouched.
	private static String csvComment( String value ){
		return value.replace('\r', ' ').replace('\n', ' ');
	}

	public static String csv( Report r ){
		StringBuilder sb = new StringBuilder();
		boolean any = false;
		if (r.synthetic) sb.append("# ").append(SYNTHETIC_NOTICE).append("\r\n");
		if (r.hasCanonical()) sb.append("# ").append(csvComment(CANONICAL_NOTICE)).append("\r\n");
		for (Report.Block b : r.blocks){
			if (b instanceof Report.Facts){
				Report.Facts facts = (Report.Facts) b;
				if (any) sb.append("\r\n");
				any = true;
				sb.append("# ").append(csvComment(r.title)).append(" facts [").append(provLabel(facts.provenance)).append("]\r\n");
				sb.append("Section,Field,Value,Detail\r\n");
				String section = "";
				for (Report.Fact fact : facts.facts){
					if (fact.value == null){ section = fact.label; continue; }
					sb.append(csv(section)).append(',').append(csv(fact.label)).append(',')
							.append(csv(fact.value.text)).append(',').append(csv(fact.detail)).append("\r\n");
				}
				continue;
			}
			if (!(b instanceof Report.Table)) continue;
			Report.Table t = (Report.Table) b;
			if (any) sb.append("\r\n");
			any = true;
			sb.append("# ").append(csvComment(t.title.isEmpty() ? r.title : t.title)).append(" [").append(provLabel(t.provenance)).append("]\r\n");
			for (int i = 0; i < t.headers.size(); i++) sb.append(i > 0 ? "," : "").append(csv(t.headers.get(i)));
			sb.append("\r\n");
			for (Report.Cell[] row : t.rows){
				for (int i = 0; i < Math.max(row.length, t.headers.size()); i++) sb.append(i > 0 ? "," : "").append(csv(displayText(i < row.length ? row[i] : null)));
				sb.append("\r\n");
			}
			if (t.omitted > 0) sb.append("# ").append(t.omitted).append(" more entries are not listed.\r\n");
		}
		if (!any) sb.append("# ").append(csvComment(r.title)).append(" has no table\r\n");
		sb.append("# marks: ? unknown, >=N at least N, ~N inferred or canonical, hidden = hidden information. ").append(csvComment(footer(r))).append("\r\n");
		return sb.toString();
	}
}
