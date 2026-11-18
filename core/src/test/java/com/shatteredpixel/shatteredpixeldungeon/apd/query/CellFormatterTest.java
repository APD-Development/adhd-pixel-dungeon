package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Test;

public class CellFormatterTest {

	private final DatasetDef def = QuerySupport.def();

	@Test
	public void unknownIsAQuestionMarkAndZeroIsZero(){
		Row r = new Row("a", null, "run", def.size());
		r.setNum(SCORE, 0, Fact.PROV_RECORDED);
		CellText zero = CellFormatter.cellOpen(r, def.column("score"));
		CellText unknown = CellFormatter.cellOpen(r, def.column("depth"));
		assertEquals("0", zero.text);
		assertEquals(CellText.EXACT, zero.state);
		assertEquals("?", unknown.text);
		assertEquals(CellText.UNKNOWN, unknown.state);
	}

	@Test
	public void lowerBoundsInferredAndImportedValuesAreMarked(){
		Row r = new Row("a", null, "run", def.size());
		r.setNum(SCORE, 12, Fact.PROV_INCOMPLETE);
		r.setNum(DEPTH, 6, Fact.PROV_INFERRED);
		r.setNum(KIND, 1, Fact.PROV_LEGACY);
		assertEquals(">=12", CellFormatter.cellOpen(r, def.column("score")).text);
		assertEquals(CellText.LOWER, CellFormatter.cellOpen(r, def.column("score")).state);
		assertEquals("~6", CellFormatter.cellOpen(r, def.column("depth")).text);
		assertEquals("green", CellFormatter.cellOpen(r, def.column("kind")).text);
		assertEquals(CellText.LEGACY, CellFormatter.cellOpen(r, def.column("kind")).state);
	}

	@Test
	public void datesAndFixedPointNumbersFormatWithoutLocaleSurprises(){
		assertEquals("1970-01-01", CellFormatter.date(0));
		assertEquals("2000-02-29", CellFormatter.date(951782400000L));
		assertEquals("1969-12-31", CellFormatter.date(-1));
		assertEquals("2026-10-07", CellFormatter.date(1791331200000L));
		ColumnDef hundredths = ColumnDef.num("m", "M").format(ColumnDef.Format.HUNDREDTHS);
		ColumnDef thousandths = ColumnDef.num("t", "T").format(ColumnDef.Format.THOUSANDTHS);
		assertEquals("1.25", CellFormatter.value(hundredths, 125));
		assertEquals("0.05", CellFormatter.value(hundredths, 5));
		assertEquals("-1.25", CellFormatter.value(hundredths, -125));
		assertEquals("1234.5", CellFormatter.value(thousandths, 1234500));
	}

	@Test
	public void ratesShowTheCountsTheyAreMadeOf(){
		AggValue v = new AggValue(true, 5.0 / 12, 5, 12, 14, 0);
		assertEquals("42% (5/12)", CellFormatter.aggregate(def, Aggregate.rate("kind", 0), v).text);
		assertEquals("12 of 14 known", CellFormatter.aggregate(def, Aggregate.rate("kind", 0), v).detail);
	}

	@Test public void invalidCalendarDatesAreRefusedInsteadOfChangingTheFilterDate(){
		ColumnDef date = ColumnDef.num("date", "Date").format(ColumnDef.Format.DATE_MILLIS);
		assertEquals(Long.valueOf(951782400000L), CellFormatter.parse(date, "2000-02-29"));
		for (String invalid : new String[]{"2026-02-29", "2026-02-31", "2026-04-31", "1900-02-29", "2026-13-01"})
			assertNull(invalid, CellFormatter.parse(date, invalid));
	}

	@Test public void nonFiniteAndOverflowingDecimalFiltersAreRefused(){
		for (ColumnDef.Format format : new ColumnDef.Format[]{ColumnDef.Format.HUNDREDTHS, ColumnDef.Format.THOUSANDTHS}){
			ColumnDef decimal = ColumnDef.num("decimal", "Decimal").format(format);
			for (String invalid : new String[]{"NaN", "Infinity", "-Infinity", "1e300", "-1e300"})
				assertNull(invalid, CellFormatter.parse(decimal, invalid));
			assertEquals(Long.valueOf(format == ColumnDef.Format.HUNDREDTHS ? 125 : 1250), CellFormatter.parse(decimal, "1.25"));
		}
	}

	@Test public void decimalFiltersUseTheTypedValueInsteadOfBinaryApproximation(){
		ColumnDef hundredths = ColumnDef.num("hundredths", "Hundredths").format(ColumnDef.Format.HUNDREDTHS);
		ColumnDef thousandths = ColumnDef.num("thousandths", "Thousandths").format(ColumnDef.Format.THOUSANDTHS);
		assertEquals(Long.valueOf(101), CellFormatter.parse(hundredths, "1.005"));
		assertEquals("preserve Math.round's negative half-step direction", Long.valueOf(-100), CellFormatter.parse(hundredths, "-1.005"));
		assertEquals(Long.valueOf(1235), CellFormatter.parse(thousandths, "1.2345"));
		assertEquals("values above double's exact-integer range keep every typed digit",
				Long.valueOf(9007199254740991L), CellFormatter.parse(hundredths, "90071992547409.91"));
		assertEquals(Long.valueOf(Long.MAX_VALUE), CellFormatter.parse(hundredths, "92233720368547758.07"));
		assertNull(CellFormatter.parse(hundredths, "92233720368547758.075"));
	}
}
