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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ByteSink;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ByteSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//The compact binary form of one log's rows, for the row cache. Cells are stored with their column ids, so a column that is added
//later reads as unknown from an older cache (and a removed one is skipped) without breaking anything. The cache is derived data:
//anything unreadable simply means "rebuild from the log".
public final class RowCodec {

	private RowCodec(){}

	private static final byte[] MAGIC = { 'A', 'P', 'D', 'R' };
	private static final int FORMAT = 2;
	private static final int SOURCE_FORMAT = 3;

	//the cache is only valid for this exact log (its size) and this exact builder
	public static byte[] encode( LogRows lr, long logLength, Map<String, DatasetDef> defs ){
		return encode(lr, logLength, defs, null);
	}

	/** Source-bound caches include the SHA-256 of their raw log. Raw histories are unchanged. */
	public static byte[] encode( LogRows lr, long logLength, Map<String, DatasetDef> defs, byte[] sourceFingerprint ){
		if (sourceFingerprint != null && sourceFingerprint.length != 32) throw new IllegalArgumentException("Invalid source fingerprint");
		Map<String, Integer> table = new LinkedHashMap<>();
		LogMeta m = lr.meta;
		intern(table, m.logId);
		intern(table, m.runId);
		intern(table, m.telemetry);
		intern(table, m.rankingId);
		intern(table, m.ruleset == null ? "" : m.ruleset);
		for (Map.Entry<String, List<Row>> e : lr.rows.entrySet()){
			DatasetDef def = defs.get(e.getKey());
			if (def == null) continue;
			for (ColumnDef c : def.columns()) intern(table, c.id);
			for (Row r : e.getValue()){
				intern(table, r.id);
				if (r.mergeKey != null) intern(table, r.mergeKey);
				intern(table, r.runId);
				intern(table, r.ruleset == null ? "" : r.ruleset);
				intern(table, r.telemetry);
				for (int i = 0; i < def.size(); i++) if (r.kind(i) == Row.K_TEXT) intern(table, r.text(i));
			}
		}
		ByteSink out = new ByteSink(4096);
		out.writeBytes(MAGIC);
		out.writeUnsigned(sourceFingerprint == null ? FORMAT : SOURCE_FORMAT);
		out.writeUnsigned(RowBuilders.BUILDER_VERSION);
		//the rows of fact datasets depend on the registry: a different registry is a different cache
		out.writeString(com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds.fingerprint());
		out.writeUnsigned(logLength);
		if (sourceFingerprint != null) out.writeBytes(sourceFingerprint);
		out.writeUnsigned(table.size());
		for (String s : table.keySet()) out.writeString(s);
		out.writeUnsigned(table.get(m.logId)).writeUnsigned(table.get(m.runId)).writeUnsigned(table.get(m.telemetry))
				.writeUnsigned(Math.max(0, m.createdAtMillis)).writeUnsigned(m.segment)
				.writeByte((m.hasRunEnd ? 1 : 0) | (m.imported ? 2 : 0) | (m.unreadable ? 4 : 0))
				.writeByte(m.ruleset == null ? 0 : m.ruleset.equals(Row.RULESET_MIXED) ? 1 : 2)
				.writeUnsigned(table.get(m.ruleset == null ? "" : m.ruleset)).writeUnsigned(table.get(m.rankingId));
		int sections = 0;
		for (String id : lr.rows.keySet()) if (defs.get(id) != null) sections++;
		out.writeUnsigned(sections);
		for (Map.Entry<String, List<Row>> e : lr.rows.entrySet()){
			DatasetDef def = defs.get(e.getKey());
			if (def == null) continue;
			out.writeString(e.getKey());
			out.writeUnsigned(def.size());
			for (ColumnDef c : def.columns()) out.writeUnsigned(table.get(c.id));
			out.writeUnsigned(e.getValue().size());
			for (Row r : e.getValue()){
				out.writeUnsigned(table.get(r.id));
				out.writeByte(r.mergeKey == null ? 0 : 1);
				if (r.mergeKey != null) out.writeUnsigned(table.get(r.mergeKey));
				out.writeUnsigned(table.get(r.runId));
				out.writeByte((r.finished ? 1 : 0) | (r.publicRow ? 2 : 0) | (r.imported ? 4 : 0) | (r.duplicate ? 8 : 0));
				out.writeByte(r.ruleset == null ? 0 : r.ruleset.equals(Row.RULESET_MIXED) ? 1 : 2);
				out.writeUnsigned(table.get(r.ruleset == null ? "" : r.ruleset));
				out.writeUnsigned(table.get(r.telemetry));
				for (int i = 0; i < def.size(); i++){
					int k = r.kind(i);
					out.writeByte(k | (r.prov(i) << 2));
					if (k == Row.K_NUM) out.writeSigned(r.num(i));
					else if (k == Row.K_TEXT) out.writeUnsigned(table.get(r.text(i)));
				}
			}
		}
		out.writeInt32(out.crc32());
		return out.toByteArray();
	}

	private static void intern( Map<String, Integer> t, String s ){
		if (!t.containsKey(s)) t.put(s, t.size());
	}

	//null when the bytes are not a cache for this log and this builder
	public static LogRows decode( byte[] data, long logLength, Map<String, DatasetDef> defs ){
		return decode(data, logLength, defs, null);
	}

