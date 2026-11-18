package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class DeathContextTest {

    private static final class Fixture {
        final Catalog catalog;
        final Row death;
        final DatasetDef attacks;

        Fixture(Catalog catalog, Row death, DatasetDef attacks) {
            this.catalog = catalog;
            this.death = death;
            this.attacks = attacks;
        }
    }

    private Fixture fixture(String runId, Long deathTurn, Long revived, Long ended) {
        Catalog catalog = new Catalog();

        DatasetDef runs = Datasets.runs();
        Row run = new Row("run-" + runId, null, runId, runs.size());
        run.setNum(runs.indexOf(FactKeys.TURNS), 30, Fact.PROV_RECORDED);
        catalog.add(new Dataset(runs, Arrays.asList(run), new ArrayList<Dataset.RunInfo>()));

        DatasetDef deaths = Datasets.deaths();
        Row death = new Row("death-" + runId, null, runId, deaths.size());
        death.setText(deaths.indexOf("death.cause"), "Gnoll brute", Fact.PROV_RECORDED);
        if (deathTurn != null) death.setNum(deaths.indexOf("death.turn"), deathTurn, Fact.PROV_RECORDED);
        if (revived != null) death.setNum(deaths.indexOf("death.revived"), revived, Fact.PROV_RECORDED);
        if (ended != null) death.setNum(deaths.indexOf("death.final"), ended, Fact.PROV_RECORDED);
        catalog.add(new Dataset(deaths, Arrays.asList(death), new ArrayList<Dataset.RunInfo>()));

        DatasetDef attacks = EventDatasets.attacks();
        return new Fixture(catalog, death, attacks);
    }

    private Row attack(DatasetDef def, String id, String runId, long turn) {
        Row row = new Row(id, null, runId, def.size());
        row.setNum(def.indexOf("attack.turn"), turn, Fact.PROV_RECORDED);
        row.setText(def.indexOf("attack.attacker"), "Gnoll brute", Fact.PROV_RECORDED);
        return row;
    }

    @Test
    public void finalDeathUsesExactBoundedChronology() {
        Fixture f = fixture("one", 20L, 0L, 1L);
        f.catalog.add(new Dataset(f.attacks, Arrays.asList(
                attack(f.attacks, "before", "one", 17),
                attack(f.attacks, "inside", "one", 18),
                attack(f.attacks, "other", "two", 19)
        ), new ArrayList<Dataset.RunInfo>()));

        DeathContext context = new DeathContext(f.catalog, f.death, 3);

        assertTrue(context.available());
        assertEquals(18, context.startTurn);
        assertEquals(20, context.endTurn);
        assertEquals(DeathContext.Outcome.FINAL, context.outcome);
        assertEquals(2, context.observedRecords());
        assertEquals(2, context.recordedTurns());
        assertEquals(18, context.entries().get(0).turn);
        assertFalse(context.entries().get(0).death);
        assertEquals(20, context.entries().get(1).turn);
        assertTrue(context.entries().get(1).death);
        assertTrue(context.summary().contains("This death ended the run."));
        assertTrue(context.summary().contains("does not prove survival, causation"));
    }

    @Test
    public void revivedDeathIsNeverPresentedAsRunEnding() {
        Fixture f = fixture("revived", 12L, 1L, 0L);

        DeathContext context = new DeathContext(f.catalog, f.death, 5);

        assertEquals(DeathContext.Outcome.REVIVED, context.outcome);
        assertTrue(context.summary().contains("Revived after this death."));
        assertFalse(context.summary().contains("This death ended the run."));
    }

    @Test
    public void missingOlderTurnFailsClosedWithoutInventingContext() {
        Fixture f = fixture("old", null, null, 1L);

        DeathContext context = new DeathContext(f.catalog, f.death, 10);

        assertFalse(context.available());
        assertEquals(0, context.deathTurn);
        assertTrue(context.entries().isEmpty());
        assertTrue(context.summary().contains("does not contain a death turn"));
        assertTrue(context.summary().contains("no timeline was reconstructed"));
    }

    @Test
    public void windowClampsAtRunStartAndRejectsUnboundedRequests() {
        Fixture f = fixture("early", 2L, 0L, 1L);
        DeathContext context = new DeathContext(f.catalog, f.death, 5);
        assertEquals(1, context.startTurn);
        assertEquals(2, context.endTurn);

        try {
            new DeathContext(f.catalog, f.death, 0);
            fail("zero window accepted");
        } catch (IllegalArgumentException expected) {
            assertEquals("windowTurns", expected.getMessage());
        }
        try {
            new DeathContext(f.catalog, f.death, DeathContext.MAX_WINDOW_TURNS + 1);
            fail("oversized window accepted");
        } catch (IllegalArgumentException expected) {
            assertEquals("windowTurns", expected.getMessage());
        }
    }

    @Test
    public void runIdentityIsCaseSensitiveAndNonFinalOutcomeStaysQualified() {
        Fixture f = fixture("Case", 7L, 0L, 0L);
        f.catalog.add(new Dataset(f.attacks, Arrays.asList(
                attack(f.attacks, "wrong-case", "case", 6)
        ), new ArrayList<Dataset.RunInfo>()));

        DeathContext context = new DeathContext(f.catalog, f.death, 3);

        assertEquals(DeathContext.Outcome.NON_FINAL, context.outcome);
        assertEquals(1, context.observedRecords());
        assertTrue(context.entries().get(0).death);
        assertTrue(context.summary().contains("Recorded as non-final"));
        assertTrue(context.summary().contains("no revival is established"));
    }

    @Test
    public void exactFloorAndEquipmentTransitionsAppearInsideDeathWindow() {
        Fixture f = fixture("timeline", 20L, 0L, 1L);

        DatasetDef equipment = EventDatasets.equipment();
        Row gear = new Row("gear", null, "timeline", equipment.size());
        gear.setText(equipment.indexOf("equip.item"), "Mail", Fact.PROV_RECORDED);
        gear.setNum(equipment.indexOf("equip.start"), 17, Fact.PROV_RECORDED);
        gear.setNum(equipment.indexOf("equip.end"), 19, Fact.PROV_RECORDED);
        f.catalog.add(new Dataset(equipment, Arrays.asList(gear), new ArrayList<Dataset.RunInfo>()));

        DatasetDef floors = EventDatasets.floors();
        Row visit = new Row("visit", null, "timeline", floors.size());
        visit.setNum(floors.indexOf("floor.depth"), 3, Fact.PROV_RECORDED);
        visit.setNum(floors.indexOf("floor.enterTurn"), 1, Fact.PROV_RECORDED);
        visit.setNum(floors.indexOf("floor.exitTurn"), 18, Fact.PROV_RECORDED);
        visit.setNum(floors.indexOf("floor.exit"), 0, Fact.PROV_RECORDED);
        f.catalog.add(new Dataset(floors, Arrays.asList(visit), new ArrayList<Dataset.RunInfo>()));

        DeathContext context = new DeathContext(f.catalog, f.death, 5);

        assertEquals(4, context.observedRecords());
        assertEquals(4, context.recordedTurns());
        assertTrue(context.summary().contains("Turn 17: Equipment put on: Item Mail"));
        assertTrue(context.summary().contains("Turn 18: Floor left"));
        assertTrue(context.summary().contains("Turn 19: Equipment taken off: Item Mail"));
        assertTrue(context.summary().contains("Turn 20: Death: killed by Gnoll brute"));
    }
}
