package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LinkIdentityTest {
    private static Row row(DatasetDef d,String id,String run,String group) {
        Row r=new Row(id,null,run,d.size());r.finished=true;r.ruleset="rules";
        r.setText(0,run,Fact.PROV_RECORDED);if(d.size()>1)r.setText(1,group,Fact.PROV_RECORDED);return r;
    }
    private static UimsdModel model() {
        DatasetDef runs=new DatasetDef("runs","Runs","").add(ColumnDef.text("run.id","Run").shown());
        DatasetDef items=new DatasetDef("items","Items","").add(ColumnDef.text("item.run","Run").shown())
            .add(ColumnDef.text("kind","Kind").shown());
        runs.link(new LinkDef("Items","items","run.id","item.run"));
        items.link(new LinkDef("The run","runs","item.run","run.id"));
        Catalog cat=new Catalog().add(new Dataset(runs,Arrays.asList(row(runs,"upper","Run-A",null),
            row(runs,"lower","run-a",null),row(runs,"second","Run-B",null)),Collections.emptyList()))
            .add(new Dataset(items,Arrays.asList(row(items,"upper-item","Run-A","wanted"),
            row(items,"lower-item","run-a","other"),row(items,"second-item","Run-B","wanted")),Collections.emptyList()))
            .add(new ViewDef("runs","Runs","",Query.of("runs")))
            .add(new ViewDef("items","Items","",Query.of("items")));
        UimsdModel model=new UimsdModel("rules","");model.setCatalog(cat);return model;
    }
    @Test public void rowLinksUseExactIdentifiersInBothDirectionsAndRestoreBack() {
        UimsdModel model=model();model.showRoot("runs");Query original=model.query();
        model.openLink(model.dataset().rows.get(0),model.dataset().def.links.get(0));
        assertEquals(1,model.result().rows.size());assertEquals("upper-item",model.result().rows.get(0).id);
        model.openLink(model.result().rows.get(0),model.dataset().def.links.get(0));
        assertEquals(1,model.result().rows.size());assertEquals("upper",model.result().rows.get(0).id);
        assertTrue(model.back());assertTrue(model.back());assertEquals(original,model.query());
    }
    @Test public void groupRunLinksContainOnlyTheSelectedCaseSensitiveIdentities() {
        UimsdModel model=model();model.showRoot("items");
        model.setGroup(new GroupBy(Arrays.asList("kind"),Arrays.asList(Aggregate.count())));
        GroupRow selected=null;for(GroupRow g:model.result().groups)if("wanted".equals(g.sample.text(1)))selected=g;
        assertNotNull(selected);Query original=model.query();
        assertEquals(2,model.openGroupRuns(selected));
        Set<String> actual=new LinkedHashSet<>();for(Row r:model.result().rows)actual.add(r.runId);
        assertEquals(new LinkedHashSet<>(Arrays.asList("Run-A","Run-B")),actual);
        Query decoded=QueryCodec.decode(QueryCodec.encode(model.query()));assertEquals(model.query(),decoded);
        actual.clear();for(Row r:QueryEngine.run(model.dataset(),decoded,"rules").rows)actual.add(r.runId);
        assertEquals(new LinkedHashSet<>(Arrays.asList("Run-A","Run-B")),actual);
        assertTrue(model.back());assertEquals(original,model.query());
    }
}
