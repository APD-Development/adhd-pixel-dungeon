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

import com.shatteredpixel.shatteredpixeldungeon.apd.report.Report;

import java.io.File;
import java.util.List;

// A report the player can read, share or keep that shows exactly what APD holds for sending: the plain-language summary, each queued item as
// it would be sent, what has already been sent, and the evidence requests waiting. It is built only from local files and sends nothing.
public final class PrivacyReport {

	private PrivacyReport(){}

	private static final int EXACT = 0;

	public static Report build( File historyRoot, boolean sendingOn, long now, String appVersion ){
		Report r = new Report("What APD holds for sending", now, appVersion);
		r.about.add("Local files only. Nothing in this report was sent anywhere by making it.");
		r.add(new Report.Heading(1, "Summary"));
		for (String line : PrivacySummary.of(historyRoot).lines(sendingOn)) r.add(new Report.Note(Report.NoteKind.INFO, line));

		Report.Table queued = new Report.Table("Queued to send", Report.Provenance.RECORDED);
		queued.headers.add("Run");
		queued.headers.add("Exactly what would be sent");
		queued.coverage.add("");
		queued.coverage.add("");
		for (SubmissionQueue.Entry e : SubmissionQueue.entries(historyRoot)){
			queued.rows.add(new Report.Cell[]{ new Report.Cell(e.id, EXACT), new Report.Cell(e.text.replace('\n', ' ').trim(), EXACT, true) });
		}
		r.add(queued);

		Report.Table sent = new Report.Table("Already sent from this device", Report.Provenance.RECORDED);
		sent.headers.add("Run");
		sent.headers.add("Digest of its log when sent");
		sent.headers.add("Sent at (ms since 1970)");
		for (int i = 0; i < 3; i++) sent.coverage.add("");
		for (SubmittedLedger.Entry e : SubmittedLedger.list(historyRoot)){
			sent.rows.add(new Report.Cell[]{ new Report.Cell(e.runId, EXACT), new Report.Cell(e.digestHex, EXACT, true), new Report.Cell(String.valueOf(e.sentAtMillis), EXACT) });
		}
		r.add(sent);

		Report.Table asked = new Report.Table("Evidence requests waiting", Report.Provenance.RECORDED);
		asked.headers.add("Run");
		asked.headers.add("Asked for");
		asked.headers.add("Deadline (ms since 1970)");
		for (int i = 0; i < 3; i++) asked.coverage.add("");
		List<EvidenceInbox.Entry> inbox = EvidenceInbox.list(historyRoot);
		for (EvidenceInbox.Entry e : inbox){
			asked.rows.add(new Report.Cell[]{ new Report.Cell(e.asked.runId, EXACT), new Report.Cell(e.asked.request.kind.name(), EXACT), new Report.Cell(String.valueOf(e.asked.request.deadlineMillis), EXACT) });
		}
		r.add(asked);
		return r;
	}
}
