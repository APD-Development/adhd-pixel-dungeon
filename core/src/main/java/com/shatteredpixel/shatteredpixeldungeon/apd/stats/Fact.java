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


package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

//One derived value about a run, with where it came from. A value that is not known is simply not there: it is never
//stored as zero. Facts are DERIVED data: they can always be rebuilt from the raw logs and import logs.
public final class Fact {

	public static final int NUM = 1, TEXT = 2;

	//where the value comes from
	public static final int PROV_RECORDED = 1;    //counted or read from events APD recorded
	public static final int PROV_LEGACY = 2;      //copied from a record in APD's own rankings file
	public static final int PROV_INFERRED = 3;    //derived from events the recorder itself flagged as inferred
	public static final int PROV_INCOMPLETE = 4;  //recorded, but the log has gaps: the real number may be higher

	public final int kind;
	public final long num;
	public final String text;
	public final int prov;

	private Fact( int kind, long num, String text, int prov ){
		this.kind = kind;
		this.num = num;
		this.text = text;
		this.prov = prov;
	}

	public static Fact num( long value, int prov ){
		return new Fact(NUM, value, "", prov);
	}

	public static Fact text( String value, int prov ){
		return new Fact(TEXT, 0, value == null ? "" : value, prov);
	}

	public boolean isNum(){
		return kind == NUM;
	}

	@Override
	public boolean equals( Object o ){
		if (!(o instanceof Fact)) return false;
		Fact f = (Fact) o;
		return kind == f.kind && num == f.num && text.equals(f.text) && prov == f.prov;
	}

	@Override
	public int hashCode(){
		return (int) (num * 31 + text.hashCode() + kind * 7 + prov);
	}

	@Override
	public String toString(){
		return (kind == NUM ? Long.toString(num) : "\"" + text + "\"") + "/p" + prov;
	}
}