	/** An old unbound cache cannot satisfy a source-bound read. */
	public static LogRows decode( byte[] data, long logLength, Map<String, DatasetDef> defs, byte[] sourceFingerprint ){
		if (sourceFingerprint != null && sourceFingerprint.length != 32) return null;
		try {
			if (data.length < 4) return null;
            int bodyLength = data.length - 4;
            int expected = new ByteSource(data,bodyLength,4).readInt32();
            java.util.zip.CRC32 checksum = new java.util.zip.CRC32();
            checksum.update(data,0,bodyLength);
            if (expected != (int)checksum.getValue()) return null;
            ByteSource in = new ByteSource(data,0,bodyLength);
            java.nio.charset.CharsetDecoder textDecoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder();
			for (byte b : MAGIC) if (in.readByte() != (b & 0xFF)) return null;
			int format = in.readUnsignedInt();
			if (format != FORMAT && format != SOURCE_FORMAT) return null;
			if (sourceFingerprint != null && format != SOURCE_FORMAT) return null;
			if (in.readUnsignedInt() != RowBuilders.BUILDER_VERSION) return null;
			if (!com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds.fingerprint().equals(readText(in, textDecoder))) return null;
			if (in.readUnsigned() != logLength) return null;
			if (format == SOURCE_FORMAT){
				byte[] actualFingerprint = in.readBytes(32);
				if (sourceFingerprint != null && !java.util.Arrays.equals(sourceFingerprint, actualFingerprint)) return null;
			}
			int n = in.readUnsignedInt();
			if (n > in.remaining()) return null; // Every string needs at least its length byte.
			String[] table = new String[n];
			for (int i = 0; i < n; i++) table[i] = readText(in, textDecoder);
			LogRows lr = new LogRows();
			LogMeta m = lr.meta;
			m.logId = table[in.readUnsignedInt()];
			m.runId = table[in.readUnsignedInt()];
			m.telemetry = table[in.readUnsignedInt()];
			m.createdAtMillis = in.readUnsigned();
			m.segment = in.readUnsignedInt();
			int flags = in.readByte();
			if ((flags & ~7) != 0) return null;
			m.hasRunEnd = (flags & 1) != 0;
			m.imported = (flags & 2) != 0;
			m.unreadable = (flags & 4) != 0;
			int rk = in.readByte();
			if (rk > 2) return null;
			String rs = table[in.readUnsignedInt()];
			m.ruleset = rk == 0 ? null : rk == 1 ? Row.RULESET_MIXED : rs;
			m.rankingId = table[in.readUnsignedInt()];
			int sections = in.readUnsignedInt();
			for (int s = 0; s < sections; s++){
				String id = readText(in, textDecoder);
				DatasetDef def = defs.get(id);
				int cols = in.readUnsignedInt();
				if (cols > in.remaining()) return null; // Each column has a table-index byte.
				int[] map = new int[cols];
				for (int c = 0; c < cols; c++) {
                    String columnId = table[in.readUnsignedInt()];
                    map[c] = def == null ? -1 : def.indexOf(columnId);
                }
				int rows = in.readUnsignedInt();
				if (rows > in.remaining()) return null; // Even a zero-column row has metadata bytes.
				List<Row> list = new ArrayList<>(rows);
				for (int i = 0; i < rows; i++){
					String rid = table[in.readUnsignedInt()];
					int hasMergeKey = in.readByte();
					if (hasMergeKey > 1) return null;
					String mergeKey = hasMergeKey == 1 ? table[in.readUnsignedInt()] : null;
					String runId = table[in.readUnsignedInt()];
					Row r = new Row(rid, mergeKey, runId, def == null ? 0 : def.size());
					int f = in.readByte();
					if ((f & ~15) != 0) return null;
					r.finished = (f & 1) != 0;
					r.publicRow = (f & 2) != 0;
					r.imported = (f & 4) != 0;
					r.duplicate = (f & 8) != 0;
					int rrk = in.readByte();
					if (rrk > 2) return null;
					String rrs = table[in.readUnsignedInt()];
					r.ruleset = rrk == 0 ? null : rrk == 1 ? Row.RULESET_MIXED : rrs;
					r.telemetry = table[in.readUnsignedInt()];
					for (int c = 0; c < cols; c++){
						int meta = in.readByte();
						int k = meta & 3, prov = (meta >> 2) & 63;
						if (k > Row.K_TEXT) return null;
						int col = map[c];
						if (k == Row.K_NUM){
							long v = in.readSigned();
							if (col >= 0) r.setNum(col, v, prov);
						} else if (k == Row.K_TEXT){
							String t = table[in.readUnsignedInt()];
							if (col >= 0) r.setText(col, t, prov);
						}
					}
					list.add(r);
				}
				if (def != null) lr.rows.put(id, list);
			}
			if (in.hasRemaining()) return null;
			return lr;
		} catch (RuntimeException | java.nio.charset.CharacterCodingException e){
			return null;
		}
	}
    // Cache text must be valid UTF-8: malformed bytes require rebuilding from the raw log.
    private static String readText(ByteSource in, java.nio.charset.CharsetDecoder decoder)
            throws java.nio.charset.CharacterCodingException {
        byte[] bytes = in.readBytes(in.readUnsignedInt());
        boolean ascii = true;
        for (byte value : bytes) if (value < 0) { ascii = false; break; }
        return ascii ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                : decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    }

}

