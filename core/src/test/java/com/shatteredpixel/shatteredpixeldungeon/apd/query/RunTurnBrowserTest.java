package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class RunTurnBrowserTest {
    private RunTurnBrowser example() {
        Catalog cat=new Catalog();
        DatasetDef runs=Datasets.runs();
        Row run=new Row("run-one",null,"id-one",runs.size());
        run.setNum(runs.indexOf(FactKeys.TURNS),5000,Fact.PROV_RECORDED);
        cat.add(new Dataset(runs,Arrays.asList(run),new ArrayList<>()));
        DatasetDef def=EventDatasets.attacks();
        Row r=new Row("event-one",null,"id-one",def.size());
        r.setNum(def.indexOf("attack.turn"),21,Fact.PROV_RECORDED);
        int attacker=def.indexOf("attack.attacker");
        if(attacker>=0)r.setText(attacker,"Rat",Fact.PROV_RECORDED);
        cat.add(new Dataset(def,Arrays.asList(r),new ArrayList<>()));
        return new RunTurnBrowser(cat,run);
    }
    @Test public void startsAtOneAndPreservesSparseEmptyTurns(){RunTurnBrowser b=example();assertEquals(1,b.turn());assertTrue(b.events().isEmpty());assertEquals(5000,b.lastTurn);}
    @Test public void correctTurnAndCustomNavigation(){RunTurnBrowser b=example();b.go(21);assertEquals(1,b.events().size());b.step(100);b.next();assertEquals(121,b.turn());b.previous();assertEquals(21,b.turn());b.go(999999);assertEquals(5000,b.turn());}
    @Test public void zeroStepRejected(){RunTurnBrowser b=example();try{b.step(0);fail();}catch(IllegalArgumentException expected){}}
    @Test public void importedRunWithoutEventsIsExplicitlyEmpty(){Catalog cat=new Catalog();DatasetDef d=Datasets.runs();Row r=new Row("x",null,"x",d.size());r.imported=true;RunTurnBrowser browser=new RunTurnBrowser(cat,r);assertTrue(browser.events().isEmpty());assertTrue(browser.summary().contains("No recorded event"));}

    @Test public void floorAndEquipmentTransitionsUseExactPublicTurns(){
        Catalog cat=new Catalog();
        DatasetDef runs=Datasets.runs();
        Row run=new Row("timeline",null,"timeline",runs.size());
        run.setNum(runs.indexOf(FactKeys.TURNS),20,Fact.PROV_RECORDED);
        cat.add(new Dataset(runs,Arrays.asList(run),new ArrayList<Dataset.RunInfo>()));

        DatasetDef equipment=EventDatasets.equipment();
        Row gear=new Row("gear",null,"timeline",equipment.size());
        gear.setText(equipment.indexOf("equip.item"),"Sword",Fact.PROV_RECORDED);
        gear.setNum(equipment.indexOf("equip.depth"),2,Fact.PROV_RECORDED);
        gear.setNum(equipment.indexOf("equip.level"),1,Fact.PROV_RECORDED);
        gear.setNum(equipment.indexOf("equip.start"),6,Fact.PROV_RECORDED);
        gear.setNum(equipment.indexOf("equip.end"),10,Fact.PROV_RECORDED);
        Row hidden=new Row("hidden",null,"timeline",equipment.size());
        hidden.publicRow=false;
        hidden.setText(equipment.indexOf("equip.item"),"Secret",Fact.PROV_RECORDED);
        hidden.setNum(equipment.indexOf("equip.start"),7,Fact.PROV_RECORDED);
        cat.add(new Dataset(equipment,Arrays.asList(gear,hidden),new ArrayList<Dataset.RunInfo>()));

        DatasetDef floors=EventDatasets.floors();
        Row visit=new Row("visit",null,"timeline",floors.size());
        visit.setNum(floors.indexOf("floor.depth"),2,Fact.PROV_RECORDED);
        visit.setText(floors.indexOf("floor.entry"),"stairs",Fact.PROV_RECORDED);
        visit.setText(floors.indexOf("floor.levelClass"),"RegularLevel",Fact.PROV_RECORDED);
        visit.setText(floors.indexOf("floor.feeling"),"none",Fact.PROV_RECORDED);
        visit.setNum(floors.indexOf("floor.enterTurn"),1,Fact.PROV_RECORDED);
        visit.setNum(floors.indexOf("floor.exitTurn"),12,Fact.PROV_RECORDED);
        visit.setNum(floors.indexOf("floor.exit"),0,Fact.PROV_RECORDED);
        cat.add(new Dataset(floors,Arrays.asList(visit),new ArrayList<Dataset.RunInfo>()));

        RunTurnBrowser timeline=new RunTurnBrowser(cat,run);
        timeline.go(1);
        assertTrue(timeline.summary().contains("Floor entered"));
        timeline.go(6);
        assertTrue(timeline.summary().contains("Equipment put on: Item Sword"));
        timeline.go(7);
        assertTrue(timeline.events().isEmpty());
        timeline.go(10);
        assertTrue(timeline.summary().contains("Equipment taken off: Item Sword"));
        timeline.go(12);
        assertTrue(timeline.summary().contains("Floor left"));
    }

    @Test public void openIntervalsDoNotInventEquipmentOrFloorExitTurns(){
        Catalog cat=new Catalog();
        DatasetDef runs=Datasets.runs();
        Row run=new Row("open",null,"open",runs.size());
        run.setNum(runs.indexOf(FactKeys.TURNS),50,Fact.PROV_RECORDED);
        cat.add(new Dataset(runs,Arrays.asList(run),new ArrayList<Dataset.RunInfo>()));

        DatasetDef equipment=EventDatasets.equipment();
        Row gear=new Row("gear-open",null,"open",equipment.size());
        gear.setText(equipment.indexOf("equip.item"),"Staff",Fact.PROV_RECORDED);
        gear.setNum(equipment.indexOf("equip.start"),5,Fact.PROV_RECORDED);
        cat.add(new Dataset(equipment,Arrays.asList(gear),new ArrayList<Dataset.RunInfo>()));

        DatasetDef floors=EventDatasets.floors();
        Row visit=new Row("visit-open",null,"open",floors.size());
        visit.setNum(floors.indexOf("floor.depth"),1,Fact.PROV_RECORDED);
        visit.setNum(floors.indexOf("floor.enterTurn"),1,Fact.PROV_RECORDED);
        cat.add(new Dataset(floors,Arrays.asList(visit),new ArrayList<Dataset.RunInfo>()));

        RunTurnBrowser timeline=new RunTurnBrowser(cat,run);
        assertEquals(2,timeline.recordedTurns());
        timeline.go(5);
        assertEquals(1,timeline.events().size());
        timeline.go(50);
        assertTrue(timeline.events().isEmpty());
        assertFalse(timeline.summary().contains("taken off"));
        assertFalse(timeline.summary().contains("Floor left"));
    }

    @Test public void exactOccurrenceBridgeOpensCanonicalClockForEventIntervalAndDeathRows(){
        Catalog cat=new Catalog();
        DatasetDef runs=Datasets.runs();
        Row run=new Row("recorded",null,"A",runs.size());
        run.setNum(runs.indexOf(FactKeys.TURNS),30,Fact.PROV_RECORDED);
        cat.add(new Dataset(runs,Arrays.asList(run),new ArrayList<Dataset.RunInfo>()));

        DatasetDef attacks=EventDatasets.attacks();
        Row attack=new Row("attack",null,"A",attacks.size());
        attack.setNum(attacks.indexOf("attack.turn"),21,Fact.PROV_RECORDED);
        attack.setText(attacks.indexOf("attack.attacker"),"Rat",Fact.PROV_RECORDED);
        cat.add(new Dataset(attacks,Arrays.asList(attack),new ArrayList<Dataset.RunInfo>()));

        DatasetDef equipment=EventDatasets.equipment();
        Row gear=new Row("gear",null,"A",equipment.size());
        gear.setText(equipment.indexOf("equip.item"),"Sword",Fact.PROV_RECORDED);
        gear.setNum(equipment.indexOf("equip.start"),7,Fact.PROV_RECORDED);
        cat.add(new Dataset(equipment,Arrays.asList(gear),new ArrayList<Dataset.RunInfo>()));

        DatasetDef floors=EventDatasets.floors();
        Row floor=new Row("floor",null,"A",floors.size());
        floor.setNum(floors.indexOf("floor.enterTurn"),5,Fact.PROV_RECORDED);
        cat.add(new Dataset(floors,Arrays.asList(floor),new ArrayList<Dataset.RunInfo>()));

        DatasetDef deaths=Datasets.deaths();
        Row death=new Row("death",null,"A",deaths.size());
        death.setNum(deaths.indexOf("death.turn"),9,Fact.PROV_RECORDED);
        cat.add(new Dataset(deaths,Arrays.asList(death),new ArrayList<Dataset.RunInfo>()));

        assertOpenAt(cat,EventDatasets.ATTACKS,attack,21);
        assertOpenAt(cat,EventDatasets.EQUIPMENT,gear,7);
        assertOpenAt(cat,EventDatasets.FLOORS,floor,5);
        assertOpenAt(cat,Datasets.DEATHS,death,9);
    }

    @Test public void occurrenceBridgeFailsClosedForUncertainHiddenStaleMissingAndAmbiguousOwnership(){
        Catalog cat=new Catalog();
        DatasetDef runs=Datasets.runs();
        Row run=new Row("recorded",null,"A",runs.size());
        cat.add(new Dataset(runs,Arrays.asList(run),new ArrayList<Dataset.RunInfo>()));
        DatasetDef attacks=EventDatasets.attacks();
        Row attack=new Row("attack",null,"A",attacks.size());
        attack.setNum(attacks.indexOf("attack.turn"),4,Fact.PROV_RECORDED);
        cat.add(new Dataset(attacks,Arrays.asList(attack),new ArrayList<Dataset.RunInfo>()));
        QueryResult source=result(cat,EventDatasets.ATTACKS);
        assertTrue(RunTurnBrowser.canOpenAt(cat,source,attack));

        attack.setNum(attacks.indexOf("attack.turn"),4,Fact.PROV_INCOMPLETE);
        assertFalse(RunTurnBrowser.canOpenAt(cat,source,attack));
        attack.setNum(attacks.indexOf("attack.turn"),4,Fact.PROV_RECORDED);
        attack.publicRow=false;
        assertFalse(RunTurnBrowser.canOpenAt(cat,source,attack));
        attack.publicRow=true;
        attack.imported=true;
        assertFalse(RunTurnBrowser.canOpenAt(cat,source,attack));
        attack.imported=false;
        assertFalse(RunTurnBrowser.canOpenAt(cat,source,new Row("stale",null,"A",attacks.size())));
        assertFalse(RunTurnBrowser.canOpenAt(new Catalog(),source,attack));

        Row second=new Row("second",null,"A",runs.size());
        cat.add(new Dataset(runs,Arrays.asList(run,second),new ArrayList<Dataset.RunInfo>()));
        assertFalse(RunTurnBrowser.canOpenAt(cat,result(cat,EventDatasets.ATTACKS),attack));
        try{RunTurnBrowser.openAt(cat,result(cat,EventDatasets.ATTACKS),attack);fail();}
        catch(IllegalArgumentException expected){}
    }

    private static QueryResult result(Catalog cat,String dataset){
        return QueryEngine.run(cat.dataset(dataset),Query.of(dataset)
                .withScope(new Scope(Scope.Rules.ALL,"",true,false,true,false)),"");
    }

    private static void assertOpenAt(Catalog cat,String dataset,Row row,long turn){
        QueryResult source=result(cat,dataset);
        assertTrue(RunTurnBrowser.canOpenAt(cat,source,row));
        RunTurnBrowser browser=RunTurnBrowser.openAt(cat,source,row);
        assertEquals("A",browser.runId);
        assertEquals(turn,browser.turn());
    }
}
