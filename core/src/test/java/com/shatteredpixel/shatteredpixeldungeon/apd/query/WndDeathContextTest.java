package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDeathContext;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class WndDeathContextTest {

    private static QueryResult result(DatasetDef def, Row row) {
        row.finished = true;
        row.ruleset = "rules";
        Dataset dataset = new Dataset(def, Arrays.asList(row), new ArrayList<Dataset.RunInfo>());
        return QueryEngine.run(dataset, Query.of(def.id), "rules");
    }

    @Test
    public void presetsAreBoundedAndReturnedDefensively() {
        assertArrayEquals(new int[]{5, 10, 25, 50, 100}, WndDeathContext.presetWindows());
        int[] changed = WndDeathContext.presetWindows();
        changed[0] = 999;
        assertArrayEquals(new int[]{5, 10, 25, 50, 100}, WndDeathContext.presetWindows());
        for (int turns : WndDeathContext.presetWindows()) {
            assertTrue(turns >= 1);
            assertTrue(turns <= DeathContext.MAX_WINDOW_TURNS);
        }
    }

    @Test
    public void customWindowAcceptsOnlyTheDocumentedRange() {
        assertEquals(1, WndDeathContext.parseWindow(" 1 "));
        assertEquals(1000, WndDeathContext.parseWindow("1000"));
        assertEquals(25, WndDeathContext.parseWindow("025"));
        for (String bad : new String[]{null, "", "0", "-1", "1001", "1.5", "turns", "999999999999"}) {
            assertEquals(bad, -1, WndDeathContext.parseWindow(bad));
        }
    }

    @Test
    public void actionEligibilityIsOnlyForIndividualRecordedDeathRows() {
        DatasetDef deaths = Datasets.deaths();
        Row death = new Row("death", null, "Run-A", deaths.size());
        death.setNum(deaths.indexOf("death.turn"), 9, Fact.PROV_RECORDED);
        QueryResult deathResult = result(deaths, death);
        assertTrue(WndDeathContext.canOpen(deathResult, death));

        DatasetDef attacks = EventDatasets.attacks();
        Row attack = new Row("attack", null, "Run-A", attacks.size());
        attack.setNum(attacks.indexOf("attack.turn"), 8, Fact.PROV_RECORDED);
        assertFalse(WndDeathContext.canOpen(result(attacks, attack), attack));
        assertFalse(WndDeathContext.canOpen(deathResult, null));

        Row imported = new Row("imported", null, "", deaths.size());
        assertFalse(WndDeathContext.canOpen(deathResult, imported));
    }

    @Test
    public void nativeRowActionUsesTheVerifiedModelAndKeepsUnknownsVisible() throws Exception {
        File root = new File(System.getProperty("apd.game.src"));
        String row = new String(Files.readAllBytes(new File(root, "windows/WndUimsdRow.java").toPath()), "UTF-8");
        String window = new String(Files.readAllBytes(new File(root, "windows/WndDeathContext.java").toPath()), "UTF-8");

        assertTrue(row.contains("WndDeathContext.canOpen(r,row)"));
        assertTrue(row.contains("new Action(\"Death context\""));
        assertTrue(row.contains("WndDeathContext.show(m,row)"));
        assertTrue(window.contains("new DeathContext(catalog, death, turns)"));
        assertTrue(window.contains("Empty turns and older missing details stay unknown"));
        assertTrue(window.contains("does not prove survival or causation"));
        assertFalse(window.contains("Dungeon.hero"));
        assertFalse(window.contains("Dungeon.level"));
        assertFalse(window.contains("Random."));
    }
}
