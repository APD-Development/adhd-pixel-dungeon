package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;
public class RowMergerOverflowTest {
 private final DatasetDef def = new DatasetDef("merge", "Merge", "").add(ColumnDef.num("total", "Total").merge(ColumnDef.Merge.SUM));
 private Row part(String id,long n){return new Row(id,"same","run",1).setNum(0,n,Fact.PROV_RECORDED);}
 @Test public void unrepresentablePositiveSumNeverWrapsIntoAnExactNegative(){
  Row a=part("a",Long.MAX_VALUE),b=part("b",1);
  Row merged=RowMerger.merge(def,Arrays.asList(a,b));
  assertFalse("Out-of-range total must not be presented as an exact wrapped number",merged.isKnown(0));
  assertEquals(Long.MAX_VALUE,a.num(0));assertEquals(1,b.num(0));
 }
 @Test public void unrepresentableNegativeSumNeverWrapsIntoAnExactPositive(){
  assertFalse(RowMerger.merge(def,Arrays.asList(part("a",Long.MIN_VALUE),part("b",-1))).isKnown(0));
 }
 @Test public void transientOverflowCanCancelIntoAnExactlyRepresentableTotal(){
  Row merged=RowMerger.merge(def,Arrays.asList(part("a",Long.MAX_VALUE),part("b",1),part("c",-1)));
  assertTrue(merged.isKnown(0));assertEquals(Long.MAX_VALUE,merged.num(0));assertEquals(Fact.PROV_RECORDED,merged.prov(0));
 }
 @Test public void exactBoundariesAndOrdinaryTotalsStayKnown(){
  for(long n:new long[]{Long.MIN_VALUE,Long.MAX_VALUE,0,42}){
   Row merged=RowMerger.merge(def,Arrays.asList(part("a",n),part("b",0)));
   assertTrue(merged.isKnown(0));assertEquals(n,merged.num(0));
  }
 }
 @Test public void cancelingOverflowKeepsIncompleteProvenance(){
  Row unknown=new Row("unknown","same","run",1);
  Row merged=RowMerger.merge(def,Arrays.asList(part("a",Long.MIN_VALUE),part("b",-1),part("c",1),unknown));
  assertTrue(merged.isKnown(0));assertEquals(Long.MIN_VALUE,merged.num(0));assertEquals(Fact.PROV_INCOMPLETE,merged.prov(0));
 }
 @Test public void mixedSignedSegmentsMatchAnIndependentExactSumOracle(){
  java.util.Random random=new java.util.Random(671150);
  for(int sample=0;sample<1000;sample++){
   java.util.List<Row> parts=new java.util.ArrayList<>();java.math.BigInteger exact=java.math.BigInteger.ZERO;
   for(int i=0,n=2+random.nextInt(9);i<n;i++){
    long value=random.nextLong();parts.add(part("p"+i,value));exact=exact.add(java.math.BigInteger.valueOf(value));
   }
   Row merged=RowMerger.merge(def,parts);
   if(exact.bitLength()>63)assertFalse(merged.isKnown(0));
   else {assertTrue(merged.isKnown(0));assertEquals(exact.longValue(),merged.num(0));}
  }
 }
}
