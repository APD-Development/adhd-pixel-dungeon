package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestionRateBoundsTest {
 private static final FactKind KIND=FactKinds.register(FactKind.builder("test.ratebounds", "Resource observation").value("amount","Amount").build());
 Questions.Line answer(long numerator,boolean numeratorLower,long denominator,boolean denominatorLower){
  DatasetDef facts=FactDatasets.def(KIND),floors=EventDatasets.def(EventDatasets.FLOORS);
  Row value=new Row("value",null,"run",facts.size());value.finished=true;value.ruleset="r1-test";
  value.setNum(facts.indexOf("fact.v.amount"),numerator,numeratorLower?Fact.PROV_INCOMPLETE:Fact.PROV_RECORDED);
  Row floor=new Row("floor",null,"run",floors.size());floor.finished=true;floor.ruleset="r1-test";
  floor.setText(floors.indexOf("floor.run"),"run",Fact.PROV_RECORDED);
  floor.setNum(floors.indexOf("floor.turns"),denominator,denominatorLower?Fact.PROV_INCOMPLETE:Fact.PROV_RECORDED);
  Catalog catalog=new Catalog().add(QuerySupport.dataset(facts,Arrays.asList(value))).add(QuerySupport.dataset(floors,Arrays.asList(floor)));
  Question question=Question.about(facts.id).withScope(Scope.DEFAULT.withRules(Scope.Rules.ALL,"")).withMeasure(Question.Measure.TOTAL,"fact.v.amount").withPer(PerUnit.Unit.TURN);
  Questions.Answer result=Questions.answer(catalog,question,"r1-test");assertEquals(1,result.lines.size());return result.lines.get(0);
 }
 @Test public void twoIncompleteTotalsCannotEstablishARateBound(){
  Questions.Line line=answer(12,true,3,true);assertEquals(CellFormatter.UNKNOWN_TEXT,line.value);assertTrue(Double.isNaN(line.number));assertFalse(line.lowerBound);assertTrue(line.detail.contains("Both totals are lower bounds"));
 }
 @Test public void individualBoundsAndExactValuesRemainCorrect(){
  Questions.Line exact=answer(12,false,3,false);assertEquals("4.00",exact.value);assertEquals(4,exact.number,0);assertFalse(exact.lowerBound);
  Questions.Line lower=answer(12,true,3,false);assertEquals(">=4.00",lower.value);assertTrue(lower.lowerBound);
  Questions.Line upper=answer(12,false,3,true);assertEquals("4.00 or less",upper.value);assertFalse(upper.lowerBound);
 }
 @Test public void negativeNumeratorsReverseTheDenominatorBoundAndZeroRemainsExact(){
  Questions.Line negative=answer(-12,false,3,true);assertEquals("-4.00 or more",negative.value);assertTrue(negative.lowerBound);
  Questions.Line zero=answer(0,false,3,true);assertEquals("0.00",zero.value);assertFalse(zero.lowerBound);
 }
 @Test public void largeValidTotalsDoNotSaturateTheTwoDecimalDisplay(){
  Questions.Line large=answer(Long.MAX_VALUE,false,1,false);assertEquals("9223372036854776000.00",large.value);assertTrue(large.number>9e18);
  Questions.Line negative=answer(Long.MIN_VALUE,false,1,false);assertEquals("-9223372036854776000.00",negative.value);assertTrue(negative.number< -9e18);
 }
}
