package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

//A character sprite for headless tests: it does nothing, so the game's combat code can run without a screen.
class QuietSprite extends CharSprite {

	QuietSprite( Char owner ){
		super();
		ch = owner;
		owner.sprite = this;
	}

	@Override public void showStatus( int color, String text, Object... args ){}
	@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){}
	@Override public void die(){}
	@Override public void bloodBurstA( PointF from, int damage ){}
	@Override public void flash(){}
	@Override public void burst( int color, int n ){}
	@Override public Emitter emitter(){ return new Emitter(); }
	@Override public Emitter centerEmitter(){ return new Emitter(); }
	@Override public Emitter bottomEmitter(){ return new Emitter(); }
	@Override public void attack( int cell ){}
	@Override public void zap( int cell ){}
	@Override public void idle(){}
	@Override public void showSleep(){}
	@Override public void hideSleep(){}
	@Override public void showAlert(){}
	@Override public void hideAlert(){}
	@Override public void showLost(){}
	@Override public void hideLost(){}
	@Override public void add( State state ){}
	@Override public void remove( State state ){}
	@Override public void resetColor(){}
	@Override public void kill(){}
}
