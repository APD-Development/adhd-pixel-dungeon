package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChartBarTest {
    @Test public void positiveBarsUseRealZeroRatherThanTruncatedMinimum(){
        ChartBar bar=ChartBar.of(50,100,50);
        assertEquals(0,bar.zero,0);assertEquals(0,bar.start,0);assertEquals(.5,bar.end,0);
    }
    @Test public void negativeAndMixedBarsShareZeroBaseline(){
        ChartBar neg=ChartBar.of(-100,-50,-50);
        assertEquals(1,neg.zero,0);assertEquals(.5,neg.start,0);assertEquals(1,neg.end,0);
        ChartBar mixed=ChartBar.of(-20,80,-10);
        assertEquals(.2,mixed.zero,1e-12);assertEquals(.1,mixed.start,1e-12);assertEquals(.2,mixed.end,1e-12);
    }
    @Test public void zeroIsZeroLengthAndExtremeSpansStayFinite(){
        assertEquals(0,ChartBar.of(0,0,0).end,0);
        ChartBar extreme=ChartBar.of(-Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE);
        assertEquals(.5,extreme.zero,0);assertEquals(1,extreme.end,0);
    }
    @Test public void unknownNonFiniteAndInvalidRangesHaveNoGeometry(){
        assertNull(ChartBar.of(0,1,Double.NaN));assertNull(ChartBar.of(0,Double.POSITIVE_INFINITY,1));
        assertNull(ChartBar.of(2,1,1));assertNull(ChartBar.of(0,1,2));
    }
}
