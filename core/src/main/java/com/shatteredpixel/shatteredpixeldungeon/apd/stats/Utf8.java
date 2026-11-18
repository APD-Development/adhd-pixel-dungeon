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

import java.nio.charset.Charset;

final class Utf8 {

	static final Charset CHARSET = Charset.forName("UTF-8");

    // ASCII stays on the existing fast String path. Other text must be valid UTF-8,
    // rather than silently replacing damaged provenance or string identities.
    static String decode(byte[] bytes) {
        for (byte value : bytes) {
            if (value < 0) {
                try {
                    return CHARSET.newDecoder()
                            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                            .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
                } catch (java.nio.charset.CharacterCodingException invalid) {
                    throw new FormatException("invalid UTF-8 text");
                }
            }
        }
        return new String(bytes, CHARSET);
    }

	private Utf8(){}
}
