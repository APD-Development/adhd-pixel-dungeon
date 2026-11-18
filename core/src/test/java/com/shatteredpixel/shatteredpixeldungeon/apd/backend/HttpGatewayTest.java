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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import static org.junit.Assert.*;

import com.sun.net.httpserver.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HttpGatewayTest {

	private HttpServer server;
	private int status = 200;
	private final List<String> seen = new ArrayList<>();
	private String url;

	@Before
	public void start() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/envelope", ex -> {
			byte[] body = ex.getRequestBody().readAllBytes();
			seen.add(ex.getRequestHeaders().getFirst("Idempotency-Key") + "|" + ex.getRequestHeaders().getFirst("X-APD-Schema") + "|" + ex.getRequestHeaders().getFirst("X-APD-Version")
					+ "|" + ex.getRequestHeaders().getFirst("Authorization") + "|" + new String(body, StandardCharsets.UTF_8));
			byte[] reply = "nope".getBytes(StandardCharsets.UTF_8);
			ex.sendResponseHeaders(status, reply.length);
			ex.getResponseBody().write(reply);
			ex.close();
		});
		server.start();
		url = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@After
	public void stop(){
		server.stop(0);
	}

	private static Envelope env(){
		return new Envelope("apd.run-submission", 1, 0, "runId=r1\n");
	}

	@Test
	public void sendsTheEnvelopeWithItsKeyAndMapsServerAnswers(){
		HttpGateway g = new HttpGateway(url, null, 3000);
		assertEquals(Gateway.Status.OK, g.send("ob_r1", env()).status);
		assertEquals("ob_r1|apd.run-submission|1.0|null|runId=r1\n", seen.get(0));
		status = 409;
		assertEquals("already have it is fine", Gateway.Status.OK, g.send("ob_r1", env()).status);
		status = 503;
		assertEquals(Gateway.Status.RETRY_LATER, g.send("ob_r1", env()).status);
		status = 429;
		assertEquals(Gateway.Status.RETRY_LATER, g.send("ob_r1", env()).status);
		status = 400;
		Gateway.Response bad = g.send("ob_r1", env());
		assertEquals(Gateway.Status.REJECTED, bad.status);
		assertTrue(bad.detail.contains("400"));
		status = 403;
		assertEquals(Gateway.Status.REJECTED, g.send("ob_r1", env()).status);
		status = 301;
		Gateway.Response moved = g.send("ob_r1", env());
		assertEquals("a redirect is not followed and the item stays queued", Gateway.Status.RETRY_LATER, moved.status);
		assertTrue(moved.detail.contains("moved"));
		status = 401;
		Gateway.Response signIn = g.send("ob_r1", env());
		assertEquals("not signed in: the item stays queued", Gateway.Status.RETRY_LATER, signIn.status);
		assertEquals("sign-in needed", signIn.detail);
	}

	@Test
	public void theSessionTokenIsSentOnlyWhenSomeoneIsSignedIn(){
		new HttpGateway(url, () -> "tok123", 3000).send("k", env());
		assertTrue(seen.get(0).contains("|Bearer tok123|"));
		new HttpGateway(url, () -> null, 3000).send("k", env());
		assertTrue(seen.get(1).contains("|null|"));
	}

	@Test
	public void anUnreachableServerIsOfflineNotAFailureOfTheItem(){
		server.stop(0);
		assertEquals(Gateway.Status.OFFLINE, new HttpGateway(url, null, 500).send("k", env()).status);
	}

	@Test
	public void aQueueDrainsThroughTheHttpGatewayAndKeepsItemsWhileOffline() throws Exception {
		Outbox box = new Outbox();
		box.enqueue(Id.of(Id.Kind.OUTBOX, "ob_1"), env(), 0);
		HttpGateway g = new HttpGateway(url, null, 3000);
		status = 503;
		assertEquals(1, box.flush(g, 1_000_000_000L).kept);
		assertEquals(1, box.size());
		status = 200;
		assertEquals(1, box.flush(g, 2_000_000_000L).sent);
		assertEquals(0, box.size());
	}

	@Test
	public void sendingIsOffUnlessSwitchedOnWithASecureAddress(){
		assertTrue(Gateways.choose(false, "https://example.org", null) instanceof OfflineGateway);
		assertTrue("no address", Gateways.choose(true, null, null) instanceof OfflineGateway);
		assertTrue("plain http to a remote host", Gateways.choose(true, "http://example.org", null) instanceof OfflineGateway);
		assertTrue(Gateways.choose(true, "https://example.org/", null) instanceof HttpGateway);
		assertTrue("the local machine may use http for testing", Gateways.choose(true, "http://localhost:8080", null) instanceof HttpGateway);
		assertNull(Gateways.cleanBaseUrl("https://x.org/a?b=1"));
		assertEquals("https://x.org/api", Gateways.cleanBaseUrl(" https://x.org/api/ "));
		try {
			new HttpGateway("ftp://x", null, 1000);
			fail("unusable address");
		} catch (IllegalArgumentException expected){
		}
	}
}
