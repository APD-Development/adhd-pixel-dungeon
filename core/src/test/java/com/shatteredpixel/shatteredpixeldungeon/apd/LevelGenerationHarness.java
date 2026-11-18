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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.journal.Guidebook;
import com.shatteredpixel.shatteredpixeldungeon.items.journal.GuidePage;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

//Generates real dungeon levels from a seed, headless, and fingerprints them.
//The fingerprint is a SHA-256 of the level's full saved state (map, mobs, heaps, traps, plants, blobs...),
//written as JSON with sorted keys so it does not depend on hash-map ordering.
public final class LevelGenerationHarness {

	private LevelGenerationHarness(){}

	//Only GoldenChildMain uses the pre-fix snapshot format; actual maps still require visible exits.
	static boolean historicalTutorialFingerprint;

	//Level generation is NOT a pure function of the seed: it also reads persistent journal state.
	// - Document (guidebook/alchemy/lore page) found states decide which pages get placed on levels
	// - SPDSettings.intro() changes the tutorial elements of the first floors
	//So the harness always starts from one of these two fixed contexts.
	public enum Context {
		//a brand new install: intro still on, no journal pages found
		FRESH,
		//a veteran player: intro off, all adventurer's guide and alchemy guide pages found
		VETERAN,
		//what a floor replay uses (ReplayContext): guide pages found, NO lore page found, intro off
		REFERENCE,
		//an install that has been used (tutorial over) but whose guidebook pages were never found: what a returning player who skipped them has
		RETURNING
	}

	private static java.util.Map<Document, java.util.Map<String, Integer>> initialPageStates;

	private static void applyContext( Context context ) throws Exception {
		java.lang.reflect.Field field = Document.class.getDeclaredField("pagesStates");
		field.setAccessible(true);

		if (initialPageStates == null){
			initialPageStates = new java.util.HashMap<>();
			for (Document doc : Document.values()){
				initialPageStates.put(doc, new java.util.LinkedHashMap<>((java.util.Map<String, Integer>) field.get(doc)));
			}
		}

		for (Document doc : Document.values()){
			java.util.Map<String, Integer> states = (java.util.Map<String, Integer>) field.get(doc);
			states.clear();
			states.putAll(initialPageStates.get(doc));
		}

		if (context == Context.REFERENCE){
			for (Document doc : Document.values()){
				boolean guide = doc == Document.ADVENTURERS_GUIDE || doc == Document.ALCHEMY_GUIDE;
				for (String page : doc.pageNames().toArray(new String[0])){
					if (guide) doc.findPage(page);
					else doc.deletePage(page);
				}
			}
			SPDSettings.intro(false);
		} else if (context == Context.RETURNING){
			SPDSettings.intro(false);
		} else if (context == Context.VETERAN){
			for (Document doc : new Document[]{Document.ADVENTURERS_GUIDE, Document.ALCHEMY_GUIDE}){
				for (String page : doc.pageNames()) doc.findPage(page);
			}
			SPDSettings.intro(false);
		} else {
			//Debug/headless defaults mark guide pages READ; a fresh intro fixture must explicitly be unread.
			Document.ADVENTURERS_GUIDE.deletePage(Document.GUIDE_INTRO);
			SPDSettings.intro(true);
		}
	}

	public static final class LevelPrint {
		public final long seed;
		public final int challenges;
		public final int depth;
		public final String levelClass;
		public final int width, height;
		public final int mobs, heaps, items;
		public final String hash;

		LevelPrint(long seed, int challenges, int depth, Level level, String hash){
			this.seed = seed;
			this.challenges = challenges;
			this.depth = depth;
			this.levelClass = level.getClass().getSimpleName();
			this.width = level.width();
			this.height = level.height();
			this.mobs = level.mobs.size();
			this.heaps = level.heaps.size;
			int n = 0;
			for (Heap h : level.heaps.valueList()) n += h.items.size();
			this.items = n;
			this.hash = hash;
		}

		//one line of the golden file
		public String line(){
			return seed + " " + challenges + " " + depth + " " + levelClass + " " + width + "x" + height
					+ " mobs=" + mobs + " heaps=" + heaps + " items=" + items + " sha256=" + hash;
		}
	}

	//tests that need to look at each level the harness builds (after the unseeded heaps are removed) set this
	public static java.util.function.BiConsumer<Integer, Level> levelObserver;

