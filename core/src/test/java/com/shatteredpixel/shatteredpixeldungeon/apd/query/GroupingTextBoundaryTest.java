package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class GroupingTextBoundaryTest {
    @Test public void textContainingSeparatorCannotMergeDifferentGroupingValues() {
        DatasetDef def=new DatasetDef("text","Text","test")
                .add(ColumnDef.text("first","First").shown())
                .add(ColumnDef.text("second","Second").shown());
        Row a=new Row("a",null,"run-a",2);a.finished=true;a.ruleset="r";
        Row b=new Row("b",null,"run-b",2);b.finished=true;b.ruleset="r";
        a.setText(0,"a\u0001Tb",Fact.PROV_RECORDED).setText(1,"c",Fact.PROV_RECORDED);
        b.setText(0,"a",Fact.PROV_RECORDED).setText(1,"b\u0001Tc",Fact.PROV_RECORDED);
        Query query=Query.of("text").withGroup(new GroupBy(Arrays.asList("first","second"),Arrays.asList(Aggregate.count())));
        QueryResult result=QueryEngine.run(QuerySupport.dataset(def,Arrays.asList(a,b)),query,"r");
        assertEquals("distinct text tuples must remain separate groups",2,result.groups.size());
        assertEquals(1,result.groups.get(0).n);
        assertEquals(1,result.groups.get(1).n);
    }
}
