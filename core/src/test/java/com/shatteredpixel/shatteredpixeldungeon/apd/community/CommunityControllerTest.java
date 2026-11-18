package com.shatteredpixel.shatteredpixeldungeon.apd.community;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.age.CommunityAgeGate;
import com.sun.net.httpserver.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** The live Community screens, driven through a stub server: age gate, sign-up, hub, threads, voting, errors. */
public class CommunityControllerTest {
	private HttpServer server;
	private final Map<String, String[]> replies = new HashMap<>(); // "GET /v1/me" -> {status, body}
	private final List<String> requests = new ArrayList<>();

	private final List<CommunityController.Screen> screens = new ArrayList<>();
	private final Deque<String> answers = new ArrayDeque<>();
	private final List<String> asked = new ArrayList<>();
	private boolean left, planned;

	private final CommunityController.Ui ui = new CommunityController.Ui(){
		public void show(CommunityController.Screen s){ screens.add(s); }
		public void ask(String title, String body, String initial, int max, boolean multi, Consumer<String> answer){
			asked.add(title);
			answer.accept(answers.isEmpty() ? null : answers.poll());
		}
		public void busy(String m){}
		public void leave(){ left = true; }
		public void planned(){ planned = true; }
	};

	private static final class Store implements CommunityController.Storage {
		String token, age;
		public String token(){ return token; }
		public void token(String t){ token = t; }
		public String get(){ return age; }
		public void put(String v){ age = v; }
	}
	private final Store store = new Store();

