package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.research.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

/** Real binary recording -> closed file -> catalog -> second correlated query -> exact evidence. */
public class DamageRecoveryWindowTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private static final String RULES="rules";
    private static Scope all(){return new Scope(Scope.Rules.ALL,"",false,false,false,false);}
    private SyntheticLog log(String id){return new SyntheticLog(tmp.getRoot(),id,TelemetryVersion.CURRENT,100)
            .start(id,0,"WARRIOR",RULES,0,30,false).enter(1);}
    private Catalog recorded(){
        SyntheticLog s=log("A");
        s.clock=10;s.damageToHero("rat","Rat",8);
        s.clock=10;s.heal(99,true,"same turn");
        s.clock=11;s.heal(4,true,"potion");
        s.clock=20;s.heal(3,true,"regeneration");
        s.clock=20;s.damageFromHero("rat","Rat",200);
        s.clock=21;s.heal(9,true,"late");
        s.clock=30;s.end(LifecycleEvents.OUTCOME_DIED,"rat",1,1,10).close(true);
        SyntheticLog other=log("a");
        other.clock=10;other.damageToHero("rat","Rat",8);other.clock=11;other.heal(50,true,"other run");
        other.end(LifecycleEvents.OUTCOME_ABANDONED,"",1,1,10).close(true);
        return new DatasetStore(tmp.getRoot()).load();
    }
    private QueryResult anchors(Catalog c){
        return ResearchData.select(c,Query.of(EventDatasets.DAMAGE).withScope(all())
                .addFilter(Filter.text("damage.run",Op.GROUP_TEXT,"A"))
                .addFilter(Filter.num("damage.targetKind",Op.EQ,CombatEvents.KIND_HERO)),RULES);
    }
    private ResearchDocument answer(Catalog c,int turns){
        QueryResult a=anchors(c);
        return DamageRecoveryWindow.answer(c,a,a.rows.get(0),turns,RULES);
    }

    @Test public void actualRecordingUsesExclusiveDamageTurnInclusiveEndAndExactRun(){
        Catalog c=recorded();
        ResearchDocument d=answer(c,10);
        assertEquals(1,d.cards.size());
        ResearchDocument.Card card=d.cards.get(0);
        assertEquals("7 observed (at least)",card.value);
        assertEquals(CellText.LOWER,card.state);
        assertTrue(d.notes.get(0).contains("turn 10 through turn 20 inclusive"));
        assertEquals(2,card.evidence.get(1).rows.size());
        for(Row r:card.evidence.get(1).rows)assertEquals("A",r.runId);
        assertEquals("4 observed (at least)",answer(c,1).cards.get(0).value);
        assertEquals("16 observed (at least)",answer(c,20).cards.get(0).value);
    }

    @Test public void evidenceQueryRoundTripsAfterReloadWithoutChangingRawHistory()throws Exception{
        Catalog c=recorded();
        java.io.File file=new java.io.File(tmp.getRoot(),"runs/A.apdlog");
        byte[] before=Files.readAllBytes(file.toPath());
        ResearchDocument.Card card=answer(c,10).cards.get(0);
        Query evidence=card.evidence.get(1).source.query;
        Query restored=QueryCodec.decode(QueryCodec.encode(evidence));
        assertEquals(evidence,restored);
        Catalog loaded=new DatasetStore(tmp.getRoot()).load();
        assertEquals(2,ResearchData.select(loaded,restored,RULES).rows.size());
        assertEquals(card.value,answer(loaded,10).cards.get(0).value);
        assertArrayEquals(before,Files.readAllBytes(file.toPath()));
    }

    @Test public void nonHeroUncertainAndOverflowingAnchorsFailClosed(){
        Catalog c=recorded();
        QueryResult a=anchors(c);
        Row anchor=a.rows.get(0);
        int kind=a.def.indexOf("damage.targetKind"),turn=a.def.indexOf("damage.turn");
        anchor.setNum(kind,CombatEvents.KIND_ENEMY,Fact.PROV_RECORDED);
        assertTrue(DamageRecoveryWindow.answer(c,a,anchor,10,RULES).cards.isEmpty());
        anchor.setNum(kind,CombatEvents.KIND_HERO,Fact.PROV_RECORDED);
        anchor.setNum(turn,10,Fact.PROV_INCOMPLETE);
        assertTrue(DamageRecoveryWindow.answer(c,a,anchor,10,RULES).cards.isEmpty());
        anchor.setNum(turn,Long.MAX_VALUE-2,Fact.PROV_RECORDED);
        assertTrue(DamageRecoveryWindow.answer(c,a,anchor,10,RULES).cards.isEmpty());
        assertTrue(DamageRecoveryWindow.answer(c,a,anchor,0,RULES).cards.isEmpty());
    }

    @Test public void missingCoverageIsUnknownWhileNoExactHealIsOnlyLowerBoundZero(){
        Catalog c=recorded();
        QueryResult a=anchors(c);
        Row anchor=a.rows.get(0);
        anchor.setNum(a.def.indexOf("damage.turn"),100,Fact.PROV_RECORDED);
        ResearchDocument zero=DamageRecoveryWindow.answer(c,a,anchor,10,RULES);
        assertEquals("0 observed (at least)",zero.cards.get(0).value);
        assertTrue(zero.cards.get(0).detail.contains("not proof"));
        Dataset heals=c.dataset(EventDatasets.HEALS);
        c.add(new Dataset(heals.def,heals.rows,Collections.singletonList(
                new Dataset.RunInfo("A",true,RULES,false,false))));
        ResearchDocument unknown=DamageRecoveryWindow.answer(c,a,anchor,10,RULES);
        assertTrue(unknown.cards.isEmpty());
        assertTrue(unknown.notes.get(unknown.notes.size()-1).contains("unknown, not zero"));
        assertTrue(DamageRecoveryWindow.answer(new Catalog(),a,anchor,10,RULES).cards.isEmpty());
    }

    @Test public void uncertainHealingIsExcludedAndInterpretationSurvivesExport(){
        Catalog c=recorded();
        Dataset heals=c.dataset(EventDatasets.HEALS);
        for(Row r:heals.rows)if(r.runId.equals("A")&&r.num(heals.def.indexOf("heal.turn"))==11)
            r.setNum(heals.def.indexOf("heal.amount"),4,Fact.PROV_INFERRED);
        ResearchDocument d=answer(c,10);
        assertEquals("3 observed (at least)",d.cards.get(0).value);
        assertEquals(1,d.cards.get(0).evidence.get(1).rows.size());
        assertTrue(d.cards.get(0).detail.contains("excluded=1"));
        com.shatteredpixel.shatteredpixeldungeon.apd.report.Report report=d.report(1,"test",false);
        assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportRenderer.csv(report).contains("strictly after"));
    }
}
