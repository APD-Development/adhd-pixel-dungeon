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


package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

//Combat and health recording for one run. Package-private; the game reaches it through ApdCombat/ApdRecorder.
//
//Everything here READS the game after the game has decided. It never rolls dice, never calls anything that has side
//effects (no attackSkill/defenseSkill/damageRoll/drRoll, which consume random numbers or buffs), and never changes a
//character. Damage, attacks, kills and deaths are caught where the game itself decides them. Hero healing, shield gains,
//max HP changes and hunger transitions have no single place in SPD (about 40 separate HP writes), so they are found by
//looking at the hero between turns and are marked inferred.
final class CombatTracker {

	private final RunLogWriter w;
	private final ItemTracker items;

	//the hero as last looked at; changes since then are what gets explained
	private boolean haveBaseline;
	private int lastHp, lastHt, lastShield, lastLevel, lastHungerState;
	private final Map<Class<?>, Integer> shieldParts = new HashMap<>();

	CombatTracker( RunLogWriter w, ItemTracker items ){
		this.w = w;
		this.items = items;
	}

	//---- naming --------------------------------------------------------------------------------------------------

	private static long clock(){
		return GameSnapshot.turn();
	}

	private static CombatEvents.Where where(){
		CombatEvents.Where x = new CombatEvents.Where();
		x.depth = Dungeon.depth;
		x.branch = Dungeon.branch;
		return x;
	}

	private static int kindOf( Char c ){
		if (c == Dungeon.hero) return CombatEvents.KIND_HERO;
		if (c.alignment == Char.Alignment.ALLY) return CombatEvents.KIND_ALLY;
		if (c.alignment == Char.Alignment.ENEMY) return CombatEvents.KIND_ENEMY;
		return CombatEvents.KIND_NEUTRAL;
	}

	static String nameOf( Char c ){
		try {
			String n = c.name();
			return n == null ? "" : n;
		} catch (Throwable t){
			return "";
		}
	}

	private static CombatEvents.FighterRef ref( Char c ){
		CombatEvents.FighterRef a = new CombatEvents.FighterRef();
		if (c == null) return a;
		a.kind = kindOf(c);
		a.name = nameOf(c);
		a.pos = c.pos;
		return a;
	}

	//can the player see this creature right now (the hero always counts: its own state is always on screen)
	static boolean visible( Char c ){
		if (c == null) return false;
		if (c == Dungeon.hero) return true;
		Level l = Dungeon.level;
		return l != null && l.heroFOV != null && c.pos >= 0 && c.pos < l.heroFOV.length && l.heroFOV[c.pos];
	}

	//a fight is shown to the player when they are in it or can see either side
	private static boolean shown( Char a, Char b ){
		return a == Dungeon.hero || b == Dungeon.hero || visible(a) || visible(b);
	}

	private static CombatEvents.SourceRef describe( Object src ){
		CombatEvents.SourceRef s = new CombatEvents.SourceRef();
		try {
			if (src instanceof Char){
				Char c = (Char) src;
				s.kind = CombatEvents.SRC_CHAR;
				s.label = nameOf(c);
				s.fighterKind = kindOf(c);
				s.pos = c.pos;
			} else if (src instanceof Buff){
				s.kind = CombatEvents.SRC_BUFF;
				s.label = String.valueOf(((Buff) src).name());
			} else if (src instanceof Blob){
				s.kind = CombatEvents.SRC_BLOB;
			} else if (src instanceof Trap){
				s.kind = CombatEvents.SRC_TRAP;
			} else if (src instanceof Item){
				s.kind = CombatEvents.SRC_ITEM;
				s.label = String.valueOf(((Item) src).name());
			} else if (src instanceof Weapon.Enchantment || src instanceof Armor.Glyph){
				s.kind = CombatEvents.SRC_ENCHANT;
			} else if (src instanceof Class){
				s.kind = CombatEvents.SRC_CLASS;
				s.label = ((Class<?>) src).getSimpleName();
			}
		} catch (Throwable t){
			//a source that cannot be described stays "other"
		}
		return s;
	}

