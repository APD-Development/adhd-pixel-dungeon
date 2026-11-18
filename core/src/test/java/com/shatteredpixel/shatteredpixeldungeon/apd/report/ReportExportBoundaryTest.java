package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class ReportExportBoundaryTest {

	private static final File FALLBACK = new File(".");

	private static String repeat( char value, int count ){
		char[] out = new char[count];
		java.util.Arrays.fill(out, value);
		return new String(out);
	}

	private static Report reportWithRenderedTextLength( int target ){
		Report emptyNote = new Report("x", 0, "test")
				.add(new Report.Note(Report.NoteKind.INFO, ""));
		Report oneCharacterNote = new Report("x", 0, "test")
				.add(new Report.Note(Report.NoteKind.INFO, "x"));
		int framingLength = ReportRenderer.text(emptyNote).length();
		assertEquals("note body must contribute one character per input character",
				1, ReportRenderer.text(oneCharacterNote).length() - framingLength);
		int noteLength = target - framingLength;
		assertTrue("target must leave room for renderer framing", noteLength > 0);
		Report report = new Report("x", 0, "test")
				.add(new Report.Note(Report.NoteKind.INFO, repeat('x', noteLength)));
		assertEquals(target, ReportRenderer.text(report).length());
		return report;
	}

	private static abstract class Provider implements ApdFiles.Provider {
		@Override
		public void chooseImport( ApdFiles.ImportSource source ){
			throw new AssertionError("no import expected");
		}

		@Override
		public String where(){
			return "test";
		}
	}

	@Test
	public void fileNamesUseUtcAndSafeFallbackSlugs(){
		assertEquals("apd-cr-me-19700101-0000.md",
				ReportExport.fileName(new Report("Crème / 日本語", 0, "test"),
						ReportExport.Format.MARKDOWN));
		assertEquals("apd-report-19700101-0000.txt",
				ReportExport.fileName(new Report("日本語", 0, "test"),
						ReportExport.Format.TEXT));
		assertEquals("apd-a-b-19700101-0000.csv",
				ReportExport.fileName(new Report(" A---B ", 0, "test"),
						ReportExport.Format.CSV));
	}

	@Test
	public void shareAcceptsExactLimitAndRejectsOneCharacterMore(){
		final int[] shares = {0};
		final String[] sharedText = {null};
		ApdFiles.Provider old = ApdFiles.provider;
		try {
			ApdFiles.provider = new Provider(){
				@Override
				public void chooseExport( String name, ApdFiles.ExportTarget target ){
					throw new AssertionError("no export expected");
				}

				@Override
				public boolean shareText( String subject, String text ){
					shares[0]++;
					sharedText[0] = text;
					return true;
				}
			};

			final String[] message = {null};
			ReportExport.share(FALLBACK,
					reportWithRenderedTextLength(ReportExport.SHARE_LIMIT),
					value -> message[0] = value);
			assertEquals(1, shares[0]);
			assertNotNull(sharedText[0]);
			assertEquals(ReportExport.SHARE_LIMIT, sharedText[0].length());
			assertEquals("Choose where to send the report.", message[0]);

			ReportExport.share(FALLBACK,
					reportWithRenderedTextLength(ReportExport.SHARE_LIMIT + 1),
					value -> message[0] = value);
			assertEquals("oversize text must never reach the platform share provider", 1, shares[0]);
			assertEquals("This report is too long to share as text (100001 characters). Save it as a file instead.",
					message[0]);
		} finally {
			ApdFiles.provider = old;
		}
	}

	@Test
	public void saveReportsPickerCancellationAndFailureExactlyOnce(){
		ApdFiles.Provider old = ApdFiles.provider;
		try {
			final int[] callbacks = {0};
			final String[] message = {null};
			ApdFiles.provider = new Provider(){
				@Override
				public void chooseExport( String name, ApdFiles.ExportTarget target ){
					target.onCancelled();
				}
			};
			ReportExport.save(FALLBACK, new Report("Test", 0, "test"),
					ReportExport.Format.TEXT, value -> {
						callbacks[0]++;
						message[0] = value;
					});
			assertEquals(1, callbacks[0]);
			assertEquals("Cancelled. Nothing was saved.", message[0]);

			callbacks[0] = 0;
			ApdFiles.provider = new Provider(){
				@Override
				public void chooseExport( String name, ApdFiles.ExportTarget target ){
					target.onFailed("blocked");
				}
			};
			ReportExport.save(FALLBACK, new Report("Test", 0, "test"),
					ReportExport.Format.TEXT, value -> {
						callbacks[0]++;
						message[0] = value;
					});
			assertEquals(1, callbacks[0]);
			assertEquals("The report could not be saved: blocked", message[0]);
		} finally {
			ApdFiles.provider = old;
		}
	}
}
