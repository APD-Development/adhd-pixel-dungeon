package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//Seed floors of ONE finished run: what each floor of the dungeon holds, floor 1 to 26, with the provenance of every row explicit.
//
//BASIS is the point. A floor the run generated and recorded (telemetry 1.4+) shows what the run's own log says: RECORDED, and it is
//authoritative. A floor that never existed during the run shows a CANONICAL floor: what the seed gives after the floors before it
//were generated in order under APD's reference context (see FloorReplay). That is never what this run "would have" met: play changes
//what later floors hold. Canonical values carry the ~ mark (worked out, not recorded). A reached floor the log did not record
//(older telemetry) is canonical too, labelled as such. The comparison dataset sets canonical against recorded for floors that have both.
public final class SeedFloors {

	private SeedFloors(){}

	public static final String ITEMS = "seedfloors", MOBS = "seedmobs", COMPARE = "seedcompare";

	public static final int BASIS_RECORDED = 1, BASIS_UNREACHED = 2, BASIS_UNRECORDED = 3, BASIS_REACH_UNKNOWN = 4, BASIS_COMPARISON = 5;

	//the run cannot be replayed (unfinished, no seed, ...): the message says why, in words for the player
	public static final class NotAvailable extends Exception {
		public NotAvailable( String why ){
			super(why);
		}
	}

	//what a replay of a run needs, read from the run's row
	public static final class Spec {
		public String runId = "";
		public long seed;
		//the hero class name as recorded (WARRIOR, MAGE, ...)
		public String heroClass = "";
		public int challenges;
		public int exoticChance;
		public boolean exoticKnown;
		//the run's difficulty (3 when it was not recorded: such a run was played at the baseline balance)
		public int difficulty = 3;
		//and the balance revision it was played with (0 when none was recorded: no modifiers)
		public int balance;
		//the deepest floor the run entered, -1 when the log does not say
		public int reached = -1;
		public String ruleset;
		//what a replay of this run depends on (the replay engine adds its own version to it)
		public String key(){
			return seed + "|" + heroClass + "|" + challenges + "|" + exoticChance + "|" + difficulty + "|" + balance;
		}
	}

	//one item the run's own log says was generated on a floor (true identity, finished runs only)
	public static final class Recorded {
		public int floor;
		public String trueClass = "";
		public int level;
		public boolean cursed;
		public String enchant = "";
		public String container = "";
		public String room = "";
		//true when level/cursed/enchant are the item's state AT GENERATION (recorded then). Otherwise they are unknown: the log only has a later
		//state, which is no proof of how the game generated the item.
		public boolean stateKnown;
	}

	public static final class Built {
		public Spec spec;
		public Dataset items, mobs, compare;
		public int recordedFloors, canonicalFloors;
	}

	//---- schema ----

	private static ColumnDef basis( String id ){
		return ColumnDef.enumeration(id, "Basis").shown().group("Where it comes from")
				.label(BASIS_RECORDED, "Recorded (the run's own log)")
				.label(BASIS_UNREACHED, "Canonical (run never got here)")
				.label(BASIS_UNRECORDED, "Canonical (run was here, floor not recorded)")
				.label(BASIS_REACH_UNKNOWN, "Canonical (how deep the run got is unknown)")
				.label(BASIS_COMPARISON, "Comparison")
				.describe("Where the entry comes from. Recorded entries are what the run's own log says. Canonical entries are what the seed gives when its floors are "
						+ "built in order under fixed, standard rules: never exactly what this run would have met.");
	}

	private static ColumnDef yesNo( ColumnDef c ){
		return c.label(0, "No").label(1, "Yes");
	}