	private static String trueClass( Object src ){
		if (src == null) return "";
		return GameSnapshot.shortName(src instanceof Class ? (Class<?>) src : src.getClass());
	}

	//---- attacks -------------------------------------------------------------------------------------------------

	//The roll to hit came out. Writes the public ATTACK (names as they are at this moment) and, for a miss, its detail.
	void onAttackRolled( Char attacker, Char defender, boolean hit ){
		publicAttack(attacker, defender, hit, false);
		if (!hit) attackDetail(attacker, defender, false, false, 0, 0, 0);
	}

	//the numbers behind a hit, written just before its damage is dealt
	void onAttackDetail( Char attacker, Char defender, int rolled, int armorReduction, int effective ){
		attackDetail(attacker, defender, true, false, rolled, armorReduction, effective);
	}

	//spells and bolts that roll to hit without going through Char.attack
	void onMagicRoll( Char attacker, Char defender, boolean hit ){
		publicAttack(attacker, defender, hit, true);
		attackDetail(attacker, defender, hit, true, 0, 0, 0);
	}

	private void publicAttack( Char a, Char d, boolean hit, boolean magic ){
		if (a == null || d == null || !shown(a, d)) return;
		CombatEvents.Attack e = new CombatEvents.Attack();
		e.where = where();
		e.attacker = ref(a);
		e.defender = ref(d);
		e.hit = hit;
		e.magic = magic;
		if (a instanceof Hero){
			Hero h = (Hero) a;
			Item weapon = h.belongings == null ? null : h.belongings.attackingWeapon();
			e.thrown = h.belongings != null && h.belongings.thrownWeapon != null;
			if (weapon != null){
				try {
					e.weaponName = String.valueOf(weapon.name());
				} catch (Throwable t){
					e.weaponName = "";
				}
				String uid = items == null ? null : items.ensureUid(weapon, com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.ORIGIN_OBSERVED);
				e.weaponUid = uid == null ? "" : uid;
			}
		}
		CombatEvents.emitAttack(w, clock(), e);
	}

	private void attackDetail( Char a, Char d, boolean hit, boolean magic, int rolled, int dr, int effective ){
		if (a == null || d == null) return;
		CombatEvents.AttackDetail x = new CombatEvents.AttackDetail();
		x.where = where();
		x.attackerClass = trueClass(a);
		x.attackerId = a.apdPeekId();
		x.defenderClass = trueClass(d);
		x.defenderId = d.apdPeekId();
		x.hit = hit;
		x.magic = magic;
		x.rolled = rolled;
		x.armorReduction = dr;
		x.effective = effective;
		x.publicEmitted = shown(a, d);
		CombatEvents.emitAttackDetail(w, clock(), x);
	}

	//---- damage --------------------------------------------------------------------------------------------------

