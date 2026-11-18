package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

//Getting a Report out of the app: as text handed to the system share sheet, or as a file the player saves wherever they choose (the same
//file picker the history archive uses). Formats are plain text files on purpose: they open on every device, need no library, and a web page
//can be printed to PDF from any browser. A native PDF writer would add a layout and font engine for one platform only (see
//docs/APD-REPORTS.md).
public final class ReportExport {

	private ReportExport(){}

	public enum Format {
		MARKDOWN("Markdown", "md", "text/markdown"),
		TEXT("Plain text", "txt", "text/plain"),
		HTML("Web page (print to PDF from a browser)", "html", "text/html"),
		CSV("Spreadsheet (CSV, tables only)", "csv", "text/csv");

		public final String label, extension, mime;

		Format( String label, String extension, String mime ){
			this.label = label;
			this.extension = extension;
			this.mime = mime;
		}
	}

	//the longest text the share sheet is given (larger reports are saved as a file instead)
	public static final int SHARE_LIMIT = 100000;

	public interface Done {
		void finished( String message );
	}

	public static String render( Report r, Format f ){
		switch (f){
			case TEXT: return ReportRenderer.text(r);
			case HTML: return ReportRenderer.html(r);
			case CSV: return ReportRenderer.csv(r);
			default: return ReportRenderer.markdown(r);
		}
	}

	// Keep the entire suggested filename below common 255-byte filesystem component limits.
	// Slugs contain only ASCII, leaving room for "apd-", the UTC timestamp and every extension.
	private static final int MAX_SLUG_LENGTH = 180;

	static String slug( String title ){
		StringBuilder sb = new StringBuilder();
		for (char c : title.toLowerCase(Locale.ROOT).toCharArray()){
			if (sb.length() >= MAX_SLUG_LENGTH) break;
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) sb.append(c);
			else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') sb.append('-');
		}
		while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') sb.setLength(sb.length() - 1);
		return sb.length() == 0 ? "report" : sb.toString();
	}

	public static String fileName( Report r, Format f ){
		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT);
		df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
		return "apd-" + slug(r.title) + "-" + df.format(new java.util.Date(r.generatedAt)) + "." + f.extension;
	}

    // A picker may hand off a stream before Java rejects thread construction/start.
    // Release the stream and report one failure instead of silently leaking it.
    interface WorkerStarter { void start(Runnable job); }

    static void startSaveWorker(OutputStream stream, Done once, Runnable job, WorkerStarter starter){
        try {
            starter.start(job);
        } catch (OutOfMemoryError exhausted){
            closeFailedStream(stream);
            once.finished("The report could not start saving because memory was exhausted.");
        } catch (RuntimeException failure){
            closeFailedStream(stream);
            once.finished("The report could not start saving: " + failure.getClass().getSimpleName());
        }
    }

    private static void closeFailedStream(OutputStream stream){
        try { stream.close(); } catch (IOException | RuntimeException ignored) {}
    }

    // Saves through the file picker; an accepted stream's worker owns the final outcome.
    public static void save(File fallbackBase, final Report r, final Format f, final Done done){
        final byte[] bytes;
        try {
            bytes = render(r, f).getBytes(Charset.forName("UTF-8"));
        } catch (Throwable e){
            done.finished("The report could not be made: " + e);
            return;
        }
        final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicBoolean streamAccepted = new java.util.concurrent.atomic.AtomicBoolean();
        final Done once = message -> {
            if (finished.compareAndSet(false, true)) done.finished(message);
        };
        try {
            ApdFiles.provider(fallbackBase).chooseExport(fileName(r, f), f.mime, new ApdFiles.ExportTarget(){
                @Override
                public void onStream(final OutputStream out, final String displayName){
                    streamAccepted.set(true);
                    startSaveWorker(out, once, () -> {
                        String message;
                        try (OutputStream target = out){
                            target.write(bytes);
                            target.flush();
                            message = "Saved " + f.label.split(" \\(")[0] + " report to " + displayName + " (" + bytes.length + " bytes).";
                        } catch (IOException | RuntimeException e){
                            message = "The report could not be saved: " + e;
                        } catch (OutOfMemoryError exhausted){
                            message = "The report could not be saved because memory was exhausted during export.";
                        }
                        once.finished(message);
                    }, job -> {
                        Thread worker = new Thread(job, "apd-report-save");
                        worker.setDaemon(true);
                        worker.start();
                    });
                }

                @Override
                public void onCancelled(){
                    if (!streamAccepted.get()) once.finished("Cancelled. Nothing was saved.");
                }

                @Override
                public void onFailed(String why){
                    if (!streamAccepted.get()) once.finished("The report could not be saved: " + why);
                }
            });
        } catch (Throwable e){
            // An exception after stream handoff does not supersede the real save result.
            if (!streamAccepted.get()) once.finished("The report could not be saved: " + e);
        }
    }

	//Shares as plain text through the system share sheet.
	public static void share( File fallbackBase, Report r, Done done ){
		String text;
		try {
			text = ReportRenderer.text(r);
		} catch (Throwable e){
			done.finished("The report could not be made: " + e);
			return;
		}
		if (text.length() > SHARE_LIMIT){
			done.finished("This report is too long to share as text (" + text.length() + " characters). Save it as a file instead.");
			return;
		}
		boolean ok;
		try {
			ok = ApdFiles.provider(fallbackBase).shareText(r.title, text);
		} catch (Throwable e){
			ok = false;
		}
		done.finished(ok ? "Choose where to send the report." : "Sharing is not available here. Save the report as a file instead.");
	}
}