	static DatasetDef itemsDef(){
		DatasetDef d = new DatasetDef(ITEMS, "Seed floors: items",
				"What every floor of this run's seed holds. Floors the run played show its own recorded log; every other floor is canonical: rebuilt from the seed under fixed, standard rules, and marked ~.");
		d.countLabel = "Items";
		d.add(ColumnDef.text("sf.run", "Run").group("Where it comes from"));
		d.add(ColumnDef.num("sf.floor", "Floor").shown().group("Where"));
		d.add(basis("sf.basis"));
		d.add(ColumnDef.text("sf.item", "Item").shown().format(ColumnDef.Format.CLASS_NAME).internal().group("What"));
		d.add(ColumnDef.text("sf.kind", "Kind").shown().internal().group("What"));
		d.add(ColumnDef.num("sf.level", "Level when generated").shown().internal().group("What"));
		d.add(yesNo(ColumnDef.enumeration("sf.cursed", "Cursed when generated")).shown().internal().group("What"));
		d.add(ColumnDef.text("sf.enchant", "Enchantment when generated").internal().group("What"));
		d.add(ColumnDef.num("sf.qty", "Quantity").internal().group("What"));
		d.add(ColumnDef.text("sf.container", "Placed in").shown().internal().group("Where"));
		d.add(ColumnDef.text("sf.room", "Room").internal().group("Where"));
		d.add(ColumnDef.num("sf.pos", "Cell").internal().group("Where"));
		return d;
	}

	static DatasetDef mobsDef(){
		DatasetDef d = new DatasetDef(MOBS, "Seed floors: creatures",
				"Creatures placed on the canonical floors (rebuilt from the seed, marked ~). Floors the run played are not listed here: their creatures are in the run's own history.");
		d.countLabel = "Creatures";
		d.add(ColumnDef.text("sm.run", "Run").group("Where it comes from"));
		d.add(ColumnDef.num("sm.floor", "Floor").shown().group("Where"));
		d.add(basis("sm.basis"));
		d.add(ColumnDef.text("sm.mob", "Creature").shown().format(ColumnDef.Format.CLASS_NAME).internal().group("What"));
		d.add(ColumnDef.num("sm.hp", "Health").shown().internal().group("What"));
		d.add(ColumnDef.text("sm.champion", "Champion").internal().group("What"));
		d.add(ColumnDef.text("sm.room", "Room").internal().group("Where"));
		d.add(ColumnDef.num("sm.pos", "Cell").internal().group("Where"));
		return d;
	}

	static DatasetDef compareDef(){
		DatasetDef d = new DatasetDef(COMPARE, "Seed floors: recorded vs canonical",
				"For every floor the run played: how many items its own log has, how many a canonical rebuild of the seed gives, and how many are the same. "
						+ "Items whose state when generated was not recorded are matched by kind only. It shows how far play moved the floors away from the seed's starting point.");
		d.countLabel = "Floors";
		d.add(ColumnDef.text("sc.run", "Run").group("Where it comes from"));
		d.add(ColumnDef.num("sc.floor", "Floor").shown().group("Where"));
		d.add(basis("sc.basis"));
		d.add(ColumnDef.num("sc.recorded", "Recorded items").shown().internal().group("Counts"));
		d.add(ColumnDef.num("sc.canonical", "Canonical items").shown().internal().group("Counts"));
		d.add(ColumnDef.num("sc.same", "Same in both").shown().internal().group("Counts"));
		d.add(ColumnDef.num("sc.kindOnly", "Same kind, state not recorded").shown().internal().group("Counts"));
		d.add(ColumnDef.num("sc.onlyRecorded", "Only recorded").shown().internal().group("Counts"));
		d.add(ColumnDef.num("sc.onlyCanonical", "Only canonical").shown().internal().group("Counts"));
		d.add(ColumnDef.num("sc.agree", "Agreement %").shown().format(ColumnDef.Format.HUNDREDTHS).internal().group("Counts"));
		return d;
	}

