package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.List;

//Helpers for tests that drive items through the real game classes with the recorder on.
final class ItemTestSupport {

	private ItemTestSupport(){}

	//a run on level 1 with a fresh history folder
	static RecorderTestSupport.Session startRun( long seed ){
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, seed);
		RecorderTestSupport.enterLevel(1);
		return s;
	}

	//what the hero does at the start of every turn: the recorder looks at what is held and in sight
	static void turn(){
		com.shatteredpixel.shatteredpixeldungeon.Statistics.duration += 1;
		ApdItems.onHeroTurn();
	}

	//a free passable cell, different each call
	private static int nextCell = 0;

	static int freeCell(){
		com.shatteredpixel.shatteredpixeldungeon.levels.Level l = Dungeon.level;
		while (nextCell < l.length()){
			int c = nextCell++;
			if (c > 100 && l.passable[c] && l.heaps.get(c) == null && c != Dungeon.hero.pos
					&& l.map[c] != com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.CHASM && !l.pit[c]) return c;
		}
		throw new IllegalStateException("no free cell");
	}

	//puts an item on the floor in plain sight: the heap is seen, its cell is in the hero's view
	static Heap putInSight( Item item ){
		return putInSight(item, freeCell());
	}

	//Level.drop needs a running game scene, so the heap is made the way it does, without the scene parts
	static Heap putInSight( Item item, int cell ){
		Heap h = Dungeon.level.heaps.get(cell);
		if (h == null){
			h = new Heap();
			h.pos = cell;
			h.drop(item);
			Dungeon.level.heaps.put(cell, h);
		} else {
			h.drop(item);
		}
		h.seen = true;
		h.hidden = false;
		Dungeon.level.heroFOV[cell] = true;
		return h;
	}

	static List<ApdEvent> ofType( ReadResult r, int... types ){
		List<ApdEvent> out = new ArrayList<>();
		for (ApdEvent e : r.events){
			for (int t : types) if (e.type == t) out.add(e);
		}
		return out;
	}

	static String uidOf( Item item ){
		return item.apdUid;
	}

	//the run's log, read back (this closes the recorder, so call it last)
	static ReadResult finish( RecorderTestSupport.Session s ) throws Exception {
		String id = ApdRecorder.currentRunId();
		return s.read(id);
	}

	static List<ItemEvents.Acquired> acquired( ReadResult r ){
		List<ItemEvents.Acquired> out = new ArrayList<>();
		for (ApdEvent e : ofType(r, EventTypes.ITEM_ACQUIRED)) out.add(ItemEvents.decodeAcquired(e, r));
		return out;
	}

	static List<ItemEvents.Left> left( ReadResult r ){
		List<ItemEvents.Left> out = new ArrayList<>();
		for (ApdEvent e : ofType(r, EventTypes.ITEM_LEFT)) out.add(ItemEvents.decodeLeft(e, r));
		return out;
	}

	static List<ItemEvents.Lineage> lineage( ReadResult r ){
		List<ItemEvents.Lineage> out = new ArrayList<>();
		for (ApdEvent e : ofType(r, EventTypes.ITEM_LINEAGE)) out.add(ItemEvents.decodeLineage(e, r));
		return out;
	}

	static List<ItemEvents.Seen> seen( ReadResult r ){
		List<ItemEvents.Seen> out = new ArrayList<>();
		for (ApdEvent e : ofType(r, EventTypes.ITEM_SEEN)) out.add(ItemEvents.decodeSeen(e, r));
		return out;
	}

	static List<ItemEvents.Registered> registered( ReadResult r ){
		List<ItemEvents.Registered> out = new ArrayList<>();
		for (ApdEvent e : ofType(r, EventTypes.ITEM_REGISTERED)) out.add(ItemEvents.decodeRegistered(e, r));
		return out;
	}
}
