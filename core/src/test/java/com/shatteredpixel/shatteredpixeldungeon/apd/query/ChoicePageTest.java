package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChoicePageTest {
 @Test public void boundariesKeepTheFortyFirstAndLastChoicesReachable(){
  for(int total:new int[]{0,1,39,40,41,80,81,1000}){
   boolean[] seen=new boolean[total];ChoicePage first=new ChoicePage(total,0);
   for(int p=0;p<first.pageCount;p++){
    ChoicePage page=new ChoicePage(total,p);
    assertTrue(page.end-page.start<=40);assertEquals(p>0,page.hasPrevious());assertEquals(p+1<page.pageCount,page.hasNext());
    for(int i=page.start;i<page.end;i++){assertFalse("duplicate choice "+i,seen[i]);seen[i]=true;}
   }
   for(boolean present:seen)assertTrue("every available comparison must remain reachable",present);
  }
 }
 @Test public void invalidPageClampsWithoutDroppingTheFinalPartialPage(){
  assertEquals(0,new ChoicePage(81,-1).start);ChoicePage last=new ChoicePage(81,Integer.MAX_VALUE);
  assertEquals(80,last.start);assertEquals(81,last.end);assertFalse(last.hasNext());assertTrue(last.hasPrevious());
 }
 @Test public void largeCountsDoNotOverflowOrImposeATotalChoiceLimit(){
  ChoicePage last=new ChoicePage(Integer.MAX_VALUE,Integer.MAX_VALUE);
  assertEquals(Integer.MAX_VALUE,last.end);assertTrue(last.start>=0);assertTrue(last.end-last.start<=40);
 }
}
