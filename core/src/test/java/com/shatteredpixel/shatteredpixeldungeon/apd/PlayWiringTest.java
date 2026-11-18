package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

//The recording of what the hero does depends on one-line ApdPlay hooks in the game code. This test reads the game's source and fails when a hook
//is removed, duplicated or forgotten, so a refactor of the game cannot silently stop a kind of history from being recorded.
public class PlayWiringTest {

	//file -> number of ApdPlay.<method> calls it must contain (the restore pair counts as two)
	private static final Map<String, Integer> HOOKS = new TreeMap<>();
	static {
		HOOKS.put("actors/hero/Hero.java", 7);          //turn, move, rest, talent, search, NPC interaction
		HOOKS.put("items/Item.java", 1);                //item action
		HOOKS.put("scenes/AlchemyScene.java", 1);       //successful alchemy craft
		HOOKS.put("actors/buffs/Buff.java", 2);         //attached, detached
		HOOKS.put("actors/Char.java", 3);               //restore begin / end, summon ownership
		HOOKS.put("items/TengusMask.java", 1);          //subclass
		HOOKS.put("items/KingsCrown.java", 1);          //armor ability chosen
		HOOKS.put("items/armor/curses/Overgrowth.java", 2);
		HOOKS.put("windows/WndClericSpells.java", 2);   //spell casts
		HOOKS.put("items/artifacts/HolyTome.java", 1);  //quick spell
		HOOKS.put("actors/hero/abilities/ArmorAbility.java", 2);
		HOOKS.put("items/Gold.java", 2);                //the call and its cause lookup on one line
		HOOKS.put("windows/WndTradeItem.java", 8);      //buy: 2, sell: 3, sell one: 3
		HOOKS.put("actors/mobs/npcs/Shopkeeper.java", 2);
		HOOKS.put("levels/traps/Trap.java", 1);
		HOOKS.put("items/Heap.java", 1);               //container opened
		HOOKS.put("levels/Level.java", 1);             //secret revealed
		HOOKS.put("actors/blobs/WellWater.java", 1);   //well used
		HOOKS.put("journal/Document.java", 2);         //page found, page read
		HOOKS.put("actors/mobs/Goo.java", 1);             //enraged phase
		HOOKS.put("actors/mobs/Tengu.java", 1);           //arena phase
		HOOKS.put("actors/mobs/DM300.java", 2);           //supercharged and normal/final
		HOOKS.put("actors/mobs/DwarfKing.java", 2);       //shielded and final
		HOOKS.put("actors/mobs/YogDzewa.java", 2);        //fist phases and final
		HOOKS.put("actors/mobs/npcs/Ghost.java", 3);      //quest given, ready, done
		HOOKS.put("actors/mobs/npcs/Wandmaker.java", 2);  //quest given, done
		HOOKS.put("actors/mobs/npcs/Blacksmith.java", 2); //quest given, done
		HOOKS.put("actors/mobs/npcs/Imp.java", 4);        //quest given (old and new), done (old and new)
		HOOKS.put("journal/Notes.java", 4);            //landmark added/removed, key added/used
	}

	@Test
	public void everyHookIsStillInTheGameSource() throws Exception {
		File root = new File(System.getProperty("apd.game.src"));
		for (Map.Entry<String, Integer> e : HOOKS.entrySet()){
			String src = new String(Files.readAllBytes(new File(root, e.getKey()).toPath()), "UTF-8");
			int n = 0, at = 0;
			while ((at = src.indexOf("ApdPlay.", at)) >= 0){
				n++;
				at += 8;
			}
			assertEquals(e.getKey() + " must call ApdPlay " + e.getValue() + " times", (int) e.getValue(), n);
		}
	}
}
