package com.shatteredpixel.shatteredpixeldungeon.apd.history;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import org.junit.Test;
import static org.junit.Assert.*;
public class ApdSpriteRefTest {
 @Test public void existingCrystalPotionAndScrollSpritesResolve(){
  assertEquals(Integer.valueOf(ItemSpriteSheet.CRYSTAL_KEY),ApdSpriteRef.itemImage("ITEM_CRYSTAL_KEY"));
  assertEquals(Integer.valueOf(ItemSpriteSheet.POTION_AMBER),ApdSpriteRef.itemImage("ITEM_POTION_AMBER"));
  assertEquals(Integer.valueOf(ItemSpriteSheet.SCROLL_KAUNAN),ApdSpriteRef.itemImage("ITEM_SCROLL_KAUNAN"));
 }
 @Test public void existingIconAndChangeReferencesKeepTheirOriginalResolver(){
  assertNull(ApdSpriteRef.itemImage("ICON_INFO"));
  assertNull(ApdSpriteRef.itemImage("V074_SHPX"));
 }
 @Test public void missingAndMalformedReferencesCannotCrashMenus(){
  assertNull(ApdSpriteRef.itemImage(null));assertNull(ApdSpriteRef.itemImage(""));
  assertNull(ApdSpriteRef.itemImage("ITEM_NOT_A_SPRITE"));
 }
}
