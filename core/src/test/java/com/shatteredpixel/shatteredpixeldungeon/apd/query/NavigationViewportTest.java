package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Navigation acceptance: positions belong to places, not dataset names or cameras. */
public class NavigationViewportTest {
    @Test public void paginationReusesTheExactResultAndStillClampsTheLastPage(){
        UimsdModel m=model();QueryResult original=m.result();
        m.setPage(1);assertSame(original,m.result());assertEquals(1,m.page());
        m.setPage(Integer.MAX_VALUE);assertSame(original,m.result());assertEquals(2,m.page());
        m.setPage(-1);assertSame(original,m.result());assertEquals(0,m.page());
    }

    @Test public void nearbyBackRestoresTheAlreadyComputedExactQueryResult(){
        UimsdModel m=model();m.addFilter(Filter.num("c0",Op.GE,10));m.setPage(1);
        QueryResult original=m.result();
        m.selectView("child");assertTrue(m.back());
        assertSame("Back must not rerun the parent query",original,m.result());
        assertEquals(120,m.result().rows.size());assertEquals(1,m.page());
    }

    @Test public void resultsBeyondTheFourEntryCacheStillRecomputeCorrectly(){
        UimsdModel m=model();QueryResult first=m.result();
        for(int i=0;i<5;i++)m.selectView(i%2==0?"child":"parent");
        for(int i=0;i<5;i++)assertTrue(m.back());
        assertNotSame("older result is released rather than retained indefinitely",first,m.result());
        assertEquals(130,m.result().rows.size());assertEquals("parent",m.query().dataset);
    }

    @Test public void replacedDatasetCannotReuseAnEarlierResult(){
        UimsdModel m=model();QueryResult first=m.result();Dataset previous=m.dataset();
        m.selectView("child");
        m.catalog().add(QuerySupport.dataset(previous.def,Collections.emptyList()));
        assertTrue(m.back());assertNotSame(first,m.result());assertEquals(0,m.result().rows.size());
    }
    private UimsdModel model(){
        Catalog cat=new Catalog();
        for(String name:Arrays.asList("parent","child")){
            DatasetDef d=new DatasetDef(name,name,"");
            for(int c=0;c<12;c++)d.add(ColumnDef.num("c"+c,"Statistic "+c).shown());
            List<Row> rows=new ArrayList<>();
            for(int i=0;i<130;i++){
                Row r=new Row(name+i,null,"run",d.size());r.finished=true;r.ruleset="rules";
                for(int c=0;c<12;c++)r.setNum(c,i+c,Fact.PROV_RECORDED);
                rows.add(r);
            }
            cat.add(QuerySupport.dataset(d,rows));
            cat.add(new ViewDef(name,name,"",query(name)));
        }
        UimsdModel m=new UimsdModel("rules","");m.setCatalog(cat);m.showRoot("parent");return m;
    }
    private Query query(String name){return QuerySupport.allScopes(Query.of(name));}
    private TableViewport at(UimsdModel m,float x,float y){return TableViewport.capture(m.table(),x,y);}
    private void position(UimsdModel m,float x,float y){
        assertNotNull(m.viewport());assertArrayEquals(new float[]{x,y},m.viewport().position(m.table(),130,80),.01f);
    }

    @Test public void crossDatasetBackRestoresQueryPageSortAndLateOutgoingViewport(){
        UimsdModel m=model();m.addFilter(Filter.num("c0",Op.GE,10));m.setSorts(Collections.singletonList(new Sort("c8",true)));m.setPage(1);
        Query original=m.query();long parent=m.placeId();TableViewport saved=at(m,330,88);
        assertTrue(m.openResearchQuery(query("child")));assertNull("child starts at its own origin",m.viewport());
        m.rememberViewport(parent,saved); // scene checkpoints AFTER model navigation
        m.rememberViewport(m.placeId(),at(m,120,44));
        assertTrue(m.back());assertEquals(original,m.query());assertEquals(1,m.page());position(m,330,88);
    }

    @Test public void nestedEvidenceOnTheSameDatasetKeepsIndependentPlaces(){
        UimsdModel m=model();long a=m.placeId();TableViewport av=at(m,300,70);
        assertTrue(m.openResearchQuery(query("parent").addFilter(Filter.num("c0",Op.GE,20))));
        long b=m.placeId();assertNotEquals(a,b);assertNull(m.viewport());m.rememberViewport(a,av);
        TableViewport bv=at(m,170,30);assertTrue(m.openResearchQuery(query("parent").addFilter(Filter.num("c0",Op.GE,40))));
        long c=m.placeId();m.rememberViewport(b,bv);m.rememberViewport(c,at(m,90,10));
        assertTrue(m.back());position(m,170,30);assertEquals(20,m.query().filters.get(0).nums[0]);
        // A late child checkpoint must not replace the restored parent.
        m.rememberViewport(c,at(m,600,200));position(m,170,30);
        assertTrue(m.back());position(m,300,70);assertFalse(m.canGoBack());
    }

    @Test public void rotationCheckpointAndSamePlaceFilterRefreshRetainPosition(){
        UimsdModel m=model();long id=m.placeId();m.rememberViewport(id,at(m,280,60));
        position(m,280,60);m.addFilter(Filter.num("c0",Op.GE,3));assertEquals(id,m.placeId());position(m,280,60);
        m.setSorts(Collections.singletonList(new Sort("c8",true)));
        float[] p=m.viewport().position(m.table(),130,80);
        assertEquals("new sorting shows the first result",0,p[1],0);
        TableGeometry g=TableGeometry.of(m.table());float left=g.lefts[8]-g.firstWidth();
        assertTrue(p[0]<=left);assertTrue(p[0]+130>=left+g.widths[8]);
    }

    @Test public void newRootAndCatalogRejectStaleCheckpoints(){
        UimsdModel m=model();long old=m.placeId();TableViewport saved=at(m,200,60);
        m.showRoot("parent");m.rememberViewport(old,saved);assertNull(m.viewport());
        old=m.placeId();m.setCatalog(m.catalog());m.rememberViewport(old,saved);assertNull(m.viewport());
        assertFalse(m.canGoBack());
    }

    @Test public void viewSelectionAndBackClampAfterScreenSizeChanges(){
        UimsdModel m=model();long parent=m.placeId();TableViewport saved=at(m,300,300);
        m.selectView("child");m.rememberViewport(parent,saved);assertNull(m.viewport());assertTrue(m.back());
        float[] p=m.viewport().position(m.table(),10000,10000);assertArrayEquals(new float[]{0,0},p,0);
        position(m,300,300); // clamping a wide rotated view does not mutate the saved checkpoint
    }

    @Test public void groupedAndIndividualTableShapesDoNotReuseEachOthersOffsets(){
        UimsdModel m=model();TableViewport raw=at(m,300,60);
        m.setGroup(new GroupBy(Collections.singletonList("c0"),Collections.singletonList(Aggregate.count())));
        assertArrayEquals(new float[]{0,0},raw.position(m.table(),130,80),0);
    }
}

