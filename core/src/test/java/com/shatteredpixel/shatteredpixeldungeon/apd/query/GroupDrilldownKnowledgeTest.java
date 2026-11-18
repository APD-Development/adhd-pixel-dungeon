package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class GroupDrilldownKnowledgeTest {
    private static Row row(DatasetDef def,String id,Object value,int prov) {
        Row r=new Row(id,null,id,def.size());r.finished=true;r.publicRow=true;r.ruleset="rules";
        if(value instanceof String)r.setText(0,(String)value,prov);
        else if(value instanceof Number)r.setNum(0,((Number)value).longValue(),prov);
        return r;
    }
    private static void verifyEveryGroup(ColumnDef.Type type,Object... values) {
        DatasetDef def=new DatasetDef("things","Things","things");
        def.add(type==ColumnDef.Type.TEXT?ColumnDef.text("key","Key"):
                type==ColumnDef.Type.ENUM?ColumnDef.enumeration("key","Key"):ColumnDef.num("key","Key"));
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<values.length;i++)rows.add(row(def,"row-"+i,values[i],i==1?Fact.PROV_INCOMPLETE:Fact.PROV_RECORDED));
        Query grouped=Query.of(def.id).withGroup(new GroupBy(Arrays.asList("key"),Arrays.asList(Aggregate.count())));
        Catalog cat=new Catalog().add(new Dataset(def,rows,Collections.emptyList()))
                .add(new ViewDef("things","Things","",grouped).withInstances(Query.of("things")));
        UimsdModel model=new UimsdModel("rules","");model.setCatalog(cat);
        Query original=model.query();
        List<GroupRow> groups=new ArrayList<>(model.result().groups);
        for(GroupRow group:groups) {
            Set<String> expected=new LinkedHashSet<>();for(Row r:group.rows)expected.add(r.id);
            model.openGroupRows(group);
            assertFalse(model.result().isGrouped());
            assertTrue(model.result().problems.toString(),model.result().problems.isEmpty());
            Set<String> actual=new LinkedHashSet<>();for(Row r:model.result().rows)actual.add(r.id);
            assertEquals("Drilldown must list exactly the records represented by this group",expected,actual);
            Query restored=QueryCodec.decode(QueryCodec.encode(model.query()));
            assertEquals(model.query(),restored);
            Set<String> decoded=new LinkedHashSet<>();for(Row r:QueryEngine.run(cat.dataset("things"),restored,"rules").rows)decoded.add(r.id);
            assertEquals(expected,decoded);
            SavedViews saved = new SavedViews();
            assertEquals(SavedViews.Result.SAVED,saved.save("Group",restored,123));
            try {
                java.nio.file.Path directory=java.nio.file.Files.createTempDirectory("apd-group-view-");
                try {
                    java.io.File file=directory.resolve("saved_views.txt").toFile();
                    saved.store(file);
                    SavedViews loaded=SavedViews.load(file);
                    assertEquals(0,loaded.skippedOnLoad);
                    assertEquals(restored,loaded.get("Group").query());
                } finally {
                    java.nio.file.Files.deleteIfExists(directory.resolve("saved_views.txt"));
                    java.nio.file.Files.delete(directory);
                }
            } catch(java.io.IOException e) { throw new AssertionError(e); }
            assertTrue(model.back());assertEquals(original,model.query());
        }
    }
    @Test public void numericGroupsKeepExactLowerBoundAndUnknownRecordsSeparate() {
        verifyEveryGroup(ColumnDef.Type.NUM,10L,10L,11L,null);
    }
    @Test public void enumGroupsKeepIncompleteAndRecordedValuesSeparate() {
        verifyEveryGroup(ColumnDef.Type.ENUM,0L,0L,1L,null);
    }
    @Test public void textGroupsKeepCaseEmptyAndUnknownValuesSeparate() {
        verifyEveryGroup(ColumnDef.Type.TEXT,"Sword","sword","",null);
    }
    @Test public void normalFilteringAndOfferedOperatorsKeepTheirExistingBehavior() {
        DatasetDef def=new DatasetDef("x","x","x").add(ColumnDef.text("key","Key"));
        Dataset ds=new Dataset(def,Arrays.asList(row(def,"one","Sword",Fact.PROV_RECORDED),
                row(def,"two","sword",Fact.PROV_RECORDED)),Collections.emptyList());
        assertEquals(2,QueryEngine.run(ds,Query.of("x").addFilter(Filter.text("key",Op.EQ,"Sword")),"rules").rows.size());
        for(ColumnDef.Type type:ColumnDef.Type.values())for(Op offered:Op.ops(type))
            assertFalse(offered.name().startsWith("GROUP_"));
        try { QueryCodec.decode("dataset\tx\nfilter\tkey\tFUTURE_UNSUPPORTED_PREDICATE\t\tSword\t1\n"); fail(); }
        catch(IllegalArgumentException expected) { /* Unsupported predicates fail closed, never drop the filter. */ }
    }
}
