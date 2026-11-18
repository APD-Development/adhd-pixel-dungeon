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

//Encoders and decoders for the combat and health events of telemetry 1.3. Pure data, no game classes.
//
//As with item events there are two kinds. PUBLIC events say only what the player could see: the names the game shows, damage
//numbers, their own HP and shield, and only for fights they were part of or could see. The *_DETAIL events are INTERNAL and
//hold the truth behind them: exact classes, ids, enemy HP, the numbers that went into a hit.
//
//Hero healing is INFERRED in this version: the recorder sees the hero's HP rise between two looks and names the likely source,
//and every such event carries FLAG_INFERRED, except the heals the game's hooks report exactly (telemetry 1.6, HEAL_DETAIL).
//
//Field order inside a payload is part of the format. A telemetry 1.x version may only ADD fields at the END of a payload.
public final class CombatEvents {

	private CombatEvents(){}

	//event flag: the event is a deduction from state changes, not a direct observation of the thing happening
	public static final int FLAG_INFERRED = 8;

	//who is involved
	public static final int KIND_HERO = 0, KIND_ALLY = 1, KIND_ENEMY = 2, KIND_NEUTRAL = 3, KIND_NONE = 4;

	//what caused damage
	public static final int SRC_CHAR = 0, SRC_BUFF = 1, SRC_BLOB = 2, SRC_TRAP = 3, SRC_ITEM = 4, SRC_ENCHANT = 5,
			SRC_CLASS = 6, SRC_OTHER = 7;

	//HUNGER_STATE.state
	public static final int HUNGER_WELL_FED = 0, HUNGER_NORMAL = 1, HUNGER_HUNGRY = 2, HUNGER_STARVING = 3;

	//XP_GAIN.category
	public static final int XP_KILL = 0, XP_POTION = 1, XP_ASCENSION = 2, XP_OTHER = 3;

	//HERO_REVIVED.kind
	public static final int REVIVE_BLESSED_ANKH = 0, REVIVE_RESURRECT = 1;

	public static final class Where {
		public int depth;
		public int branch;
	}

	private static void putWhere( EventBuilder b, Where w ){
		b.unsigned(Math.max(0, w.depth)).unsigned(Math.max(0, w.branch));
	}

	private static Where readWhere( PayloadReader p ){
		Where w = new Where();
		w.depth = p.unsignedInt();
		w.branch = p.unsignedInt();
		return w;
	}

	//someone in a fight, as the player knows them
	public static final class FighterRef {
		public int kind = KIND_NONE;
		public String name = "";   //what the game calls it right now ("chest" for an unrevealed mimic)
		public int pos = -1;
	}

	private static void putFighter( EventBuilder b, FighterRef a ){
		b.unsigned(a.kind).string(a.name).signed(a.pos);
	}

	private static FighterRef readFighter( PayloadReader p ){
		FighterRef a = new FighterRef();
		a.kind = p.unsignedInt();
		a.name = str(p.string());
		a.pos = (int) p.signed();
		return a;
	}

	//what caused damage, as the player could tell
	public static final class SourceRef {
		public int kind = SRC_OTHER;
		public String label = "";  //a creature's or item's shown name, a status effect's name, a hazard's name; empty when not apparent
		public int fighterKind = KIND_NONE; //for SRC_CHAR
		public int pos = -1;
	}

	private static void putSource( EventBuilder b, SourceRef s ){
		b.unsigned(s.kind).string(s.label).unsigned(s.fighterKind).signed(s.pos);
	}

	private static SourceRef readSource( PayloadReader p ){
		SourceRef s = new SourceRef();
		s.kind = p.unsignedInt();
		s.label = str(p.string());
		s.fighterKind = p.unsignedInt();
		s.pos = (int) p.signed();
		return s;
	}

	//---- ATTACK (PUBLIC) ---------------------------------------------------------------------------------------------

