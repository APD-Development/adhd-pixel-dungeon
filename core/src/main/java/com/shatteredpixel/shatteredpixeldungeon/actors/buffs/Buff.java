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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdPlay;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamage;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.TimedDamageOrigins;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.HashSet;

public class Buff extends Actor {
	
	public Char target;

    // Display-only damage history, saved with the buff. Never feeds back into combat.
    private long apdDotRaw, apdDotHp, apdDotShield;
    public long apdDotRaw(){return apdDotRaw;}
    public long apdDotHp(){return apdDotHp;}
    public long apdDotShield(){return apdDotShield;}
    public void apdRecordDot(int input,int shield,int hp){
        if (!(this instanceof DOTbuff)) return;
        apdDotRaw+=Math.max(0,input);
        apdDotShield+=Math.max(0,shield);
        apdDotHp+=Math.max(0,hp);
    }

	// Transient metadata used only while a newly-created offensive buff is attaching.
	// It is deliberately not serialized: persistent damage buffs own their saved provenance.
	private OffensiveDamageOrigin apdAttachOrigin = OffensiveDamageOrigin.UNATTRIBUTED;
	private TimedDamageOrigins apdAttachOrigins;

	private void apdPrepareAttach(OffensiveDamageOrigin origin) {
		if (origin == null) throw new IllegalArgumentException("Missing buff attachment origin");
		apdAttachOrigin = origin;
		apdAttachOrigins = null;
	}

	private void apdPrepareAttach(TimedDamageOrigins origins) {
		if (origins == null) throw new IllegalArgumentException("Missing mixed buff attachment origin");
		apdAttachOrigin = OffensiveDamageOrigin.UNATTRIBUTED;
		apdAttachOrigins = origins;
	}

	/** Scale an immediate attachment reaction without replacing this buff as its source. */
	public int apdScaleAttachDamage(int amount) {
		if (apdAttachOrigins != null) return apdAttachOrigins.scale(amount);
		if (apdAttachOrigin == OffensiveDamageOrigin.UNATTRIBUTED
				|| apdAttachOrigin.percentage() == 100) return amount;
		return OffensiveDamage.raw(amount, this, apdAttachOrigin).scaled().amount;
	}

	//whether this buff was already extended by the mnemonic prayer spell
	public boolean mnemonicExtended = false;

	{
		actPriority = BUFF_PRIO; //low priority, towards the end of a turn
	}

	//determines how the buff is announced when it is shown.
	public enum buffType {POSITIVE, NEGATIVE, NEUTRAL}
	public buffType type = buffType.NEUTRAL;
	
	//whether or not the buff announces its name
	public boolean announced = false;

	//whether a buff should persist through revive effects or similar (e.g. transmogrify)
	public boolean revivePersists = false;
	
	protected HashSet<Class> resistances = new HashSet<>();
	
	public HashSet<Class> resistances() {
		return new HashSet<>(resistances);
	}
	
	protected HashSet<Class> immunities = new HashSet<>();
	
	public HashSet<Class> immunities() {
		return new HashSet<>(immunities);
	}
	
	public boolean attachTo( Char target ) {

		if (target.isImmune( getClass() )) {
			return false;
		}
		
		this.target = target;

		boolean attached;
		try {
			attached = target.add(this);
		} finally {
			apdAttachOrigin = OffensiveDamageOrigin.UNATTRIBUTED;
			apdAttachOrigins = null;
		}
		if (attached) {
			if (target.sprite != null) fx( true );
			ApdPlay.onBuffAttached( this, target );
			return true;
		} else {
			this.target = null;
			return false;
		}
	}
	
	public void detach() {
		boolean removed = target.remove( this );
		if (removed) ApdPlay.onBuffDetached( this, target );
		if (removed && target.sprite != null) fx( false );
	}
	
	@Override
	public boolean act() {
		diactivate();
		return true;
	}
	
	public int icon() {
		return BuffIndicator.NONE;
	}

	//some buffs may want to tint the base texture color of their icon
	public void tintIcon( Image icon ){
		//do nothing by default
	}

	//percent (0-1) to fade out out the buff icon, usually if buff is expiring
	public float iconFadePercent(){
		return 0;
	}

	//text to display on large buff icons in the desktop UI
	public String iconTextDisplay(){
		return "";
	}

	//visual effect usually attached to the sprite of the character the buff is attacked to
	public void fx(boolean on) {
		//do nothing by default
	}

	public String heroMessage(){
		String msg = Messages.get(this, "heromsg");
		if (msg.isEmpty()) {
			return null;
		} else {
			return msg;
		}
	}

	public String name() {
		return Messages.get(this, "name");
	}

	public String desc(){
		return Messages.get(this, "desc");
	}

