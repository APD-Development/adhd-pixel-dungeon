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

package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

//The kinds of fact the game's own mechanics declare (see FactKinds). Declaring a kind is the whole job of making a mechanic's history
//queryable: UIMSD generates its dataset, columns, filters and aggregates from the declaration. Keys and field order are permanent.
//
//PUBLIC kinds hold only what the player could see or do at that moment. Anything that would give away hidden information (the true
//class of an unidentified item, an effect with no icon) is an INTERNAL kind, shown only once the run is over.
public final class GameFacts {

	private GameFacts(){}

	/** forces the kinds to be registered */
	static void init(){
	}

	//---- movement and exploring ----

	//the hero changed cell: one observation per step, at full resolution (delta-encoded in the log)
	public static final FactKind MOVE_STEP = FactKinds.register(FactKind.builder("move.step", "Step").area("Movement")
			.description("Every step the hero took.")
			.value("cell", "Cell", "The cell the hero stepped onto (row times floor width plus column).", FactKind.PLAIN)
			.value("width", "Floor width", "How many cells wide the floor is, to turn a cell into a row and column.", FactKind.PLAIN)
			.value("travelling", "Walking", "1 for ordinary physical walking, 0 for an instantaneous relocation that also changed the hero's cell.", FactKind.PLAIN)
			.series().build());

	//one observation per deliberate movement command; separate from the per-cell move.step series
	public static final FactKind MOVE_COMMAND = FactKinds.register(FactKind.builder("move.command", "Movement command").area("Movement")
			.description("Every manual step, point-to-point travel or stair navigation command the hero began.")
			.value("destination", "Destination", "The command's destination cell (row times floor width plus column).", FactKind.PLAIN)
			.value("width", "Floor width", "How many cells wide the floor is, to turn the destination into a row and column.", FactKind.PLAIN)
			.tag("mode", "Mode", "step, travel or stairs")
			.since(1, 15).build());

	//the hero's condition at the start of each turn the hero takes (hunger is the game's hunger counter, higher is hungrier)
	public static final FactKind HERO_STATE = FactKinds.register(FactKind.builder("hero.state", "Hero state").plural("Hero states").area("Character")
			.description("The hero's health, shield and hunger at the start of every turn the hero took.")
			.value("hp", "Health", "Hit points at the start of the turn.", FactKind.PLAIN)
			.value("hpMax", "Max health", "Maximum hit points at the start of the turn.", FactKind.PLAIN)
			.value("shield", "Shield", "Shielding at the start of the turn.", FactKind.PLAIN)
			.value("hunger", "Hunger", "The hunger counter at the start of the turn. It goes up as time passes, and is reset by eating.", FactKind.PLAIN)
			.since(1, 9).series().build());

	//a turn spent resting or waiting
	public static final FactKind REST = FactKinds.register(FactKind.builder("explore.rest", "Rest turn").plural("Rest turns").area("Movement")
			.description("Every turn the hero waited or rested.")
			.value("full", "Resting", "1 for resting (searching as well), 0 for a single wait.", FactKind.PLAIN)
			.series().build());

	public static final FactKind SEARCH = FactKinds.register(FactKind.builder("explore.search", "Search").area("Movement")
			.description("Every time the hero searched, on purpose or just by walking about.")
			.value("intentional", "On purpose", "1 when the hero chose to search.", FactKind.PLAIN)
			.value("found", "Found something", "1 when it turned something up.", FactKind.PLAIN).build());

	//a chest, tomb, skeleton or other container was opened
	public static final FactKind OPEN = FactKinds.register(FactKind.builder("explore.open", "Container opened").plural("Containers opened").area("Movement")
			.description("Every chest, tomb, skeleton or pile of remains the hero opened.").subject("Container")
			.value("items", "Items inside", "How many items were in it when it was opened.", FactKind.PLAIN).build());

	public static final FactKind TRAP = FactKinds.register(FactKind.builder("trap.trigger", "Trap triggered").plural("Traps triggered").area("Movement")
			.description("Every trap that went off where the hero could see it.").subject("Trap").actor()
			.value("hero", "Hero set it off", "1 when the hero triggered it, 0 for someone else.", FactKind.PLAIN).build());

	//a page of a journal document (the guidebook, a region's notes) was found or read for the first time
	public static final FactKind PAGE = FactKinds.register(FactKind.builder("journal.page", "Journal page").plural("Journal pages").area("Exploration")
			.description("Every journal page found, and every page read for the first time.").subject("Document")
			.tag("action", "Change", "found or read").tag("page", "Page", "The page's name in the journal.").since(1, 9).build());

	//a hidden door or trap was revealed
	public static final FactKind DISCOVER = FactKinds.register(FactKind.builder("explore.discover", "Secret revealed").plural("Secrets revealed").area("Movement")
			.description("Every secret door or trap that was revealed, by searching or by magic.").subject("Secret")
			.value("cell", "Cell", "The cell it was on.", FactKind.PLAIN)
			.since(1, 9).build());