	//Generates depths 1..maxDepth in order, the way a real run reaches them.
	//The default RNG is not touched: Dungeon.init and Level.create seed their own generators from the dungeon seed.
	public static List<LevelPrint> generate( long seed, int challenges, HeroClass heroClass, Context context, int maxDepth ) throws Exception {
		HeadlessTestSupport.start();
		applyContext(context);

		SPDSettings.challenges(challenges);
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.seed = seed;
		GamesInProgress.selectedClass = heroClass;
		GamesInProgress.randomizedClass = false;

		Dungeon.init();

		List<LevelPrint> out = new ArrayList<>();
		for (int depth = 1; depth <= maxDepth; depth++){
			Dungeon.depth = depth;
			Dungeon.branch = 0;
			Dungeon.level = Dungeon.newLevel();
			removeUnseededHeaps(Dungeon.level);
			out.add(new LevelPrint(seed, challenges, depth, Dungeon.level, fingerprint(Dungeon.level)));
			if (levelObserver != null) levelObserver.accept(depth, Dungeon.level);
		}
		return out;
	}

	//EntranceRoom.placeEarlyGuidePages deliberately uses an UNSEEDED random generator ("so meta progression doesn't
	//affect levelgen"), so the floor 1 guidebook and the floor 2 "Searching" guide page land in a different spot on
	//every generation, and sometimes on top of another item's heap. They are the only unseeded item placements in
	//level generation, so the baseline removes exactly those two items (and a heap only if that leaves it empty).
	//Everything else, including every other guide page, is fingerprinted.
	private static void removeUnseededHeaps( Level level ){
		for (Heap heap : level.heaps.valueList().toArray(new Heap[0])){
			for (Item item : heap.items.toArray(new Item[0])){
				boolean unseeded = item instanceof Guidebook
						|| (item instanceof GuidePage && Document.GUIDE_SEARCHING.equals(((GuidePage) item).page()));
				if (unseeded) heap.items.remove(item);
			}
			if (heap.items.isEmpty()) level.heaps.remove(heap.pos);
		}
	}

	//Like generate(), but the hero really arrives on each level (Dungeon.switchLevel, which saves the game), the way a
	//played run reaches the next depth. Used to check that recording does not change what happens between levels.
	public static List<LevelPrint> generateFlow( long seed, int challenges, HeroClass heroClass, Context context, int maxDepth ) throws Exception {
		HeadlessTestSupport.start();
		applyContext(context);

		SPDSettings.challenges(challenges);
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.seed = seed;
		GamesInProgress.selectedClass = heroClass;
		GamesInProgress.randomizedClass = false;
		GamesInProgress.curSlot = 1;

		Dungeon.init();

		List<LevelPrint> out = new ArrayList<>();
		for (int depth = 1; depth <= maxDepth; depth++){
			Dungeon.depth = depth;
			Dungeon.branch = 0;
			Dungeon.level = Dungeon.newLevel();
			removeUnseededHeaps(Dungeon.level);
			out.add(new LevelPrint(seed, challenges, depth, Dungeon.level, fingerprint(Dungeon.level)));
			Dungeon.switchLevel(Dungeon.level, -1);
		}
		return out;
	}

	public static String fingerprint( Level level ) throws Exception {
		return sha256(canonicalJson(level));
	}

	public static String canonicalJson( Level level ) throws Exception {
		Bundle bundle = new Bundle();
		bundle.put("level", level);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Bundle.write(bundle, bytes, false);
		Object json = new org.json.JSONTokener(new String(bytes.toByteArray(), StandardCharsets.UTF_8)).nextValue();

		normalizePaintingVariance(((JSONObject) json).getJSONObject("level"));
		//Keep the old golden baseline meaningful after the explicitly authorized entrance-door fix.
		//Normalize ONLY those boundary door bytes in the serialized test snapshot, never the real level.
		//Every other tile/item/mob/state remains compared against the unchanged historical hashes.
		if (historicalTutorialFingerprint && Dungeon.depth == 1 && SPDSettings.intro()
				&& level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel){
			JSONArray map = ((JSONObject) json).getJSONObject("level").getJSONArray("map");
			for (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room room :
					((com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel) level).rooms()){
				if (!room.isEntrance()) continue;
				for (int y = room.top; y <= room.bottom; y++) for (int x = room.left; x <= room.right; x++){
					if (x != room.left && x != room.right && y != room.top && y != room.bottom) continue;
					int cell = x + y * level.width();
					if (map.getInt(cell) == Terrain.DOOR) map.put(cell, Terrain.SECRET_DOOR);
				}
			}
		}


		StringBuilder canonical = new StringBuilder();
		canonicalize(json, canonical);
		return canonical.toString();
	}

