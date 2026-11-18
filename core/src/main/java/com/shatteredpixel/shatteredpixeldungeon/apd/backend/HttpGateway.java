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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

// A Gateway that speaks plain HTTPS to whatever server the base URL points at. It knows no provider: the URL is configuration, there is no
// credential in the app (an optional bearer token comes from a TokenSource the caller supplies, e.g. after sign-in), and without a URL the app
// uses OfflineGateway instead (see Gateways).
public final class HttpGateway implements Gateway, Mailbox {

	//Supplies the signed-in player's session token, or null when nobody is signed in. Never stored by this class.
	public interface TokenSource {
		String token();
	}

	private final String baseUrl;
	private final TokenSource tokens;
	private final int timeoutMillis;

	public HttpGateway( String baseUrl, TokenSource tokens, int timeoutMillis ){
		this.baseUrl = Gateways.cleanBaseUrl(baseUrl);
		if (this.baseUrl == null) throw new IllegalArgumentException("a secure server address is required");
		this.tokens = tokens;
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	public Response send( String idempotencyKey, Envelope envelope ){
		HttpURLConnection c = null;
		try {
			c = (HttpURLConnection) java.net.URI.create(baseUrl + "/v1/envelope").toURL().openConnection();
			c.setConnectTimeout(timeoutMillis);
			c.setReadTimeout(timeoutMillis);
			c.setRequestMethod("POST");
			//a redirect would resend the data somewhere the player never configured
			c.setInstanceFollowRedirects(false);
			c.setDoOutput(true);
			c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
			c.setRequestProperty("Idempotency-Key", idempotencyKey);
			c.setRequestProperty("X-APD-Schema", envelope.schema);
			c.setRequestProperty("X-APD-Version", envelope.major + "." + envelope.minor);
			String token = tokens == null ? null : tokens.token();
			if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
			byte[] body = envelope.payload.getBytes(StandardCharsets.UTF_8);
			c.setFixedLengthStreamingMode(body.length);
			try (OutputStream out = c.getOutputStream()){
				out.write(body);
			}
			return fromStatus(c.getResponseCode(), readSmall(c));
		} catch (IOException e){
			//no connection, timeout, DNS failure: nothing is wrong with the item
			return new Response(Status.OFFLINE, "no connection");
		} catch (RuntimeException e){
			return new Response(Status.RETRY_LATER, "unexpected failure");
		} finally {
			if (c != null) c.disconnect();
		}
	}

	@Override
	public Fetched fetch(){
		HttpURLConnection c = null;
		try {
			c = (HttpURLConnection) java.net.URI.create(baseUrl + "/v1/requests").toURL().openConnection();
			c.setConnectTimeout(timeoutMillis);
			c.setReadTimeout(timeoutMillis);
			c.setRequestMethod("GET");
			c.setInstanceFollowRedirects(false);
			String token = tokens == null ? null : tokens.token();
			if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
			int code = c.getResponseCode();
			if (code == 401) return new Fetched(false, new java.util.ArrayList<Envelope>(), "sign-in needed");
			if (code < 200 || code >= 300) return new Fetched(false, new java.util.ArrayList<Envelope>(), "server answered " + code);
			StringBuilder sb = new StringBuilder();
			try (InputStream in = c.getInputStream()){
				byte[] buf = new byte[4096];
				int n;
				while ((n = in.read(buf)) > 0 && sb.length() < 1_000_000) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
			}
			return new Fetched(true, MailboxCodec.decode(sb.toString()), "");
		} catch (IOException e){
			return new Fetched(false, new java.util.ArrayList<Envelope>(), "no connection");
		} catch (RuntimeException e){
			return new Fetched(false, new java.util.ArrayList<Envelope>(), "unexpected failure");
		} finally {
			if (c != null) c.disconnect();
		}
	}

	//How a server answer is read: success and "already have it" are OK; busy or broken servers are retried; anything else is a refusal for good.
	static Response fromStatus( int code, String detail ){
		if (code >= 200 && code < 300) return new Response(Status.OK, "");
		if (code == 409) return new Response(Status.OK, "already received");
		//moved: the item is not at fault and nothing was sent anywhere else, so it stays queued until the address is corrected
		if (code >= 300 && code < 400) return new Response(Status.RETRY_LATER, "server address moved (" + code + ")");
		if (code == 408 || code == 425 || code == 429 || code >= 500) return new Response(Status.RETRY_LATER, "server busy (" + code + ")");
		//not signed in (or the session ended): nothing is wrong with the item, so it stays queued
		if (code == 401) return new Response(Status.RETRY_LATER, "sign-in needed");
		if (code == 403) return new Response(Status.REJECTED, "not allowed (" + code + ")" + (detail.isEmpty() ? "" : ": " + detail));
		return new Response(Status.REJECTED, "refused (" + code + ")" + (detail.isEmpty() ? "" : ": " + detail));
	}

	private static String readSmall( HttpURLConnection c ){
		try {
			InputStream in = c.getErrorStream() != null ? c.getErrorStream() : c.getInputStream();
			if (in == null) return "";
			byte[] buf = new byte[256];
			int n = in.read(buf);
			in.close();
			return n <= 0 ? "" : new String(buf, 0, n, StandardCharsets.UTF_8).replace('\n', ' ').trim();
		} catch (IOException e){
			return "";
		}
	}
}
