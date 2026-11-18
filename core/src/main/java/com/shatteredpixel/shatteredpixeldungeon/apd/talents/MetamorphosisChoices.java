package com.shatteredpixel.shatteredpixeldungeon.apd.talents;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import java.util.*;

/** Same-position alternatives; already-owned talents retain SPD's duplicate protection. */
public final class MetamorphosisChoices {
 private MetamorphosisChoices(){}
 public static final class Choice {
  public final HeroClass heroClass;
  public final Talent talent;
  public final int transferredPoints;
  public final boolean alreadyOwned;
  Choice(HeroClass heroClass,Talent talent,int points,boolean alreadyOwned){this.heroClass=heroClass;this.talent=talent;this.transferredPoints=points;this.alreadyOwned=alreadyOwned;}
 }
 public static List<Choice> atPosition(HeroClass playing,int tier,LinkedHashMap<Talent,Integer> current,Talent replacing) {
  if(playing==null || tier<1 || tier>3 || current==null || !current.containsKey(replacing))throw new IllegalArgumentException("invalid class talent position");
  int position=0;for(Talent talent:current.keySet()){if(talent==replacing)break;position++;}
  List<Choice> choices=new ArrayList<>();
  for(HeroClass cls:HeroClass.values()) {
   if(cls==playing)continue;
   ArrayList<LinkedHashMap<Talent,Integer>> base=new ArrayList<>();Talent.initClassTalents(cls,base);
   List<Talent> row=new ArrayList<>(base.get(tier-1).keySet());
   if(position>=row.size())throw new IllegalArgumentException("position is not a class talent");
   Talent candidate=row.get(position);choices.add(new Choice(cls,candidate,current.get(replacing),current.containsKey(candidate)));
  }
  return Collections.unmodifiableList(choices);
 }
 /** Shattered's original Scroll of Metamorphosis: one random talent from each other
  * class's same tier, skipping any class whose row holds the talent being replaced or
  * talents the hero already has. Points carry over unchanged. */
 public static List<Choice> original(HeroClass playing,int tier,LinkedHashMap<Talent,Integer> current,Talent replacing) {
  if(playing==null || tier<1 || tier>3 || current==null || !current.containsKey(replacing))throw new IllegalArgumentException("invalid class talent position");
  List<Choice> choices=new ArrayList<>();
  for(HeroClass cls:HeroClass.values()) {
   ArrayList<LinkedHashMap<Talent,Integer>> base=new ArrayList<>();Talent.initClassTalents(cls,base);
   ArrayList<Talent> row=new ArrayList<>(base.get(tier-1).keySet());
   if(row.contains(replacing))continue;
   row.removeAll(current.keySet());
   if(row.isEmpty())continue;
   choices.add(new Choice(cls,com.watabou.utils.Random.element(row),current.get(replacing),false));
  }
  return Collections.unmodifiableList(choices);
 }
 /** Greater Metamorphosis: every talent of every other class in the same tier.
  * Talents already in the hero's row are listed but marked owned (cannot be picked). */
 public static List<Choice> anyInTier(HeroClass playing,int tier,LinkedHashMap<Talent,Integer> current,Talent replacing) {
  if(playing==null || tier<1 || tier>3 || current==null || !current.containsKey(replacing))throw new IllegalArgumentException("invalid class talent position");
  List<Choice> choices=new ArrayList<>();
  for(HeroClass cls:HeroClass.values()) {
   if(cls==playing)continue;
   ArrayList<LinkedHashMap<Talent,Integer>> base=new ArrayList<>();Talent.initClassTalents(cls,base);
   for(Talent t:base.get(tier-1).keySet())if(t!=replacing)choices.add(new Choice(cls,t,current.get(replacing),current.containsKey(t)));
  }
  return Collections.unmodifiableList(choices);
 }
 public static boolean apply(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero,int tier,Talent replacing,Talent target) {
  if(hero==null||tier<1||tier>3||tier>hero.talents.size())return false;
  LinkedHashMap<Talent,Integer> row=hero.talents.get(tier-1);
  if(!row.containsKey(replacing))return false;
  Set<Talent> allowed=new HashSet<>();
  for(Choice choice:atPosition(hero.heroClass,tier,row,replacing))if(!choice.alreadyOwned)allowed.add(choice.talent);
  return applyFrom(hero,tier,replacing,target,allowed);
 }
 /** Applies a replacement the caller has already offered. */
 public static boolean applyFrom(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero,int tier,Talent replacing,Talent target,Collection<Talent> allowed) {
  if(hero==null||tier<1||tier>3||tier>hero.talents.size()||allowed==null||!allowed.contains(target))return false;
  LinkedHashMap<Talent,Integer> row=hero.talents.get(tier-1);
  if(!row.containsKey(replacing)||row.containsKey(target))return false;
  LinkedHashMap<Talent,Integer> replacement=new LinkedHashMap<>();
  for(Talent talent:row.keySet())replacement.put(talent==replacing?target:talent,row.get(talent));
  // Keep SPD's original alias simplification and the same point count/row order.
  if(!hero.metamorphedTalents.containsValue(replacing))hero.metamorphedTalents.put(replacing,target);
  else if(hero.metamorphedTalents.get(target)==replacing)hero.metamorphedTalents.remove(target);
  else for(Talent original:hero.metamorphedTalents.keySet())
   if(hero.metamorphedTalents.get(original)==replacing)hero.metamorphedTalents.put(original,target);
  hero.talents.set(tier-1,replacement);
  return true;
 }
}
