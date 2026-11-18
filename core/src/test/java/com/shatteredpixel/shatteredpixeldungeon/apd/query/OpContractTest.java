package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class OpContractTest {

	@Test
	public void eachColumnTypeOffersExactlyItsPublicOperatorsInPickerOrder(){
		assertArrayEquals(new Op[]{
				Op.EQ, Op.NE, Op.LT, Op.LE, Op.GT, Op.GE, Op.BETWEEN, Op.KNOWN, Op.UNKNOWN
		}, Op.ops(ColumnDef.Type.NUM));
		assertArrayEquals(new Op[]{
				Op.EQ, Op.NE, Op.CONTAINS, Op.IN, Op.KNOWN, Op.UNKNOWN
		}, Op.ops(ColumnDef.Type.TEXT));
		assertArrayEquals(new Op[]{
				Op.EQ, Op.NE, Op.IN, Op.KNOWN, Op.UNKNOWN
		}, Op.ops(ColumnDef.Type.ENUM));
	}

	@Test
	public void onlyPresenceOperatorsOmitAValueAndInternalOperatorsAreNeverOffered(){
		for (Op op : Op.values()){
			assertEquals(op.name(), op != Op.KNOWN && op != Op.UNKNOWN, op.takesValue());
		}

		EnumSet<Op> offered = EnumSet.noneOf(Op.class);
		for (ColumnDef.Type type : ColumnDef.Type.values()) offered.addAll(Arrays.asList(Op.ops(type)));
		for (Op internal : new Op[]{Op.GROUP_VALUE, Op.GROUP_BOUND, Op.GROUP_TEXT, Op.IDENTIFIER_IN}){
			assertFalse(internal.name(), offered.contains(internal));
		}
	}
}
