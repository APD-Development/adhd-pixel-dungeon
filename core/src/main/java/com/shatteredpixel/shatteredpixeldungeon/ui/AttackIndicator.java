/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndKeyBindings;
import com.watabou.input.GameAction;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//FIXME needs a refactor, lots of weird thread interaction here.
public class AttackIndicator extends Tag {
	
	private static final float ENABLED	= 1.0f;
	private static final float DISABLED	= 0.3f;

	
	private static AttackIndicator instance;
	
	private CharSprite sprite = null;
	private com.watabou.noosa.Image idleIcon;
	
	private Mob lastTarget;
	private ArrayList<Mob> candidates = new ArrayList<>();
	
	public AttackIndicator() {
		super( DangerIndicator.COLOR );

		synchronized (this) {
			instance = this;
			lastTarget = null;

			setSize(SIZE, SIZE);
			visible(false);
			enable(false);
		}
	}
	
	@Override
	public GameAction keyAction() {
		return SPDAction.TAG_ATTACK;
	}
	
	@Override
	protected void createChildren() {
		super.createChildren();
		idleIcon = Icons.get(Icons.TARGET);
		add(idleIcon);
	}
	
	@Override
	protected synchronized void layout() {
		super.layout();

		if (idleIcon != null) {
			idleIcon.x = flipped ? x + width - (SIZE + idleIcon.width())/2f - 1 : x + (SIZE - idleIcon.width())/2f + 1;
			idleIcon.y = y + (height-idleIcon.height())/2f;
			PixelScene.align(idleIcon);
		}

		if (sprite != null) {
			if (!flipped)   sprite.x = x + (SIZE - sprite.width()) / 2f + 1;
			else            sprite.x = x + width - (SIZE + sprite.width()) / 2f - 1;
			sprite.y = y + (height - sprite.height()) / 2f;
			PixelScene.align(sprite);
		}
	}
	
	@Override
	public synchronized void update() {
		super.update();
		boolean alive = Dungeon.hero != null && Dungeon.hero.isAlive();
		bg.visible = alive;
		active = true; // The slot remains present even with no eligible target.
		boolean targetPresent = lastTarget != null && lastTarget.isAlive();
		if (idleIcon != null) { idleIcon.visible = alive && !targetPresent; idleIcon.alpha(DISABLED); }
		if (sprite != null) sprite.visible = alive && targetPresent;
		enable(alive && targetPresent && Dungeon.hero.ready);
	}

	private synchronized void checkEnemies() {

		candidates.clear();
		int v = Dungeon.hero.visibleEnemies();
		for (int i=0; i < v; i++) {
			Mob mob = Dungeon.hero.visibleEnemy( i );
			if ( Dungeon.hero.canAttack( mob) ) {
				candidates.add( mob );
			}
		}
		
		// Keep a manual attack target unless it is the invulnerable DM-300.
		// The candidates already passed normal visibility and melee-reach checks.
		Mob activePylon = com.shatteredpixel.shatteredpixeldungeon.apd.qol.BossPylonTargeting.activeAttackablePylon(candidates);
		if (lastTarget == null || !candidates.contains( lastTarget )
				|| com.shatteredpixel.shatteredpixeldungeon.apd.qol.BossPylonTargeting.replaceInvulnerableBoss(lastTarget, activePylon)) {
			if (candidates.isEmpty()) {
				lastTarget = null;
			} else {
				active = true;
				lastTarget = activePylon != null ? activePylon : Random.element( candidates );
				updateImage();
				flash();
			}
		} else {
			active = true;
			if (!bg.visible) {
				flash();
			}
		}
		
		visible( lastTarget != null );
		enable( lastTarget != null && Dungeon.hero.ready );
	}
	
	private synchronized void updateImage() {
		
		if (sprite != null) {
			sprite.killAndErase();
			sprite = null;
		}
		
		sprite = Reflection.newInstance(lastTarget.spriteClass);
		active = true;
		sprite.linkVisuals(lastTarget);
		sprite.idle();
		sprite.paused = true;
		sprite.visible = bg.visible;

		if (sprite.width() > 20 || sprite.height() > 20){
			sprite.scale.set(sprite.scale.x * PixelScene.align(20f/Math.max(sprite.width(), sprite.height())));
		}

		add( sprite );

		layout();
	}
	
	private boolean enabled = true;
	private synchronized void enable( boolean value ) {
		enabled = value;
		if (sprite != null) {
			sprite.alpha( value ? ENABLED : DISABLED );
		}
	}
	
	private synchronized void visible( boolean value ) {
		bg.visible = Dungeon.hero != null && Dungeon.hero.isAlive();
	}
	
    public static boolean canActivate(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero, Mob target) {
        return hero != null && hero.isAlive() && hero.ready && target != null && target.isAlive() && hero.canAttack(target);
    }

	@Override
	protected void onClick() {
		super.onClick();
		if (enabled && canActivate(Dungeon.hero, lastTarget)) {
			if (Dungeon.hero.handle( lastTarget.pos )) {
				Dungeon.hero.next();
			}
		}
	}

	@Override
	protected String hoverText() {
		return Messages.titleCase(Messages.get(WndKeyBindings.class, "tag_attack"));
	}

	public static void target(Char target ) {
		if (target == null) return;
		synchronized (instance) {
			instance.lastTarget = (Mob) target;
			instance.updateImage();

			QuickSlotButton.target(target);
		}
	}
	
	public static void updateState() {
		instance.checkEnemies();
	}
}
