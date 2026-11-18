package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ReplayFloor;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//Reads a freshly generated level (read only) into a ReplayFloor, with the same notion of "item with an identity" the recorder uses.
public final class ReplayScan {

	private ReplayScan(){}

	public static ReplayFloor scan( Level level ){
		ReplayFloor f = new ReplayFloor();
		f.depth = Dungeon.depth;
		f.levelClass = GameSnapshot.shortName(level.getClass());
		f.feeling = level.feeling == null ? "" : level.feeling.name();
		f.width = level.width();
		f.height = level.height();
		for (Trap t : level.traps.valueList()){
			if (t.visible) f.trapsVisible++; else f.trapsHidden++;
		}
		if (level instanceof RegularLevel){
			for (Room r : ((RegularLevel) level).rooms()) f.rooms.add(r.getClass().getSimpleName());
		}

		List<Heap> heaps = new ArrayList<>(level.heaps.valueList());
		Collections.sort(heaps, new Comparator<Heap>(){
			@Override
			public int compare( Heap a, Heap b ){
				return a.pos < b.pos ? -1 : (a.pos > b.pos ? 1 : 0);
			}
		});
		for (Heap h : heaps){
			for (Item it : new ArrayList<>(h.items)){
				add(f, level, it, h.type.name(), h.pos, h.hidden, "");
			}
		}
		List<Mob> mobs = new ArrayList<>(level.mobs);
		Collections.sort(mobs, new Comparator<Mob>(){
			@Override
			public int compare( Mob a, Mob b ){
				if (a.pos != b.pos) return a.pos < b.pos ? -1 : 1;
				return a.getClass().getName().compareTo(b.getClass().getName());
			}
		});
		for (Mob m : mobs){
			String owner = GameSnapshot.shortName(m.getClass());
			try {
				if (m instanceof Mimic && ((Mimic) m).items != null){
					for (Item it : new ArrayList<>(((Mimic) m).items)) add(f, level, it, "MIMIC", m.pos, false, owner);
				}
				if (m instanceof Statue){
					add(f, level, ((Statue) m).weapon(), "STATUE", m.pos, false, owner);
					if (m instanceof ArmoredStatue) add(f, level, ((ArmoredStatue) m).armor(), "STATUE", m.pos, false, owner);
				}
				if (m instanceof Ghost){
					add(f, level, Ghost.Quest.weapon, "QUEST_REWARD", m.pos, false, owner);
					add(f, level, Ghost.Quest.armor, "QUEST_REWARD", m.pos, false, owner);
				} else if (m instanceof Wandmaker){
					add(f, level, Wandmaker.Quest.wand1, "QUEST_REWARD", m.pos, false, owner);
					add(f, level, Wandmaker.Quest.wand2, "QUEST_REWARD", m.pos, false, owner);
				} else if (m instanceof Blacksmith && Blacksmith.Quest.smithRewards != null){
					for (Item it : new ArrayList<>(Blacksmith.Quest.smithRewards)) add(f, level, it, "QUEST_REWARD", m.pos, false, owner);
				} else if (m instanceof Imp && Imp.Quest.rewardOptions != null){
					for (Item it : new ArrayList<>(Imp.Quest.rewardOptions)) add(f, level, it, "QUEST_REWARD", m.pos, false, owner);
				}
			} catch (Throwable t){
				//a creature whose held items cannot be read is still listed as a creature
			}
			ReplayFloor.Mob rm = new ReplayFloor.Mob();
			rm.mobClass = GameSnapshot.shortName(m.getClass());
			rm.pos = m.pos;
			rm.hp = m.HP;
			rm.room = roomAt(level, m.pos);
			for (Buff b : new ArrayList<>(m.buffs())){
				if (b instanceof ChampionEnemy) rm.champion.add(b.getClass().getSimpleName());
			}
			f.mobs.add(rm);
		}
		return f;
	}

	private static void add( ReplayFloor f, Level level, Item item, String container, int pos, boolean hidden, String owner ){
		if (item == null || !ItemFacts.tracks(item)) return;
		ItemFacts.Truth t = ItemFacts.truth(item);
		ReplayFloor.Item i = new ReplayFloor.Item();
		i.trueClass = t.trueClass;
		i.level = t.level;
		i.cursed = t.cursed;
		i.enchant = t.enchant;
		i.quantity = t.quantity;
		i.container = container;
		i.pos = pos;
		i.hidden = hidden;
		i.room = roomAt(level, pos);
		i.owner = owner;
		f.items.add(i);
	}

	private static String roomAt( Level level, int pos ){
		if (!(level instanceof RegularLevel) || pos < 0) return "";
		try {
			Room r = ((RegularLevel) level).room(pos);
			return r == null ? "" : r.getClass().getSimpleName();
		} catch (Throwable t){
			return "";
		}
	}
}