	public static final class Attack {
		public Where where = new Where();
		public FighterRef attacker = new FighterRef();
		public FighterRef defender = new FighterRef();
		public boolean hit;
		public boolean magic;      //a spell or bolt that rolled to hit, not a weapon
		public boolean thrown;     //hero attacks only: the weapon was thrown
		public String weaponName = "";   //hero attacks: the weapon as the player sees it
		public String weaponUid = "";    //hero attacks: its apd_uid, so the weapon's whole history can be joined in
	}

	public static boolean emitAttack( RunLogWriter w, long clock, Attack a ){
		EventBuilder b = w.event(EventTypes.ATTACK).clock(clock);
		putWhere(b, a.where);
		putFighter(b, a.attacker);
		putFighter(b, a.defender);
		b.bool(a.hit).bool(a.magic).bool(a.thrown).string(a.weaponName).string(a.weaponUid);
		return b.emit();
	}

	public static Attack decodeAttack( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Attack a = new Attack();
		a.where = readWhere(p);
		a.attacker = readFighter(p);
		a.defender = readFighter(p);
		a.hit = p.bool();
		a.magic = p.bool();
		a.thrown = p.bool();
		a.weaponName = str(p.string());
		a.weaponUid = str(p.string());
		return a;
	}

	//---- ATTACK_DETAIL (INTERNAL) ------------------------------------------------------------------------------------

	public static final class AttackDetail {
		public Where where = new Where();
		public String attackerClass = "", defenderClass = "";
		public long attackerId, defenderId;
		public boolean hit, magic;
		public int rolled;          //damage rolled, after multipliers, before armor
		public int armorReduction;  //what the defender's armor took off
		public int effective;       //after armor and on-hit effects, what is passed on as damage
		public boolean publicEmitted;
	}

	public static boolean emitAttackDetail( RunLogWriter w, long clock, AttackDetail d ){
		EventBuilder b = w.event(EventTypes.ATTACK_DETAIL).clock(clock);
		putWhere(b, d.where);
		b.string(d.attackerClass).signed(d.attackerId).string(d.defenderClass).signed(d.defenderId).bool(d.hit).bool(d.magic)
				.signed(d.rolled).signed(d.armorReduction).signed(d.effective).bool(d.publicEmitted);
		return b.emit();
	}

	public static AttackDetail decodeAttackDetail( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		AttackDetail d = new AttackDetail();
		d.where = readWhere(p);
		d.attackerClass = str(p.string());
		d.attackerId = p.signed();
		d.defenderClass = str(p.string());
		d.defenderId = p.signed();
		d.hit = p.bool();
		d.magic = p.bool();
		d.rolled = (int) p.signed();
		d.armorReduction = (int) p.signed();
		d.effective = (int) p.signed();
		d.publicEmitted = p.bool();
		return d;
	}

	//---- DAMAGE (PUBLIC) -----------------------------------------------------------------------------------------

	//Damage that actually landed on something the player was involved with or could see.
	public static final class Damage {
		public Where where = new Where();
		public FighterRef target = new FighterRef();
		public SourceRef source = new SourceRef();
		public int shown;            //the number the game shows: HP lost plus what shielding absorbed
		public boolean killed;
		//only filled in when the target is the hero (its own HP and shield are always on screen), otherwise -1
		public int heroHp = -1, heroHpMax = -1, heroShield = -1, shieldAbsorbed = -1;
	}

	public static boolean emitDamage( RunLogWriter w, long clock, Damage d ){
		EventBuilder b = w.event(EventTypes.DAMAGE).clock(clock);
		putWhere(b, d.where);
		putFighter(b, d.target);
		putSource(b, d.source);
		b.unsigned(Math.max(0, d.shown)).bool(d.killed).signed(d.heroHp).signed(d.heroHpMax).signed(d.heroShield).signed(d.shieldAbsorbed);
		return b.emit();
	}