	//The "extras" pass of RegularPainter (traps, grass, decoration) is not reproducible from the seed alone:
	//generating the same seed repeatedly gives the same rooms, doors, items and enemies but occasionally moves a few
	//dozen traps, decoration tiles and grass tiles. (SPD runs that pass on its own RNG on purpose, "so that extra
	//painting variance doesn't affect the rest of levelgen".) The baseline therefore ignores that variance:
	// - the trap list is left out
	// - tiles are folded into their plain variant: trap/decoration floor -> floor, decorated wall -> wall,
	//   high grass -> grass
	//Layout, doors, water, stairs, special rooms, every item and every enemy are still fingerprinted exactly.
	private static void normalizePaintingVariance( JSONObject level ) throws Exception {
		level.remove("traps");
		JSONArray map = level.getJSONArray("map");
		for (int i = 0; i < map.length(); i++){
			int tile = map.getInt(i);
			switch (tile){
				case Terrain.EMPTY_DECO: case Terrain.TRAP: case Terrain.SECRET_TRAP: case Terrain.INACTIVE_TRAP:
					tile = Terrain.EMPTY; break;
				case Terrain.WALL_DECO:
					tile = Terrain.WALL; break;
				case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
					tile = Terrain.GRASS; break;
			}
			map.put(i, tile);
		}
	}

	private static String sha256( String text ) throws Exception {
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
		byte[] digest = sha.digest(text.getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder();
		for (byte b : digest) hex.append(String.format("%02x", b));
		return hex.toString();
	}

	static void canonicalize( Object value, StringBuilder out ){
		if (value instanceof JSONObject){
			JSONObject obj = (JSONObject) value;
			TreeMap<String, Object> sorted = new TreeMap<>();
			Iterator<?> keys = obj.keys();
			while (keys.hasNext()){
				String key = (String) keys.next();
				//MissileWeapon "set_id" is a random identifier used only to group thrown weapons for identification.
				//It is not derived from the dungeon seed (it differs between generations of the same seed) and does
				//not affect generation, so it is left out of the fingerprint.
				if (key.equals("set_id")) continue;
				//apd_uid is the statistics recorder's item id (a random UUID, assigned to generated items since Phase 5). It is
				//not part of the dungeon, so it must not be part of the fingerprint.
				if (key.equals("apd_uid")) continue;
				// Original golden snapshots predate collection bookkeeping. Only that legacy
				// comparison omits this one key; live fingerprints and saved items retain it.
				if (historicalTutorialFingerprint && key.equals("apd_progression_origins_v1")) continue;
				// The archived pre-feature snapshot cannot contain optional item-origin metadata.
				if (historicalTutorialFingerprint && key.equals("apd_item_origin_v1")) continue;
				if (historicalTutorialFingerprint && key.equals("apd_item_upgrade_range_v1")) continue;
				Object val = obj.opt(key);
				//ShopRoom.ChooseBag picks the bag for sale from a HashMap keyed by Bag objects, and ties between bags
				//are broken by that map's iteration order, which depends on object identity hashes. Which bag a shop
				//sells can therefore differ between runs of the same seed, so every bag is fingerprinted as just "a bag".
				if (key.equals("__className") && val instanceof String && ((String) val).contains(".items.bags.")){
					val = "BAG";
				}
				sorted.put(key, val);
			}
			out.append('{');
			boolean first = true;
			for (String key : sorted.keySet()){
				if (!first) out.append(',');
				first = false;
				out.append('"').append(key).append("\":");
				canonicalize(sorted.get(key), out);
			}
			out.append('}');
		} else if (value instanceof JSONArray){
			JSONArray arr = (JSONArray) value;
			boolean allObjects = arr.length() > 0;
			for (int i = 0; i < arr.length(); i++){
				if (!(arr.opt(i) instanceof JSONObject)) allObjects = false;
			}
			out.append('[');
			if (allObjects){
				//lists of things (mobs, heaps, traps...) come out of hash sets in an order that varies between
				//runs, so they are fingerprinted as a sorted collection. Plain arrays such as the tile map keep their order.
				List<String> parts = new ArrayList<>();
				for (int i = 0; i < arr.length(); i++){
					StringBuilder part = new StringBuilder();
					canonicalize(arr.opt(i), part);
					parts.add(part.toString());
				}
				java.util.Collections.sort(parts);
				out.append(String.join(",", parts));
			} else {
				for (int i = 0; i < arr.length(); i++){
					if (i > 0) out.append(',');
					canonicalize(arr.opt(i), out);
				}
			}
			out.append(']');
		} else if (value instanceof java.math.BigDecimal){
            // Modern public-domain JSON-java parses decimals without immediately rounding to Double.
            // The unchanged historical goldens used Double.toString. Match only its spelling,
            // and reject any decimal whose numeric value would change. Never narrow integers.
            java.math.BigDecimal decimal = (java.math.BigDecimal)value;
            double legacy = decimal.doubleValue();
            if (!Double.isFinite(legacy)
                    || new java.math.BigDecimal(Double.toString(legacy)).compareTo(decimal) != 0) {
                throw new AssertionError("Fingerprint decimal cannot use legacy spelling without losing precision: " + decimal);
            }
            out.append(Double.toString(legacy));
		} else if (value instanceof String){
			out.append('"').append(value).append('"');
		} else {
			out.append(String.valueOf(value));
		}
	}
}
