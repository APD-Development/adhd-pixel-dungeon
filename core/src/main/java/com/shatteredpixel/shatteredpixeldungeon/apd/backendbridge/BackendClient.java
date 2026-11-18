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

package com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge;

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.EvidenceMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateways;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Mailbox;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox;

import java.io.File;

// What the game does with a server, kept to explicit player actions: nothing here runs by itself, and with sending off or no address the
// queue simply stays on the device.
public final class BackendClient {

	public interface Done {
		void finished( String message );
	}

	private BackendClient(){}

	public static String status( boolean sendingOn, String address ){
		if (!sendingOn) return "Off. Nothing leaves this device.";
		if (Gateways.cleanBaseUrl(address) == null) return "On, but there is no usable server address (it must start with https://).";
		return "On, server: " + Gateways.cleanBaseUrl(address);
	}

	//The player's own summary of a send attempt.
	public static String describe( Outbox.FlushResult r, int pendingAfter ){
		if (r.sent == 0 && r.rejected == 0 && r.kept == 0) return "Nothing was queued.";
		StringBuilder sb = new StringBuilder();
		if (r.sent > 0) sb.append(r.sent).append(" sent. ");
		if (r.rejected > 0){
			sb.append(r.rejected).append(" refused and removed");
			if (!r.rejectedDetails.isEmpty()) sb.append(" (").append(r.rejectedDetails.get(0)).append(")");
			sb.append(". ");
		}
		if (pendingAfter > 0) sb.append(pendingAfter).append(" still queued, to try again later.");
		return sb.toString().trim();
	}

	//Asks the server for requests it has for this player and stores the readable evidence requests until the player answers them. Returns how
	//many new ones were added, or -1 if the server could not be asked. Nothing is sent to the server and no data is built here.
	public static int collectRequests( File historyRoot, Mailbox mailbox, long nowMillis ){
		EvidenceInbox.prune(historyRoot, nowMillis);
		Mailbox.Fetched f = mailbox.fetch();
		if (!f.ok) return -1;
		int added = 0;
		for (com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope e : f.items){
			EvidenceMessages.Asked asked = EvidenceMessages.askedFromEnvelope(e);
			if (asked != null && EvidenceInbox.add(historyRoot, asked, nowMillis)) added++;
			com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages.Offer offer = com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages.offerFromEnvelope(e);
			if (offer != null) VoteActions.store(historyRoot, offer);
		}
		return added;
	}

	//Sends the queue on a background thread (never on the game thread) and reports once. Without sending on and a secure address it does
	//nothing but say so.
	public static void sendNow( final File historyRoot, boolean sendingOn, String address, final Done done ){
		final Gateway gateway = Gateways.choose(sendingOn, address, new SessionStore(historyRoot).tokenSource());
		if (!sendingOn || Gateways.cleanBaseUrl(address) == null){
			done.finished(status(sendingOn, address));
			return;
		}
		String queueProblem = SubmissionQueue.readProblem(historyRoot);
		if (queueProblem != null){
			done.finished(queueProblem + " Nothing was sent.");
			return;
		}
		Thread t = new Thread(new Runnable(){
			@Override
			public void run(){
				String msg;
				try {
					Outbox.FlushResult r = SubmissionQueue.flush(historyRoot, gateway, System.currentTimeMillis(), true);
					msg = describe(r, SubmissionQueue.pending(historyRoot));
					String problem = SubmissionQueue.readProblem(historyRoot);
					if (problem != null) msg += " " + problem;
					else if (gateway instanceof Mailbox){
						int added = collectRequests(historyRoot, (Mailbox) gateway, System.currentTimeMillis());
						if (added > 0) msg += " " + added + " new request" + (added == 1 ? "" : "s") + " for more information; see Evidence requests.";
					}
				} catch (Throwable e){
					msg = "Sending failed; the queue is unchanged.";
				}
				done.finished(msg);
			}
		}, "apd-send");
		t.setDaemon(true);
		t.start();
	}
}