	public static Damage decodeDamage( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Damage d = new Damage();
		d.where = readWhere(p);
		d.target = readFighter(p);
		d.source = readSource(p);
		d.shown = p.unsignedInt();
		d.killed = p.bool();
		d.heroHp = (int) p.signed();
		d.heroHpMax = (int) p.signed();
		d.heroShield = (int) p.signed();
		d.shieldAbsorbed = (int) p.signed();
		return d;
	}

	//---- DAMAGE_DETAIL (INTERNAL) ---------------------------------------------------------------------------------

	public static final class DamageDetail {
		public Where where = new Where();
		public String targetClass = "";
		public long targetId;
		public int targetPos = -1;
		public int hpBefore, hpAfter, hpMax;
		public int shieldBefore, shieldAfter;
		public int requested;        //what the caller asked for, before shielding, resistance and the like
		public int absorbed;         //taken by shielding
		public int hpLost;           //taken from HP
		public int srcKind;
		public String srcClass = "";
		public long srcCharId = -1;
		public int srcPos = -1;
		public boolean publicEmitted;
	}

	public static boolean emitDamageDetail( RunLogWriter w, long clock, DamageDetail d ){
		EventBuilder b = w.event(EventTypes.DAMAGE_DETAIL).clock(clock);
		putWhere(b, d.where);
		b.string(d.targetClass).signed(d.targetId).signed(d.targetPos).signed(d.hpBefore).signed(d.hpAfter).signed(d.hpMax)
				.signed(d.shieldBefore).signed(d.shieldAfter).signed(d.requested).signed(d.absorbed).signed(d.hpLost)
				.unsigned(d.srcKind).string(d.srcClass).signed(d.srcCharId).signed(d.srcPos).bool(d.publicEmitted);
		return b.emit();
	}

	public static DamageDetail decodeDamageDetail( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		DamageDetail d = new DamageDetail();
		d.where = readWhere(p);
		d.targetClass = str(p.string());
		d.targetId = p.signed();
		d.targetPos = (int) p.signed();
		d.hpBefore = (int) p.signed();
		d.hpAfter = (int) p.signed();
		d.hpMax = (int) p.signed();
		d.shieldBefore = (int) p.signed();
		d.shieldAfter = (int) p.signed();
		d.requested = (int) p.signed();
		d.absorbed = (int) p.signed();
		d.hpLost = (int) p.signed();
		d.srcKind = p.unsignedInt();
		d.srcClass = str(p.string());
		d.srcCharId = p.signed();
		d.srcPos = (int) p.signed();
		d.publicEmitted = p.bool();
		return d;
	}

	//---- HEAL / MAX_HP_CHANGE / SHIELD_GAIN / HP_CHANGE_INFERRED (PUBLIC, inferred) -------------------------------------

	//The hero's HP rose. Inferred: 'guess' names the likely source, overheal is unknown (-1).
	public static final class Heal {
		public Where where = new Where();
		public int amount;
		public int hpAfter, hpMax;
		public String guess = "";
		public int overheal = -1;
	}

	//an inferred heal (found by looking at the hero's HP between turns)
	public static boolean emitHeal( RunLogWriter w, long clock, Heal h ){
		return emitHeal(w, clock, h, false);
	}

	//exact = a hook saw the game heal the hero (telemetry 1.6): the same payload, without the INFERRED flag
	public static boolean emitHeal( RunLogWriter w, long clock, Heal h, boolean exact ){
		EventBuilder b = w.event(EventTypes.HEAL).clock(clock).flags(exact ? 0 : FLAG_INFERRED);
		putWhere(b, h.where);
		b.unsigned(Math.max(0, h.amount)).signed(h.hpAfter).signed(h.hpMax).string(h.guess).signed(h.overheal);
		return b.emit();
	}

	public static Heal decodeHeal( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Heal h = new Heal();
		h.where = readWhere(p);
		h.amount = p.unsignedInt();
		h.hpAfter = (int) p.signed();
		h.hpMax = (int) p.signed();
		h.guess = str(p.string());
		h.overheal = (int) p.signed();
		return h;
	}

