package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.LegacyImporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.SeedHistory;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.noosa.Scene;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Shared post-selection notice for entered seeds and the ranking page's Copy action. */
public final class SeedHistoryNotice {
    private SeedHistoryNotice() {}

    private static volatile long generation;
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(1), runnable -> {
                Thread worker = new Thread(runnable, "APD seed history");
                worker.setDaemon(true);
                return worker;
            }, new ThreadPoolExecutor.DiscardOldestPolicy());

    /** A new entry/copy dialog supersedes a pending notice even before its selection changes. */
    public static void cancel() {
        ++generation;
    }

    /** Called on the render thread after the existing selection logic has stored the seed. */
    public static void selected(final String seedText) {
        final long request = ++generation;
        if (seedText == null || seedText.isEmpty()) return;
        final Scene scene = Game.scene();
        final File root = ApdRecorder.historyRoot();
        WORKER.execute(() -> {
            if (request != generation) return;
            final SeedHistory.Summary summary = SeedHistory.lookup(root, seedText,
                    LegacyImporter.extractRankingsFile(System.currentTimeMillis()));
            Game.runOnRenderThread(() -> {
                if (request != generation || Game.scene() != scene || !seedText.equals(SPDSettings.customSeed())) return;
                scene.addToFront(new WndTitledMessage(Icons.get(Icons.SEED),
                        Messages.get(HeroSelectScene.class, "seed_history_title"), text(summary)));
            });
        });
    }

    public static String text(SeedHistory.Summary summary) {
        if (summary.kind == SeedHistory.Kind.GENERATED_LOCALLY) {
            return Messages.get(HeroSelectScene.class, "seed_history_generated");
        }
        if (summary.kind == SeedHistory.Kind.UNKNOWN_IMPORTED) {
            return Messages.get(HeroSelectScene.class, "seed_history_unknown");
        }
        String depth = summary.deepestFloor < 0
                ? Messages.get(HeroSelectScene.class, "seed_history_depth_unknown")
                : Messages.get(HeroSelectScene.class, "seed_history_depth", summary.deepestFloor);
        if (summary.unfinishedAttempts > 0) {
            depth = Messages.get(HeroSelectScene.class, "seed_history_unfinished",
                    summary.attempts - summary.unfinishedAttempts, summary.unfinishedAttempts) + "\n" + depth;
        }
        String text = Messages.get(HeroSelectScene.class, "seed_history_match", summary.attempts, depth);
        if (summary.attemptsWithKnownDepth < summary.attempts) {
            text += "\n\n" + Messages.get(HeroSelectScene.class, "seed_history_depth_partial",
                    summary.attemptsWithKnownDepth, summary.attempts);
        }
        if (summary.incompleteHistory) text += "\n\n" + Messages.get(HeroSelectScene.class, "seed_history_incomplete");
        return text;
    }
}
