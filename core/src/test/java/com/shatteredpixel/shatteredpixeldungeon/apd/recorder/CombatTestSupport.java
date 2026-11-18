package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;

import java.util.ArrayList;
import java.util.List;

//Helpers for tests that fight with the real combat code, headless.
final class CombatTestSupport {

	private CombatTestSupport(){}

	//a run with a hero that can fight without a screen
	static RecorderTestSupport.Session startFight( long seed ){
		RecorderTestSupport.Session s = ItemTestSupport.startRun(seed);
		new QuietSprite(Dungeon.hero);
		Dungeon.hero.damageInterrupt = false;
		return s;
	}

	static Rat rat( int offset, boolean inView ){
		Rat r = new Rat();
		r.pos = Dungeon.hero.pos + offset;
		new QuietSprite(r);
		Dungeon.level.heroFOV[r.pos] = inView;
		Dungeon.level.heroFOV[Dungeon.hero.pos] = inView;
		return r;
	}

	static Hero hero(){
		return Dungeon.hero;
	}

	//attacks until the target is dead, returns how many attacks it took
	static int fightToTheDeath( Char attacker, Char target ){
		int n = 0;
		while (target.isAlive() && n < 200){
			attacker.attack(target);
			n++;
		}
		return n;
	}

	static List<ApdEvent> of( ReadResult r, int type ){
		List<ApdEvent> out = new ArrayList<>();
		for (ApdEvent e : r.events) if (e.type == type) out.add(e);
		return out;
	}

	static List<CombatEvents.Damage> damages( ReadResult r ){
		List<CombatEvents.Damage> out = new ArrayList<>();
		for (ApdEvent e : of(r, EventTypes.DAMAGE)) out.add(CombatEvents.decodeDamage(e, r));
		return out;
	}

	static List<CombatEvents.DamageDetail> damageDetails( ReadResult r ){
		List<CombatEvents.DamageDetail> out = new ArrayList<>();
		for (ApdEvent e : of(r, EventTypes.DAMAGE_DETAIL)) out.add(CombatEvents.decodeDamageDetail(e, r));
		return out;
	}

	static List<CombatEvents.Attack> attacks( ReadResult r ){
		List<CombatEvents.Attack> out = new ArrayList<>();
		for (ApdEvent e : of(r, EventTypes.ATTACK)) out.add(CombatEvents.decodeAttack(e, r));
		return out;
	}

	static List<CombatEvents.AttackDetail> attackDetails( ReadResult r ){
		List<CombatEvents.AttackDetail> out = new ArrayList<>();
		for (ApdEvent e : of(r, EventTypes.ATTACK_DETAIL)) out.add(CombatEvents.decodeAttackDetail(e, r));
		return out;
	}

	static boolean isAMob( Char c ){
		return c instanceof Mob;
	}
}
