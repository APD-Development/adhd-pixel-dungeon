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

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateways;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.AccountActions;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.BackendClient;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.EvidenceFlow;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.EvidenceInbox;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.PrivacyReport;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.PrivacySummary;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.SessionStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.VoteActions;
import com.watabou.noosa.Game;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Aggregate;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.GroupBy;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Question;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryEngine;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.SavedViews;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;

import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Entry;

import java.util.ArrayList;
import java.util.List;

//APD: the options of a UIMSD screen, in sections: what you can see (hidden information), which runs count (rules, unfinished runs, imported records), how the
//table is built (grouping, smallest group), and how to read it (what is known, what the marks mean), plus sharing. A switch shows whether it is on; the line under
//it says what it does and how many entries it is leaving out right now. Tapping an option applies it and reopens this list with the new state.
public final class WndUimsdOptions {

	private WndUimsdOptions(){}

	private static String left( int n ){
		return n > 0 ? "  Leaving out " + n + " now." : "";
	}

	public static void show( final UimsdModel m ){
		final QueryResult r = m.result();
		if (r == null) return;
		final Query q = m.query();
		final Scope s = q.scope;
		final List<Entry> entries = new ArrayList<>();
		final List<Runnable> actions = new ArrayList<>();

		entries.add(Entry.heading("What you can see"));
		entries.add(Entry.item("Show hidden information", "True identities of items and similar. Only for runs that are finished, never for a run still going.").checked(q.showInternal));
		actions.add(new Runnable(){ public void run(){ m.setShowInternal(!q.showInternal); show(m); } });

		entries.add(Entry.heading("Which runs count"));
		String rules = s.rules == Scope.Rules.ALL ? "All rules (mixed)" : s.rules == Scope.Rules.SPECIFIC ? "One set of rules" : "Your current rules";
		entries.add(Entry.item("Rules: " + rules,
				"Rules = difficulty, challenges and exotic-item setting. This changes which rules count, not whether a run is finished. Use Include runs still going below for unfinished runs. Runs played under different rules are only counted together if you choose all rules." + left(r.stats.otherRulesets)));
		actions.add(new Runnable(){ public void run(){ pickRuleset(m); } });
		if (s.finishedOnly){
			entries.add(Entry.note("This view includes finished runs only."));
		} else {
			entries.add(Entry.item("Include runs still going", "Only what you could see during play is used for them; their hidden information never is." + left(r.stats.unfinished))
					.checked(s.includeUnfinished));
			actions.add(new Runnable(){ public void run(){ m.setScope(s.withUnfinished(!s.includeUnfinished)); show(m); } });
		}
		entries.add(Entry.item("Include runs with unknown rules", "Older runs that never recorded their rules." + left(r.stats.unknownRuleset)).checked(s.includeUnknownRuleset));
		actions.add(new Runnable(){ public void run(){ m.setScope(s.withUnknownRuleset(!s.includeUnknownRuleset)); show(m); } });
		entries.add(Entry.item("Include runs whose rules changed", "Runs where a setting was changed while playing." + left(r.stats.mixedRuleset)).checked(s.includeMixedRuleset));
		actions.add(new Runnable(){ public void run(){ m.setScope(s.withMixedRuleset(!s.includeMixedRuleset)); show(m); } });
		entries.add(Entry.item("Include repeated ranking records", "Imported ranking records of a run that is also recorded; normally counted once." + left(r.stats.duplicates)).checked(s.includeDuplicates));
		actions.add(new Runnable(){ public void run(){ m.setScope(s.withDuplicates(!s.includeDuplicates)); show(m); } });

		entries.add(Entry.heading("How the table is built"));
		entries.add(Entry.item(q.group == null ? "Group by..." : "Grouping: change or remove...",
				"Count entries with the same value together, like runs per class."));
		actions.add(new Runnable(){ public void run(){ pickGroup(m); } });
		entries.add(Entry.item("Arrange the columns...", "Choose the order of the columns you show. Which columns show is in Columns."));
		actions.add(new Runnable(){ public void run(){ WndUimsdColumns.arrange(m); } });
		entries.add(Entry.item("Smallest group shown: " + (q.minSample <= 1 ? "any size" : q.minSample + " or more"),
				"Small groups give shaky averages, so you can hide them. They are counted, not dropped."));
		actions.add(new Runnable(){ public void run(){ pickMinSample(m); } });

		entries.add(Entry.heading("Understanding this table"));
        entries.add(Entry.item("Research lab", "Run DNA, luck laboratory, automatic discoveries and population comparisons, each with inspectable evidence."));
        actions.add(() -> WndUimsdResearch.show(m));
		entries.add(Entry.item("Charts", "Compare this table's groups as bars, with the same filters, values and uncertainty marks."));
		actions.add(() -> WndUimsdChart.show(m));
		entries.add(Entry.item("How much is known, per column", "How many values were recorded exactly, only partly, or not at all."));
		actions.add(new Runnable(){ public void run(){ coverage(m); } });
		entries.add(Entry.item("What the marks mean", "?  >=  ~  and hidden, and the colours."));
		actions.add(new Runnable(){ public void run(){ legend(); } });

		entries.add(Entry.heading("Saved views"));
		entries.add(Entry.item("Save this table as a view...", "Keeps the table exactly as it is now (filters, sorting, columns, grouping, rules) under a name. It saves the question, not the numbers."));
		actions.add(new Runnable(){ public void run(){ saveView(m); } });
		entries.add(Entry.item("Open a saved view...", "Asks one of your saved questions again, of your history as it is now."));
		actions.add(new Runnable(){ public void run(){ openViews(m); } });

		entries.add(Entry.heading("Privacy"));
		entries.add(Entry.item("What stays on this device", "What APD holds, and that nothing is sent anywhere."));
		actions.add(new Runnable(){ public void run(){ privacy(m); } });

		final java.io.File realRoot = ApdRecorder.historyRoot();
		if (realRoot != null && DemoData.isSyntheticRoot(DemoData.demoRootFor(realRoot))){
			entries.add(Entry.heading("Demo data"));
			entries.add(Entry.item(DemoData.viewingDemo() ? "Showing: demo data (made up)" : "Showing: your own history", "Tap to switch. Demo data is made up and never mixed with your own history, ranked or sent anywhere."));
			actions.add(new Runnable(){ public void run(){ DemoData.viewDemo(!DemoData.viewingDemo()); UimsdScene.reopen(); } });
			entries.add(Entry.item("Delete all demo data...", "Removes only the made-up runs. Your own history and saves are not touched, and UIMSD stays unlocked."));
			actions.add(new Runnable(){ public void run(){ deleteDemoData(realRoot); } });
		}

		entries.add(Entry.heading("Share and reset"));
		entries.add(Entry.item("Share a report of this table...", "As text, or saved as a file."));
		actions.add(new Runnable(){ public void run(){ WndUimsdReport.ofView(m); } });
		if (WndUimsdReport.isSeedView(m)){
			entries.add(Entry.item("Share the seed floors report...", "Recorded and canonical floors, kept apart."));
			actions.add(new Runnable(){ public void run(){ WndUimsdReport.ofSeedFloors(m, WndUimsdReport.seedRunId(m)); } });
		}
		entries.add(Entry.item("Reset this table", "Back to how it opened: no filters, sorting or grouping you added."));
		actions.add(new Runnable(){ public void run(){ m.resetView(); } });

		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Options", "", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				actions.get(i).run();
			}
		}).restoreWith(() -> show(m)));
	}

	//---- saved views ----

	private static SavedViews views( UimsdModel m ){
		return m.root() == null ? null : SavedViews.load(SavedViews.fileIn(m.root()));
	}

	private static void say( String text ){
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", text, false));
	}

	private static void saveView( final UimsdModel m ){
		if (m.root() == null){
			say("There is no history folder to keep views in yet.");
			return;
		}
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdTextInput("Save this view", "A name for this table (up to " + SavedViews.MAX_NAME + " characters). An existing view with the same name is replaced.",
				"", SavedViews.MAX_NAME, false, "Save", "Cancel", (positive, text) -> {
				if (!positive) return;
				SavedViews v = views(m);
				SavedViews.Result r = v.save(text, m.query(), System.currentTimeMillis());
				try {
					if (r == SavedViews.Result.SAVED || r == SavedViews.Result.REPLACED) v.store(SavedViews.fileIn(m.root()));
				} catch (java.io.IOException e){
					say("The view could not be saved: " + e.getMessage());
					return;
				}
				say(r == SavedViews.Result.SAVED ? "Saved." : r == SavedViews.Result.REPLACED ? "Saved, replacing the view with that name."
						: r == SavedViews.Result.TOO_MANY ? "You have " + SavedViews.MAX_VIEWS + " saved views. Delete one first." : "That is not a usable name.");
			}));
	}

	private static void openViews( final UimsdModel m ){
		final SavedViews v = views(m);
		if (v == null || v.list().isEmpty()){
			say(v != null && v.skippedOnLoad > 0 ? SavedViews.LOAD_WARNING
					: "You have no saved views yet. Use \"Save this table as a view...\" to make one.");
			return;
		}
		final List<SavedViews.View> list = v.list();
		final List<Entry> entries = new ArrayList<>();
		for (SavedViews.View x : list) entries.add(Entry.item(x.name, x.query() == null ? "This build cannot read this view." : "Tap to open, then choose to open or delete it."));
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Saved views", v.skippedOnLoad > 0 ? SavedViews.LOAD_WARNING : "", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				viewActions(m, list.get(i));
			}
		}).restoreWith(() -> openViews(m)));
	}

	private static void deleteDemoData( final java.io.File realRoot ){
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdChoices("Delete all demo data?", "This removes the made-up demo runs. Your own history and your saves are not touched, and UIMSD stays unlocked.", "Delete the demo data", "Keep it"){
			@Override
			protected void onSelect( int index ){
				if (index != 0) return;
				boolean gone = UimsdUnlock.deleteDemoData(realRoot);
				DemoData.viewDemo(false);
				if (gone) UimsdScene.reopen();
				else say("The demo data could not be removed.");
			}
		}.restoreWith(() -> deleteDemoData(realRoot)));
	}

	private static void privacy( final UimsdModel m ){
		final List<Entry> entries = new ArrayList<>();
		entries.add(Entry.item("What stays on this device", "What APD holds, and what is queued."));
		entries.add(Entry.item("Sending to a server: " + (SPDSettings.apdSending() ? "on" : "off"), BackendClient.status(SPDSettings.apdSending(), SPDSettings.apdServer())));
		entries.add(Entry.item("Server address...", "Where queued runs go when you press Send. Must start with https://."));
		entries.add(Entry.item("Send queued runs now", "Sends only what you queued, only when you press this."));
		final SessionStore.Session session = new SessionStore(m.root()).current(System.currentTimeMillis());
		EvidenceInbox.prune(m.root(), System.currentTimeMillis());
		final int asked = EvidenceInbox.pending(m.root());
		entries.add(Entry.item("Evidence requests (" + asked + ")", asked == 0 ? "Nothing is waiting for your answer." : "A server asked to see the full log of a run you submitted. Nothing is sent unless you agree."));
		entries.add(Entry.item("Share what is queued and sent", "A readable copy of everything APD holds for sending: each queued item exactly as it would be sent, what was sent, and any requests. Makes no connection."));
		entries.add(Entry.item(session == null ? "Account: signed out" : "Account: signed in", session == null ? "There is no sign-in method in this version, so nothing can be sent to an account yet."
				: "Tap to sign out. Signing out never touches your history or saves."));
		entries.add(Entry.item("Ask for a copy of my account data", session == null ? "Needs an account; none can be made in this version." : "Queues a request. It is sent the next time you press Send."));
		entries.add(Entry.item("Delete my account", session == null ? "Needs an account; none can be made in this version." : "Queues a request to delete your account on the server. Your history and saves here are not touched."));
		final int open = VoteActions.offers(m.root()).size();
		entries.add(Entry.item("Community votes (" + open + ")", open == 0 ? "No vote is open. Votes the server opens appear here after you press Send." : "Votes the server has opened. Needs an account to cast."));
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Privacy", "", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				if (i == 0){
					privacySummary(m);
				} else if (i == 1){
					SPDSettings.apdSending(!SPDSettings.apdSending());
					say("Sending is now " + (SPDSettings.apdSending() ? "on. Nothing is sent until you press \"Send queued runs now\"." : "off. Nothing leaves this device."));
				} else if (i == 2){
					serverAddress();
				} else if (i == 4){
					evidenceRequests(m);
				} else if (i == 5){
					WndUimsdReport.show(m, "A readable copy of everything APD holds for sending.", new WndUimsdReport.Source(){
						@Override
						public com.shatteredpixel.shatteredpixeldungeon.apd.report.Report build(){
							return PrivacyReport.build(m.root(), SPDSettings.apdSending(), System.currentTimeMillis(), Game.version);
						}
					});
				} else if (i == 9){
					communityVotes(m);
				} else if (i == 7 || i == 8){
					accountRequest(m, i == 7 ? com.shatteredpixel.shatteredpixeldungeon.apd.backend.AccountRequests.Kind.EXPORT : com.shatteredpixel.shatteredpixeldungeon.apd.backend.AccountRequests.Kind.DELETE);
				} else if (i == 6){
					if (session == null){
						say("There is no sign-in method in this version, so you cannot sign in yet.");
					} else {
						new SessionStore(m.root()).signOut();
						say("Signed out. Your history and saves were not touched.");
					}
				} else {
					BackendClient.sendNow(m.root(), SPDSettings.apdSending(), SPDSettings.apdServer(), new BackendClient.Done(){
						@Override
						public void finished( final String message ){
							Game.runOnRenderThread(new com.watabou.utils.Callback(){
								@Override
								public void call(){
									say(message);
								}
							});
						}
					});
				}
			}
		}).restoreWith(() -> privacy(m)));
	}

	private static void evidenceRequests( final UimsdModel m ){
		evidenceRequests(m, new ArrayList<>(EvidenceInbox.list(m.root())));
	}

	private static void evidenceRequests( final UimsdModel m, final List<EvidenceInbox.Entry> list ){
		if (list.isEmpty()){
			say("Nothing is waiting for your answer.");
			return;
		}
		List<Entry> entries = new ArrayList<>();
		for (EvidenceInbox.Entry e : list) entries.add(Entry.item("Run " + e.asked.runId, "Asked for: " + (e.asked.request.kind == com.shatteredpixel.shatteredpixeldungeon.apd.backend.Evidence.Kind.FULL_RUN_LOG ? "the full run log" : "other data (not supported)")));
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Evidence requests", "Tap one to decide.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				answerRequest(m, list.get(i));
			}
		}).restoreWith(() -> evidenceRequests(m, list)));
	}

	private static void answerRequest( final UimsdModel m, final EvidenceInbox.Entry entry ){
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdChoices("Share this run's full log?",
				"A server asked to check the run you submitted (" + entry.asked.runId + "). Agreeing queues its complete recorded log for sending the next time you press Send. Declining sends nothing.",
				"Agree", "Decline", "Decide later"){
			@Override
			protected void onSelect( int index ){
				if (index == 2) return;
				EvidenceFlow.Result r = EvidenceFlow.answer(m.root(), entry.asked, index == 0, System.currentTimeMillis());
				String msg;
				switch (r){
					case QUEUED: msg = "Queued. It is sent the next time you press \"Send queued runs now\"."; break;
					case DECLINED: msg = "Declined. Nothing will be sent."; break;
					case EXPIRED: msg = "That request has expired."; break;
					case LOG_CHANGED: msg = "The log on this device no longer matches what was submitted, so it was not sent."; break;
					case NOT_SUBMITTED: msg = "This device did not submit that run, so there is nothing to send."; break;
					case UNSUPPORTED: msg = "That kind of data is not supported. Nothing was sent."; break;
					case TOO_LARGE: msg = "The log is too large to send. Nothing was sent."; break;
					default: msg = "It could not be answered. Try again.";
				}
				say(msg);
			}
		}.restoreWith(() -> answerRequest(m, entry)));
	}

	private static void communityVotes( final UimsdModel m ){
		communityVotes(m, new ArrayList<>(VoteActions.offers(m.root())));
	}

	private static void communityVotes( final UimsdModel m,
			final List<com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages.Offer> offers ){
		if (offers.isEmpty()){
			say("No vote is open. Votes the server opens appear here after you press Send.");
			return;
		}
		List<Entry> entries = new ArrayList<>();
		for (com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages.Offer o : offers){
			entries.add(Entry.item(o.title.isEmpty() ? "Vote " + o.round.id.value : o.title, "Pick up to " + o.round.maxChoices + " of " + o.round.options.size() + "."));
		}
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Community votes", "Tap a vote to choose.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				castVote(m, offers.get(i));
			}
		}).restoreWith(() -> communityVotes(m, offers)));
	}

	private static void castVote( final UimsdModel m, final com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages.Offer offer ){
		castVote(m, offer, new java.util.TreeSet<>());
	}

	private static void castVote( final UimsdModel m,
			final com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages.Offer offer,
			final java.util.Set<String> chosen ){
		final List<String> options = new ArrayList<>(offer.round.options);
		List<Entry> entries = new ArrayList<>();
		for (String o : options) entries.add(Entry.item(o).checked(chosen.contains(o)));
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdList(offer.title.isEmpty() ? "Vote" : offer.title,
				"Tick up to " + offer.round.maxChoices + ", then tap Done. Nothing is sent until you press Send. Back leaves without voting.", true, entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				if (checked) chosen.add(options.get(i)); else chosen.remove(options.get(i));
			}
		}){
			@Override
			protected void onDone(){
				super.onDone();
				String msg;
				switch (VoteActions.cast(m.root(), offer.round.id, chosen, System.currentTimeMillis())){
					case QUEUED: msg = "Your choice is queued. Nothing has been sent yet."; break;
					case NOT_SIGNED_IN: msg = "You are not signed in, so you cannot vote yet."; break;
					case NOT_OPEN: msg = "This vote has not opened yet."; break;
					case CLOSED: msg = "This vote has closed."; break;
					case TOO_MANY_CHOICES: msg = "You picked too many. Pick up to " + offer.round.maxChoices + "."; break;
					case EMPTY: msg = "You did not pick anything."; break;
					default: msg = "Your choice could not be queued.";
				}
				say(msg);
			}
		}.restoreWith(() -> castVote(m, offer, new java.util.TreeSet<>(chosen))));
	}

	private static void accountRequest( final UimsdModel m, final com.shatteredpixel.shatteredpixeldungeon.apd.backend.AccountRequests.Kind kind ){
		final boolean delete = kind == com.shatteredpixel.shatteredpixeldungeon.apd.backend.AccountRequests.Kind.DELETE;
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdChoices(delete ? "Delete your account?" : "Ask for a copy of your account data?",
				delete ? "This queues a request to delete your account and what the server holds about it. It is sent the next time you press Send. Your history and saves on this device stay as they are."
						: "This queues a request for a copy of what the server holds about your account. It is sent the next time you press Send.",
				delete ? "Queue the deletion" : "Queue the request", "Cancel"){
			@Override
			protected void onSelect( int index ){
				if (index != 0) return;
				switch (AccountActions.request(m.root(), kind, System.currentTimeMillis())){
					case QUEUED: say("Queued. Nothing has been sent yet."); break;
					case ALREADY_QUEUED: say("That request is already queued."); break;
					case NOT_SIGNED_IN: say("You are not signed in, so there is no account to ask about."); break;
					default: say("The request could not be queued.");
				}
			}
		}.restoreWith(() -> accountRequest(m, kind)));
	}

	private static void serverAddress(){
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdTextInput("Server address", "An address starting with https://. Leave empty to clear it.", SPDSettings.apdServer(), 200, false, "Save", "Cancel", (positive, text) -> {
				if (!positive) return;
				String t = text == null ? "" : text.trim();
				if (!t.isEmpty() && Gateways.cleanBaseUrl(t) == null){
					say("That is not a usable address. It must start with https://.");
					return;
				}
				SPDSettings.apdServer(t);
				say(t.isEmpty() ? "Address cleared." : "Saved.");
			}));
	}

	private static void privacySummary( UimsdModel m ){
		StringBuilder sb = new StringBuilder();
		for (String line : PrivacySummary.of(m.root()).lines(SPDSettings.apdSending())) sb.append(line).append("\n\n");
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", sb.toString().trim(), false));
	}

	private static void viewActions( final UimsdModel m, final SavedViews.View view ){
		final List<Entry> entries = new ArrayList<>();
		if (view.question() != null)
			entries.add(Entry.item("Open question", "Restores the exact question-builder choices and asks it of your history now."));
		entries.add(Entry.item(view.question() == null ? "Open" : "Open as table", "Shows this view now."));
		entries.add(Entry.item("Delete", "Removes this saved view. Your history is not touched."));
		showViewActions(view.name, entries, m, view);
	}

	private static void showViewActions( String title, List<Entry> entries, final UimsdModel m, final SavedViews.View view ){
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu(title, "", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				final Question question = view.question();
				if (question != null && i == 0){
					if (m.catalog() == null || m.catalog().dataset(question.dataset) == null){
						say("This question cannot be opened: what it asks about is not available in this version.");
						return;
					}
					WndUimsdAsk.builder(m.catalog(), m.currentRuleset(), new WndUimsdAsk.Host(){
						@Override public void openAsTable( Query q, Runnable backToAnswer ){ m.showQuery(q); }
						@Override public java.io.File root(){ return m.root(); }
					}, question);
					return;
				}
				if ((question == null && i == 0) || (question != null && i == 1)){
					Query q = view.query();
					if (q == null || m.catalog() == null || m.catalog().view(q.dataset) == null){
						say("This view cannot be opened: the table it uses is not available in this version.");
						return;
					}
					m.showQuery(q);
					return;
				}
				SavedViews v = views(m);
				v.delete(view.name);
				try {
					v.store(SavedViews.fileIn(m.root()));
				} catch (java.io.IOException e){
					say("The view could not be deleted: " + e.getMessage());
				}
			}
		}).restoreWith(() -> viewActions(m, view)));
	}

	private static void pickRuleset( final UimsdModel m ){
		final Catalog cat = m.catalog();
		final Scope now = m.query().scope;
		final List<String> ids = new ArrayList<>();
		final List<Entry> entries = new ArrayList<>();
		ids.add("*current");
		entries.add(Entry.item("Your current rules", "The rules a new run would be played with now, or the run you are in.").current(now.rules == Scope.Rules.CURRENT));
		ids.add("*all");
		entries.add(Entry.item("All rules, mixed together", "Every run, whatever it was played with. The numbers then mix different rules and are not directly comparable.")
				.current(now.rules == Scope.Rules.ALL));
		if (cat != null && !cat.rulesets.isEmpty()){
			entries.add(Entry.heading("Or one set of rules"));
			for (Catalog.RulesetInfo ri : cat.rulesets){
				ids.add(ri.id);
				entries.add(Entry.item(ri.label, ri.runs + (ri.runs == 1 ? " run" : " runs")).current(now.rules == Scope.Rules.SPECIFIC && ri.id.equals(now.ruleset)));
			}
		}
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Rules",
				"Rules are the difficulty, challenges and exotic-item setting a run was played with. Runs with different rules are kept apart so they are never compared by accident.",
				entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				Scope s = m.query().scope;
				String id = ids.get(i);
				if (id.equals("*current")) m.setScope(s.withRules(Scope.Rules.CURRENT, ""));
				else if (id.equals("*all")) m.setScope(s.withRules(Scope.Rules.ALL, ""));
				else m.setScope(s.withRules(Scope.Rules.SPECIFIC, id));
				show(m);
			}
		}).restoreWith(() -> pickRuleset(m)));
	}

	private static void pickMinSample( final UimsdModel m ){
		final int[] sizes = { 0, 3, 5, 10, 20, 50 };
		List<Entry> entries = new ArrayList<>();
		int now = m.query().minSample;
		for (int n : sizes){
			entries.add(Entry.item(n == 0 ? "Show every group" : n + " or more", n == 0 ? "even a group of one" : "hide groups with fewer than " + n).current(n == now || (n == 0 && now <= 1)));
		}
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Smallest group shown",
				"A group is a set of entries with the same value. Hidden groups are counted in the notes, not lost.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				m.setMinSample(sizes[i]);
				show(m);
			}
		}).restoreWith(() -> pickMinSample(m)));
	}

	private static void pickGroup( final UimsdModel m ){
		final List<ColumnDef> cols = new ArrayList<>();
		List<Entry> entries = new ArrayList<>();
		entries.add(Entry.item("No grouping", "show every entry on its own line").current(m.query().group == null));
		String last = null;
		for (ColumnDef c : m.offeredColumns()){
			String g = c.group.isEmpty() ? "Details" : c.group.equals("Hidden") ? "Hidden information (finished runs only)" : c.group;
			if (!g.equals(last)){
				entries.add(Entry.heading(g));
				last = g;
			}
			cols.add(c);
			boolean on = m.query().group != null && m.query().group.keys.contains(c.id);
			entries.add(Entry.item(c.label, "").current(on));
		}
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Group by",
				"Entries with the same value are counted together, with their totals and averages.", entries,
				new WndUimsdList.Picker(){
					@Override
					public void onPick( int i, boolean checked ){
						if (i == 0){
							m.setGroup(null);
						} else {
							ColumnDef key = cols.get(i - 1);
							List<Aggregate> aggs = new ArrayList<>();
							aggs.add(Aggregate.count());
							int numeric = 0;
							for (ColumnDef c : m.offeredColumns()){
								if (c != key && c.type == ColumnDef.Type.NUM && c.defaultVisible && numeric < 3){
									aggs.add(Aggregate.of(Aggregate.Fn.AVG, c.id));
									numeric++;
								}
							}
							List<String> keys = new ArrayList<>();
							keys.add(key.id);
							m.setGroup(new GroupBy(keys, aggs));
						}
						show(m);
					}
				}).restoreWith(() -> pickGroup(m)));
	}

	private static void coverage( UimsdModel m ){
		if (m.dataset() == null) return;
		List<String> ids = new ArrayList<>();
		for (ColumnDef c : m.offeredColumns()) ids.add(c.id);
		QueryResult r = QueryEngine.run(m.dataset(), m.query().withColumns(ids).withGroup(null), m.currentRuleset());
		StringBuilder b = new StringBuilder();
		b.append("This table has ").append(r.stats.matching).append(r.stats.matching == 1 ? " entry" : " entries").append(" at the moment. For each column, how many of them have a recorded value:\n\n");
		for (QueryResult.ColumnCoverage c : r.coverage){
			b.append(c.column.label).append(": ").append(c.known).append(" of ").append(c.total).append(" recorded");
			if (c.lowerBound > 0) b.append(", ").append(c.lowerBound).append(" only partly (at least)");
			if (c.inferred > 0) b.append(", ").append(c.inferred).append(" worked out, not recorded");
			if (c.hidden > 0) b.append(", ").append(c.hidden).append(" hidden");
			b.append('\n');
		}
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("How much is known", b.toString()));
	}

	private static void legend(){
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("What the marks mean",
				"# How sure a value is\n"
						+ "[exact] 12   exact: recorded exactly.\n"
						+ "[lower] >=12   at least 12: the real number may be higher, because the log has gaps or an older recording could not count everything.\n"
						+ "[inferred] ~6   worked out, not recorded exactly: for example healing taken from changes in your health between turns, or a floor rebuilt from the seed.\n"
						+ "[unknown] ?   unknown: it was never recorded. This is not the same as zero.\n"
						+ "[hidden] hidden   not available: the run is not finished yet, or hidden information is off.\n"
						+ "[legacy] Blue   comes from an imported ranking record.\n"
						+ "# Totals and averages\n"
						+ "They only use values that are known, and say how many that was. A total that includes an \"at least\" value is itself an \"at least\"."));
	}
}

