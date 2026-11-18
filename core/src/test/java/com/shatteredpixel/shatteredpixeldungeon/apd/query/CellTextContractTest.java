package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Direct contracts for the immutable text/provenance value shared by tables,
 * home cards, row details and report builders.
 */
public class CellTextContractTest {

	@Test
	public void knowledgeStateIdsRemainStableAndContiguous(){
		assertEquals(0, CellText.EXACT);
		assertEquals(1, CellText.LOWER);
		assertEquals(2, CellText.INFERRED);
		assertEquals(3, CellText.LEGACY);
		assertEquals(4, CellText.UNKNOWN);
		assertEquals(5, CellText.HIDDEN);
	}

	@Test
	public void twoArgumentConstructorDefaultsToNormalSampleAndReturnsItsText(){
		CellText cell = new CellText(">=12", CellText.LOWER);

		assertEquals(">=12", cell.text);
		assertEquals(CellText.LOWER, cell.state);
		assertFalse(cell.small);
		assertEquals(cell.text, cell.toString());
	}

	@Test
	public void threeArgumentConstructorPreservesSmallSampleAndKnowledgeState(){
		CellText cell = new CellText("~6", CellText.INFERRED, true);

		assertEquals("~6", cell.text);
		assertEquals(CellText.INFERRED, cell.state);
		assertTrue(cell.small);
		assertEquals(cell.text, cell.toString());
	}
}
