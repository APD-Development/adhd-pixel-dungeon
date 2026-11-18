package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import org.junit.Test;
import static org.junit.Assert.*;
public class DerivedShockScalingTest {
 static class Guaranteed extends Shocking { @Override protected float procChanceMultiplier(Char attacker){return 3f;} }
 static class Quiet extends SpecialBombDamageProvenanceTest.Quiet {
  @Override public Emitter centerEmitter(){return new Emitter();}
  @Override public void flash(){}
 }
 @Test public void realShockSplashDoesNotScaleTheAlreadyScaledAttackTwice() throws Exception {
  SpecialBombDamageProvenanceTest f=new SpecialBombDamageProvenanceTest();f.setup();
  SpecialBombDamageProvenanceTest.Target defender=new SpecialBombDamageProvenanceTest.Target(false);
  SpecialBombDamageProvenanceTest.Target nearby=new SpecialBombDamageProvenanceTest.Target(false);
  try {
   defender.pos=312;nearby.pos=313;defender.sprite=new Quiet();nearby.sprite=new Quiet();
   defender.alignment=nearby.alignment=Char.Alignment.ENEMY;
   Actor.add(defender);Actor.add(nearby);
   Char[] creators={Dungeon.hero,new Rat(),new Goo()};
   for(Char creator:creators){creator.sprite=new Quiet();new Group().add(creator.sprite);}
   Guaranteed shock=new Guaranteed();
   for(int role=0;role<creators.length;role++)for(int p=25;p<=500;p+=25){
    Char creator=creators[role];
    defender.alignment=nearby.alignment=role==0?Char.Alignment.ENEMY:Char.Alignment.ALLY;
    f.rules(role==0?p:275,role==1?p:325,role==2?p:425);nearby.hits.clear();
    int attack=Math.round(CustomOffensiveHooks.attackDamage(creator,40));
    Random.pushGenerator(765);try{assertEquals(attack,shock.proc(new Shortsword(),creator,defender,attack));}finally{Random.popGenerator();}
    assertEquals(1,nearby.hits.size());assertEquals(p/5,(int)nearby.hits.get(0));assertSame(shock,nearby.sources.get(nearby.sources.size()-1));
   }
  }finally{Actor.remove(defender);Actor.remove(nearby);f.cleanup();}
 }
}