	//to handle the common case of showing how many turns are remaining in a buff description.
	protected String dispTurns(float input){
		return Messages.decimalFormat("#.##", input);
	}

	//buffs act after the hero, so it is often useful to use cooldown+1 when display buff time remaining
	public float visualcooldown(){
		return cooldown()+1f;
	}

	private static final String MNEMONIC_EXTENDED    = "mnemonic_extended";
    private static final String APD_DOT_RAW = "apd_dot_raw", APD_DOT_HP="apd_dot_hp", APD_DOT_SHIELD="apd_dot_shield";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (mnemonicExtended) bundle.put(MNEMONIC_EXTENDED, mnemonicExtended);
        if (apdDotRaw>0)bundle.put(APD_DOT_RAW,apdDotRaw);
        if (apdDotHp>0)bundle.put(APD_DOT_HP,apdDotHp);
        if (apdDotShield>0)bundle.put(APD_DOT_SHIELD,apdDotShield);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(MNEMONIC_EXTENDED)) {
			mnemonicExtended = bundle.getBoolean(MNEMONIC_EXTENDED);
		}
	}

	//creates a fresh instance of the buff and attaches that, this allows duplication.
	public static<T extends Buff> T append( Char target, Class<T> buffClass ) {
		T buff = Reflection.newInstance(buffClass);
		buff.attachTo( target );
		return buff;
	}

	public static<T extends FlavourBuff> T append( Char target, Class<T> buffClass, float duration ) {
		T buff = append( target, buffClass );
		buff.spend( duration * target.resist(buffClass) );
		return buff;
	}

	public static<T extends Buff> T appendAttributed( Char target, Class<T> buffClass, OffensiveDamageOrigin origin ) {
		T buff = Reflection.newInstance(buffClass);
		((Buff)buff).apdPrepareAttach(origin);
		buff.attachTo(target);
		return buff;
	}

	public static<T extends Buff> T appendAttributed( Char target, Class<T> buffClass, TimedDamageOrigins origins ) {
		T buff = Reflection.newInstance(buffClass);
		((Buff)buff).apdPrepareAttach(origins);
		buff.attachTo(target);
		return buff;
	}

	//same as append, but prevents duplication.
	public static<T extends Buff> T affect( Char target, Class<T> buffClass ) {
		T buff = target.buff( buffClass );
		if (buff != null) {
			return buff;
		} else {
			return append( target, buffClass );
		}
	}
	
	public static<T extends FlavourBuff> T affect( Char target, Class<T> buffClass, float duration ) {
		T buff = affect( target, buffClass );
		buff.spend( duration * target.resist(buffClass) );
		return buff;
	}

	public static<T extends Buff> T affectAttributed( Char target, Class<T> buffClass, OffensiveDamageOrigin origin ) {
		T buff = target.buff(buffClass);
		return buff != null ? buff : appendAttributed(target, buffClass, origin);
	}

	public static<T extends Buff> T affectAttributed( Char target, Class<T> buffClass, TimedDamageOrigins origins ) {
		T buff = target.buff(buffClass);
		return buff != null ? buff : appendAttributed(target, buffClass, origins);
	}

	public static<T extends FlavourBuff> T affectAttributed( Char target, Class<T> buffClass, float duration, OffensiveDamageOrigin origin ) {
		T buff = affectAttributed(target, buffClass, origin);
		buff.spend(duration * target.resist(buffClass));
		return buff;
	}

	public static<T extends FlavourBuff> T affectAttributed( Char target, Class<T> buffClass, float duration, TimedDamageOrigins origins ) {
		T buff = affectAttributed(target, buffClass, origins);
		buff.spend(duration * target.resist(buffClass));
		return buff;
	}

	//postpones an already active buff, or creates & attaches a new buff and delays that.
	public static<T extends FlavourBuff> T prolong( Char target, Class<T> buffClass, float duration ) {
		T buff = affect( target, buffClass );
		buff.postpone( duration * target.resist(buffClass) );
		return buff;
	}

	public static<T extends FlavourBuff> T prolongAttributed( Char target, Class<T> buffClass, float duration, OffensiveDamageOrigin origin ) {
		T buff = affectAttributed(target, buffClass, origin);
		buff.postpone(duration * target.resist(buffClass));
		return buff;
	}

	public static<T extends CounterBuff> T count( Char target, Class<T> buffclass, float count ) {
		T buff = affect( target, buffclass );
		buff.countUp( count );
		return buff;
	}
	
	public static void detach( Char target, Class<? extends Buff> cl ) {
		for ( Buff b : target.buffs( cl )){
			b.detach();
		}
	}

	//Buffs which declare their total incoming damage over time (even an average)
	// This is used in health bar visuals to show total incoming damage
	public interface DOTbuff {
		int totalIncomingDMG();
	}
}
