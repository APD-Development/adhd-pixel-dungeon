package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RecordedRuleSettings;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Referential-integrity checks for the UIMSD schemas and their shipped navigation. */
public class DatasetSchemaContractTest {

	private static List<DatasetDef> allDefs(){
		List<DatasetDef> defs = new ArrayList<>();
		defs.addAll(Datasets.defs());
		defs.addAll(EventDatasets.defs());
		defs.addAll(FactDatasets.defs());
		return defs;
	}

	@Test
	public void everyDatasetViewAndLinkResolvesToCurrentSchema(){
		Map<String, DatasetDef> defs = new HashMap<>();
		for (DatasetDef def : allDefs())
			assertTrue("duplicate dataset id " + def.id, defs.put(def.id, def) == null);

		Map<String, ViewDef> views = new HashMap<>();
		for (ViewDef view : Datasets.allViews()){
			assertTrue("duplicate view id " + view.id, views.put(view.id, view) == null);
			assertNotNull(view.id + " reads a missing dataset " + view.dataset(), defs.get(view.dataset()));
			if (view.instanceQuery() != null)
				assertEquals(view.id + " instances must read the same dataset", view.dataset(), view.instanceQuery().dataset);
		}

		for (DatasetDef def : defs.values()){
			for (LinkDef link : def.links){
				assertNotNull(def.id + " link uses missing source column " + link.ourColumn, def.column(link.ourColumn));
				ViewDef targetView = views.get(link.targetView);
				assertNotNull(def.id + " link targets missing shipped view " + link.targetView, targetView);
				DatasetDef target = defs.get(targetView.dataset());
				assertNotNull(def.id + " link targets missing dataset " + targetView.dataset(), target);
				assertNotNull(def.id + " link uses missing target column " + link.targetColumn, target.column(link.targetColumn));
			}
		}
	}

	@Test
	public void runsSchemaContainsEachFrozenCustomSettingExactlyOnce(){
		DatasetDef runs = Datasets.runs();
		assertEquals("the permanent wire schema has exactly 18 settings", 18, RecordedRuleSettings.KEYS.size());
		Set<String> expected = new HashSet<>();
		for (String setting : RecordedRuleSettings.KEYS){
			String id = CustomRuleFacts.key(setting);
			expected.add(id);
			ColumnDef column = runs.column(id);
			assertNotNull(setting, column);
			assertEquals(setting, ColumnDef.Type.NUM, column.type);
		}

		Set<String> actual = new HashSet<>();
		for (ColumnDef column : runs.columns())
			if (column.id.startsWith(CustomRuleFacts.PREFIX) && expected.contains(column.id)) actual.add(column.id);
		assertEquals(expected, actual);
	}
}
