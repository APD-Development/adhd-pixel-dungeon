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

//Reads the primitives written by ByteSink from a byte array. Every read is bounds-checked and throws
//FormatException instead of an array error, so damaged data can never crash the reader.
public final class ByteSource {

	private final byte[] data;
	private int pos;
	private final int limit;

	public ByteSource( byte[] data ){
		this(data, 0, data.length);
	}

	public ByteSource( byte[] data, int offset, int length ){
		if (offset < 0 || length < 0 || offset + length > data.length || offset + length < 0){
			throw new FormatException("bad source range");
		}
		this.data = data;
		this.pos = offset;
		this.limit = offset + length;
	}

	public int position(){
		return pos;
	}

	public int remaining(){
		return limit - pos;
	}

	public boolean hasRemaining(){
		return pos < limit;
	}

	public int readByte(){
		if (pos >= limit) throw new FormatException("unexpected end of data");
		return data[pos++] & 0xFF;
	}

	public byte[] readBytes( int len ){
		if (len < 0 || len > limit - pos) throw new FormatException("unexpected end of data");
		byte[] out = new byte[len];
		System.arraycopy(data, pos, out, 0, len);
		pos += len;
		return out;
	}

	public byte[] readRemaining(){
		return readBytes(limit - pos);
	}

	public void skip( int len ){
		if (len < 0 || len > limit - pos) throw new FormatException("unexpected end of data");
		pos += len;
	}

	public int readInt32(){
		if (limit - pos < 4) throw new FormatException("unexpected end of data");
		int v = ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16)
				| ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
		pos += 4;
		return v;
	}

	public long readInt64(){
		long hi = readInt32() & 0xFFFFFFFFL;
		long lo = readInt32() & 0xFFFFFFFFL;
		return (hi << 32) | lo;
	}

	public long readUnsigned(){
		long result = 0;
		int shift = 0;
		for (int i = 0; i < 10; i++){
			int b = readByte();
			if (i == 9 && (b & 0x7E) != 0) throw new FormatException("varint overflows 64 bits");
			result |= (long) (b & 0x7F) << shift;
			if ((b & 0x80) == 0) return result;
			shift += 7;
		}
		throw new FormatException("varint too long");
	}

	public long readSigned(){
		long zz = readUnsigned();
		return (zz >>> 1) ^ -(zz & 1);
	}

	public int readUnsignedInt(){
		long v = readUnsigned();
		if (v < 0 || v > Integer.MAX_VALUE) throw new FormatException("value does not fit an int");
		return (int) v;
	}

	public boolean readBoolean(){
		int b = readByte();
		if (b > 1) throw new FormatException("bad boolean");
		return b == 1;
	}

	public float readFloat(){
		return Float.intBitsToFloat(readInt32());
	}

	public double readDouble(){
		return Double.longBitsToDouble(readInt64());
	}

	public String readString(){
		int len = readUnsignedInt();
		return Utf8.decode(readBytes(len));
	}
}
