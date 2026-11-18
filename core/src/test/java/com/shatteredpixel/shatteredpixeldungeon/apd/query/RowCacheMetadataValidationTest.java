package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ByteSink;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RowCacheMetadataValidationTest {

	private Map<String, DatasetDef> definitions(){
		Map<String, DatasetDef> defs = new LinkedHashMap<>();
		defs.put("things", QuerySupport.def());
		return defs;
	}

	private byte[] cache( int logFlags, int logRulesetKind, int mergeKeyMarker,
			int rowFlags, int rowRulesetKind, int cellKind ){
		String[] table = { "", "things", "name", "row", "run", "ruleset", "telemetry", "visible" };
		ByteSink out = new ByteSink(256);
		out.writeBytes(new byte[]{'A', 'P', 'D', 'R'})
				.writeUnsigned(2)
				.writeUnsigned(RowBuilders.BUILDER_VERSION)
				.writeString(FactKinds.fingerprint())
				.writeUnsigned(0)
				.writeUnsigned(table.length);
		for (String value : table) out.writeString(value);

		out.writeUnsigned(0).writeUnsigned(0).writeUnsigned(0)
				.writeUnsigned(0).writeUnsigned(0)
				.writeByte(logFlags).writeByte(logRulesetKind)
				.writeUnsigned(5).writeUnsigned(0);

		out.writeUnsigned(1).writeString("things")
				.writeUnsigned(1).writeUnsigned(2)
				.writeUnsigned(1).writeUnsigned(3)
				.writeByte(mergeKeyMarker);
		if (mergeKeyMarker == 1) out.writeUnsigned(0);
		out.writeUnsigned(4)
				.writeByte(rowFlags).writeByte(rowRulesetKind)
				.writeUnsigned(5).writeUnsigned(6)
				.writeByte(cellKind);
		if (cellKind == Row.K_NUM) out.writeSigned(7);
		else if (cellKind == Row.K_TEXT) out.writeUnsigned(7);
		return out.writeInt32(out.crc32()).toByteArray();
	}

	private LogRows decode( int logFlags, int logRulesetKind, int mergeKeyMarker,
			int rowFlags, int rowRulesetKind, int cellKind ){
		return RowCodec.decode(cache(logFlags, logRulesetKind, mergeKeyMarker,
				rowFlags, rowRulesetKind, cellKind), 0, definitions());
	}

	@Test public void validMetadataStillDecodesExactly(){
		LogRows rows = decode(0, 0, 0, 3, 0, Row.K_TEXT);
		assertNotNull(rows);
		assertEquals("visible", rows.rows.get("things").get(0).text(QuerySupport.NAME));
	}

	@Test public void reservedLogFlagsRequireRawLogRebuild(){
		assertNull(decode(8, 0, 0, 3, 0, Row.K_TEXT));
	}

	@Test public void reservedLogRulesetTagRequiresRawLogRebuild(){
		assertNull(decode(0, 3, 0, 3, 0, Row.K_TEXT));
	}

	@Test public void nonBooleanMergeKeyMarkerRequiresRawLogRebuild(){
		assertNull(decode(0, 0, 2, 3, 0, Row.K_TEXT));
	}

	@Test public void reservedRowFlagsRequireRawLogRebuild(){
		assertNull(decode(0, 0, 0, 16, 0, Row.K_TEXT));
	}

	@Test public void reservedRowRulesetTagRequiresRawLogRebuild(){
		assertNull(decode(0, 0, 0, 3, 3, Row.K_TEXT));
	}

	@Test public void reservedCellKindRequiresRawLogRebuild(){
		assertNull(decode(0, 0, 0, 3, 0, 3));
	}
}
