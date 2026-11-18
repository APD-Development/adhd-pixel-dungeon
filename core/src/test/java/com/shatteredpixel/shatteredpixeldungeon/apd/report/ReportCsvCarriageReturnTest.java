package com.shatteredpixel.shatteredpixeldungeon.apd.report;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReportCsvCarriageReturnTest {
 @Test public void formulaLikeCarriageReturnCellsRemainQuoted() {
  assertEquals("\"'=SUM(A1)\rcontinued\"",ReportRenderer.csv("=SUM(A1)\rcontinued"));
 }
 @Test public void ordinaryCarriageReturnCellsRemainQuoted() {
  assertEquals("\"one\rtwo\"",ReportRenderer.csv("one\rtwo"));
 }
 @Test public void numericSignsAndEscapedQuotesRemainCompatible() {
  assertEquals("-42",ReportRenderer.csv("-42"));
  assertEquals("\"'=\"\"name\"\"\rcontinued\"",ReportRenderer.csv("=\"name\"\rcontinued"));
 }
}
