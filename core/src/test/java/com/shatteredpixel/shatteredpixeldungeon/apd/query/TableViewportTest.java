package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TableViewportTest {
    private ResultTable table(List<Sort> sorts,List<String> columns,int count){
        DatasetDef d=new DatasetDef("wide","Wide","Viewport regression");
        for(int i=0;i<12;i++)d.add(ColumnDef.num("c"+i,"Statistic "+i).shown());
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<count;i++){
            Row r=new Row("r"+i,null,"run"+i,d.size());r.finished=true;r.ruleset="r1-test";
            for(int c=0;c<12;c++)r.setNum(c,i+c,Fact.PROV_RECORDED);rows.add(r);
        }
        Query q=QuerySupport.allScopes(Query.of("wide")).withSorts(sorts).withColumns(columns);
        return ResultTable.build(QueryEngine.run(QuerySupport.dataset(d,rows),q,"r1-test"),0,100);
    }
    private ResultTable table(List<Sort> sorts){return table(sorts,Collections.emptyList(),80);}
    private float left(ResultTable t,int c){TableGeometry g=TableGeometry.of(t);return g.lefts[c]-g.firstWidth();}

    @Test public void sortingRightHandColumnPreservesHorizontalAndShowsFirstSortedRows(){
        ResultTable before=table(Collections.emptyList());float x=left(before,6);
        TableViewport saved=TableViewport.capture(before,x,120);
        ResultTable after=table(Collections.singletonList(new Sort("c6",true)));
        float[] at=saved.position(after,150,100);
        assertEquals(x,at[0],0.01f);assertEquals(0,at[1],0);
        assertEquals(85,after.lines.get(0).row.num(6));
    }
    @Test public void reversingDirectionKeepsSortedColumnVisible(){
        ResultTable before=table(Collections.singletonList(new Sort("c6",true)));
        TableViewport saved=TableViewport.capture(before,left(before,6),80);
        ResultTable after=table(Collections.singletonList(new Sort("c6",false)));
        float[] at=saved.position(after,150,100);
        assertEquals(left(after,6),at[0],0.01f);assertEquals(6,after.lines.get(0).row.num(6));
    }
    @Test public void addingAndReversingSecondarySortNeverJumpsToThePrimaryColumn(){
        ResultTable before=table(Collections.singletonList(new Sort("c1",false)));
        TableViewport saved=TableViewport.capture(before,left(before,8),44);
        ResultTable after=table(Arrays.asList(new Sort("c1",false),new Sort("c8",true)));
        float[] at=saved.position(after,150,100);
        assertEquals(left(after,8),at[0],0.01f);assertEquals(0,at[1],0);
        saved=TableViewport.capture(after,at[0],55);
        ResultTable reversed=table(Arrays.asList(new Sort("c1",false),new Sort("c8",false)));
        assertEquals(left(reversed,8),saved.position(reversed,150,100)[0],0.01f);
    }
    @Test public void reorderingColumnsDoesNotMistakeSortKeyOrderForANewSort(){
        List<Sort> sorts=Arrays.asList(new Sort("c3",false),new Sort("c7",true));
        ResultTable before=table(sorts);TableViewport saved=TableViewport.capture(before,left(before,8),55);
        List<String> columns=new ArrayList<>();for(int i=0;i<12;i++)columns.add("c"+i);
        Collections.swap(columns,3,7);
        ResultTable reordered=table(sorts,columns,80);
        assertEquals(55,saved.position(reordered,150,100)[1],0);
    }
    @Test public void refreshingSettingsOrFilteringPreservesBothOffsetsAndClampsShortResults(){
        ResultTable t=table(Collections.emptyList());TableViewport saved=TableViewport.capture(t,190,120);
        assertArrayEquals(new float[]{190,120},saved.position(t,150,100),0.01f);
        ResultTable fewer=table(Collections.emptyList(),Collections.emptyList(),2);
        assertArrayEquals(new float[]{190,0},saved.position(fewer,150,100),0.01f);
    }
    @Test public void changingEarlierColumnsKeepsTheSameColumnAnchor(){
        ResultTable t=table(Collections.emptyList());TableViewport saved=TableViewport.capture(t,left(t,6)+5,44);
        List<String> columns=new ArrayList<>();for(int i=0;i<12;i++)if(i!=2)columns.add("c"+i);
        ResultTable changed=table(Collections.emptyList(),columns,80);
        float[] at=saved.position(changed,150,100);
        assertEquals(left(changed,5)+5,at[0],0.01f);assertEquals(44,at[1],0);
    }
    @Test public void newSortOffscreenMovesOnlyEnoughToRevealItsHeader(){
        ResultTable t=table(Collections.emptyList());TableViewport saved=TableViewport.capture(t,0,0);
        ResultTable sorted=table(Collections.singletonList(new Sort("c8",true)));
        float[] at=saved.position(sorted,150,100);TableGeometry g=TableGeometry.of(sorted);
        assertEquals(left(sorted,8)+g.widths[8]-150,at[0],0.01f);
    }
    @Test public void emptyResultsAndRemovedAnchorNeverProduceInvalidOffsets(){
        ResultTable t=table(Collections.emptyList());TableViewport saved=TableViewport.capture(t,500,500);
        ResultTable empty=table(Collections.emptyList(),Arrays.asList("c0","c1"),0);
        assertArrayEquals(new float[]{0,0},saved.position(empty,150,100),0.01f);
    }
    @Test public void sortIndicatorPrecedesLongNamesAndReflectsDirection(){
        ResultTable t=table(Collections.singletonList(new Sort("c6",true)));
        assertTrue(TableViewport.heading(t.headers.get(6)).startsWith("v "));
        t=table(Collections.singletonList(new Sort("c6",false)));
        assertTrue(TableViewport.heading(t.headers.get(6)).startsWith("^ "));
    }
}
