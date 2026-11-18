package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;

public class OffensiveDamageAttributionTest {
    private Hero previousHero;
    @Before public void init(){HeadlessTestSupport.start();previousHero=Dungeon.hero;Dungeon.hero=new Hero();}
    @After public void clear(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=previousHero;}
    private void rules(int player,int enemy,int boss) {
        Bundle b=new Bundle(),r=new Bundle();
        CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,player)
            .with(CustomDungeonRules.Slider.ENEMY_DAMAGE,enemy).with(CustomDungeonRules.Slider.BOSS_DAMAGE,boss).store(r);
        b.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(b);
    }
    private static class Receiver extends Char {
        int received; Object originalSource;
        @Override public void damage(int amount,Object source){received=amount;originalSource=source;}
    }
    private static class EngineReceiver extends Char {
        EngineReceiver(){HP=HT=1000;alignment=Alignment.ENEMY;sprite=new QuietSprite();}
        void immune(Class source){immunities.add(source);}
        void resistant(Class source){resistances.add(source);}
    }
    private static class QuietSprite extends com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite {
        @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
    }
    @Test public void actualDamageEngineRetainsSourceImmunityResistanceAndBarrier() {
        rules(200,300,500);Poison source=new Poison();
        EngineReceiver shielded=new EngineReceiver();
        com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(shielded,
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class).setShield(15);
        OffensiveDamage.raw(20,source,OffensiveDamageOrigin.PLAYER).apply(shielded);
        assertEquals(975,shielded.HP);assertEquals(0,shielded.shielding());
        EngineReceiver immune=new EngineReceiver();immune.immune(Poison.class);
        OffensiveDamage.raw(20,source,OffensiveDamageOrigin.BOSS).apply(immune);
        assertEquals(1000,immune.HP);
        EngineReceiver resistant=new EngineReceiver();resistant.resistant(Poison.class);
        OffensiveDamage.raw(20,source,OffensiveDamageOrigin.PLAYER).apply(resistant);
        assertEquals(980,resistant.HP);
    }
    @Test public void bossAndSummonOwnershipSelectOneFactorAndSurviveSerialization() {
        rules(200,300,500);
        assertEquals(OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.capture(new Hero(),null));
        assertEquals(OffensiveDamageOrigin.BOSS,OffensiveDamageOrigin.capture(new Goo(),null));
        assertEquals(OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.capture(new Rat(),null));
        Rat summon=new Rat();summon.alignment=Char.Alignment.ALLY;
        assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveDamageOrigin.capture(summon,null));
        assertEquals(OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.capture(summon,OffensiveDamageOrigin.PLAYER));
        for(OffensiveDamageOrigin origin:OffensiveDamageOrigin.values()) {
            Bundle b=new Bundle();origin.store(b,"origin");assertEquals(origin,OffensiveDamageOrigin.restore(b,"origin"));
            int expected=origin==OffensiveDamageOrigin.BOSS?50:origin==OffensiveDamageOrigin.ENEMY?30:origin==OffensiveDamageOrigin.PLAYER?20:10;
            assertEquals(expected,OffensiveDamage.raw(10,summon,origin).scaled().amount);
        }
        Bundle bad=new Bundle();bad.put("origin","future-origin");
        assertThrows(IllegalArgumentException.class,()->OffensiveDamageOrigin.restore(bad,"origin"));
        OffensiveDamageOrigin.BOSS.store(bad,"origin");
        OffensiveDamageOrigin.UNATTRIBUTED.store(bad,"origin");
        assertFalse(bad.contains("origin"));
    }
    @Test public void independentlyOwnedEffectsKeepTheirSourcesAndScaleOnlyOnce() {
        rules(200,300,500);Receiver target=new Receiver();
        Object[] sources={new Poison(),new Burning(),new Object()};
        OffensiveDamageOrigin[] origins={OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS};
        for(int i=0;i<sources.length;i++) {
            OffensiveDamage raw=OffensiveDamage.raw(11,sources[i],origins[i]);
            OffensiveDamage scaled=raw.scaled();assertSame(scaled,scaled.scaled());scaled.apply(target);
            assertEquals(new int[]{22,33,55}[i],target.received);assertSame(sources[i],target.originalSource);
            OffensiveDamage.alreadyScaled(target.received,sources[i],origins[i]).apply(target);
            assertEquals(new int[]{22,33,55}[i],target.received);
        }
    }
    @Test public void neutralUnattributedAndSentinelAmountsPreserveValuesAndRng() {
        Object source=new Object();rules(100,100,100);
        Random.pushGenerator(82931);long next;
        try{next=Random.Long();}finally{Random.popGenerator();}
        Random.pushGenerator(82931);
        try {
            for(OffensiveDamageOrigin origin:OffensiveDamageOrigin.values())for(int n:new int[]{-1,0,1,91,Integer.MAX_VALUE})
                assertEquals(n,OffensiveDamage.raw(n,source,origin).scaled().amount);
            assertEquals(next,Random.Long());
        }finally{Random.popGenerator();}
        rules(500,500,500);
        assertEquals(37,OffensiveDamage.raw(37,source,OffensiveDamageOrigin.UNATTRIBUTED).scaled().amount);
        assertEquals(-1,OffensiveDamage.raw(-1,source,OffensiveDamageOrigin.BOSS).scaled().amount);
        assertEquals(Integer.MAX_VALUE,OffensiveDamage.raw(Integer.MAX_VALUE,source,OffensiveDamageOrigin.BOSS).scaled().amount);
    }
}
