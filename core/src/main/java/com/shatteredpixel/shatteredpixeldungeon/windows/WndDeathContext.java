package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DeathContext;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;

import java.util.ArrayList;
import java.util.List;

/** Native read-only entry point for the recorded context surrounding one hero death. */
public final class WndDeathContext {

    private static final int[] PRESETS = {5, 10, 25, 50, 100};

    private WndDeathContext() {
    }

    public static int[] presetWindows() {
        return PRESETS.clone();
    }

    public static int parseWindow(String value) {
        if (value == null) return -1;
        try {
            int turns = Integer.parseInt(value.trim());
            return turns >= 1 && turns <= DeathContext.MAX_WINDOW_TURNS ? turns : -1;
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }

    public static boolean canOpen(QueryResult result, Row row) {
        return result != null && row != null && !result.isGrouped()
                && Datasets.DEATHS.equals(result.def.id)
                && row.runId != null && !row.runId.isEmpty();
    }

    public static void show(final UimsdModel model, final Row death) {
        if (model == null || model.catalog() == null || death == null) {
            tell("Recorded history is not ready.");
            return;
        }

        final Catalog catalog = model.catalog();
        final List<WndUimsdList.Entry> entries = new ArrayList<>();
        for (int turns : PRESETS) {
            entries.add(WndUimsdList.Entry.item(
                    "Last " + turns + " turns",
                    "Ends on the recorded death turn. Shows recorded public observations only."));
        }
        entries.add(WndUimsdList.Entry.item(
                "Custom final-turn window...",
                "Choose 1-" + DeathContext.MAX_WINDOW_TURNS + " turns ending on the death."));

        WndUimsdList menu = WndUimsdList.menu(
                "Death context",
                "Choose how far back to look. Empty turns and older missing details stay unknown; this does not prove survival or causation.",
                entries,
                (index, checked) -> {
                    if (model.catalog() != catalog) {
                        tell("History changed. Reopen the death row for current evidence.");
                    } else if (index >= 0 && index < PRESETS.length) {
                        present(model, catalog, death, PRESETS[index]);
                    } else if (index == PRESETS.length) {
                        custom(model, catalog, death);
                    }
                });
        ShatteredPixelDungeon.scene().addToFront(menu.restoreWith(() -> {
            if (model.catalog() == catalog) show(model, death);
            else tell("History changed. Reopen the death row for current evidence.");
        }));
    }

    private static void custom(final UimsdModel model, final Catalog catalog, final Row death) {
        ShatteredPixelDungeon.scene().addToFront(new WndTextInput(
                "Death context window",
                "Positive whole number from 1 to " + DeathContext.MAX_WINDOW_TURNS + ". The window ends on the recorded death.",
                "25", 4, false, "Open", "Cancel") {
            @Override
            public void onSelect(boolean confirmed, String value) {
                if (!confirmed) {
                    if (model.catalog() == catalog) show(model, death);
                    return;
                }
                int turns = parseWindow(value);
                if (turns < 1) {
                    tell("Enter a whole number from 1 to " + DeathContext.MAX_WINDOW_TURNS + ".");
                    return;
                }
                present(model, catalog, death, turns);
            }
        });
    }

    private static void present(UimsdModel model, Catalog catalog, Row death, int turns) {
        if (model.catalog() != catalog) {
            tell("History changed. Reopen the death row for current evidence.");
            return;
        }
        final DeathContext context;
        try {
            context = new DeathContext(catalog, death, turns);
        } catch (IllegalArgumentException invalid) {
            tell("This death record cannot be opened.");
            return;
        }
        ShatteredPixelDungeon.scene().addToFront(
                new WndUimsdText("Death context - last " + turns + " turns", context.summary(), false));
    }

    private static void tell(String message) {
        ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("Death context", message, false));
    }
}
