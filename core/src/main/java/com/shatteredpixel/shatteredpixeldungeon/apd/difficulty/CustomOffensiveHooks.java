package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
/** Explicit source boundaries: unowned environmental/self costs never enter this API. */
public final class CustomOffensiveHooks {
 private CustomOffensiveHooks(){}
 private static boolean neutral(){
  CustomDungeonRules r=Dungeon.customRules();
  return r==null || r.get(CustomDungeonRules.Slider.PLAYER_DAMAGE)==100
   && r.get(CustomDungeonRules.Slider.ENEMY_DAMAGE)==100 && r.get(CustomDungeonRules.Slider.BOSS_DAMAGE)==100;
 }
 /** Scale offensive attack damage before armor and defense procs, retaining float precision. */
 public static float attackDamage(Char attacker,float amount){
  if(neutral() || amount<=0)return amount;
  int percentage=OffensiveDamageOrigin.capture(attacker,null).percentage();
  if(percentage==100)return amount;
  return (float)Math.min(Integer.MAX_VALUE,(double)amount*percentage/100d);
 }
 /** A separate spell/direct effect; source object is unchanged for defenses and death logic. */
 public static void raw(Char target,int amount,Object originalSource,Char attacker){
  if(neutral()){target.damage(amount,originalSource);return;}
  OffensiveDamage.raw(amount,originalSource,OffensiveDamageOrigin.capture(attacker,null)).apply(target);
 }
}
