package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.Test;
import static org.junit.Assert.*;
public class FormatterBoundaryTest {
 @Test public void smallestSignedValueKeepsSingleSignAndValidDecimals(){
  assertEquals("-92233720368547758.08", CellFormatter.value(ColumnDef.num("v","V").format(ColumnDef.Format.HUNDREDTHS), Long.MIN_VALUE));
  assertEquals("-9223372036854775.8", CellFormatter.value(ColumnDef.num("v","V").format(ColumnDef.Format.THOUSANDTHS), Long.MIN_VALUE));
 }
 @Test public void boundaryAndSmallValuesKeepExistingFixedPointConventions(){
  ColumnDef h=ColumnDef.num("v","V").format(ColumnDef.Format.HUNDREDTHS);
  ColumnDef t=ColumnDef.num("v","V").format(ColumnDef.Format.THOUSANDTHS);
  assertEquals("92233720368547758.07", CellFormatter.value(h,Long.MAX_VALUE));
  assertEquals("9223372036854775.8", CellFormatter.value(t,Long.MAX_VALUE));
  assertEquals("0.00",CellFormatter.value(h,0));
  assertEquals("-0.01",CellFormatter.value(h,-1));
  assertEquals("-0.1",CellFormatter.value(t,-100));
  assertEquals("-92233720368547758.07",CellFormatter.value(h,Long.MIN_VALUE+1));
 }
}