	//The hidden half of an exact heal (telemetry 1.6): what healed the hero, and what it asked for before the maximum HP capped it (-1 when
	//the source has no such number). INTERNAL, because the source can be hidden information (an unidentified potion or enchantment).
	//It is written right after the PUBLIC HEAL it belongs to and says only what that event does not (where, the HP before and after and the
	//maximum are all in the HEAL), which keeps it small: natural regeneration alone writes thousands of these in a long run (measured by
	//HealingVolumeTest). 'applied' repeats the HEAL's amount so the pair can still be read if either of them is ever lost.
	public static final class HealDetail {
		public String source = "";
		public int requested = -1;
		public int applied;

		public int applied(){
			return Math.max(0, applied);
		}
	}

	public static boolean emitHealDetail( RunLogWriter w, long clock, HealDetail h ){
		EventBuilder b = w.event(EventTypes.HEAL_DETAIL).clock(clock);
		b.string(h.source).signed(h.requested).unsigned(Math.max(0, h.applied));
		return b.emit();
	}

	public static HealDetail decodeHealDetail( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		HealDetail h = new HealDetail();
		h.source = str(p.string());
		h.requested = (int) p.signed();
		h.applied = p.unsignedInt();
		return h;
	}

	//The heal sources the game's hooks name (the keys are stored in the log, so they never change meaning), and the few categories UIMSD
	//sums them into. A source this build does not know, from a newer one, counts as OTHER.
	public static final String HC_REGEN = "regen", HC_POTION = "potion", HC_FOOD = "food", HC_PLANT = "plant", HC_GEAR = "gear",
			HC_SKILL = "skill", HC_WAND = "wand", HC_OTHER = "other";
	public static final String[] HEAL_CATEGORIES = { HC_REGEN, HC_POTION, HC_FOOD, HC_PLANT, HC_GEAR, HC_SKILL, HC_WAND, HC_OTHER };

	//the category is the part of the key before the first dot: "potion.healing" -> potion
	public static String healCategory( String source ){
		if (source == null) return HC_OTHER;
		int dot = source.indexOf('.');
		String c = dot < 0 ? source : source.substring(0, dot);
		for (String known : HEAL_CATEGORIES) if (known.equals(c)) return known;
		return HC_OTHER;
	}

	public static final class MaxHpChange {
		public Where where = new Where();
		public int oldMax, newMax, hpAfter;
		public String guess = "";
	}

	public static boolean emitMaxHpChange( RunLogWriter w, long clock, MaxHpChange m ){
		EventBuilder b = w.event(EventTypes.MAX_HP_CHANGE).clock(clock).flags(FLAG_INFERRED);
		putWhere(b, m.where);
		b.signed(m.oldMax).signed(m.newMax).signed(m.hpAfter).string(m.guess);
		return b.emit();
	}

	public static MaxHpChange decodeMaxHpChange( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		MaxHpChange m = new MaxHpChange();
		m.where = readWhere(p);
		m.oldMax = (int) p.signed();
		m.newMax = (int) p.signed();
		m.hpAfter = (int) p.signed();
		m.guess = str(p.string());
		return m;
	}

	public static final class ShieldGain {
		public Where where = new Where();
		public int amount;
		public int shieldAfter;
		public String source = "";   //the shield effect that grew, as named in the status icons
	}

	public static boolean emitShieldGain( RunLogWriter w, long clock, ShieldGain s ){
		EventBuilder b = w.event(EventTypes.SHIELD_GAIN).clock(clock).flags(FLAG_INFERRED);
		putWhere(b, s.where);
		b.unsigned(Math.max(0, s.amount)).signed(s.shieldAfter).string(s.source);
		return b.emit();
	}