	@Before public void start() throws Exception {
		CommunityAgeGate.play = null; // no Google Play in tests: the birth month/year question is used
		CommunityController.google = null;
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", ex -> {
			String key = ex.getRequestMethod() + " " + ex.getRequestURI().getPath();
			requests.add(key + " " + new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			String[] r = replies.getOrDefault(key, new String[]{"404", "{\"error\":\"no_route\"}"});
			byte[] out = r[1].getBytes(StandardCharsets.UTF_8);
			ex.sendResponseHeaders(Integer.parseInt(r[0]), out.length);
			try (OutputStream o = ex.getResponseBody()) { o.write(out); }
		});
		server.start();
	}

	@After public void stop(){ server.stop(0); }

	private CommunityController controller(){
		CommunityApi api = new CommunityApi("http://127.0.0.1:" + server.getAddress().getPort(), 3000);
		return new CommunityController(api, ui, (call, then) -> then.accept(call.get()), store, new int[]{11, 2026});
	}

	private void reply(String route, int status, String json){ replies.put(route, new String[]{String.valueOf(status), json}); }
	private CommunityController.Screen last(){ return screens.get(screens.size() - 1); }
	private void tap(String labelStart){
		for (CommunityController.Row r : last().items()) if (r.label.startsWith(labelStart)){ r.action.run(); return; }
		fail("no row '" + labelStart + "' on " + last().title + ": " + labels(last()));
	}
	private static List<String> labels(CommunityController.Screen s){ List<String> l = new ArrayList<>(); for (CommunityController.Row r : s.rows) l.add(r.label); return l; }
	private static boolean has(CommunityController.Screen s, String text){ for (CommunityController.Row r : s.rows) if (r.label.contains(text) || r.detail.contains(text)) return true; return s.message != null && s.message.contains(text); }

	private void signedInWorld(boolean admin){
		reply("GET /v1/me", 200, "{\"id\":\"aa01\",\"displayName\":\"Chris\",\"emailVerified\":true,\"ageOk\":true,\"admin\":" + admin + "}");
		reply("GET /v1/season", 200, "{\"season\":{\"id\":\"s1\",\"title\":\"Season 1\",\"phase\":\"voting\"}}");
	}

	@Test public void underSixteenLocksForGood_andTheDateIsNeverStored(){
		answers.add("5"); answers.add("2012");
		controller().start();
		tap("Enter birth month and year");
		assertEquals("fail", store.age);
		assertTrue(has(last(), "16 and older"));
		assertTrue(requests.isEmpty());
		screens.clear(); answers.add("1"); answers.add("1990");
		controller().start();
		assertTrue("no second try", has(last(), "16 and older"));
		assertFalse(asked.size() > 2);
	}

	@Test public void signUpVerifyAndLandOnTheHub(){
		store.age = "pass";
		signedInWorld(false);
		reply("POST /v1/auth/register", 201, "{\"accountId\":\"aa01\"}");
		reply("POST /v1/auth/verify", 200, "{\"token\":\"tok123\"}");
		answers.add("me@x.com"); answers.add("longpassword"); answers.add("Chris"); answers.add("123456");
		controller().start();
		tap("Create an account");
		assertEquals("tok123", store.token);
		CommunityController.Screen hub = last();
		assertEquals("Community", hub.title);
		assertTrue(has(hub, "Signed in as Chris"));
		assertTrue(has(hub, "Month 2: voting"));
		assertFalse("not an admin", has(hub, "Moderation"));
		assertTrue(requests.get(0).startsWith("POST /v1/auth/register {\"email\":\"me@x.com\",\"password\":\"longpassword\",\"displayName\":\"Chris\"}"));
	}

	@Test public void savedSessionGoesStraightToTheHub_andAdminsSeeModeration(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(true);
		controller().start();
		assertTrue(has(last(), "Moderation"));
	}

	@Test public void threadsShowRepliesUnderTheirParent(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(false);
		reply("GET /v1/submissions", 200, "{\"submissions\":[{\"id\":\"5501\",\"title\":\"Shield rework\",\"author\":{\"id\":\"bb02\",\"displayName\":\"Bo\"},\"comments\":3}]}");
		reply("GET /v1/submissions/5501", 200, "{\"id\":\"5501\",\"title\":\"Shield rework\",\"body\":\"Shields should have worked\",\"author\":{\"id\":\"bb02\",\"displayName\":\"Bo\"},\"comments\":["
				+ "{\"id\":\"c1\",\"parentId\":null,\"body\":\"Love it\",\"author\":{\"id\":\"aa01\",\"displayName\":\"Chris\"}},"
				+ "{\"id\":\"c2\",\"parentId\":null,\"body\":null,\"author\":null},"
				+ "{\"id\":\"c3\",\"parentId\":\"c1\",\"body\":\"Thanks\",\"author\":{\"id\":\"bb02\",\"displayName\":\"Bo\"}}]}");
		controller().start();
		tap("Submissions");
		assertTrue(has(last(), "by Bo"));
		tap("Shield rework");
		List<String> l = labels(last());
		int a = l.indexOf("Chris: Love it"), b = l.indexOf("  > Bo: Thanks"), c = l.indexOf("[removed]");
		assertTrue(l.toString(), a >= 0 && b == a + 1 && c > b);
		assertTrue("voting phase, someone else's idea", has(last(), "Upvote this idea"));
		tap("  > Bo: Thanks");
		assertTrue(has(last(), "Report this comment"));
	}

	@Test public void votingStandingsAndPlainErrors(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(false);
		reply("GET /v1/season/results", 200, "{\"season\":{\"phase\":\"voting\"},\"standings\":[{\"id\":\"5501\",\"title\":\"Idea A\",\"upvotes\":4}],\"myUpvotes\":[\"5501\"]}");
		controller().start();
		tap("Voting and results");
		assertTrue(has(last(), "1. Idea A"));
		assertEquals("4 upvotes · you upvoted", last().items().get(0).detail);
		CommunityApi.Result r = new CommunityApi.Result(409, null, "own_submission");
		assertEquals("You can't vote for your own idea.", CommunityController.message(r));
		assertTrue(CommunityController.message(new CommunityApi.Result(0, null, "offline")).contains("Can't reach"));
	}

	@Test public void anExpiredSessionSignsOutCleanly(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(false);
		controller().start();
		reply("GET /v1/submissions", 401, "{\"error\":\"signed_out\"}");
		tap("Submissions");
		assertNull(store.token);
		assertTrue(has(last(), "Sign in with email"));
	}

	@Test public void googleSignInSendsTheTokenAndCancelGoesBack(){
		store.age = "pass";
		signedInWorld(false);
		reply("POST /v1/auth/google", 200, "{\"token\":\"gtok\"}");
		CommunityController.google = done -> done.accept("google-id-token");
		controller().start();
		tap("Sign in with Google");
		assertEquals("gtok", store.token);
		assertTrue(requests.get(0).startsWith("POST /v1/auth/google {\"idToken\":\"google-id-token\"}"));
		assertTrue(has(last(), "Signed in as Chris"));
		store.token = null; screens.clear(); requests.clear();
		CommunityController.google = done -> done.accept(null);
		controller().start();
		tap("Sign in with Google");
		assertTrue(has(last(), "Sign in with email"));
		assertTrue("cancelling sends nothing", requests.isEmpty());
		CommunityController.google = null;
	}

	@Test public void unblockFromTheAccountScreen(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(false);
		reply("GET /v1/blocks", 200, "{\"blocked\":[{\"id\":\"dd04\",\"displayName\":\"Pest\"}]}");
		reply("DELETE /v1/blocks/dd04", 200, "{\"ok\":true}");
		controller().start();
		tap("Account");
		tap("Blocked players");
		tap("Pest");
		assertTrue(has(last(), "Pest is unblocked."));
		assertTrue(requests.stream().anyMatch(r -> r.startsWith("DELETE /v1/blocks/dd04")));
	}

	@Test public void adminStartsASeasonWithDaysTurnedIntoTheServersMilliseconds(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(true);
		reply("GET /v1/admin/reports", 200, "{\"reports\":[]}");
		reply("POST /v1/admin/seasons", 201, "{\"id\":\"s2\"}");
		answers.add("Season 2"); answers.add("10"); answers.add("10"); answers.add("28");
		controller().start();
		tap("Moderation");
		tap("Start a new season");
		assertEquals("Start Season 2?", last().title);
		tap("Start it");
		assertTrue(requests.stream().anyMatch(r -> r.startsWith("POST /v1/admin/seasons {\"title\":\"Season 2\",\"submissionMs\":864000000,\"votingMs\":864000000,\"tournamentMs\":2419200000}")));
		assertTrue(has(last(), "Season started."));
	}

	@Test public void adminFindsAndUnbansAPlayer(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(true);
		reply("GET /v1/admin/reports", 200, "{\"reports\":[]}");
		reply("GET /v1/admin/accounts", 200, "{\"accounts\":[{\"id\":\"ee05\",\"displayName\":\"Troll\",\"email\":\"t@x.com\",\"banned\":true}]}");
		reply("POST /v1/admin/unban", 200, "{\"ok\":true}");
		answers.add("tro ll");
		controller().start();
		tap("Moderation");
		tap("Find a player");
		assertTrue(has(last(), "Troll  (banned)"));
		tap("Troll");
		assertTrue(has(last(), "Troll is unbanned."));
		assertTrue(requests.stream().anyMatch(r -> r.startsWith("POST /v1/admin/unban {\"accountId\":\"ee05\"}")));
	}

	@Test public void theUpvoteButtonSaysWhetherIUpvotedAndTheCount(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(false);
		reply("GET /v1/submissions", 200, "{\"submissions\":[{\"id\":\"5501\",\"title\":\"Idea A\",\"author\":{\"id\":\"bb02\",\"displayName\":\"Bo\"},\"comments\":0}]}");
		reply("GET /v1/submissions/5501", 200, "{\"id\":\"5501\",\"title\":\"Idea A\",\"body\":\"b\",\"author\":{\"id\":\"bb02\",\"displayName\":\"Bo\"},\"comments\":[]}");
		reply("GET /v1/season/results", 200, "{\"season\":{\"phase\":\"voting\"},\"standings\":[{\"id\":\"5501\",\"title\":\"Idea A\",\"upvotes\":3}],\"myUpvotes\":[\"5501\"]}");
		reply("DELETE /v1/submissions/5501/upvote", 200, "{\"upvoted\":false}");
		controller().start();
		tap("Submissions");
		tap("Idea A");
		assertTrue(has(last(), "Take back my upvote"));
		assertTrue(has(last(), "3 upvotes"));
		tap("Take back my upvote");
		assertTrue(requests.stream().anyMatch(r -> r.startsWith("DELETE /v1/submissions/5501/upvote")));
	}

	@Test public void theHubLinksToWhatsComingNext(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(false);
		controller().start();
		tap("What's coming next");
		assertTrue(planned);
	}

	@Test public void weekOnePicksToggleLocallyAndOnlySaveSendsThem(){
		store.age = "pass"; store.token = "tok";
		signedInWorld(false);
		reply("GET /v1/season/results", 200, "{\"season\":{\"phase\":\"tournament\"},\"week\":1,\"qualifiers\":[{\"id\":\"a1\",\"title\":\"Alpha\",\"picks\":2},{\"id\":\"b2\",\"title\":\"Beta\",\"picks\":0}],\"rounds\":[]}");
		reply("POST /v1/tournament/picks", 200, "{\"picks\":[\"b2\"]}");
		controller().start();
		tap("Voting and results");
		int before = requests.size();
		tap("Beta");
		tap("Alpha");
		tap("Alpha");
		assertEquals("no network for toggles", before, requests.size());
		assertTrue(has(last(), "1 picked"));
		tap("Save my picks");
		assertTrue(requests.get(requests.size() - 1).startsWith("POST /v1/tournament/picks {\"submissionIds\":[\"b2\"]}"));
	}

	@Test public void noConnectionKeepsTheSignInAndOffersRetry(){
		store.age = "pass"; store.token = "tok";
		CommunityApi dead = new CommunityApi("http://127.0.0.1:1", 500);
		new CommunityController(dead, ui, (call, then) -> then.accept(call.get()), store, new int[]{11, 2026}).start();
		assertEquals("tok", store.token);
		assertTrue(has(last(), "You're still signed in."));
		assertTrue(has(last(), "Try again"));
	}

	// Every error code the server can send has plain words in the app (except a few that only a broken client causes).
	@Test public void everyServerErrorHasPlainWords() throws Exception {
		java.io.File app = new java.io.File(System.getProperty("apd.game.src"), "../../../../../../../server/community/src/app.js");
		String src = new String(java.nio.file.Files.readAllBytes(app.toPath()), java.nio.charset.StandardCharsets.UTF_8);
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("fail\\(\\d+, '([a-z_]+)'\\)").matcher(src);
		java.util.Set<String> internal = new java.util.HashSet<>(java.util.Arrays.asList("bad_json", "bad_kind", "only_pass_is_recorded"));
		List<String> unmapped = new ArrayList<>();
		int seen = 0;
		while (m.find()){
			seen++;
			String code = m.group(1);
			if (internal.contains(code)) continue;
			String text = CommunityController.message(new CommunityApi.Result(400, null, code));
			if (text.startsWith("Something went wrong")) unmapped.add(code);
		}
		assertTrue("found the server's errors", seen > 30);
		assertTrue(unmapped.toString(), unmapped.isEmpty());
	}
}