	//the hero used a well
	public static final FactKind WELL = FactKinds.register(FactKind.builder("explore.well", "Well used").plural("Wells used").area("Exploration")
			.description("Every time the hero drank from a well.").subject("Well").since(1, 9).build());

	//---- the journal: what the player learned about the dungeon, and keys ----

	//a landmark was written in (or removed from) the journal: a shop, an alchemy pot, a well, a floor feeling
	public static final FactKind LANDMARK = FactKinds.register(FactKind.builder("journal.landmark", "Journal landmark").plural("Journal landmarks").area("Exploration")
			.description("Every landmark the journal noted or dropped: shops, alchemy, wells and what the floor felt like.").subject("Landmark")
			.value("depth", "Floor", "The floor the landmark is on.", FactKind.PLAIN)
			.tag("action", "Change", "added or removed").since(1, 9).build());

	//the hero successfully invoked an interaction with an NPC; only its player-visible name and current floor are recorded
	public static final FactKind NPC_INTERACT = FactKinds.register(FactKind.builder("npc.interact", "NPC interaction").plural("NPC interactions").area("Exploration")
			.description("Every time the hero interacted with a visible non-player character.").subject("Character")
			.value("depth", "Floor", "The floor the interaction happened on.", FactKind.PLAIN).since(1, 12).build());

	//a player-owned allied creature was created by a character the player could see; names are only what the player saw
	public static final FactKind ALLY_SUMMON = FactKinds.register(FactKind.builder("ally.summon", "Ally summoned").plural("Allies summoned").area("Abilities")
			.description("Every player-owned allied creature created by the hero or another visible ally.").subject("Ally").actor()
			.value("depth", "Floor", "The floor the ally appeared on.", FactKind.PLAIN).since(1, 13).build());

	//a visible boss crossed one of its explicit combat phases; phase numbering is local to that boss
	public static final FactKind BOSS_PHASE = FactKinds.register(FactKind.builder("boss.phase", "Boss phase").plural("Boss phases").area("Enemies")
			.description("Every visible boss phase transition after combat began.").subject("Boss")
			.value("phase", "Phase", "The boss-specific phase number after the transition.", FactKind.PLAIN)
			.value("depth", "Floor", "The floor where the transition happened.", FactKind.PLAIN)
			.tag("state", "State", "The visible kind of phase, such as enraged, arena, supercharged, shielded, fist or final.")
			.since(1, 16).build());

	//an NPC quest moved on a step: given, ready (what was asked for was found or beaten) or done (the reward stage was reached)
	public static final FactKind QUEST = FactKinds.register(FactKind.builder("npc.quest", "Quest step").plural("Quest steps").area("Exploration")
			.description("Each step of the Sad ghost, Wandmaker, Blacksmith and Imp quests as it happened.").subject("Quest giver")
			.value("depth", "Floor", "The floor the player was on.", FactKind.PLAIN)
			.tag("step", "Step", "given, ready or done").since(1, 10).build());

	//a key was picked up or used
	public static final FactKind KEY = FactKinds.register(FactKind.builder("journal.key", "Key").plural("Keys").area("Exploration")
			.description("Every key picked up or used.").subject("Key")
			.value("depth", "Floor", "The floor the key belongs to.", FactKind.PLAIN)
			.value("quantity", "Number", "How many keys changed hands.", FactKind.PLAIN)
			.tag("action", "Change", "added or used").since(1, 9).build());

	//---- items ----

	//an item action chosen by the hero (drink, read, zap, eat, equip, throw, drop, ...). The subject is the item's name as the player sees it
	public static final FactKind ITEM_USE = FactKinds.register(FactKind.builder("item.use", "Item action").plural("Item actions").area("Items")
			.description("Every action chosen for an item: drink, read, zap, eat, throw, equip and the rest.").subject("Item")
			.value("quantity", "How many in the stack", "", FactKind.PLAIN)
			.tag("action", "Action").build());

	//a completed alchemy recipe using only the names visible to the player at the time
	public static final FactKind ALCHEMY_CRAFT = FactKinds.register(FactKind.builder("alchemy.craft", "Alchemy craft").plural("Alchemy crafts").area("Items")
			.description("Every successful alchemy recipe, with the ingredients and result as the player saw them.").subject("Result")
			.value("depth", "Floor", "The floor where the recipe was completed.", FactKind.PLAIN)
			.value("quantity", "Result quantity", "How many items the recipe produced.", FactKind.PLAIN)
			.value("energy", "Energy used", "Total energy spent on the recipe.", FactKind.PLAIN)
			.value("carriedEnergy", "Carried energy used", "Energy spent from the run's main energy pool.", FactKind.PLAIN)
			.value("toolkitEnergy", "Toolkit energy used", "Energy supplied by the Alchemist's Toolkit.", FactKind.PLAIN)
			.tag("ingredients", "Ingredients", "Displayed ingredient names and quantities.")
			.since(1, 14).build());

