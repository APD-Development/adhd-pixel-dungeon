package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class PerUnitProvenanceTest {
    private Row row(DatasetDef d, String id, int prov, long turns) {
        Row r = new Row(id, null, id, d.size());
        r.finished = true; r.publicRow = true; r.ruleset = "rules";
        String prefix = d.id.equals(EventDatasets.FLOORS) ? "floor" : "attack";
        r.setNum(d.indexOf(prefix + ".runDeepest"), 10, prov);
        if (prefix.equals("floor")) {
            r.setNum(d.indexOf("floor.turns"), turns, Fact.PROV_RECORDED);
            r.setText(d.indexOf("floor.run"), id, Fact.PROV_RECORDED);
        }
        return r;
    }
    @Test public void exactAndAtLeastDepthGroupsUseTheirOwnDenominators() {
        DatasetDef f = EventDatasets.def(EventDatasets.FLOORS), a = EventDatasets.def(EventDatasets.ATTACKS);
        Catalog catalog = new Catalog().add(new Dataset(f, Arrays.asList(
                row(f,"exact",Fact.PROV_RECORDED,10), row(f,"lower-a",Fact.PROV_INCOMPLETE,20),
                row(f,"lower-b",Fact.PROV_INCOMPLETE,30)), Collections.emptyList()));
        Dataset attacks = new Dataset(a, Arrays.asList(row(a,"exact",Fact.PROV_RECORDED,0),
                row(a,"lower-a",Fact.PROV_INCOMPLETE,0)), Collections.emptyList());
        Query q = Query.of(a.id).withMinSample(1)
                .withGroup(new GroupBy(Arrays.asList("attack.runDeepest"), Arrays.asList(Aggregate.count())));
        QueryResult answer = QueryEngine.run(attacks,q,"rules");
        assertEquals(2, answer.groups.size());
        for (PerUnit.Unit unit : Arrays.asList(PerUnit.Unit.FLOOR,PerUnit.Unit.RUN,PerUnit.Unit.TURN,PerUnit.Unit.THOUSAND_TURNS)) {
            PerUnit.Result result = PerUnit.compute(catalog,q,answer,unit,"rules");
            for (int i=0;i<answer.groups.size();i++) {
                boolean lower = answer.groups.get(i).sample.prov(a.indexOf("attack.runDeepest")) == Fact.PROV_INCOMPLETE;
                double expected = unit==PerUnit.Unit.TURN||unit==PerUnit.Unit.THOUSAND_TURNS ? (lower?50:10) : (lower?2:1);
                if(unit==PerUnit.Unit.THOUSAND_TURNS) expected/=1000;
                assertEquals(unit + " must not confuse 10 with at least 10",expected,result.denominators[i],0);
            }
        }
    }
}