	public static ShieldGain decodeShieldGain( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		ShieldGain s = new ShieldGain();
		s.where = readWhere(p);
		s.amount = p.unsignedInt();
		s.shieldAfter = (int) p.signed();
		s.source = str(p.string());
		return s;
	}

	public static final class HpChangeInferred {
		public Where where = new Where();
		public int delta;     //negative: HP fell with nothing recorded as damage
		public int hpAfter, hpMax;
	}

	public static boolean emitHpChangeInferred( RunLogWriter w, long clock, HpChangeInferred h ){
		EventBuilder b = w.event(EventTypes.HP_CHANGE_INFERRED).clock(clock).flags(FLAG_INFERRED);
		putWhere(b, h.where);
		b.signed(h.delta).signed(h.hpAfter).signed(h.hpMax);
		return b.emit();
	}

	public static HpChangeInferred decodeHpChangeInferred( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		HpChangeInferred h = new HpChangeInferred();
		h.where = readWhere(p);
		h.delta = (int) p.signed();
		h.hpAfter = (int) p.signed();
		h.hpMax = (int) p.signed();
		return h;
	}

	//---- hunger (PUBLIC) -------------------------------------------------------------------------------------------

	public static final class HungerChange {
		public Where where = new Where();
		public int energy;     //positive: fed (eating), negative: hungrier. As the game applied it.
		public int before, after;   //the hunger counter (higher is hungrier)
		public String cause = "";   //the action and item when known ("EAT", the food's shown name)
	}

	public static boolean emitHungerChange( RunLogWriter w, long clock, HungerChange h ){
		EventBuilder b = w.event(EventTypes.HUNGER_CHANGE).clock(clock);
		putWhere(b, h.where);
		b.signed(h.energy).signed(h.before).signed(h.after).string(h.cause);
		return b.emit();
	}

	public static HungerChange decodeHungerChange( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		HungerChange h = new HungerChange();
		h.where = readWhere(p);
		h.energy = (int) p.signed();
		h.before = (int) p.signed();
		h.after = (int) p.signed();
		h.cause = str(p.string());
		return h;
	}

	public static final class HungerState {
		public Where where = new Where();
		public int state;
		public int previous;
	}

	public static boolean emitHungerState( RunLogWriter w, long clock, HungerState h ){
		EventBuilder b = w.event(EventTypes.HUNGER_STATE).clock(clock);
		putWhere(b, h.where);
		b.unsigned(h.state).unsigned(h.previous);
		return b.emit();
	}

	public static HungerState decodeHungerState( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		HungerState h = new HungerState();
		h.where = readWhere(p);
		h.state = p.unsignedInt();
		h.previous = p.unsignedInt();
		return h;
	}

	//---- XP / levels (PUBLIC) --------------------------------------------------------------------------------------

	public static final class XpGain {
		public Where where = new Where();
		public int amount;
		public int expBefore, maxExp, levelBefore;
		public int category;
	}

	public static boolean emitXpGain( RunLogWriter w, long clock, XpGain x ){
		EventBuilder b = w.event(EventTypes.XP_GAIN).clock(clock);
		putWhere(b, x.where);
		b.unsigned(Math.max(0, x.amount)).unsigned(Math.max(0, x.expBefore)).unsigned(Math.max(0, x.maxExp)).unsigned(Math.max(0, x.levelBefore)).unsigned(x.category);
		return b.emit();
	}

	public static XpGain decodeXpGain( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		XpGain x = new XpGain();
		x.where = readWhere(p);
		x.amount = p.unsignedInt();
		x.expBefore = p.unsignedInt();
		x.maxExp = p.unsignedInt();
		x.levelBefore = p.unsignedInt();
		x.category = p.unsignedInt();
		return x;
	}

	public static final class LevelUp {
		public Where where = new Where();
		public int oldLevel, newLevel;
	}