	//Damage was applied to 'target'. hpBefore/shieldBefore were read before shielding, 'absorbed' is what the shield took
	//and 'hpLost' what HP lost, both from the game's own numbers.
	void onDamage( Char target, Object src, int requested, int hpBefore, int shieldBefore, int absorbed, int hpLost ){
		if (target == null) return;
		Char srcChar = src instanceof Char ? (Char) src : null;
		boolean show = target == Dungeon.hero || srcChar == Dungeon.hero || visible(target) || visible(srcChar);
		boolean killed = target.HP <= 0;
		int shieldAfter = 0;
		try {
			shieldAfter = target.shielding();
		} catch (Throwable t){
			shieldAfter = 0;
		}
		CombatEvents.Where where = where();
		if (show){
			CombatEvents.Damage e = new CombatEvents.Damage();
			e.where = where;
			e.target = ref(target);
			e.source = describe(src);
			e.shown = hpLost + absorbed;
			e.killed = killed;
			if (target == Dungeon.hero){
				e.heroHp = target.HP;
				e.heroHpMax = target.HT;
				e.heroShield = shieldAfter;
				e.shieldAbsorbed = absorbed;
			}
			CombatEvents.emitDamage(w, clock(), e);
		}
		CombatEvents.DamageDetail d = new CombatEvents.DamageDetail();
		d.where = where;
		d.targetClass = trueClass(target);
		d.targetId = target.apdPeekId();
		d.targetPos = target.pos;
		d.hpBefore = hpBefore;
		d.hpAfter = target.HP;
		d.hpMax = target.HT;
		d.shieldBefore = shieldBefore;
		d.shieldAfter = shieldAfter;
		d.requested = requested;
		d.absorbed = absorbed;
		d.hpLost = hpLost;
		d.srcKind = describe(src).kind;
		d.srcClass = trueClass(src);
		d.srcCharId = srcChar == null ? -1 : srcChar.apdPeekId();
		d.srcPos = srcChar == null ? -1 : srcChar.pos;
		d.publicEmitted = show;
		CombatEvents.emitDamageDetail(w, clock(), d);

		if (target == Dungeon.hero){
			//the game explained this change itself, so it is not an unexplained one
			lastHp = target.HP;
			lastShield = shieldAfter;
			snapshotShields((Hero) target);
		}
	}

	//---- exact healing ---------------------------------------------------------------------------------------------

	//The game healed a creature: only the hero's HP is recorded. The amount is what the game really applied (HP now minus HP before).
	//The baseline moves past it, so the look between turns does not count it a second time; whatever heals the hooks do not know about
	//still shows up there as an inferred heal.
	void onHeal( Char target, int hpBefore, String source, int requested ){
		Hero h = Dungeon.hero;
		if (target == null || target != h) return;
		int applied = h.HP - hpBefore;
		if (applied <= 0) return;
		if (!haveBaseline) baseline();   //the heal is already part of the baseline it takes
		else lastHp += applied;
		long clock = clock();
		CombatEvents.Where where = where();
		CombatEvents.Heal e = new CombatEvents.Heal();
		e.where = where;
		e.amount = applied;
		e.hpAfter = h.HP;
		e.hpMax = h.HT;
		e.guess = guessHealSource(h, applied);
		CombatEvents.emitHeal(w, clock, e, true);
		CombatEvents.HealDetail d = new CombatEvents.HealDetail();
		d.source = source == null ? "" : source;
		d.requested = requested;
		d.applied = applied;
		CombatEvents.emitHealDetail(w, clock, d);
	}

	//---- deaths --------------------------------------------------------------------------------------------------

	//a creature other than the hero died
	void onCharDied( Char ch, Object src ){
		if (ch == null || ch == Dungeon.hero) return;
		Char killerChar = src instanceof Char ? (Char) src : null;
		boolean show = visible(ch) || killerChar == Dungeon.hero;
		CombatEvents.Where where = where();
		if (show){
			CombatEvents.Kill k = new CombatEvents.Kill();
			k.where = where;
			k.victim = ref(ch);
			k.killer = describe(src);
			CombatEvents.emitKill(w, clock(), k);
		}
		CombatEvents.KillDetail d = new CombatEvents.KillDetail();
		d.where = where;
		d.victimClass = trueClass(ch);
		d.victimId = ch.apdPeekId();
		d.victimKind = kindOf(ch);
		d.expValue = ch instanceof Mob ? ((Mob) ch).EXP : 0;
		d.killerClass = trueClass(src);
		d.publicEmitted = show;
		CombatEvents.emitKillDetail(w, clock(), d);
	}

	void onHeroDying( Object cause ){
		Hero h = Dungeon.hero;
		CombatEvents.HeroDeath d = new CombatEvents.HeroDeath();
		d.where = where();
		d.cause = describe(cause);
		d.pos = h == null ? -1 : h.pos;
		CombatEvents.emitHeroDeath(w, clock(), d);
	}