	//the same action with the hidden truth: the true class, and the numbers the player may not know yet
	public static final FactKind ITEM_USE_TRUTH = FactKinds.register(FactKind.builder("item.useTruth", "Item action (true item)").plural("Item actions (true item)")
			.area("Items").description("The same item actions with what the item really was.").subject("True item").internal()
			.value("level", "True level", "", FactKind.PLAIN)
			.value("charges", "Charges", "-1 when the item has none.", FactKind.PLAIN)
			.tag("action", "Action").build());

	//---- status effects ----

	public static final FactKind STATUS_START = FactKinds.register(FactKind.builder("status.start", "Effect began").plural("Effects began").area("Status effects")
			.description("Every status effect with an icon that began on the hero, or on a creature the hero could see.").subject("Effect").actor()
			.value("duration", "Duration", "In turns; -1 when it has no fixed length.", FactKind.PLAIN).build());

	public static final FactKind STATUS_END = FactKinds.register(FactKind.builder("status.end", "Effect ended").plural("Effects ended").area("Status effects")
			.description("Every status effect with an icon that ended.").subject("Effect").actor()
			.value("remaining", "Turns left", "In turns; -1 when it has no fixed length.", FactKind.PLAIN).build());

	//the real class of every effect: some effects (ring and armor ones) can give away what an unidentified item is, so this is hidden information
	public static final FactKind STATUS_START_TRUTH = FactKinds.register(FactKind.builder("status.startTruth", "Effect began (true effect)").plural("Effects began (true effect)")
			.area("Status effects").description("Every status effect that began, including ones with no icon, by what it really was.").subject("True effect").actor().internal()
			.value("duration", "Duration", "", FactKind.PLAIN).build());

	public static final FactKind STATUS_END_TRUTH = FactKinds.register(FactKind.builder("status.endTruth", "Effect ended (true effect)").plural("Effects ended (true effect)")
			.area("Status effects").description("Every status effect that ended, including ones with no icon, by what it really was.").subject("True effect").actor().internal()
			.value("remaining", "Turns left", "", FactKind.PLAIN).build());

	//---- character ----

	public static final FactKind TALENT = FactKinds.register(FactKind.builder("talent.upgrade", "Talent point").plural("Talent points").area("Character")
			.description("Every talent point spent.").subject("Talent")
			.value("points", "Points in it now", "", FactKind.PLAIN)
			.value("tier", "Tier", "", FactKind.PLAIN).build());

	public static final FactKind SUBCLASS = FactKinds.register(FactKind.builder("subclass.choose", "Subclass chosen").plural("Subclasses chosen").area("Character")
			.description("Every time a subclass was taken or lost.").subject("Subclass").build());

	public static final FactKind ABILITY_CHOOSE = FactKinds.register(FactKind.builder("ability.choose", "Armor ability chosen").plural("Armor abilities chosen")
			.area("Character").description("Every time an armor ability was chosen.").subject("Ability").build());

	public static final FactKind SPELL_CAST = FactKinds.register(FactKind.builder("spell.cast", "Spell cast").plural("Spell casts").area("Abilities")
			.description("Every spell the hero cast.").subject("Spell")
			.value("cost", "Charge cost", "", FactKind.PLAIN).build());

	public static final FactKind ABILITY_USE = FactKinds.register(FactKind.builder("ability.use", "Armor ability used").plural("Armor abilities used")
			.area("Abilities").description("Every time the hero used the ability of their class armor.").subject("Ability")
			.value("cost", "Charge cost", "", FactKind.PLAIN).build());

	//---- economy ----

	public static final FactKind GOLD = FactKinds.register(FactKind.builder("economy.gold", "Gold change").plural("Gold changes").area("Economy")
			.description("Every change in the hero's gold, with where it came from or went.")
			.value("delta", "Gold gained or spent", "Negative when spent.", FactKind.PLAIN)
			.value("total", "Gold after", "", FactKind.PLAIN)
			.tag("cause", "Cause").build());

	public static final FactKind TRADE = FactKinds.register(FactKind.builder("economy.trade", "Shop trade").plural("Shop trades").area("Economy")
			.description("Every item bought from or sold to a shopkeeper.").subject("Item")
			.value("price", "Price", "", FactKind.PLAIN)
			.value("quantity", "Quantity", "", FactKind.PLAIN)
			.tag("side", "Bought or sold").build());

	//---- time ----

	//cumulative time actually spent playing (idle gaps are not counted): the "per hour" measure is built from it
	public static final FactKind TIME_ACTIVE = FactKinds.register(FactKind.builder("time.active", "Time played").plural("Time played").area("Time")
			.description("How long the run has been played so far, checked at every floor change and save.")
			.value("millis", "Milliseconds played", "", FactKind.PLAIN).build());
}
