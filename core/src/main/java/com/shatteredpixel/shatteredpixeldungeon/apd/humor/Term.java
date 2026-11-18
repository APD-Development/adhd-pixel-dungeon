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

package com.shatteredpixel.shatteredpixeldungeon.apd.humor;

// One condition of a line: `key op value`, with op one of = != >= <= > <. Numbers compare as numbers, anything else as text ignoring case.
// A fact the context does not have makes the term false, whatever the operator.
final class Term {

	final String key;
	final String op;
	final String value;

	Term( String key, String op, String value ){
		this.key = key;
		this.op = op;
		this.value = value;
	}

	static Term parse( String s ){
		String[] ops = { ">=", "<=", "!=", "=", ">", "<" };
		for (String op : ops){
			int i = s.indexOf( op );
			if (i > 0){
				String key = s.substring( 0, i ).trim();
				String value = s.substring( i + op.length() ).trim();
				if (key.isEmpty() || value.isEmpty()) break;
				if (!Context.KEYS.contains( key )) throw new IllegalArgumentException( "unknown key '" + key + "'" );
				return new Term( key, op, value );
			}
		}
		throw new IllegalArgumentException( "cannot read condition '" + s + "'" );
	}

	boolean holds( Context c ){
		String have = c.get( key );
		if (have == null) return false;
		Long a = number( have ), b = number( value );
		if (a != null && b != null){
			switch (op){
				case "=": return a.longValue() == b.longValue();
				case "!=": return a.longValue() != b.longValue();
				case ">=": return a >= b;
				case "<=": return a <= b;
				case ">": return a > b;
				default: return a < b;
			}
		}
		if (op.equals( "=" )) return have.equalsIgnoreCase( value );
		if (op.equals( "!=" )) return !have.equalsIgnoreCase( value );
		return false;
	}

	private static Long number( String s ){
		try {
			return Long.valueOf( s.trim() );
		} catch (NumberFormatException e){
			return null;
		}
	}
}
