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
import java.util.Collections;
import java.util.List;

//The topics of UIMSD's Guide: plain explanations in sections, each optionally tied to a step of the interactive tour so a player can read it
//or be shown it on the real screen. Pure data. The text uses a little markup (see the Guide window): "# " starts a heading, "- " a bullet, "! " a
//warning, and a line can start with a value colour tag such as [exact], [lower], [inferred], [unknown], [hidden] or [legacy].
//The marks explained here are the ones CellFormatter really draws.
public final class UimsdGuide {

	private UimsdGuide(){}

	public static final class Topic {
		public final String id;
		public final String section;
		public final String title;
		public final String text;
		//the tour step that shows this on the real screen, or ""
		public final String tourStep;

		Topic( String id, String section, String title, String text, String tourStep ){
			this.id = id;
			this.section = section;
			this.title = title;
			this.text = text;
			this.tourStep = tourStep;
		}
	}

	private static final List<Topic> TOPICS;
	static {
		List<Topic> t = new ArrayList<>();
		String start = "Start here", read = "Reading the tables", find = "Finding things", trust = "Trusting the numbers", share = "Sharing and seed floors", words = "Words";
		t.add(new Topic("what", start, "What UIMSD is",
				"UIMSD is the game's statistics system. APD records every run you play, and UIMSD lets you look through that record: how each class does, "
						+ "which gear you rely on, what ends your runs, how difficulty changes your odds.\n\n"
						+ "You do not need to know anything about databases. Start on the Home page, which shows the numbers most players want, and tap the buttons on its cards to go deeper.\n\n"
						+ "UIMSD only reads what was recorded. It never changes your game.", "welcome"));
		t.add(new Topic("home", start, "The Home page",
				"Home sums up your finished runs in a few cards: how many you played and won, how each character does, how each difficulty does, the gear you use, "
						+ "what you meet and what ended your runs, your recent runs and your records.\n\n"
						+ "Each card has buttons that open the full table behind it. The Home button on the other screens brings you back here. "
						+ "The Report button turns this page into something you can share.\n\n"
						+ "Home shows runs played under your own rules by default. \"Rules: all (mixed)\" adds runs played under other rules, and says so, because "
						+ "numbers from different rules are not directly comparable.", "home"));
		t.add(new Topic("ask", start, "Asking a question",
				"The Ask a question card on Home is the easy way in. A question is a sentence with blanks: how many, or the average, total, highest, lowest or median of a number; "
						+ "of what; per floor, per run, per turn or per hour you played; split by class, spell, floor and so on; only where some condition is true.\n\n"
						+ "# What it can answer\n"
						+ "APD writes down what happens while you play: every step, rest and search, every item you use, every effect that starts or ends, every talent, spell and "
						+ "ability, every change in your gold and every trade, as well as the fights, healing and floors. If it was recorded, you can ask about it, including questions "
						+ "nobody thought of when the game was made.\n\n"
						+ "# Per floor, per run, per hour\n"
						+ "When you divide by what you did, every floor, run, turn or hour counts, including the quiet ones where nothing happened, so averages are fair.\n\n"
						+ "# Going further\n"
						+ "Open as a table gives the very same question in the table tools, with every filter, sort and column, for anyone who wants to keep digging. "
						+ "Things you could not have known during a run (what an unidentified item really was) are shown only once the run is over and only if you switch hidden "
						+ "details on.", ""));
		t.add(new Topic("datasets", read, "Tables",
				"UIMSD keeps four kinds of record: Runs, Items, Enemies and Deaths. A table is one way of looking at a record: Weapons and Armor are tables of Items, "
						+ "Characters is a table of Runs.\n\n"
						+ "The button at the top (it shows the name of the table you are in) opens the list of all tables, grouped by what they are about. Under it, a line says what the table shows.", "view"));
		t.add(new Topic("rows", read, "Lines, kinds and groups",
				"A table has one line for each thing it lists. In tables like Weapons, each line is one KIND of thing, for example every War Hammer together, "
						+ "with numbers about all of them: how many you met, how often you equipped them, how often you won with them.\n\n"
						+ "Tap a column heading to sort by it. Tap a line to open it.", "table"));
		t.add(new Topic("research", read, "Research lab",
                "Open Research lab on Home, inside a table, or through Options. Run DNA describes a run using explicitly defined rates and can compare two fingerprints. Tap a metric to inspect the source run and its recorded context events.\n\n"
                        + "Luck laboratory compares recorded generation values with matching finished random-seed runs. Generation details require your explicit hidden-information choice and never expose unfinished-run secrets. Empirical ranks describe your reference history, not theoretical RNG odds or a score multiplier.\n\n"
                        + "Automatic discoveries screen recorded class/subclass win-rate contrasts with at least 20 known outcomes per group, matching rules and version, and adjusted uncertainty bands. No result is invented when data are insufficient. Inspect both groups; association does not establish a cause.\n\n"
                        + "For a custom investigation, apply table filters and capture population A, change the filters and capture B, then choose a numeric metric or run win rate. Pins save the question separately from your saved views; values are recalculated. Grouping and paging are not population filters. Unknown and uncertain values are excluded explicitly, and overlapping or unmatched samples suppress inference. Every analysis can be exported with its definitions and evidence.", ""));
        t.add(new Topic("charts", read, "Charts",
                "In a grouped table, open Options > Charts. Choose the count or one of the table's totals or averages. "
                        + "Bars share a zero baseline, including negative values. Unknown values have no bar. The same filters, rules, group order and uncertainty marks apply.\n\n"
                        + "Tap a group for its details and links to the matching entries. At most 24 groups are shown; extra groups remain in the table. "
                        + "For an ungrouped table, choose Options > Group by first.", ""));
		t.add(new Topic("drill", read, "Opening a line: going deeper",
				"Tapping a line opens a window with everything known about it, value by value, with the colour and mark of how sure each one is.\n\n"
						+ "From there you can go one level down: to the individual items of that kind, or to the runs they appeared in. Each level down is a new table you can sort and filter too. "
						+ "The line under the table's name says where you are.", "open"));
		t.add(new Topic("back", read, "Back and Home",
				"Back goes up one level, to where you were before you opened a line. From the top of a table, Back takes you to Home, and from Home it leaves UIMSD.\n\n"
						+ "The Home button jumps straight to the Home page.", "back"));
		t.add(new Topic("columns", read, "Columns",
				"Tap Columns to choose which columns the table shows. Tables offer many more columns than fit on the screen, grouped by topic. "
						+ "The first column stays in place while the rest scroll sideways; a small mark at the edge shows that more columns wait that way. "
						+ "Your choice only changes what you see, never the data.", "columns"));
		t.add(new Topic("sort", read, "Sorting",
				"Tap a column heading to sort by it. Tap again to reverse, and again to go back to the original order. "
						+ "Lines where the value is unknown (?) always go to the end, so unknown never looks like zero.\n\n"
						+ "The Sort button lets you sort by up to three columns, so ties are broken by the next one.", "sort"));
        t.add(new Topic("search", find, "Search all statistics",
                "Tap Search UIMSD on Home or inside any table. Type the name of a mechanic, item type, enemy, number or category, "
                        + "and UIMSD finds matching tables and public statistic fields. Tap a result to open that table with normal filters and sorting.\n\n"
                        + "Search works offline, including Unicode text and a few common terms across languages. It does not use an AI service, "
                        + "so it does not understand arbitrary questions in every language yet. For custom calculations, use Ask a question.\n\n"
                        + "Search does not read secret row values or expose unidentified items; incomplete recordings still show unknown data as unknown.", ""));
		t.add(new Topic("filters", find, "Filters",
				"A filter keeps only the lines that match a rule, for example \"Class is Mage\" or \"Deepest is at least 5\".\n\n"
						+ "Tap Filter and follow three steps: choose what to filter on, choose how to compare (is, is not, is at least, is between, is one of...), "
						+ "then choose a value. Where the possible values are known, you pick them from a list: the classes you have played, Won or Died, the difficulty levels. "
						+ "Numbers, dates and free text are typed.\n\n"
						+ "Each filter appears as a button above the table. Tap it to change or remove it.", "filter"));
		t.add(new Topic("stacked", find, "Several filters at once",
				"Filters stack: a line has to pass ALL of them to stay. Add a second filter and the table gets smaller again. Remove a filter to widen the table.\n\n"
						+ "Lines where a filter's column was never recorded are left out, not counted as a mismatch. The Notes button says how many each filter left out.", "filter2"));
		t.add(new Topic("options", find, "Options",
				"Options holds everything that is not a filter:\n"
						+ "- Show hidden information, for finished runs.\n"
						+ "- Which runs count: your own rules or all, runs still going, runs with unknown or changed rules.\n"
						+ "- Grouping and the smallest group shown.\n"
						+ "- How much is known in each column, and what the marks mean.\n"
						+ "- Sharing a report of the table, and resetting it.", ""));
		t.add(new Topic("marks", trust, "The marks: ?, >=, ~ and hidden",
				"Every value says how sure it is, by its mark and its colour:\n"
						+ "[exact] 12   exact: recorded exactly.\n"
						+ "[lower] >=12   at least: the real number could be higher, because the recording has gaps or an older version of APD could not count it.\n"
						+ "[inferred] ~6   worked out: not recorded exactly, for example healing taken from changes in your health, or a floor rebuilt from a seed.\n"
						+ "[unknown] ?   unknown: it was never recorded. It is not zero.\n"
						+ "[hidden] hidden   not available: it belongs to a run you are still playing, or hidden information is off.\n"
						+ "[legacy] Blue   comes from an imported ranking record.", "marks"));
		t.add(new Topic("sample", trust, "Small samples",
				"A number based on very few values can be misleading: a 100% win rate from 2 runs means little. Numbers resting on fewer than 5 values are shown dimmed. "
						+ "Small groups are left out of grouped tables unless you ask for them in Options.", "sample"));
		t.add(new Topic("coverage", trust, "How much is known",
				"Runs recorded by older versions of APD lack some details, so some cells show ?. Under each column heading a small count such as 17/28 says how many of the lines have a recorded value.\n\n"
						+ "The Notes button says what you are looking at, which runs are included, what was left out and why. Lines marked as tappable include what was left out. "
						+ "Options > How much is known lists every column.", "coverage"));
		t.add(new Topic("difficulty", trust, "Rules and difficulty",
				"The rules of a run are its difficulty (1 to 5, 3 is the normal game), its challenges and its exotic-item setting. Runs played under different rules are kept apart, "
						+ "so you never compare an easy run with a hard one by accident.\n\n"
						+ "Tables show your own rules by default and the notes say how many runs used other rules. If you choose all rules, the table says MIXED RULES and the numbers are "
						+ "not directly comparable; use the Difficulty and Challenges tables, or filter by Difficulty, to compare like with like.\n\n"
						+ "Runs recorded before difficulty existed show a ? for it: they were played with the normal balance, but their difficulty was never recorded.", ""));
		t.add(new Topic("reports", share, "Sharing: reports",
				"A report is a readable summary you can send to someone or keep: your overall numbers, the table you are looking at, one run, a group (a character, an item kind, an enemy), "
						+ "two runs or two groups side by side, or the seed floors of a run.\n\n"
						+ "Find it under Options (Share a report of this table), on Home (Report), and in the window of a run or a group. Share it as text, or save it as Markdown, "
						+ "a web page (open it in a browser and print it to PDF) or CSV for a spreadsheet.\n\n"
						+ "Reports keep the marks and a legend, and say clearly which data is canonical. They never include hidden information of a run you are still playing; "
						+ "hidden information of finished runs is included only if you switched it on, and then the report says so.", "reports"));
		t.add(new Topic("seedfloors", share, "Seed floors and canonical data",
				"Every dungeon grows from a seed. For a finished run, open its window and choose Seed floors to see what every floor of its seed holds, including floors the run never reached.\n\n"
						+ "Floors the run actually played show what the run recorded: that is history. All other floors are CANONICAL: they are what the seed gives when its floors are built in order "
						+ "under fixed rules. That is not what the run would have met, because play changes later floors (items used, loot taken, monsters killed). "
						+ "Canonical lines carry the ~ mark and a Basis column, and are never mixed up with recorded ones.\n\n"
						+ "Seed floors need a finished run and are not available while a game is open.", ""));
		t.add(new Topic("glossary", words, "Words used in UIMSD",
				"# Run\nOne game, from the first floor to its end.\n"
						+ "# Finished and still going\nA finished run has ended (won, died or abandoned). Hidden information of a run still going is never shown.\n"
						+ "# Line (row)\nOne line of a table: one run, one kind of item, one meeting with an enemy.\n"
						+ "# Kind and individual\nA kind is every War Hammer together. An individual item is one particular War Hammer in one run.\n"
						+ "# Recorded\nThe game wrote it down while you played. Exact unless marked.\n"
						+ "# Worked out (inferred, ~)\nCalculated from other facts instead of being recorded directly.\n"
						+ "# Canonical\nRebuilt from a seed under fixed rules. It shows what the seed holds, not what you met. Always marked ~.\n"
						+ "# Unknown (?)\nNever recorded. Not the same as zero.\n"
						+ "# At least (>=)\nA lower bound: the real value may be higher.\n"
						+ "# Hidden\nNot available: the run is not finished, or hidden information is off.\n"
						+ "# Rules\nDifficulty, challenges and exotic-item setting. Runs with different rules are kept apart.\n"
						+ "# Known (coverage)\nHow many of the values a number rests on were actually recorded.\n"
						+ "# Group\nLines with the same value counted together, like runs per class.", ""));
		TOPICS = Collections.unmodifiableList(t);
	}

	public static List<Topic> topics(){
		return TOPICS;
	}

	public static Topic topic( String id ){
		for (Topic t : TOPICS) if (t.id.equals(id)) return t;
		return null;
	}
}