	void onHeroRevived( int kind ){
		Hero h = Dungeon.hero;
		CombatEvents.HeroRevived r = new CombatEvents.HeroRevived();
		r.where = where();
		r.kind = kind;
		r.hpAfter = h == null ? 0 : h.HP;
		r.hpMax = h == null ? 0 : h.HT;
		CombatEvents.emitHeroRevived(w, clock(), r);
		//coming back to life is explained by this event, not an unexplained heal
		baseline();
	}

	//---- experience and hunger -----------------------------------------------------------------------------------

	void onXp( Hero hero, int amount, Class<?> source ){
		if (hero == null) return;
		CombatEvents.XpGain x = new CombatEvents.XpGain();
		x.where = where();
		x.amount = amount;
		x.expBefore = hero.exp;
		x.maxExp = hero.maxExp();
		x.levelBefore = hero.lvl;
		x.category = categoryOf(source);
		CombatEvents.emitXpGain(w, clock(), x);
	}

	private static int categoryOf( Class<?> source ){
		if (source == null) return CombatEvents.XP_OTHER;
		if (Mob.class.isAssignableFrom(source)) return CombatEvents.XP_KILL;
		if (PotionOfExperience.class.isAssignableFrom(source)) return CombatEvents.XP_POTION;
		if (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge.class.isAssignableFrom(source)) return CombatEvents.XP_ASCENSION;
		return CombatEvents.XP_OTHER;
	}

	//hunger was changed by eating or an effect
	void onHungerChanged( float energy, int before, int after ){
		CombatEvents.HungerChange h = new CombatEvents.HungerChange();
		h.where = where();
		h.energy = Math.round(energy);
		h.before = before;
		h.after = after;
		String action = items == null ? "" : items.hintedAction();
		String name = items == null ? "" : items.hintedItemName();
		h.cause = action.isEmpty() ? "" : (name.isEmpty() ? action : action + " " + name);
		CombatEvents.emitHungerChange(w, clock(), h);
	}

	//---- looking at the hero between turns ------------------------------------------------------------------------

	//Sets what "unchanged" means without writing anything (a new run, a load, a revival).
	void baseline(){
		Hero h = Dungeon.hero;
		if (h == null) return;
		lastHp = h.HP;
		lastHt = h.HT;
		lastShield = safeShield(h);
		lastLevel = h.lvl;
		lastHungerState = hungerState(h);
		snapshotShields(h);
		haveBaseline = true;
	}

	private static int safeShield( Hero h ){
		try {
			return h.shielding();
		} catch (Throwable t){
			return 0;
		}
	}

	private static int hungerState( Hero h ){
		Hunger hg = h.buff(Hunger.class);
		if (hg != null && hg.isStarving()) return CombatEvents.HUNGER_STARVING;
		if (hg != null && hg.hunger() >= Hunger.HUNGRY) return CombatEvents.HUNGER_HUNGRY;
		if (h.buff(WellFed.class) != null) return CombatEvents.HUNGER_WELL_FED;
		return CombatEvents.HUNGER_NORMAL;
	}

	private void snapshotShields( Hero h ){
		shieldParts.clear();
		for (Buff b : new LinkedHashSet<>(h.buffs())){
			if (b instanceof ShieldBuff){
				Class<?> c = b.getClass();
				Integer old = shieldParts.get(c);
				shieldParts.put(c, (old == null ? 0 : old) + ((ShieldBuff) b).shielding());
			}
		}
	}

	//what the game was doing this turn, as far as the item hints know
	private String contextHint(){
		String action = items == null ? "" : items.hintedAction();
		if (action.isEmpty()) return "";
		String name = items.hintedItemName();
		return name.isEmpty() ? action : action + " " + name;
	}