	static List<ViewDef> views(){
		Scope all = Scope.DEFAULT.withRules(Scope.Rules.ALL, "");
		List<ViewDef> v = new ArrayList<>();
		v.add(new ViewDef(ITEMS, "Seed floors: items", "Every item on every floor of the seed. Floors the run played show its own log; the rest are canonical (rebuilt from the seed, marked ~).",
				Query.of(ITEMS).withScope(all).withSorts(Arrays.asList(new Sort("sf.floor", false)))).group("Seed floors (canonical, not recorded history)"));
		v.add(new ViewDef(MOBS, "Seed floors: creatures", "Creatures on the canonical floors, rebuilt from the seed (marked ~).",
				Query.of(MOBS).withScope(all).withSorts(Arrays.asList(new Sort("sm.floor", false)))).group("Seed floors (canonical, not recorded history)"));
		v.add(new ViewDef(COMPARE, "Seed floors: compare", "The run's own recorded floors set against a canonical rebuild, floor by floor.",
				Query.of(COMPARE).withScope(all).withSorts(Arrays.asList(new Sort("sc.floor", false)))).group("Seed floors (canonical, not recorded history)"));
		return v;
	}

	//---- reading what the run row and the log say ----

	private static Row val( Row r, DatasetDef d, String col ){
		return d.indexOf(col) >= 0 && r.isKnown(d.indexOf(col)) ? r : null;
	}

	public static boolean isSeedView( String viewId ){
		return ITEMS.equals(viewId) || MOBS.equals(viewId) || COMPARE.equals(viewId);
	}

	public static Spec spec( Catalog cat, Row run ) throws NotAvailable {
		Dataset runs = cat == null ? null : cat.dataset(Datasets.RUNS);
		if (runs == null || run == null) throw new NotAvailable("No run to replay.");
		DatasetDef d = runs.def;
		if (!run.finished) throw new NotAvailable("This run is not finished. Seed floors are only shown for runs that are over.");
		if (val(run, d, FactKeys.SEED) == null) throw new NotAvailable("This run's seed was not recorded, so its floors cannot be rebuilt.");
		if (val(run, d, FactKeys.HERO_CLASS) == null) throw new NotAvailable("This run's hero class was not recorded, so its floors cannot be rebuilt.");
		if (Row.RULESET_MIXED.equals(run.ruleset)) throw new NotAvailable("This run's rules changed while it was being played, so there is no single set of rules to rebuild its floors with.");
		if (run.imported) throw new NotAvailable("This is an imported ranking record, not a recorded run, so its floors cannot be rebuilt.");
		if (val(run, d, FactKeys.CHALLENGES) == null) throw new NotAvailable("This run's challenge settings were not recorded, so its floors cannot be rebuilt reliably.");
		if (val(run, d, FactKeys.EXOTIC_CHANCE) == null) throw new NotAvailable("This run's exotic-item setting was not recorded, so its floors cannot be rebuilt reliably.");
		Spec s = new Spec();
		s.runId = run.runId;
		s.seed = run.num(d.indexOf(FactKeys.SEED));
		s.heroClass = run.text(d.indexOf(FactKeys.HERO_CLASS)).toUpperCase();
		s.challenges = val(run, d, FactKeys.CHALLENGES) != null ? (int) run.num(d.indexOf(FactKeys.CHALLENGES)) : 0;
		s.exoticKnown = val(run, d, FactKeys.EXOTIC_CHANCE) != null;
		s.exoticChance = s.exoticKnown ? (int) run.num(d.indexOf(FactKeys.EXOTIC_CHANCE)) : 0;
		if (val(run, d, FactKeys.DIFFICULTY) != null){
			s.difficulty = (int) run.num(d.indexOf(FactKeys.DIFFICULTY));
			s.balance = val(run, d, FactKeys.DIFFICULTY_BALANCE) != null ? (int) run.num(d.indexOf(FactKeys.DIFFICULTY_BALANCE)) : 0;
		}
		int reached = -1;
		for (String k : new String[]{ FactKeys.DEEPEST, FactKeys.DEEPEST_ENTERED }){
			if (val(run, d, k) != null) reached = Math.max(reached, (int) run.num(d.indexOf(k)));
		}
		s.reached = reached;
		s.ruleset = run.ruleset;
		return s;
	}

