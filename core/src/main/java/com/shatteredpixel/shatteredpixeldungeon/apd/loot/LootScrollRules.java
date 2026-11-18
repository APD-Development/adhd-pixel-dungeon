package com.shatteredpixel.shatteredpixeldungeon.apd.loot;
/** Shared generation and routing authority for both gathering scroll variants. */
public final class LootScrollRules {
 private LootScrollRules(){}
 public static boolean canGenerate(int depth){return depth>=1 && depth<=19;}
 /** Main-dungeon shop, independent of spawn eligibility; final casts return to the Imp. */
 public static int destinationDepth(int sourceDepth){
  if(sourceDepth<1 || sourceDepth>26)return -1;
  if(sourceDepth<=5)return 6;
  if(sourceDepth<=10)return 11;
  if(sourceDepth<=15)return 16;
  return 20;
 }
}
