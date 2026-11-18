package com.shatteredpixel.shatteredpixeldungeon.apd.history;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ApdNewsTest {
 private String row(String id,String status,String date){return id+"\t"+status+"\t"+date+"\tV061_PAGE\tHeadline\tShort announcement.\t\n";}
 @Test public void newestFirstAndDraftsHidden(){List<ApdNews.Article>a=ApdNews.published(row("old","published","2026-10-08")+row("secret","draft","2026-11-01")+row("new","published","2026-10-10"));assertEquals(2,a.size());assertEquals("new",a.get(0).id);assertEquals("old",a.get(1).id);}
 @Test public void dateUsesStablePlayerFormat(){assertEquals("Oct 8, 2026",ApdNews.published(row("a","published","2026-10-08")).get(0).displayDate());}
 @Test(expected=IllegalArgumentException.class) public void duplicateIdsRejected(){ApdNews.published(row("a","published","2026-10-08")+row("a","draft","2026-10-08"));}
 @Test(expected=java.time.format.DateTimeParseException.class) public void invalidDatesRejected(){ApdNews.published(row("a","published","not-a-date"));}
 @Test public void thousandsOfArticlesRenderOnlyOnePage(){StringBuilder s=new StringBuilder();for(int i=0;i<1000;i++)s.append(row("a"+i,"published","2026-10-08"));List<ApdNews.Article>a=ApdNews.published(s.toString());assertEquals(12,ApdNews.page(a,0).size());assertEquals(0,ApdNews.page(a,Integer.MAX_VALUE).size());}
 @Test public void buildIdentifiersStaySeparateAndNavigationClamps(){List<ApdHistory.Build>b=ApdHistory.builds("build-1\tBuild 1\tUnknown\tIncomplete\ncodex-test-1\tCodex TEST 1\tUnknown\tIncomplete\n");assertEquals(2,b.size());assertEquals(1,ApdHistory.clamp(999,2));assertEquals(0,ApdHistory.clamp(-1,2));}
 @Test public void commentaryAndChangesStayInPopups(){List<ApdHistory.Entry>e=ApdHistory.entries("Bug Fixes\tV061_BUGFIX\tSave protection\tFailed writes preserve existing saves.\nDeveloper Commentary\tV061_PAGE\tDeveloper Commentary\tShort summary.\n");assertEquals(2,e.size());assertEquals("Developer Commentary",e.get(1).category);}
}
