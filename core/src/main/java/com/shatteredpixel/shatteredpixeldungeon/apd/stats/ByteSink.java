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

import java.util.Arrays;

//A growable byte buffer for building frames and payloads.
public final class ByteSink {

	private byte[] buf;
	private int size;

	public ByteSink(){
		this(64);
	}

	public ByteSink( int capacity ){
		buf = new byte[Math.max(16, capacity)];
	}

	private void ensure( int extra ){
		int need = size + extra;
		if (need < 0) throw new FormatException("buffer too large");
		if (need > buf.length){
			int cap = Math.max(need, buf.length * 2);
			if (cap < 0) cap = need;
			buf = Arrays.copyOf(buf, cap);
		}
	}

	public ByteSink writeByte( int b ){
		ensure(1);
		buf[size++] = (byte) b;
		return this;
	}

	public ByteSink writeBytes( byte[] b ){
		return writeBytes(b, 0, b.length);
	}

	public ByteSink writeBytes( byte[] b, int off, int len ){
		ensure(len);
		System.arraycopy(b, off, buf, size, len);
		size += len;
		return this;
	}

	public ByteSink writeInt32( int v ){
		ensure(4);
		buf[size++] = (byte) (v >>> 24);
		buf[size++] = (byte) (v >>> 16);
		buf[size++] = (byte) (v >>> 8);
		buf[size++] = (byte) v;
		return this;
	}

	public ByteSink writeInt64( long v ){
		writeInt32((int) (v >>> 32));
		return writeInt32((int) v);
	}

	//LEB128, 7 bits per byte, low bits first. Only for values >= 0 (use writeSigned for others)
	public ByteSink writeUnsigned( long v ){
		if (v < 0) throw new FormatException("negative value passed to writeUnsigned");
		while ((v & ~0x7FL) != 0){
			writeByte((int) ((v & 0x7F) | 0x80));
			v >>>= 7;
		}
		return writeByte((int) v);
	}

	//zigzag + LEB128, small magnitudes stay small whatever the sign
	public ByteSink writeSigned( long v ){
		long zz = (v << 1) ^ (v >> 63);
		while ((zz & ~0x7FL) != 0){
			writeByte((int) ((zz & 0x7F) | 0x80));
			zz >>>= 7;
		}
		return writeByte((int) zz);
	}

	public ByteSink writeBoolean( boolean b ){
		return writeByte(b ? 1 : 0);
	}

	public ByteSink writeFloat( float f ){
		return writeInt32(Float.floatToRawIntBits(f));
	}

	public ByteSink writeDouble( double d ){
		return writeInt64(Double.doubleToRawLongBits(d));
	}

	//length-prefixed UTF-8, used inside headers (event payloads use the string table instead)
	public ByteSink writeString( String s ){
		byte[] b = s.getBytes(Utf8.CHARSET);
		writeUnsigned(b.length);
		return writeBytes(b);
	}

	public int size(){
		return size;
	}

	//Checksum the written bytes without allocating a second copy.
	public int crc32(){
		return (int)LogFormat.crc(buf,0,size());
	}

	public byte[] toByteArray(){
		return Arrays.copyOf(buf, size);
	}

	//direct access for CRC calculation without a copy, valid until the next write
	byte[] rawBuffer(){
		return buf;
	}

	public void reset(){
		size = 0;
	}
}