	//the items the run's own log says were generated (from the Items dataset), with their true identity
	public static List<Recorded> recorded( Catalog cat, String runId ){
		List<Recorded> out = new ArrayList<>();
		Dataset items = cat == null ? null : cat.dataset(Datasets.ITEMS);
		if (items == null) return out;
		DatasetDef d = items.def;
		int gen = d.indexOf("item.genDepth"), cls = d.indexOf("item.trueClass"),
				ench = d.indexOf("item.genEnchant"), genLvl = d.indexOf("item.genLevel"), genCur = d.indexOf("item.genCursed"), cont = d.indexOf("item.container"), room = d.indexOf("item.room");
		if (gen < 0 || cls < 0) return out;
		for (Row r : items.rows){
			if (!runId.equals(r.runId) || r.duplicate || !r.isKnown(gen) || !r.isKnown(cls)) continue;
			Recorded x = new Recorded();
			x.floor = (int) r.num(gen);
			x.trueClass = r.text(cls);
			x.stateKnown = genLvl >= 0 && r.isKnown(genLvl) && genCur >= 0 && r.isKnown(genCur) && ench >= 0 && r.isKnown(ench);
			if (x.stateKnown){
				x.level = (int) r.num(genLvl);
				x.cursed = r.num(genCur) != 0;
				x.enchant = r.text(ench);
			}
			x.container = cont >= 0 && r.isKnown(cont) ? r.text(cont) : "";
			x.room = room >= 0 && r.isKnown(room) ? r.text(room) : "";
			out.add(x);
		}
		return out;
	}

	//---- assembly ----

	private static String kindOf( String trueClass ){
		String[] parts = trueClass.split("\\.");
		return parts.length >= 2 && parts[0].equals("items") ? (parts.length >= 3 ? parts[1] : "other") : "other";
	}

	private static String key( String cls, int level, boolean cursed, String enchant ){
		return cls + "|" + level + "|" + cursed + "|" + enchant;
	}

	private static Row row( DatasetDef def, String id, Spec spec ){
		Row r = new Row(id, null, spec.runId, def.size());
		r.finished = true;
		r.ruleset = spec.ruleset;
		return r;
	}

	private static void put( Row r, DatasetDef d, String col, String v, int prov ){
		r.setText(d.indexOf(col), v, prov);
	}

	private static void put( Row r, DatasetDef d, String col, long v, int prov ){
		r.setNum(d.indexOf(col), v, prov);
	}

