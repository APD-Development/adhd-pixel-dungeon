package com.shatteredpixel.shatteredpixeldungeon.apd;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import org.junit.Test;import static org.junit.Assert.*;
public class ArmorShopTest {
 @Test public void shopsStockProgressingArmorWithoutChangingRates() throws Exception{
  HeadlessTestSupport.start();
  com.shatteredpixel.shatteredpixeldungeon.GamesInProgress.selectedClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR;
  Dungeon.seed=20261012L;Dungeon.init();int original= Dungeon.depth;
  java.lang.reflect.Method method=ShopRoom.class.getDeclaredMethod("generateItems");method.setAccessible(true);
  try{for(int[] pair:new int[][]{{6,2},{11,3},{16,4},{21,5}}){Dungeon.depth=pair[0];int count=0;
   for(Object o:(java.util.List<?>)method.invoke(null))if(o instanceof Armor){assertEquals(pair[1],((Armor)o).tier);count++;}
   assertEquals(1,count);
  }}finally{Dungeon.depth=original;}
 }
}