	public static boolean emitLevelUp( RunLogWriter w, long clock, LevelUp l ){
		EventBuilder b = w.event(EventTypes.LEVEL_UP).clock(clock);
		putWhere(b, l.where);
		b.unsigned(Math.max(0, l.oldLevel)).unsigned(Math.max(0, l.newLevel));
		return b.emit();
	}

	public static LevelUp decodeLevelUp( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		LevelUp l = new LevelUp();
		l.where = readWhere(p);
		l.oldLevel = p.unsignedInt();
		l.newLevel = p.unsignedInt();
		return l;
	}

	//---- KILL (PUBLIC) / KILL_DETAIL (INTERNAL) -----------------------------------------------------------------------

	public static final class Kill {
		public Where where = new Where();
		public FighterRef victim = new FighterRef();
		public SourceRef killer = new SourceRef();
	}

	public static boolean emitKill( RunLogWriter w, long clock, Kill k ){
		EventBuilder b = w.event(EventTypes.KILL).clock(clock);
		putWhere(b, k.where);
		putFighter(b, k.victim);
		putSource(b, k.killer);
		return b.emit();
	}

	public static Kill decodeKill( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Kill k = new Kill();
		k.where = readWhere(p);
		k.victim = readFighter(p);
		k.killer = readSource(p);
		return k;
	}

	public static final class KillDetail {
		public Where where = new Where();
		public String victimClass = "";
		public long victimId;
		public int victimKind;
		public int expValue;       //the experience the creature is worth (hidden from the player)
		public String killerClass = "";
		public boolean publicEmitted;
	}

	public static boolean emitKillDetail( RunLogWriter w, long clock, KillDetail k ){
		EventBuilder b = w.event(EventTypes.KILL_DETAIL).clock(clock);
		putWhere(b, k.where);
		b.string(k.victimClass).signed(k.victimId).unsigned(k.victimKind).signed(k.expValue).string(k.killerClass).bool(k.publicEmitted);
		return b.emit();
	}

	public static KillDetail decodeKillDetail( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		KillDetail k = new KillDetail();
		k.where = readWhere(p);
		k.victimClass = str(p.string());
		k.victimId = p.signed();
		k.victimKind = p.unsignedInt();
		k.expValue = (int) p.signed();
		k.killerClass = str(p.string());
		k.publicEmitted = p.bool();
		return k;
	}

	//---- HERO_DEATH / HERO_REVIVED (PUBLIC) ---------------------------------------------------------------------------

	//The hero's HP reached zero. An ankh may still bring them back (HERO_REVIVED follows); the run only ends with RUN_END.
	public static final class HeroDeath {
		public Where where = new Where();
		public SourceRef cause = new SourceRef();
		public int pos = -1;
	}

	public static boolean emitHeroDeath( RunLogWriter w, long clock, HeroDeath d ){
		EventBuilder b = w.event(EventTypes.HERO_DEATH).clock(clock);
		putWhere(b, d.where);
		putSource(b, d.cause);
		b.signed(d.pos);
		return b.emit();
	}

	public static HeroDeath decodeHeroDeath( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		HeroDeath d = new HeroDeath();
		d.where = readWhere(p);
		d.cause = readSource(p);
		d.pos = (int) p.signed();
		return d;
	}

	public static final class HeroRevived {
		public Where where = new Where();
		public int kind;
		public int hpAfter, hpMax;
	}

	public static boolean emitHeroRevived( RunLogWriter w, long clock, HeroRevived r ){
		EventBuilder b = w.event(EventTypes.HERO_REVIVED).clock(clock);
		putWhere(b, r.where);
		b.unsigned(r.kind).signed(r.hpAfter).signed(r.hpMax);
		return b.emit();
	}

	public static HeroRevived decodeHeroRevived( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		HeroRevived r = new HeroRevived();
		r.where = readWhere(p);
		r.kind = p.unsignedInt();
		r.hpAfter = (int) p.signed();
		r.hpMax = (int) p.signed();
		return r;
	}

	private static String str( String s ){
		return s == null ? "" : s;
	}
}
