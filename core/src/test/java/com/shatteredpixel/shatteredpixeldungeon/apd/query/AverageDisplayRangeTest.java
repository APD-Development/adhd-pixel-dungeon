package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.Test;
import java.util.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import static org.junit.Assert.*;
public class AverageDisplayRangeTest {
 private CellFormatter.AggText rendered(long value,Aggregate.Fn fn){
  DatasetDef def=new DatasetDef("display-fixture","Numbers","").add(ColumnDef.num("value","Value"));
  Row row=new Row("row",null,"run",def.size());row.finished=true;row.ruleset="test";row.setNum(0,value,Fact.PROV_RECORDED);
  Query query=Query.of(def.id).withScope(Scope.DEFAULT.withRules(Scope.Rules.ALL,"")).withGroup(new GroupBy(Collections.emptyList(),Arrays.asList(Aggregate.of(fn,"value"))));
  QueryResult result=QueryEngine.run(QuerySupport.dataset(def,Arrays.asList(row)),query,"test");
  return CellFormatter.aggregate(def,result.aggregates.get(0),result.groups.get(0).aggregates.get(0));
 }
 @Test public void largeMeansAndMediansDoNotShrinkDuringOneDecimalFormatting(){
  for(Aggregate.Fn fn:new Aggregate.Fn[]{Aggregate.Fn.AVG,Aggregate.Fn.MEDIAN}){
   assertEquals("9223372036854776000",rendered(Long.MAX_VALUE,fn).text);
   assertEquals("-9223372036854776000",rendered(Long.MIN_VALUE,fn).text);
  }
 }
 @Test public void ordinaryAveragePresentationRemainsUnchanged(){
  assertEquals("5",rendered(5,Aggregate.Fn.AVG).text);assertEquals("-5",rendered(-5,Aggregate.Fn.MEDIAN).text);
  DatasetDef def=new DatasetDef("display","Numbers","").add(ColumnDef.num("value","Value"));
  assertEquals("1.3",CellFormatter.aggregate(def,Aggregate.of(Aggregate.Fn.AVG,"value"),new AggValue(true,1.25,0,2,2,0)).text);
  assertEquals("-1.2",CellFormatter.aggregate(def,Aggregate.of(Aggregate.Fn.AVG,"value"),new AggValue(true,-1.25,0,2,2,0)).text);
 }
}
