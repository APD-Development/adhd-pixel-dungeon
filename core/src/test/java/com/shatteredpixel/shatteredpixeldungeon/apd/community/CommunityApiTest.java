package com.shatteredpixel.shatteredpixeldungeon.apd.community;

import static org.junit.Assert.*;

import com.sun.net.httpserver.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The Community client speaks the server's JSON: right paths, bodies and token, and every failure comes back as a Result. */
public class CommunityApiTest {
	private HttpServer server;
	private final List<String> seen = new ArrayList<>();
	private int nextStatus = 200;
	private String nextBody = "{}";

	@Before public void start() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", ex -> {
			String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			String auth = ex.getRequestHeaders().getFirst("Authorization");
			seen.add(ex.getRequestMethod() + " " + ex.getRequestURI().getPath() + " " + body + " " + auth);
			byte[] out = nextBody.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(nextStatus, out.length);
			try (OutputStream o = ex.getResponseBody()) { o.write(out); }
		});
		server.start();
	}

	@After public void stop(){ server.stop(0); }

	private CommunityApi api(){ return new CommunityApi("http://127.0.0.1:" + server.getAddress().getPort() + "/", 3000); }

	@Test public void signInKeepsTheTokenAndSendsItAfterwards(){
		CommunityApi api = api();
		nextBody = "{\"token\":\"abc123\"}";
		assertTrue(api.verify("a@b.com", "123456").ok());
		assertEquals("abc123", api.token());
		nextBody = "{\"id\":\"ff00\"}";
		nextStatus = 201;
		CommunityApi.Result r = api.submit("Shield \"rework\"", "line1\nline2");
		assertTrue(r.ok());
		assertEquals("ff00", r.string("id"));
		assertEquals("POST /v1/auth/verify {\"email\":\"a@b.com\",\"code\":\"123456\"} null", seen.get(0));
		assertEquals("POST /v1/submissions {\"title\":\"Shield \\\"rework\\\"\",\"body\":\"line1\\nline2\"} Bearer abc123", seen.get(1));
	}

	@Test public void serverErrorsComeBackAsCodes_andA401SignsOut(){
		CommunityApi api = api();
		api.token("t");
		nextStatus = 409; nextBody = "{\"error\":\"submissions_closed\"}";
		CommunityApi.Result r = api.submit("x", "y");
		assertFalse(r.ok());
		assertEquals("submissions_closed", r.error);
		assertTrue(api.signedIn());
		nextStatus = 401; nextBody = "{\"error\":\"signed_out\"}";
		assertEquals("signed_out", api.me().error);
		assertFalse(api.signedIn());
		nextStatus = 500; nextBody = "not json";
		assertEquals("http_500", api.season().error);
	}

	@Test public void unreachableServerIsOffline_notACrash(){
		CommunityApi api = new CommunityApi("http://127.0.0.1:1", 500);
		CommunityApi.Result r = api.submissions();
		assertEquals(0, r.status);
		assertEquals("offline", r.error);
	}

	@Test public void votingPicksAndAdminCallsHitTheirRoutes(){
		CommunityApi api = api();
		api.token("t");
		api.upvote("aa", true);
		api.upvote("aa", false);
		api.picks(Arrays.asList("01", "02"));
		api.vote(2, "0b");
		api.comment("aa", "bb", "reply");
		api.ageGatePassed();
		api.adminSetStatus("hide", "comment", "cc");
		api.adminBan("dd", true);
		api.deleteAccount();
		assertEquals(Arrays.asList(
			"POST /v1/submissions/aa/upvote  Bearer t",
			"DELETE /v1/submissions/aa/upvote  Bearer t",
			"POST /v1/tournament/picks {\"submissionIds\":[\"01\",\"02\"]} Bearer t",
			"POST /v1/tournament/vote {\"matchup\":2,\"submissionId\":\"0b\"} Bearer t",
			"POST /v1/submissions/aa/comments {\"body\":\"reply\",\"parentId\":\"bb\"} Bearer t",
			"POST /v1/account/age {\"passed\":true} Bearer t",
			"POST /v1/admin/hide {\"kind\":\"comment\",\"id\":\"cc\"} Bearer t",
			"POST /v1/admin/ban {\"accountId\":\"dd\"} Bearer t",
			"DELETE /v1/account  Bearer t"), seen);
		assertFalse("deleting the account signs out", api.signedIn());
	}

	@Test public void newerCallsHitTheirRoutes(){
		CommunityApi api = api();
		api.token("t");
		api.blocked();
		api.adminFind("Troll Face&x");
		api.adminStartSeason("S \"2\"", 1, 2, 28);
		api.google("idtok");
		assertEquals("GET /v1/blocks  Bearer t", seen.get(0));
		assertEquals("GET /v1/admin/accounts  Bearer t", seen.get(1)); // the query string isn't part of the path
		assertEquals("POST /v1/admin/seasons {\"title\":\"S \\\"2\\\"\",\"submissionMs\":86400000,\"votingMs\":172800000,\"tournamentMs\":2419200000} Bearer t", seen.get(2));
		assertEquals("POST /v1/auth/google {\"idToken\":\"idtok\"} Bearer t", seen.get(3));
	}

	@Test public void refusesUnsafeAddressesAndIds(){
		try { new CommunityApi("http://example.com", 1000); fail(); } catch (IllegalArgumentException expected) {}
		try { api().submission("../admin"); fail(); } catch (IllegalArgumentException expected) {}
		assertTrue(seen.isEmpty());
	}
}