	//Pure: combines the run's recorded items with the canonical floors. Recorded floors (any recorded item) are authoritative: their canonical
	//items appear only in the comparison.
	public static Built assemble( Spec spec, List<Recorded> recorded, List<ReplayFloor> canonical ){
		Built b = new Built();
		b.spec = spec;
		DatasetDef idef = itemsDef(), mdef = mobsDef(), cdef = compareDef();
		List<Row> irows = new ArrayList<>(), mrows = new ArrayList<>(), crows = new ArrayList<>();
		Map<Integer, List<Recorded>> byFloor = new TreeMap<>();
		for (Recorded x : recorded){
			List<Recorded> l = byFloor.get(x.floor);
			if (l == null) byFloor.put(x.floor, l = new ArrayList<>());
			l.add(x);
		}
		b.recordedFloors = byFloor.size();
		int n = 0;
		for (Map.Entry<Integer, List<Recorded>> e : byFloor.entrySet()){
			for (Recorded x : e.getValue()){
				Row r = row(idef, "sfr" + (n++), spec);
				put(r, idef, "sf.run", spec.runId, Fact.PROV_RECORDED);
				put(r, idef, "sf.floor", e.getKey(), Fact.PROV_RECORDED);
				put(r, idef, "sf.basis", BASIS_RECORDED, Fact.PROV_RECORDED);
				put(r, idef, "sf.item", x.trueClass, Fact.PROV_RECORDED);
				put(r, idef, "sf.kind", kindOf(x.trueClass), Fact.PROV_RECORDED);
				if (x.stateKnown){
					put(r, idef, "sf.level", x.level, Fact.PROV_RECORDED);
					put(r, idef, "sf.cursed", x.cursed ? 1 : 0, Fact.PROV_RECORDED);
					put(r, idef, "sf.enchant", x.enchant, Fact.PROV_RECORDED);
				}
				if (!x.container.isEmpty()) put(r, idef, "sf.container", x.container, Fact.PROV_RECORDED);
				if (!x.room.isEmpty()) put(r, idef, "sf.room", x.room, Fact.PROV_RECORDED);
				irows.add(r);
			}
		}
		int m = 0, c = 0;
		for (ReplayFloor f : canonical){
			List<Recorded> rec = byFloor.get(f.depth);
			if (rec != null){
				//recorded floor: the canonical floor is only compared. An item whose state at generation was recorded is compared in full; one whose
				//state was not recorded is compared by kind only (its later state proves nothing about how it was generated), and is never a mismatch
				//on level, curse or enchantment.
				Map<String, Integer> recCount = new LinkedHashMap<>(), canCount = new LinkedHashMap<>(), recClass = new LinkedHashMap<>();
				for (Recorded x : rec){
					if (x.stateKnown){
						String k = key(x.trueClass, x.level, x.cursed, x.enchant);
						recCount.put(k, recCount.containsKey(k) ? recCount.get(k) + 1 : 1);
					} else {
						recClass.put(x.trueClass, recClass.containsKey(x.trueClass) ? recClass.get(x.trueClass) + 1 : 1);
					}
				}
				int canTotal = 0;
				for (ReplayFloor.Item it : f.items){
					String k = key(it.trueClass, it.level, it.cursed, it.enchant);
					int q = 1;
					canCount.put(k, canCount.containsKey(k) ? canCount.get(k) + q : q);
					canTotal += q;
				}
				int same = 0;
				for (Map.Entry<String, Integer> e : recCount.entrySet()){
					Integer o = canCount.get(e.getKey());
					if (o == null) continue;
					int used = Math.min(e.getValue(), o);
					same += used;
					canCount.put(e.getKey(), o - used);
				}
				//what is left of the canonical floor, by kind
				Map<String, Integer> canClass = new LinkedHashMap<>();
				for (Map.Entry<String, Integer> e : canCount.entrySet()){
					if (e.getValue() <= 0) continue;
					String cls = e.getKey().substring(0, e.getKey().indexOf('|'));
					canClass.put(cls, canClass.containsKey(cls) ? canClass.get(cls) + e.getValue() : e.getValue());
				}
				int classOnly = 0;
				for (Map.Entry<String, Integer> e : recClass.entrySet()){
					Integer o = canClass.get(e.getKey());
					if (o != null) classOnly += Math.min(e.getValue(), o);
				}
				int recTotal = rec.size();
				Row r = row(cdef, "scr" + (c++), spec);
				put(r, cdef, "sc.run", spec.runId, Fact.PROV_RECORDED);
				put(r, cdef, "sc.floor", f.depth, Fact.PROV_RECORDED);
				put(r, cdef, "sc.basis", BASIS_COMPARISON, Fact.PROV_RECORDED);
				put(r, cdef, "sc.recorded", recTotal, Fact.PROV_RECORDED);
				put(r, cdef, "sc.canonical", canTotal, Fact.PROV_INFERRED);
				put(r, cdef, "sc.same", same, Fact.PROV_INFERRED);
				put(r, cdef, "sc.kindOnly", classOnly, Fact.PROV_INFERRED);
				put(r, cdef, "sc.onlyRecorded", recTotal - same - classOnly, Fact.PROV_INFERRED);
				put(r, cdef, "sc.onlyCanonical", canTotal - same - classOnly, Fact.PROV_INFERRED);
				//agreement is over the items whose state could be compared
				int denom = Math.max(recTotal, canTotal) - classOnly;
				if (denom > 0) put(r, cdef, "sc.agree", (long) same * 10000L / denom, Fact.PROV_INFERRED);
				crows.add(r);
				continue;
			}
			b.canonicalFloors++;
			int basisCode = spec.reached < 0 ? BASIS_REACH_UNKNOWN : f.depth <= spec.reached ? BASIS_UNRECORDED : BASIS_UNREACHED;
			for (ReplayFloor.Item it : f.items){
				Row r = row(idef, "sfc" + (n++), spec);
				put(r, idef, "sf.run", spec.runId, Fact.PROV_RECORDED);
				put(r, idef, "sf.floor", f.depth, basisCode == BASIS_RECORDED ? Fact.PROV_RECORDED : Fact.PROV_INFERRED);
				put(r, idef, "sf.basis", basisCode, Fact.PROV_RECORDED);
				put(r, idef, "sf.item", it.trueClass, Fact.PROV_INFERRED);
				put(r, idef, "sf.kind", kindOf(it.trueClass), Fact.PROV_INFERRED);
				put(r, idef, "sf.level", it.level, Fact.PROV_INFERRED);
				put(r, idef, "sf.cursed", it.cursed ? 1 : 0, Fact.PROV_INFERRED);
				put(r, idef, "sf.enchant", it.enchant, Fact.PROV_INFERRED);
				put(r, idef, "sf.qty", it.quantity, Fact.PROV_INFERRED);
				put(r, idef, "sf.container", it.container, Fact.PROV_INFERRED);
				put(r, idef, "sf.room", it.room, Fact.PROV_INFERRED);
				put(r, idef, "sf.pos", it.pos, Fact.PROV_INFERRED);
				irows.add(r);
			}
			for (ReplayFloor.Mob mob : f.mobs){
				Row r = row(mdef, "smc" + (m++), spec);
				put(r, mdef, "sm.run", spec.runId, Fact.PROV_RECORDED);
				put(r, mdef, "sm.floor", f.depth, Fact.PROV_RECORDED);
				put(r, mdef, "sm.basis", basisCode, Fact.PROV_RECORDED);
				put(r, mdef, "sm.mob", mob.mobClass, Fact.PROV_INFERRED);
				put(r, mdef, "sm.hp", mob.hp, Fact.PROV_INFERRED);
				put(r, mdef, "sm.champion", mob.champion.isEmpty() ? "" : joined(mob.champion), Fact.PROV_INFERRED);
				put(r, mdef, "sm.room", mob.room, Fact.PROV_INFERRED);
				put(r, mdef, "sm.pos", mob.pos, Fact.PROV_INFERRED);
				mrows.add(r);
			}
		}
		List<Dataset.RunInfo> info = new ArrayList<>();
		info.add(new Dataset.RunInfo(spec.runId, true, spec.ruleset, true, false));
		b.items = new Dataset(idef, irows, info);
		b.mobs = new Dataset(mdef, mrows, info);
		b.compare = new Dataset(cdef, crows, info);
		return b;
	}

	//Supplies the canonical floors of a run: implemented by the game-aware layer (FloorReplay), so this library never touches game code.
	public interface Provider {
		//whether seed floors can be asked for from this row of this dataset
		boolean applies( Catalog cat, Dataset ds, Row row );

		//heavy: replays the run's seed (off the UI thread). The exception's message is shown to the player.
		Built build( Catalog cat, Row run ) throws NotAvailable;
	}

	private static String joined( List<String> parts ){
		StringBuilder sb = new StringBuilder();
		for (String p : parts) sb.append(sb.length() > 0 ? "," : "").append(p);
		return sb.toString();
	}

	//puts the datasets and their views into a catalog, replacing those of any earlier run
	public static void install( Catalog cat, Built b ){
		cat.add(b.items).add(b.mobs).add(b.compare);
		for (ViewDef v : views()) cat.add(v);
	}
}
