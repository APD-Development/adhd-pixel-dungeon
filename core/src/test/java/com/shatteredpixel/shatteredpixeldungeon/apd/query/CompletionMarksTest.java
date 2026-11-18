/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompletionMarksTest {

	private final DatasetDef def = Datasets.runs();
	private final List<Row> rows = new ArrayList<>();

	private Row run( String cls, int outcome, int difficulty, boolean finished ){
		Row r = new Row("r" + rows.size(), null, "r" + rows.size(), def.columns().size());
		r.finished = finished;
		r.setText(def.indexOf(FactKeys.HERO_CLASS), cls, Fact.PROV_RECORDED);
		r.setNum(def.indexOf(FactKeys.OUTCOME), outcome, Fact.PROV_RECORDED);
		if (difficulty > 0) r.setNum(def.indexOf(FactKeys.DIFFICULTY), difficulty, Fact.PROV_RECORDED);
		rows.add(r);
		return r;
	}

	private Map<String, CompletionMarks.Mark> marks(){
		return CompletionMarks.compute(new Dataset(def, rows, new ArrayList<Dataset.RunInfo>()));
	}

	@Test
	public void highestDifficultyWonIsPerClassAndNothingIsInvented(){
		run("MAGE", LifecycleEvents.OUTCOME_WON, 2, true);
		run("MAGE", LifecycleEvents.OUTCOME_WON, 4, true);
		run("MAGE", LifecycleEvents.OUTCOME_DIED, 5, true);       //a loss on 5 does not count
		run("MAGE", LifecycleEvents.OUTCOME_WON, 0, true);        //won, difficulty not recorded
		run("WARRIOR", LifecycleEvents.OUTCOME_DIED, 3, true);
		run("ROGUE", LifecycleEvents.OUTCOME_WON, 5, false);      //still going: not counted
		Map<String, CompletionMarks.Mark> m = marks();
		assertEquals(4, m.get("MAGE").bestDifficultyWon);
		assertEquals(3, m.get("MAGE").wins);
		assertEquals(1, m.get("MAGE").winsWithoutDifficulty);
		assertEquals(4, m.get("MAGE").runs);
		assertEquals(0, m.get("WARRIOR").wins);
		assertEquals(0, m.get("WARRIOR").bestDifficultyWon);
		assertNull("an unfinished run does not create a mark", m.get("ROGUE"));
	}

	@Test
	public void repeatedRankingRecordsCountOnce(){
		Row dup = run("MAGE", LifecycleEvents.OUTCOME_WON, 3, true);
		dup.duplicate = true;
		run("MAGE", LifecycleEvents.OUTCOME_WON, 3, true);
		assertEquals(1, marks().get("MAGE").wins);
		assertTrue(CompletionMarks.compute(null).isEmpty());
	}
}
