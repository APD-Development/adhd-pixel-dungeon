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

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Client for the APD Community server (server/community). Plain HTTPS + JSON, nothing provider-specific: the base URL
// is configuration and the session token comes from sign-in. Calls block, so the UI runs them off the render thread.
// Every call returns a Result; nothing here throws for network or server problems.
public final class CommunityApi {

	public static final class Result {
		public final int status;          // HTTP status, or 0 when the server couldn't be reached
		public final JsonValue body;      // parsed JSON, or null
		public final String error;        // the server's error code, "offline", or null on success

		Result(int status, JsonValue body, String error){
			this.status = status; this.body = body; this.error = error;
		}

		public boolean ok(){ return error == null && status >= 200 && status < 300; }
		public String string(String key){ return body == null ? null : body.getString(key, null); }
	}

	private final String baseUrl;
	private final int timeoutMillis;
	private String token;

	public CommunityApi(String baseUrl, int timeoutMillis){
		String u = baseUrl == null ? "" : baseUrl.trim();
		while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
		boolean local = u.startsWith("http://127.0.0.1:") || u.startsWith("http://localhost:");
		if (!u.startsWith("https://") && !local) throw new IllegalArgumentException("Community needs an https address");
		this.baseUrl = u;
		this.timeoutMillis = timeoutMillis;
	}

	public void token(String token){ this.token = token; }
	public String token(){ return token; }
	public boolean signedIn(){ return token != null; }

	// ---- accounts ----

	public Result register(String email, String password, String displayName){
		return call("POST", "/v1/auth/register", obj("email", email, "password", password, "displayName", displayName));
	}

	public Result resendCode(String email){ return call("POST", "/v1/auth/resend", obj("email", email)); }

	// On success the session token is kept for the following calls.
	public Result verify(String email, String code){ return keepToken(call("POST", "/v1/auth/verify", obj("email", email, "code", code))); }
	public Result login(String email, String password){ return keepToken(call("POST", "/v1/auth/login", obj("email", email, "password", password))); }
	public Result google(String idToken){ return keepToken(call("POST", "/v1/auth/google", obj("idToken", idToken))); }

	public Result logout(){
		Result r = call("POST", "/v1/auth/logout", "{}");
		token = null;
		return r;
	}

	public Result me(){ return call("GET", "/v1/me", null); }

	// Only ever sends a pass. A failed age check never leaves the device.
	public Result ageGatePassed(){ return call("POST", "/v1/account/age", "{\"passed\":true}"); }

	public Result deleteAccount(){
		Result r = call("DELETE", "/v1/account", null);
		if (r.ok()) token = null;
		return r;
	}

	// ---- season, submissions, comments ----

	public Result season(){ return call("GET", "/v1/season", null); }
	public Result results(){ return call("GET", "/v1/season/results", null); }
	public Result submissions(){ return call("GET", "/v1/submissions", null); }
	public Result submission(String id){ return call("GET", "/v1/submissions/" + id(id), null); }
	public Result submit(String title, String body){ return call("POST", "/v1/submissions", obj("title", title, "body", body)); }

	public Result comment(String submissionId, String parentId, String body){
		return call("POST", "/v1/submissions/" + id(submissionId) + "/comments",
				parentId == null ? obj("body", body) : obj("body", body, "parentId", parentId));
	}

	public Result upvote(String submissionId, boolean on){
		return call(on ? "POST" : "DELETE", "/v1/submissions/" + id(submissionId) + "/upvote", null);
	}

	public Result picks(List<String> submissionIds){
		StringBuilder b = new StringBuilder("{\"submissionIds\":[");
		for (int i = 0; i < submissionIds.size(); i++){
			if (i > 0) b.append(',');
			b.append(quote(submissionIds.get(i)));
		}
		return call("POST", "/v1/tournament/picks", b.append("]}").toString());
	}

	public Result vote(int matchup, String submissionId){
		return call("POST", "/v1/tournament/vote", "{\"matchup\":" + matchup + ",\"submissionId\":" + quote(submissionId) + "}");
	}

	// ---- reports and blocks ----

	public Result report(String kind, String targetId, String reason){
		return call("POST", "/v1/reports", obj("kind", kind, "id", targetId, "reason", reason));
	}

	public Result block(String accountId){ return call("POST", "/v1/blocks", obj("accountId", accountId)); }
	public Result unblock(String accountId){ return call("DELETE", "/v1/blocks/" + id(accountId), null); }
	public Result blocked(){ return call("GET", "/v1/blocks", null); }

