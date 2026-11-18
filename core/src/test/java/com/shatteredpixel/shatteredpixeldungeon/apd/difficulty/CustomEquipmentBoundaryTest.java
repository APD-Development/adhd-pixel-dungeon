package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class CustomEquipmentBoundaryTest {
    @Before public void start(){HeadlessTestSupport.start();}
    @After public void clear(){Dungeon.apdRestoreCustomRules(new Bundle());}
    private void rules(int percent){Bundle b=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.EQUIPMENT,percent).store(r);b.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(b);}
    @Test public void neutralPreservesOriginalObjectsAndRngWithoutCallingExtraFactory(){
        rules(100);
        for(Item original:new Item[]{new Dagger(),new LeatherArmor(),new RingOfAccuracy(),new ChaliceOfBlood()}){
            Random.pushGenerator(837L);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
            Random.pushGenerator(837L);try{
                assertSame(original,CustomEquipmentHooks.adjust(original,()->{fail("Neutral must not generate extra loot");return null;}).get(0));
                assertEquals(expected,Random.Long());
            }finally{Random.popGenerator();}
        }
    }
    @Test public void allEightSettingsChangeEquipmentCountsAndMaximumUsesFreshItems(){
        Item original=new Dagger();
        for(int percent=25;percent<=200;percent+=25){
            rules(percent);int total=0;Random.pushGenerator(123L);
            try{for(int i=0;i<10000;i++){
                java.util.List<Item> items=CustomEquipmentHooks.adjust(original,()->new Dagger());total+=items.size();
                for(Item item:items)if(item!=original)assertEquals(Dagger.class,item.getClass());
            }}finally{Random.popGenerator();}
            assertTrue(percent+"% total "+total,Math.abs(total-10000*percent/100)<350);
            if(percent==200)assertEquals(20000,total);
        }
    }
    @Test public void nonEquipmentAndProgressionItemsNeverUseTheAvailabilityRoll(){
        rules(25);Random.pushGenerator(901L);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
        Random.pushGenerator(901L);try{
            for(Item item:new Item[]{new Food(),new GoldenKey(3),new PotionOfStrength(),new ScrollOfUpgrade(),new Gold()})
                assertSame(item,CustomEquipmentHooks.adjust(item,()->{fail("Non-equipment cannot generate extra equipment");return null;}).get(0));
            assertEquals(expected,Random.Long());
        }finally{Random.popGenerator();}
    }
}