	void poll(){
		Hero h = Dungeon.hero;
		if (h == null) return;
		if (!haveBaseline){
			baseline();
			return;
		}
		long clock = clock();
		int hp = h.HP, ht = h.HT, shield = safeShield(h);

		if (ht != lastHt){
			CombatEvents.MaxHpChange m = new CombatEvents.MaxHpChange();
			m.where = where();
			m.oldMax = lastHt;
			m.newMax = ht;
			m.hpAfter = hp;
			m.guess = h.lvl > lastLevel ? "level up" : contextHint();
			CombatEvents.emitMaxHpChange(w, clock, m);
		}

		int hpDelta = hp - lastHp;
		int htDelta = ht - lastHt;
		//a higher maximum that raised HP with it (levelling up) is not healing
		int healed = hpDelta - Math.max(0, htDelta);
		if (healed > 0){
			CombatEvents.Heal e = new CombatEvents.Heal();
			e.where = where();
			e.amount = healed;
			e.hpAfter = hp;
			e.hpMax = ht;
			e.guess = guessHealSource(h, healed);
			CombatEvents.emitHeal(w, clock, e);
		} else if (hpDelta < 0 && !(htDelta < 0 && hp == ht)){
			//lost HP with no damage recorded (and not just a lowered maximum clamping it)
			CombatEvents.HpChangeInferred e = new CombatEvents.HpChangeInferred();
			e.where = where();
			e.delta = hpDelta;
			e.hpAfter = hp;
			e.hpMax = ht;
			CombatEvents.emitHpChangeInferred(w, clock, e);
		}

		//shield effects that grew since the last look, named by the effect (decay is not an event: damage events carry
		//the shield before and after)
		Map<Class<?>, Integer> nowParts = new HashMap<>();
		Map<Class<?>, String> partNames = new HashMap<>();
		for (Buff b : new LinkedHashSet<>(h.buffs())){
			if (!(b instanceof ShieldBuff)) continue;
			Class<?> c = b.getClass();
			Integer old = nowParts.get(c);
			nowParts.put(c, (old == null ? 0 : old) + ((ShieldBuff) b).shielding());
			partNames.put(c, String.valueOf(b.name()));
		}
		for (Map.Entry<Class<?>, Integer> en : nowParts.entrySet()){
			Integer before = shieldParts.get(en.getKey());
			int was = before == null ? 0 : before;
			if (en.getValue() > was){
				CombatEvents.ShieldGain g = new CombatEvents.ShieldGain();
				g.where = where();
				g.amount = en.getValue() - was;
				g.shieldAfter = shield;
				g.source = partNames.get(en.getKey());
				CombatEvents.emitShieldGain(w, clock, g);
			}
		}

		if (h.lvl != lastLevel){
			CombatEvents.LevelUp l = new CombatEvents.LevelUp();
			l.where = where();
			l.oldLevel = lastLevel;
			l.newLevel = h.lvl;
			CombatEvents.emitLevelUp(w, clock, l);
		}

		int hs = hungerState(h);
		if (hs != lastHungerState){
			CombatEvents.HungerState e = new CombatEvents.HungerState();
			e.where = where();
			e.state = hs;
			e.previous = lastHungerState;
			CombatEvents.emitHungerState(w, clock, e);
		}

		lastHp = hp;
		lastHt = ht;
		lastShield = shield;
		lastLevel = h.lvl;
		lastHungerState = hs;
		snapshotShields(h);
	}

	//a deduction, labelled as such in the event: which of the things that heal could have done this
	private String guessHealSource( Hero h, int amount ){
		String hint = contextHint();
		if (!hint.isEmpty()) return hint;
		if (h.buff(Healing.class) != null) return "Healing";
		if (amount <= 2 && h.buff(WellFed.class) != null) return "WellFed";
		if (amount <= 2 && h.buff(Regeneration.class) != null) return "Regeneration";
		return "";
	}
}
