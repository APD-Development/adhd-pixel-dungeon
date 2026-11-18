package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.Test;
import static org.junit.Assert.*;
public class RateBoundRoundingTest {
 @Test public void displayedRateBoundsRoundOutwardAndRetainKnownNonpositiveBounds(){
  QuestionRateBoundsTest fixture=new QuestionRateBoundsTest();
  assertEquals("a lower bound must never round above the true bound",">=0.70",fixture.answer(12,true,17,false).value);
  assertEquals("an upper bound must never round below the true bound","1.72 or less",fixture.answer(12,false,7,true).value);
  assertEquals("negative lower bounds round downward","-1.72 or more",fixture.answer(-12,false,7,true).value);
  Questions.Line negative=fixture.answer(-12,true,7,true);assertEquals(">=-1.72",negative.value);assertTrue(negative.lowerBound);assertFalse(Double.isNaN(negative.number));
  Questions.Line zero=fixture.answer(0,true,3,true);assertEquals(">=0.00",zero.value);assertTrue(zero.lowerBound);
 }
}