	// ---- admin (only works for an admin account; the server checks) ----

	public Result adminReports(){ return call("GET", "/v1/admin/reports", null); }
	public Result adminSetStatus(String action, String kind, String targetId){
		if (!action.equals("hide") && !action.equals("remove") && !action.equals("restore")) throw new IllegalArgumentException(action);
		return call("POST", "/v1/admin/" + action, obj("kind", kind, "id", targetId));
	}
	// Opens a season starting now; phase lengths in days (the server stores milliseconds).
	public Result adminStartSeason(String title, int submissionDays, int votingDays, int tournamentDays){
		long day = 24L * 3600 * 1000;
		return call("POST", "/v1/admin/seasons", "{\"title\":" + quote(title) + ",\"submissionMs\":" + submissionDays * day
				+ ",\"votingMs\":" + votingDays * day + ",\"tournamentMs\":" + tournamentDays * day + "}");
	}

	public Result adminFind(String query){
		try { return call("GET", "/v1/admin/accounts?q=" + java.net.URLEncoder.encode(query, "UTF-8"), null); }
		catch (java.io.UnsupportedEncodingException e){ return new Result(0, null, "offline"); }
	}

	public Result adminBan(String accountId, boolean banned){
		return call("POST", banned ? "/v1/admin/ban" : "/v1/admin/unban", obj("accountId", accountId));
	}

	// ---- plumbing ----

	private Result keepToken(Result r){
		if (r.ok() && r.string("token") != null) token = r.string("token");
		return r;
	}

	// Ids from the server are hex; anything else is refused rather than put in a path.
	private static String id(String id){
		if (id == null || !id.matches("[0-9a-f]{1,64}")) throw new IllegalArgumentException("bad id");
		return id;
	}

	static String quote(String s){
		if (s == null) return "null";
		StringBuilder b = new StringBuilder("\"");
		for (int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			switch (c){
				case '"': b.append("\\\""); break;
				case '\\': b.append("\\\\"); break;
				case '\n': b.append("\\n"); break;
				case '\r': b.append("\\r"); break;
				case '\t': b.append("\\t"); break;
				default:
					if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
					else b.append(c);
			}
		}
		return b.append('"').toString();
	}

	static String obj(String... kv){
		StringBuilder b = new StringBuilder("{");
		for (int i = 0; i + 1 < kv.length; i += 2){
			if (i > 0) b.append(',');
			b.append(quote(kv[i])).append(':').append(quote(kv[i + 1]));
		}
		return b.append('}').toString();
	}

	private Result call(String method, String path, String json){
		HttpURLConnection c = null;
		try {
			c = (HttpURLConnection) java.net.URI.create(baseUrl + path).toURL().openConnection();
			c.setConnectTimeout(timeoutMillis);
			c.setReadTimeout(timeoutMillis);
			c.setRequestMethod(method);
			c.setInstanceFollowRedirects(false);
			c.setRequestProperty("Accept", "application/json");
			if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);
			if (json != null){
				byte[] out = json.getBytes(StandardCharsets.UTF_8);
				c.setDoOutput(true);
				c.setRequestProperty("Content-Type", "application/json");
				c.setFixedLengthStreamingMode(out.length);
				try (OutputStream o = c.getOutputStream()){ o.write(out); }
			}
			int status = c.getResponseCode();
			InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
			JsonValue body = null;
			if (in != null){
				try (InputStream i = in){
					ByteArrayOutputStream buf = new ByteArrayOutputStream();
					byte[] chunk = new byte[8192];
					int n, total = 0;
					while ((n = i.read(chunk)) > 0){
						total += n;
						if (total > 4 * 1024 * 1024) throw new IOException("reply too large");
						buf.write(chunk, 0, n);
					}
					String text = new String(buf.toByteArray(), StandardCharsets.UTF_8).trim();
					if (!text.isEmpty()) body = new JsonReader().parse(text);
				} catch (RuntimeException badJson){
					body = null;
				}
			}
			String error = null;
			if (status < 200 || status >= 300){
				error = body != null ? body.getString("error", "http_" + status) : "http_" + status;
			}
			if (status == 401) token = null; // the session is gone; the UI asks to sign in again
			return new Result(status, body, error);
		} catch (IOException e){
			return new Result(0, null, "offline");
		} finally {
			if (c != null) c.disconnect();
		}
	}
}
