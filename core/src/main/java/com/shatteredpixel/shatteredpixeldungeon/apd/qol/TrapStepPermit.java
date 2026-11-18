package com.shatteredpixel.shatteredpixeldungeon.apd.qol;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;

/** Permission belongs to one exact voluntary step, trap, level and action; never serialized. */
public final class TrapStepPermit {
    private Level level; private Trap trap; private HeroAction action; private int from,to;
    public void allow(Level level,Trap trap,HeroAction action,int from,int to){
        // An incomplete confirmation must revoke any older grant, never authorize null identities.
        clear();
        if (level==null || trap==null || action==null) return;
        this.level=level;this.trap=trap;this.action=action;this.from=from;this.to=to;
    }
    public void clear(){level=null;trap=null;action=null;}
    public boolean consume(Level level,Trap trap,HeroAction action,int from,int to){
        boolean allowed=this.level!=null && this.trap!=null && this.action!=null
                && this.level==level && this.trap==trap && this.action==action && this.from==from && this.to==to;
        clear();return allowed;
    }
    public static boolean needsWarning(boolean enabled,Level level,Hero hero,int step){
        if(!enabled || level==null || hero==null || hero.flying || hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class)!=null || step<0 || step>=level.length())return false;
        // Incomplete restored visibility, terrain or trap data cannot establish
        // an observed, identified trap. Avoid indexing partial level metadata.
        if(level.traps==null || level.map==null || step>=level.map.length
                || level.heroFOV==null || step>=level.heroFOV.length)return false;
        Trap trap=level.traps.get(step);
        // A revealed TRAP tile is SPD's identified trap representation. Knowledge in fog is insufficient.
        return trap!=null && trap.visible && trap.active && level.map[step]==Terrain.TRAP
                && level.heroFOV!=null && level.heroFOV[step];
    }
}
