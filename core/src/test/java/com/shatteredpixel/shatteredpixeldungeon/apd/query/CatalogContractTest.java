package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class CatalogContractTest {

	private Dataset dataset( String id ){
		return new Dataset(new DatasetDef(id, id, id), new ArrayList<Row>(), null);
	}

	private ViewDef view( String id, String dataset ){
		return new ViewDef(id, id, id, Query.of(dataset));
	}

	@Test
	public void returnedListsAreDetachedSnapshots(){
		Catalog catalog = new Catalog();
		Dataset dataset = dataset("runs");
		ViewDef view = view("runs", "runs");
		catalog.add(dataset).add(view);

		List<Dataset> datasets = catalog.datasets();
		List<ViewDef> views = catalog.views();
		datasets.clear();
		views.clear();

		assertEquals(1, catalog.datasets().size());
		assertEquals(1, catalog.views().size());
		assertSame(dataset, catalog.dataset("runs"));
		assertSame(view, catalog.view("runs"));
	}

	@Test
	public void replacingAnIdKeepsItsPositionAndCreatesNoDuplicate(){
		Catalog catalog = new Catalog();
		Dataset oldRuns = dataset("runs");
		Dataset items = dataset("items");
		Dataset newRuns = dataset("runs");
		ViewDef oldRunView = view("runs", "runs");
		ViewDef itemView = view("items", "items");
		ViewDef newRunView = view("runs", "runs");

		catalog.add(oldRuns).add(items).add(oldRunView).add(itemView);
		catalog.add(newRuns).add(newRunView);

		assertEquals(Arrays.asList("runs", "items"), datasetIds(catalog.datasets()));
		assertEquals(Arrays.asList("runs", "items"), viewIds(catalog.views()));
		assertSame(newRuns, catalog.dataset("runs"));
		assertSame(newRunView, catalog.view("runs"));
	}

	@Test
	public void aViewResolvesTheCurrentDatasetByIdAndMissingIdsStayMissing(){
		Catalog catalog = new Catalog();
		ViewDef runs = view("runs-view", "runs");
		Dataset first = dataset("runs");
		Dataset replacement = dataset("runs");

		assertNull(catalog.dataset("missing"));
		assertNull(catalog.view("missing"));
		assertNull(catalog.datasetOf(runs));

		catalog.add(first);
		assertSame(first, catalog.datasetOf(runs));
		catalog.add(replacement);
		assertSame(replacement, catalog.datasetOf(runs));
	}

	private List<String> datasetIds( List<Dataset> datasets ){
		List<String> ids = new ArrayList<>();
		for (Dataset dataset : datasets) ids.add(dataset.def.id);
		return ids;
	}

	private List<String> viewIds( List<ViewDef> views ){
		List<String> ids = new ArrayList<>();
		for (ViewDef view : views) ids.add(view.id);
		return ids;
	}
}
