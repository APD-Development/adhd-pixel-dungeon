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

package com.shatteredpixel.shatteredpixeldungeon.apd.community;

import com.badlogic.gdx.utils.JsonValue;
import com.shatteredpixel.shatteredpixeldungeon.apd.age.CommunityAgeGate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

// The live Community screens as plain data: every screen is a title, a message and rows, and every tappable row carries
// the action it runs. CommunityScene only draws them. Keeping the logic here means it can be tested without a phone.
public final class CommunityController {

	// ---- what the UI must provide ----

	public interface Ui {
		void show(Screen screen);
		// Ask for one line (or several) of text. answer gets null when the player cancels.
		void ask(String title, String body, String initial, int maxLength, boolean multiLine, Consumer<String> answer);
		void busy(String message);
		void leave();
		// The read-only pages about what's planned (run reviews, mods, events...). Returning from them restarts Community.
		void planned();
	}

	// Runs a network call off the render thread, then hands the result back on it. Tests run both at once.
	public interface Runner {
		void run(Supplier<CommunityApi.Result> call, Consumer<CommunityApi.Result> then);
	}

	// Where the session token and the age-gate answer live on the device.
	public interface Storage extends CommunityAgeGate.Store {
		String token();
		void token(String token);
	}

	// Google sign-in on the platform (Android Credential Manager). Calls done once with a Google ID token, or null
	// when the player cancels or Google sign-in isn't available. Null source = no Google button.
	public interface GoogleSource {
		void request(Consumer<String> done);
	}

	public static volatile GoogleSource google = null;

	public static final class Row {
		public final String label, detail;
		public final int kind; // 0 heading, 1 note, 2 item, 3 warning
		public final Runnable action;
		public final Boolean checked;
		Row(int kind, String label, String detail, Runnable action, Boolean checked){
			this.kind = kind; this.label = label; this.detail = detail == null ? "" : detail; this.action = action; this.checked = checked;
		}
	}

	public static final class Screen {
		public final String title, message;
		public final List<Row> rows = new ArrayList<>();
		public Runnable back;
		Screen(String title, String message){ this.title = title; this.message = message; }
		Screen heading(String t){ rows.add(new Row(0, t, null, null, null)); return this; }
		Screen note(String t){ rows.add(new Row(1, t, null, null, null)); return this; }
		Screen warning(String t){ rows.add(new Row(3, t, null, null, null)); return this; }
		Screen item(String label, String detail, Runnable action){ rows.add(new Row(2, label, detail, action, null)); return this; }
		Screen check(String label, String detail, boolean on, Runnable action){ rows.add(new Row(2, label, detail, action, on)); return this; }
		public List<Row> items(){
			List<Row> out = new ArrayList<>();
			for (Row r : rows) if (r.kind == 2) out.add(r);
			return out;
		}
	}

	private final CommunityApi api;
	private final Ui ui;
	private final Runner runner;
	private final Storage storage;
	private final int[] today; // {month 1-12, year}
	private JsonValue me;
	private JsonValue season;

	public CommunityController(CommunityApi api, Ui ui, Runner runner, Storage storage, int[] today){
		this.api = api; this.ui = ui; this.runner = runner; this.storage = storage; this.today = today;
		if (api != null && storage.token() != null) api.token(storage.token());
	}

