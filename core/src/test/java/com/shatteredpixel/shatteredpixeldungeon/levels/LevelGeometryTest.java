package com.shatteredpixel.shatteredpixeldungeon.levels;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Point;

import org.junit.Test;

/** Grid geometry used everywhere: king-move distance, adjacency, true distance, borders and cell/point conversion. */
public class LevelGeometryTest {
	private static SewerLevel level(){
		HeadlessTestSupport.start();
		SewerLevel l = new SewerLevel();
		l.setSize(10, 8);
		return l;
	}

	@Test public void distanceIsKingMoves(){
		SewerLevel l = level();
		int a = l.pointToCell(new Point(2, 2));
		assertEquals(0, l.distance(a, a));
		assertEquals(3, l.distance(a, l.pointToCell(new Point(5, 4))));
		assertTrue(l.adjacent(a, l.pointToCell(new Point(3, 3))));
		assertFalse(l.adjacent(a, a));
		assertFalse(l.adjacent(a, l.pointToCell(new Point(4, 2))));
		assertEquals(5f, l.trueDistance(l.pointToCell(new Point(0, 0)), l.pointToCell(new Point(3, 4))), 1e-5f);
	}

	@Test public void bordersAreNotInsideTheMap(){
		SewerLevel l = level();
		assertFalse(l.insideMap(0));
		assertFalse(l.insideMap(l.pointToCell(new Point(9, 3))));
		assertFalse(l.insideMap(l.pointToCell(new Point(4, 7))));
		assertTrue(l.insideMap(l.pointToCell(new Point(1, 1))));
		assertTrue(l.insideMap(l.pointToCell(new Point(8, 6))));
	}

	@Test public void cellsAndPointsConvertBothWays(){
		SewerLevel l = level();
		for (int c = 0; c < l.length(); c++) assertEquals(c, l.pointToCell(l.cellToPoint(c)));
	}
}
