package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.research.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

/** Real binary recording -> closed file -> catalog -> correlated query -> inspectable report. */
public class EquipmentWindowTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private static final String RULES="rules";
    private static Scope all(){return new Scope(Scope.Rules.ALL,"",false,false,false,false);}
    private SyntheticLog log(String id){return new SyntheticLog(tmp.getRoot(),id,TelemetryVersion.CURRENT,100).start(id,0,"WARRIOR",RULES,0,30,false).enter(1);}
    private Catalog recorded(){
        SyntheticLog s=log("A");s.clock=10;s.equip("armor","Leather armor",ItemEvents.CAT_ARMOR);
        s.clock=10;s.damageToHero("rat","Rat",100); // same turn, excluded
        s.clock=11;s.damageToHero("rat","Rat",4);s.clock=20;s.damageToHero("fire","Fire",3);
        s.clock=20;s.damageFromHero("rat","Rat",200);s.clock=21;s.damageToHero("rat","Rat",9);
        s.clock=30;s.end(LifecycleEvents.OUTCOME_DIED,"rat",1,1,10).close(true);
        log("a").equip("armor","Leather armor",ItemEvents.CAT_ARMOR).damageToHero("rat","Rat",99).end(LifecycleEvents.OUTCOME_ABANDONED,"",1,1,10).close(true);
        return new DatasetStore(tmp.getRoot()).load();
    }
    private QueryResult anchors(Catalog c){return ResearchData.select(c,Query.of(EventDatasets.EQUIPMENT).withScope(all()).addFilter(Filter.text("equip.run",Op.GROUP_TEXT,"A")),RULES);}
    private ResearchDocument answer(Catalog c,int turns){QueryResult a=anchors(c);return EquipmentWindow.answer(c,a,a.rows.get(0),turns,RULES);}
    @Test public void actualRecordingWindowUsesExclusiveStartInclusiveEndAndExactRunIdentity(){
        Catalog c=recorded();ResearchDocument d=answer(c,10);assertEquals(1,d.cards.size());
        ResearchDocument.Card card=d.cards.get(0);assertEquals("7 observed (at least)",card.value);assertEquals(CellText.LOWER,card.state);
        assertTrue(d.notes.get(0).contains("turn 10 through turn 20 inclusive"));
        assertEquals(2,card.evidence.get(1).rows.size());for(Row r:card.evidence.get(1).rows)assertEquals("A",r.runId);
        assertEquals("4 observed (at least)",answer(c,1).cards.get(0).value);
        assertEquals("16 observed (at least)",answer(c,20).cards.get(0).value);
    }
    @Test public void queryRoundTripReloadAndEvidenceLeaveRawHistoryUntouched()throws Exception{
        Catalog c=recorded();java.io.File file=new java.io.File(tmp.getRoot(),"runs/A.apdlog");byte[] before=Files.readAllBytes(file.toPath());
        ResearchDocument.Card card=answer(c,10).cards.get(0);Query evidence=card.evidence.get(1).source.query;
        Query restored=QueryCodec.decode(QueryCodec.encode(evidence));assertEquals(evidence,restored);
        Catalog loaded=new DatasetStore(tmp.getRoot()).load();assertEquals(2,ResearchData.select(loaded,restored,RULES).rows.size());
        assertEquals(card.value,answer(loaded,10).cards.get(0).value);assertArrayEquals(before,Files.readAllBytes(file.toPath()));
        UimsdModel m=new UimsdModel(RULES,"Rules");m.setCatalog(c);m.showQuery(anchors(c).query);Query original=m.query();
        assertTrue(m.openResearchQuery(restored));assertTrue(m.canGoBack());m.back();assertEquals(original,m.query());
    }
    @Test public void noObservationsNeverAssertACompleteZeroAndMissingSchemaHasNoAnswer(){
        Catalog c=recorded();QueryResult a=anchors(c);Row anchor=a.rows.get(0);anchor.setNum(a.def.indexOf("equip.start"),100,Fact.PROV_RECORDED);
        ResearchDocument d=EquipmentWindow.answer(c,a,anchor,10,RULES);assertEquals("0 observed (at least)",d.cards.get(0).value);assertEquals(CellText.LOWER,d.cards.get(0).state);
        assertTrue(d.cards.get(0).detail.contains("not proof that no damage happened"));
        assertTrue(EquipmentWindow.answer(new Catalog(),a,anchor,10,RULES).cards.isEmpty());
        new SyntheticLog(tmp.getRoot(),"old",new TelemetryVersion(1,2),100).start("old",0,"WARRIOR",RULES,0,30,false).enter(1)
                .equip("old-armor","Leather armor",ItemEvents.CAT_ARMOR).end(LifecycleEvents.OUTCOME_DIED,"rat",1,1,10).close(true);
        Catalog legacy=new DatasetStore(tmp.getRoot()).load();QueryResult old=ResearchData.select(legacy,Query.of(EventDatasets.EQUIPMENT).withScope(all()).addFilter(Filter.text("equip.run",Op.GROUP_TEXT,"old")),RULES);
        ResearchDocument unknown=EquipmentWindow.answer(legacy,old,old.rows.get(0),10,RULES);assertTrue(unknown.cards.isEmpty());assertTrue(unknown.notes.get(unknown.notes.size()-1).contains("unknown, not zero"));
    }
    @Test public void uncertainAnchorAndOverflowingWindowFailClosed(){
        Catalog c=recorded();QueryResult a=anchors(c);Row anchor=a.rows.get(0);
        QueryResult unsupported=QueryEngine.run(c.dataset(EventDatasets.EQUIPMENT),a.query.addFilter(Filter.num("missing.field",Op.EQ,1)),RULES);
        assertFalse(unsupported.problems.isEmpty());assertTrue(EquipmentWindow.answer(c,unsupported,anchor,10,RULES).cards.isEmpty());
        anchor.setNum(a.def.indexOf("equip.start"),10,Fact.PROV_INCOMPLETE);assertTrue(EquipmentWindow.answer(c,a,anchor,10,RULES).cards.isEmpty());
        anchor.setNum(a.def.indexOf("equip.start"),Long.MAX_VALUE-2,Fact.PROV_RECORDED);assertTrue(EquipmentWindow.answer(c,a,anchor,10,RULES).cards.isEmpty());
        assertTrue(EquipmentWindow.answer(c,a,anchor,0,RULES).cards.isEmpty());
    }
    @Test public void uncertainDamageAmountsAreExcludedAndExportsRetainInterpretation(){
        Catalog c=recorded();Dataset damage=c.dataset(EventDatasets.DAMAGE);for(Row r:damage.rows)if(r.runId.equals("A")&&r.num(damage.def.indexOf("damage.turn"))==11)r.setNum(damage.def.indexOf("damage.amount"),4,Fact.PROV_INFERRED);
        ResearchDocument d=answer(c,10);assertEquals("3 observed (at least)",d.cards.get(0).value);assertEquals(1,d.cards.get(0).evidence.get(1).rows.size());assertTrue(d.cards.get(0).detail.contains("excluded=1"));
        com.shatteredpixel.shatteredpixeldungeon.apd.report.Report report=d.report(1,"test",false);
        assertFalse(report.blocks.isEmpty());assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportRenderer.csv(report).contains("strictly after"));
    }
    @Test public void windowQueryCostAndStorageAreMeasuredWithoutAppendingFacts()throws Exception{
        Catalog c=recorded();long bytes=0;for(java.io.File f:new java.io.File(tmp.getRoot(),"runs").listFiles())bytes+=f.length();
        long begin=System.nanoTime();for(int i=0;i<1000;i++)assertEquals("7 observed (at least)",answer(c,10).cards.get(0).value);
        long elapsed=System.nanoTime()-begin;long after=0;for(java.io.File f:new java.io.File(tmp.getRoot(),"runs").listFiles())after+=f.length();assertEquals(bytes,after);
        System.out.println("EQUIPMENT_WINDOW_PROBE iterations=1000 damageRows="+c.dataset(EventDatasets.DAMAGE).rows.size()+" elapsedNs="+elapsed+" rawBytes="+bytes+" appendedBytes="+(after-bytes));
    }
}
