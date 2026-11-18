/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
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

package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.watabou.utils.Bundle;
import org.json.*;
import org.junit.Test;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.Assert.*;

public class GplJsonCompatibilityTest {
    private JSONObject fixture() throws Exception {
        return new JSONObject(Files.readString(Paths.get("../../test/resources/apd/gpl-json-legacy.json")));
    }
    private String canonical(Object value) {
        StringBuilder out = new StringBuilder();
        LevelGenerationHarness.canonicalize(value, out);
        return out.toString();
    }
    @Test public void historicalNumericSpellingsAndAllDoubleBitsRemainUnchanged() throws Exception {
        JSONArray numbers = fixture().getJSONArray("numbers");
        for (int i=0;i<numbers.length();i++) {
            JSONObject row=numbers.getJSONObject(i);
            Object parsed=new JSONTokener(row.getString("token")).nextValue();
            assertEquals(row.getString("canonical"),canonical(parsed));
            assertEquals(row.getString("double_bits"),Long.toHexString(Double.doubleToRawLongBits(((Number)parsed).doubleValue())));
        }
    }
    @Test public void realBundleReadsAndRewritesLegacyValuesWithoutLosingFields() throws Exception {
        String original=fixture().getString("save");
        Bundle bundle=Bundle.read(new ByteArrayInputStream(original.getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.MAX_VALUE,bundle.getLong("turn"));
        assertArrayEquals(new long[]{0,1,4294967296L,Long.MAX_VALUE},bundle.getLongArray("count"));
        assertEquals(Float.floatToRawIntBits(0.0001f),Float.floatToRawIntBits(bundle.getFloat("hp")));
        assertEquals("run \"A\"\n雪",bundle.getString("name"));
        for (boolean compressed:new boolean[]{false,true}) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            assertTrue(Bundle.write(bundle,bytes,compressed));
            Bundle restored=Bundle.read(new ByteArrayInputStream(bytes.toByteArray()));
            JSONObject before=new JSONObject(original),after=new JSONObject(restored.toString());
            assertEquals(before.keySet(),after.keySet());
            assertTrue(before.similar(after));
            assertEquals(Long.MAX_VALUE,restored.getLong("turn"));
            assertEquals(Double.doubleToRawLongBits(before.getDouble("damage")),Double.doubleToRawLongBits(after.getDouble("damage")));
        }
    }
    @Test public void floatWritesUseTheLegacyDoubleOverloadsAndExactNumericValues() throws Exception {
        Bundle bundle=new Bundle();
        float[] values={0.0001f,0.1f,-0.0f,Float.MIN_VALUE,Float.MAX_VALUE};
        bundle.put("scalar",values[0]);bundle.put("array",values);
        JSONObject written=new JSONObject(bundle.toString());
        assertEquals(Double.doubleToRawLongBits((double)values[0]),Double.doubleToRawLongBits(written.getDouble("scalar")));
        JSONArray array=written.getJSONArray("array");
        for(int i=0;i<values.length;i++)
            assertEquals(Double.doubleToRawLongBits((double)values[i]),Double.doubleToRawLongBits(array.getDouble(i)));
    }
    @Test public void fingerprintStillDetectsIntegerAndDecimalChanges() {
        assertNotEquals(canonical(Long.MAX_VALUE),canonical(Long.MAX_VALUE-1));
        assertNotEquals(canonical(new BigDecimal("0.0001")),canonical(new BigDecimal("0.0002")));
        assertEquals("9223372036854775807",canonical(Long.MAX_VALUE));
    }
    @Test public void fingerprintCannotHideExtraPrecisionOrOverflow() {
        for (String value:new String[]{"9007199254740993.0","0.1000000000000000000001","1e400"}) {
            try { canonical(new BigDecimal(value)); fail("Precision loss must fail: "+value); }
            catch (AssertionError expected) { assertTrue(expected.getMessage().contains("without losing precision")); }
        }
    }
}