	public static int[] now(){
		Calendar c = Calendar.getInstance();
		return new int[]{ c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR) };
	}

	// ---- entry: age gate, then account, then the hub ----

	public void start(){
		if (api == null){ offline(); return; }
		ui.busy("Checking Community access...");
		CommunityAgeGate.askPlay(play -> gateThen(CommunityAgeGate.check(play, storage)));
	}

	// Shows the locked or birth-date screen, or goes on to sign-in. (Never shows a screen after starting sign-in:
	// with a fast answer that would cover the screen sign-in just showed.)
	private void gateThen(CommunityAgeGate.Result r){
		if (r == CommunityAgeGate.Result.LOCKED) ui.show(locked());
		else if (r == CommunityAgeGate.Result.ASK_BIRTH_DATE) ui.show(askBirth());
		else signedInOrOut();
	}

	private Screen offline(){
		Screen s = new Screen("Community", "The Community server isn't live yet. Here's what's coming.");
		s.back = ui::leave;
		ui.show(s);
		return s;
	}

	private Screen locked(){
		Screen s = new Screen("Community", "Community is for players 16 and older.");
		s.note("The rest of ADHD Pixel Dungeon is yours to play. Community's submissions, comments and votes are only open to players 16 and up.");
		s.item("Back", null, ui::leave);
		s.back = ui::leave;
		return s;
	}

	private Screen askBirth(){
		Screen s = new Screen("Community", "Community is for players 16 and older.");
		s.note("Enter your birth month and year. Only whether you're old enough is kept on this phone, never the date, and you can only answer once.");
		s.item("Enter birth month and year", null, this::askMonth);
		s.item("Not now", null, ui::leave);
		s.back = ui::leave;
		return s;
	}

	private void askMonth(){
		ui.ask("Birth month", "Month you were born, as a number (1-12).", "", 2, false, m -> {
			Integer month = number(m, 1, 12);
			if (month == null){ if (m != null) ui.show(askBirth().warning("That isn't a month from 1 to 12.")); else ui.show(askBirth()); return; }
			ui.ask("Birth year", "Year you were born (for example 2004).", "", 4, false, y -> {
				Integer year = number(y, today[1] - 120, today[1]);
				if (year == null){ if (y != null) ui.show(askBirth().warning("That isn't a real birth year.")); else ui.show(askBirth()); return; }
				gateThen(CommunityAgeGate.answer(month, year, today[0], today[1], storage));
			});
		});
	}

	private static Integer number(String s, int min, int max){
		if (s == null) return null;
		try { int v = Integer.parseInt(s.trim()); return v < min || v > max ? null : v; } catch (NumberFormatException e){ return null; }
	}

	private void signedInOrOut(){
		if (!api.signedIn()){ ui.show(signedOut(null)); return; }
		call(api::me, r -> {
			if (r.status == 0 || r.status >= 500){ ui.show(cantReach()); return; } // keep the sign-in: it's the connection, not the account
			if (!r.ok()){ forget(); ui.show(signedOut(message(r))); return; }
			me = r.body;
			if (!me.getBoolean("ageOk", false)) call(api::ageGatePassed, x -> hub());
			else hub();
		});
	}

	private Screen cantReach(){
		Screen s = new Screen("Community", null);
		s.warning("Can't reach the Community server. Check your connection and try again. You're still signed in.");
		s.item("Try again", null, this::signedInOrOut);
		s.item("Back", null, ui::leave);
		s.back = ui::leave;
		return s;
	}

	// ---- signed out ----

	private Screen signedOut(String problem){
		Screen s = new Screen("Community", "Sign in to post ideas, comment and vote.");
		if (problem != null) s.warning(problem);
		if (google != null) s.item("Sign in with Google", null, this::signInGoogle);
		s.item("Sign in with email", null, this::signIn);
		s.item("Create an account", null, this::register);
		s.item("I have a verification code", null, () -> askEmailThen(this::askCode));
		s.item("Back", null, ui::leave);
		s.back = ui::leave;
		return s;
	}

	private void signInGoogle(){
		ui.busy("Waiting for Google...");
		google.request(idToken -> {
			if (idToken == null){ ui.show(signedOut(null)); return; }
			call(() -> api.google(idToken), r -> {
				if (r.ok()){ remember(); signedInOrOut(); }
				else ui.show(signedOut(message(r)));
			});
		});
	}

	private void askEmailThen(Consumer<String> next){
		ui.ask("Email", "Your email address.", "", 254, false, e -> { if (e == null) ui.show(signedOut(null)); else next.accept(e.trim()); });
	}

	private void signIn(){
		askEmailThen(email -> ui.ask("Password", "Your Community password.", "", 200, false, pw -> {
			if (pw == null){ ui.show(signedOut(null)); return; }
			call(() -> api.login(email, pw), r -> {
				if (r.ok()){ remember(); signedInOrOut(); }
				else if ("email_not_verified".equals(r.error)) askCode(email);
				else ui.show(signedOut(message(r)));
			});
		}));
	}

	private void register(){
		askEmailThen(email -> ui.ask("Password", "Pick a password, at least 8 characters.", "", 200, false, pw -> {
			if (pw == null){ ui.show(signedOut(null)); return; }
			ui.ask("Name", "The name other players see (up to 24 characters).", "", 24, false, name -> {
				if (name == null){ ui.show(signedOut(null)); return; }
				call(() -> api.register(email, pw, name), r -> {
					if (r.ok()) askCode(email);
					else ui.show(signedOut(message(r)));
				});
			});
		}));
	}

	private void askCode(String email){
		ui.ask("Verification code", "Enter the 6-digit code we emailed to " + email + ". Leave it empty to get a new code.", "", 6, false, code -> {
			if (code == null){ ui.show(signedOut(null)); return; }
			if (code.trim().isEmpty()){ call(() -> api.resendCode(email), r -> askCode(email)); return; }
			call(() -> api.verify(email, code.trim()), r -> {
				if (r.ok()){ remember(); signedInOrOut(); }
				else ui.show(signedOut(message(r)));
			});
		});
	}

	private void remember(){ storage.token(api.token()); }
	private void forget(){ api.token(null); storage.token(null); me = null; }

	// ---- hub ----

	private void hub(){
		call(api::season, r -> {
			season = r.ok() ? r.body.get("season") : null;
			if (season != null && season.isNull()) season = null;
			Screen s = new Screen("Community", "Signed in as " + me.getString("displayName", "?") + ".");
			if (season == null) s.note("No season is running right now.");
			else s.heading(season.getString("title", "Season")).note(phaseText(season.getString("phase", "")));
			s.item("Submissions", "Ideas and their discussions", this::submissions);
			s.item("Voting and results", "Upvotes, the top 16 and the tournament", this::results);
			s.item("Account", "Sign out or delete your account", this::account);
			if (me.getBoolean("admin", false)) s.item("Moderation", "Reports, hide, remove, ban", this::moderation);
			s.item("What's coming next", "Run reviews, mods, events and more", ui::planned);
			s.item("Back", null, ui::leave);
			s.back = ui::leave;
			ui.show(s);
		});
	}

	static String phaseText(String phase){
		switch (phase){
			case "submissions": return "Month 1: submissions are open. Post your ideas and discuss everyone's.";
			case "voting": return "Month 2: voting. Upvote the ideas you want; the top 16 go to the tournament. You can't upvote your own.";
			case "tournament": return "Month 3: the tournament. Week 1 everyone picks up to 8, then it's one-on-one each week.";
			case "finished": return "This season is over. See the results for the winner.";
			default: return "The season hasn't started yet.";
		}
	}

	private String phase(){ return season == null ? "" : season.getString("phase", ""); }

	// ---- submissions ----

	private void submissions(){
		call(api::submissions, r -> {
			Screen s = new Screen("Submissions", null);
			s.back = this::hub;
			if (!r.ok()){ s.warning(message(r)); s.item("Back", null, this::hub); ui.show(s); return; }
			if ("submissions".equals(phase())) s.item("+ Post an idea", null, this::post);
			JsonValue list = r.body.get("submissions");
			if (list == null || list.size == 0) s.note("No ideas yet this season.");
			else for (JsonValue v = list.child; v != null; v = v.next){
				final String id = v.getString("id");
				s.item(v.getString("title", ""), "by " + v.get("author").getString("displayName", "?") + " · " + v.getInt("comments", 0) + " comments", () -> detail(id));
			}
			s.item("Back", null, this::hub);
			ui.show(s);
		});
	}

	private void post(){
		ui.ask("Title", "A short title for your idea.", "", 80, false, title -> {
			if (title == null){ submissions(); return; }
			ui.ask("Your idea", "Describe it. Others can comment and ask you questions.", "", 4000, true, body -> {
				if (body == null){ submissions(); return; }
				call(() -> api.submit(title, body), r -> { if (r.ok()) detail(r.string("id")); else problem(r, this::submissions); });
			});
		});
	}

	private void detail(String id){
		// In Month 2 the results say how many upvotes it has and whether I've upvoted it, so the button can say so.
		if ("voting".equals(phase())){
			call(api::results, rr -> {
				int count = -1; boolean mine = false;
				if (rr.ok()){
					if (rr.body.get("myUpvotes") != null) for (JsonValue v = rr.body.get("myUpvotes").child; v != null; v = v.next) if (id.equals(v.asString())) mine = true;
					if (rr.body.get("standings") != null) for (JsonValue v = rr.body.get("standings").child; v != null; v = v.next) if (id.equals(v.getString("id", ""))) count = v.getInt("upvotes", 0);
				}
				detail(id, count, mine);
			});
		} else detail(id, -1, false);
	}

	private void detail(String id, int upvotes, boolean upvoted){
		call(() -> api.submission(id), r -> {
			if (!r.ok()){ problem(r, this::submissions); return; }
			JsonValue b = r.body;
			String authorId = b.get("author").getString("id");
			boolean mine = me != null && authorId.equals(me.getString("id"));
			Screen s = new Screen(b.getString("title", ""), "by " + b.get("author").getString("displayName", "?"));
			s.back = this::submissions;
			s.note(b.getString("body", ""));
			if ("voting".equals(phase()) && !mine){
				String count = upvotes < 0 ? null : upvotes + (upvotes == 1 ? " upvote" : " upvotes");
				s.item(upvoted ? "Take back my upvote" : "Upvote this idea", count, () ->
						call(() -> api.upvote(id, !upvoted), x -> { if (x.ok()) detail(id); else problem(x, () -> detail(id)); }));
			} else if ("voting".equals(phase()) && upvotes >= 0) s.note(upvotes + (upvotes == 1 ? " upvote" : " upvotes") + " (you can't upvote your own idea)");
			s.item("Comment", null, () -> comment(id, null));
			if (!mine){
				s.item("Report this idea", null, () -> report("submission", id, () -> detail(id)));
				s.item("Block " + b.get("author").getString("displayName", "this player"), "Hide everything they post from you", () -> block(authorId, this::submissions));
			}
			s.heading("Discussion");
			JsonValue comments = b.get("comments");
			if (comments == null || comments.size == 0) s.note("No comments yet.");
			else threaded(s, id, comments);
			s.item("Back", null, this::submissions);
			ui.show(s);
		});
	}

	// Comments in thread order: each reply right under its parent, indented by depth.
	static List<JsonValue> threadOrder(JsonValue comments, List<Integer> depths){
		List<JsonValue> all = new ArrayList<>();
		for (JsonValue c = comments.child; c != null; c = c.next) all.add(c);
		List<JsonValue> out = new ArrayList<>();
		addChildren(all, null, 0, out, depths);
		return out;
	}

	private static void addChildren(List<JsonValue> all, String parent, int depth, List<JsonValue> out, List<Integer> depths){
		for (JsonValue c : all){
			String p = c.get("parentId") == null || c.get("parentId").isNull() ? null : c.getString("parentId");
			if (parent == null ? p != null : !parent.equals(p)) continue;
			out.add(c); depths.add(depth);
			addChildren(all, c.getString("id"), Math.min(depth + 1, 6), out, depths);
		}
	}

	private void threaded(Screen s, String submissionId, JsonValue comments){
		List<Integer> depths = new ArrayList<>();
		List<JsonValue> ordered = threadOrder(comments, depths);
		for (int i = 0; i < ordered.size(); i++){
			JsonValue c = ordered.get(i);
			String indent = "";
			for (int d = 0; d < depths.get(i); d++) indent += "  ";
			final String cid = c.getString("id");
			JsonValue author = c.get("author");
			if (author == null || author.isNull()){
				s.item(indent + (depths.get(i) > 0 ? "> " : "") + "[removed]", null, () -> comment(submissionId, cid));
				continue;
			}
			final String aid = author.getString("id"), name = author.getString("displayName", "?");
			s.item(indent + (depths.get(i) > 0 ? "> " : "") + name + ": " + c.getString("body", ""), null, () -> commentOptions(submissionId, cid, aid, name));
		}
	}

	private void commentOptions(String submissionId, String commentId, String authorId, String name){
		Screen s = new Screen("Comment", "by " + name);
		s.back = () -> detail(submissionId);
		s.item("Reply", null, () -> comment(submissionId, commentId));
		if (me == null || !authorId.equals(me.getString("id"))){
			s.item("Report this comment", null, () -> report("comment", commentId, () -> detail(submissionId)));
			s.item("Block " + name, "Hide everything they post from you", () -> block(authorId, () -> detail(submissionId)));
		}
		s.item("Back", null, () -> detail(submissionId));
		ui.show(s);
	}

	private void comment(String submissionId, String parentId){
		ui.ask(parentId == null ? "Comment" : "Reply", "Be kind. Comments never count as votes.", "", 2000, true, body -> {
			if (body == null || body.trim().isEmpty()){ detail(submissionId); return; }
			call(() -> api.comment(submissionId, parentId, body), r -> { if (r.ok()) detail(submissionId); else problem(r, () -> detail(submissionId)); });
		});
	}

	private void report(String kind, String id, Runnable after){
		ui.ask("Report", "What's wrong with it? (one line)", "", 200, false, reason -> {
			if (reason == null || reason.trim().isEmpty()){ after.run(); return; }
			call(() -> api.report(kind, id, reason), r -> {
				if (r.ok()) done("Reported. Thanks, a moderator will look at it.", after); else problem(r, after);
			});
		});
	}

	private void block(String accountId, Runnable after){
		call(() -> api.block(accountId), r -> { if (r.ok()) done("Blocked. You won't see their posts or comments.", after); else problem(r, after); });
	}

	// ---- voting and the tournament ----

	private final Set<String> picks = new LinkedHashSet<>();

	private void results(){
		call(api::results, this::showResults);
	}

	private CommunityApi.Result lastResults;

	private void showResults(CommunityApi.Result r){
		lastResults = r;
		{
			Screen s = new Screen("Voting and results", null);
			s.back = this::hub;
			if (!r.ok()){ s.warning(message(r)); s.item("Back", null, this::hub); ui.show(s); return; }
			JsonValue b = r.body;
			String phase = b.get("season") == null || b.get("season").isNull() ? "" : b.get("season").getString("phase", "");
			s.note(phaseText(phase));
			if ("voting".equals(phase)){
				s.heading("Upvotes so far");
				Set<String> mine = new LinkedHashSet<>();
				if (b.get("myUpvotes") != null) for (JsonValue v = b.get("myUpvotes").child; v != null; v = v.next) mine.add(v.asString());
				int rank = 1;
				for (JsonValue v = b.get("standings").child; v != null; v = v.next, rank++){
					final String id = v.getString("id");
					s.item(rank + ". " + v.getString("title", ""), v.getInt("upvotes", 0) + " upvotes" + (mine.contains(id) ? " · you upvoted" : ""), () -> detail(id));
				}
			} else if ("tournament".equals(phase) || "finished".equals(phase)){
				int week = b.getInt("week", 1);
				if (b.get("champion") != null) s.heading("Winner: " + b.get("champion").getString("title", ""));
				if ("tournament".equals(phase) && week == 1) weekOne(s, b);
				JsonValue rounds = b.get("rounds");
				if (rounds != null) for (JsonValue round = rounds.child; round != null; round = round.next) matchups(s, round, "tournament".equals(phase) && round.getInt("week") == week);
			}
			s.item("Back", null, this::hub);
			ui.show(s);
		}
	}

	private void weekOne(Screen s, JsonValue b){
		s.heading("Week 1: pick up to 8 (" + picks.size() + " picked)");
		for (JsonValue v = b.get("qualifiers").child; v != null; v = v.next){
			final String id = v.getString("id");
			s.check(v.getString("title", ""), v.getInt("picks", 0) + " picks so far", picks.contains(id), () -> {
				if (picks.contains(id)) picks.remove(id); else if (picks.size() < 8) picks.add(id);
				showResults(lastResults); // toggling is local; only Save talks to the server
			});
		}
		s.item("Save my picks", picks.isEmpty() ? "Pick at least one" : picks.size() + " of 8", () ->
				call(() -> api.picks(new ArrayList<>(picks)), r -> { if (r.ok()) done("Picks saved. You can change them until the week ends.", this::results); else problem(r, this::results); }));
	}

	private void matchups(Screen s, JsonValue round, boolean open){
		s.heading("Week " + round.getInt("week") + (open ? " (voting now)" : ""));
		for (JsonValue m = round.get("matchups").child; m != null; m = m.next){
			final int idx = m.getInt("matchup");
			if (m.getBoolean("bye", false)){
				JsonValue a = m.get("a"), b = m.get("b");
				JsonValue only = a != null && !a.isNull() ? a : b;
				if (only != null && !only.isNull()) s.note(only.getString("title", "") + " goes through (no opponent)");
				continue; // an empty slot (fewer than 8 entries) shows nothing
			}
			JsonValue a = m.get("a"), bb = m.get("b");
			String winner = m.get("winner") == null || m.get("winner").isNull() ? null : m.getString("winner");
			for (JsonValue side : new JsonValue[]{a, bb}){
				final String id = side.getString("id");
				String label = side.getString("title", "") + (id.equals(winner) ? "  (won)" : "");
				String detail = side.getInt("votes", 0) + " votes";
				if (open) s.item("Vote: " + label, detail, () -> call(() -> api.vote(idx, id), r -> { if (r.ok()) results(); else problem(r, this::results); }));
				else s.item(label, detail, () -> detail(id));
			}
		}
	}

	// ---- account ----

	private void account(){
		Screen s = new Screen("Account", "Signed in as " + me.getString("displayName", "?") + ".");
		s.back = this::hub;
		s.item("Blocked players", "See and unblock players you blocked", this::blockedList);
		s.item("Sign out", null, () -> call(api::logout, r -> { forget(); ui.show(signedOut(null)); }));
		s.item("Delete my account", "Removes your account, ideas, comments and votes for good", () ->
				ui.ask("Delete account", "Type DELETE to remove your account and everything you posted. This can't be undone.", "", 10, false, t -> {
					if (t == null || !t.trim().equals("DELETE")){ account(); return; }
					call(api::deleteAccount, r -> { if (r.ok()){ forget(); done("Your account and everything you posted are gone.", () -> ui.show(signedOut(null))); } else problem(r, this::account); });
				}));
		s.item("Back", null, this::hub);
		ui.show(s);
	}

	private void blockedList(){
		call(api::blocked, r -> {
			Screen s = new Screen("Blocked players", "Tap a player to unblock them.");
			s.back = this::account;
			if (!r.ok()){ s.warning(message(r)); s.item("Back", null, this::account); ui.show(s); return; }
			JsonValue list = r.body.get("blocked");
			if (list == null || list.size == 0) s.note("You haven't blocked anyone.");
			else for (JsonValue v = list.child; v != null; v = v.next){
				final String id = v.getString("id"), name = v.getString("displayName", "?");
				s.item(name, "Tap to unblock", () -> call(() -> api.unblock(id), x -> {
					if (x.ok()) done(name + " is unblocked.", this::blockedList); else problem(x, this::blockedList);
				}));
			}
			s.item("Back", null, this::account);
			ui.show(s);
		});
	}

	// ---- moderation (Chris) ----

	private void moderation(){
		call(api::adminReports, r -> {
			Screen s = new Screen("Moderation", "Open reports, most reported first.");
			s.back = this::hub;
			s.item("Start a new season", "Opens submissions now", this::startSeason);
			s.item("Find a player", "Ban or unban anyone by name or email", this::findPlayer);
			if (!r.ok()){ s.warning(message(r)); s.item("Back", null, this::hub); ui.show(s); return; }
			JsonValue list = r.body.get("reports");
			if (list == null || list.size == 0) s.note("No open reports.");
			else for (JsonValue v = list.child; v != null; v = v.next){
				final String kind = v.getString("target_kind"), id = v.getString("target_id");
				final String author = v.getString("author_id", null), preview = v.getString("preview", "(deleted)");
				s.item(v.getInt("reports", 0) + "x " + kind + ": " + (preview == null ? "(deleted)" : preview), v.getString("reasons", ""), () -> moderate(kind, id, author, preview));
			}
			s.item("Back", null, this::hub);
			ui.show(s);
		});
	}

	private void moderate(String kind, String id, String authorId, String preview){
		Screen s = new Screen("Moderate " + kind, preview);
		s.back = this::moderation;
		if (!kind.equals("account")){
			s.item("Hide", "Hidden from everyone; can be restored", () -> call(() -> api.adminSetStatus("hide", kind, id), r -> after(r)));
			s.item("Remove", "Removed for good", () -> call(() -> api.adminSetStatus("remove", kind, id), r -> after(r)));
			s.item("Restore", "Make it visible again", () -> call(() -> api.adminSetStatus("restore", kind, id), r -> after(r)));
		}
		if (authorId != null){
			s.item("Ban the author", "Signs them out and hides everything they posted", () -> call(() -> api.adminBan(authorId, true), r -> after(r)));
			s.item("Unban the author", null, () -> call(() -> api.adminBan(authorId, false), r -> after(r)));
		}
		s.item("Back", null, this::moderation);
		ui.show(s);
	}

	private void findPlayer(){
		ui.ask("Find a player", "Part of their name, or their exact email.", "", 254, false, q -> {
			if (q == null || q.trim().isEmpty()){ moderation(); return; }
			call(() -> api.adminFind(q.trim()), r -> {
				Screen s = new Screen("Players matching \"" + q.trim() + "\"", null);
				s.back = this::moderation;
				if (!r.ok()){ s.warning(message(r)); s.item("Back", null, this::moderation); ui.show(s); return; }
				JsonValue list = r.body.get("accounts");
				if (list == null || list.size == 0) s.note("Nobody found.");
				else for (JsonValue v = list.child; v != null; v = v.next){
					final String id = v.getString("id"), name = v.getString("displayName", "?");
					final boolean banned = v.getBoolean("banned", false);
					String email = v.getString("email", null);
					s.item(name + (banned ? "  (banned)" : ""), (email == null ? "Google account" : email) + " · tap to " + (banned ? "unban" : "ban"),
							() -> call(() -> api.adminBan(id, !banned), x -> {
								if (x.ok()) done(name + (banned ? " is unbanned." : " is banned and signed out."), this::moderation);
								else problem(x, this::moderation);
							}));
				}
				s.item("Back", null, this::moderation);
				ui.show(s);
			});
		});
	}

	// Title, then the three phase lengths in days (30 / 30 / 28 suggested), then confirm.
	private void startSeason(){
		ui.ask("Season title", "What should this season be called?", "", 80, false, title -> {
			if (title == null || title.trim().isEmpty()){ moderation(); return; }
			ui.ask("Submissions (days)", "How many days are submissions open?", "30", 3, false, a -> {
				Integer sub = number(a, 1, 120);
				if (sub == null){ moderation(); return; }
				ui.ask("Voting (days)", "How many days does Month 2 voting run?", "30", 3, false, b -> {
					Integer vote = number(b, 1, 120);
					if (vote == null){ moderation(); return; }
					ui.ask("Tournament (days)", "How many days does the tournament run? (4 weeks = 28)", "28", 3, false, c -> {
						Integer tour = number(c, 4, 120);
						if (tour == null){ moderation(); return; }
						Screen s = new Screen("Start " + title.trim() + "?", "Submissions " + sub + " days, voting " + vote + " days, tournament " + tour + " days. Starts now.");
						s.item("Start it", null, () -> call(() -> api.adminStartSeason(title.trim(), sub, vote, tour), r -> {
							if (r.ok()) done("Season started. Submissions are open.", this::hub);
							else if ("season_overlaps".equals(r.error)) problem(r, this::moderation);
							else problem(r, this::moderation);
						}));
						s.item("Cancel", null, this::moderation);
						s.back = this::moderation;
						ui.show(s);
					});
				});
			});
		});
	}

	private void after(CommunityApi.Result r){ if (r.ok()) moderation(); else problem(r, this::moderation); }

	// ---- helpers ----

	private void call(Supplier<CommunityApi.Result> call, Consumer<CommunityApi.Result> then){
		ui.busy("Talking to the Community server...");
		runner.run(call, r -> {
			if (r.status == 401 && me != null){ forget(); ui.show(signedOut("You were signed out. Please sign in again.")); return; }
			then.accept(r);
		});
	}

	private void done(String text, Runnable after){
		Screen s = new Screen("Community", text);
		s.item("OK", null, after);
		s.back = after;
		ui.show(s);
	}

	private void problem(CommunityApi.Result r, Runnable after){
		Screen s = new Screen("Community", null);
		s.warning(message(r));
		s.item("OK", null, after);
		s.back = after;
		ui.show(s);
	}

	// Plain-language text for every error code the server sends.
	static String message(CommunityApi.Result r){
		if (r.status == 0) return "Can't reach the Community server. Check your connection and try again.";
		String e = r.error == null ? "" : r.error;
		switch (e){
			case "wrong_login": return "That email and password don't match.";
			case "email_taken": return "There's already an account with that email. Try signing in.";
			case "weak_password": return "Passwords need at least 8 characters.";
			case "bad_email": return "That doesn't look like an email address.";
			case "bad_code": return "That code isn't right.";
			case "code_expired": return "That code expired or had too many wrong tries. Ask for a new one.";
			case "email_not_verified": return "Verify your email first.";
			case "email_not_set_up": return "Sign-up emails aren't working yet. Try again later.";
			case "banned": return "This account has been banned from Community.";
			case "age_gate": return "Community is for players 16 and older.";
			case "not_allowed_words": return "That contains words that aren't allowed in Community.";
			case "submissions_closed": return "Submissions are closed for this season.";
			case "voting_closed": return "Voting isn't open right now.";
			case "own_submission": return "You can't vote for your own idea.";
			case "too_many_picks": return "You can pick up to 8.";
			case "too_many_submissions": return "You've posted a lot of ideas today. Try again tomorrow.";
			case "too_many_comments": return "Slow down a little: too many comments in a short time.";
			case "too_many_reports": return "You've sent a lot of reports today.";
			case "not_found": return "That isn't there any more.";
			case "no_season": return "No season is running right now.";
			case "not_admin": return "Only moderators can do that.";
			case "cannot_block_self": return "You can't block yourself.";
			case "bad_parent": return "That comment isn't there any more.";
			case "not_a_qualifier": return "That idea isn't in the tournament.";
			case "bye_no_vote": case "bad_matchup": case "not_in_matchup": return "That vote isn't open right now.";
			case "signed_out": return "You were signed out. Please sign in again.";
			case "server_error": return "The Community server had a problem. Try again in a moment.";
			case "slow_down": return "Too many tries. Wait a few minutes and try again.";
			case "bad_google_token": return "Google sign-in didn't work. Try again or use email.";
			case "search_too_short": return "Type at least 2 letters.";
			case "cannot_ban_self": return "You can't ban yourself.";
			case "season_overlaps": return "A season is already running in that time. Wait for it to finish first.";
			case "no_matchups_open": case "not_week_one": return "That vote isn't open right now.";
			default:
				if (e.startsWith("too_long_")) return "That's too long.";
				if (e.startsWith("missing_")) return "Something was left empty.";
				return "Something went wrong (" + e + "). Try again.";
		}
	}
}
