package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty;
import com.shatteredpixel.shatteredpixeldungeon.ui.DifficultyBar;

import org.junit.Test;

//The run-setup difficulty picker: every level 1-5 has a name, 3 is the normal game, and the explanation says the difficulty is fixed once the run starts.
public class DifficultyBarTest {

	@Test
	public void everyLevelHasAName_andThreeIsNormal(){
		for (int level = Difficulty.MIN; level <= Difficulty.MAX; level++) assertFalse("level " + level, DifficultyBar.nameOf(level).isEmpty());
		assertEquals("Normal", DifficultyBar.nameOf(Difficulty.DEFAULT));
		assertEquals(3, Difficulty.DEFAULT);
		assertEquals("", DifficultyBar.nameOf(0));
		assertEquals("", DifficultyBar.nameOf(6));
	}

	@Test
	public void theExplanationSaysThatARunKeepsItsDifficulty(){
		String d = DifficultyBar.describe();
		assertTrue(d.contains("3 is the normal game"));
		assertTrue(d.contains("fixed when the run begins"));
		assertTrue(d.contains("never changes a run that is already going"));
	}

	@Test
	public void normalIsStillTheUnmodifiedBaseline(){
		//the move to run setup changes where the level is chosen, never what a level does
		assertEquals(0, Difficulty.balanceRevision(Difficulty.DEFAULT));
	}
}
