package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.ChaliceDamagePreview;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;
public class CustomDefenseSliderTest {
    private Hero previousHero;
    @Before public void start(){HeadlessTestSupport.start();previousHero=Dungeon.hero;}
    @After public void clear(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=previousHero;}
    private void rules(CustomDungeonRules.Slider slider,int value) {
        Bundle b=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(slider,value).store(r);b.put("apd_custom_rules",r);
        Dungeon.apdRestoreCustomRules(b);
    }
    @Test public void everyDefenseValueScalesTheWholeVirtualRollOnceWithUnchangedRng() {
        for(int p=0;p<=500;p+=25) {
            rules(CustomDungeonRules.Slider.ENEMY_DEFENSE,p);Rat rat=new Rat();
            Random.pushGenerator(28473);int[] raw=new int[1000];long next;
            try{for(int i=0;i<raw.length;i++)raw[i]=rat.drRoll();next=Random.Long();}finally{Random.popGenerator();}
            Random.pushGenerator(28473);
            try{for(int i=0;i<raw.length;i++)assertEquals(Math.round(raw[i]*p/100f),rat.apdDrRoll());assertEquals(next,Random.Long());}
            finally{Random.popGenerator();}
        }
    }
    @Test public void heroArmorAndBarkskinUseTheSameRollScalingWithoutTouchingShields() {
        Hero hero=new Hero();Dungeon.hero=hero;hero.belongings.armor=new LeatherArmor();hero.STR=20;
        com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(hero,
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin.class).set(4,1);
        com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(hero,
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class).setShield(17);
        for(int p=0;p<=500;p+=25) {
            rules(CustomDungeonRules.Slider.PLAYER_DEFENSE,p);
            Random.pushGenerator(343);int raw;long next;
            try{raw=hero.drRoll();next=Random.Long();}finally{Random.popGenerator();}
            Random.pushGenerator(343);
            try{assertEquals(Math.round(raw*p/100f),hero.apdDrRoll());assertEquals(next,Random.Long());}
            finally{Random.popGenerator();}
            assertEquals(17,hero.shielding());
        }
    }
    @Test public void chalicePreviewKeepsFullSupportWithCustomDefenseAndNoPreviewRng() {
        Hero hero=new Hero();hero.belongings.armor=new LeatherArmor();hero.STR=20;hero.HP=hero.HT=1000;Dungeon.hero=hero;
        for(int p:new int[]{0,25,100,500}) {
            rules(CustomDungeonRules.Slider.PLAYER_DEFENSE,p);
            Random.pushGenerator(713);long next;try{next=Random.Long();}finally{Random.popGenerator();}
            Random.pushGenerator(713);
            try{ChaliceDamagePreview.calculate(hero,5);assertEquals(next,Random.Long());}finally{Random.popGenerator();}
            ChaliceDamagePreview preview=ChaliceDamagePreview.calculate(hero,5);
            Random.pushGenerator(1239);
            try{for(int i=0;i<1000;i++) {
                int actual=Math.max(1,Random.NormalIntRange(preview.baseMin,preview.baseMax)-hero.apdDrRoll());
                assertTrue(actual>=preview.damageMin && actual<=preview.damageMax);
            }}finally{Random.popGenerator();}
        }
    }
}
