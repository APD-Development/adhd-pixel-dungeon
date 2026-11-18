package com.shatteredpixel.shatteredpixeldungeon.apd.talents;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.watabou.utils.Random;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public class MetamorphosisChoicesTest {
 @Test public void everyClassTalentPositionOffersFiveMatchingOtherClassPositions(){
  for(HeroClass playing:HeroClass.values()){
   ArrayList<LinkedHashMap<Talent,Integer>> own=new ArrayList<>();Talent.initClassTalents(playing,own);
   for(int tier=1;tier<=3;tier++){
    LinkedHashMap<Talent,Integer> row=own.get(tier-1);List<Talent> talents=new ArrayList<>(row.keySet());
    for(int position=0;position<talents.size();position++){
     Talent replacing=talents.get(position);row.put(replacing,2);
     List<MetamorphosisChoices.Choice> options=MetamorphosisChoices.atPosition(playing,tier,row,replacing);
     assertEquals(5,options.size());Set<HeroClass> classes=new HashSet<>();Set<Talent> targets=new HashSet<>();
     for(MetamorphosisChoices.Choice choice:options){
      assertNotEquals(playing,choice.heroClass);assertTrue(classes.add(choice.heroClass));assertTrue(targets.add(choice.talent));
      ArrayList<LinkedHashMap<Talent,Integer>> other=new ArrayList<>();Talent.initClassTalents(choice.heroClass,other);
      assertEquals(new ArrayList<>(other.get(tier-1).keySet()).get(position),choice.talent);assertEquals(2,choice.transferredPoints);
     }
    }
   }
  }
 }
 @Test public void candidateCalculationDoesNotConsumeGameplayRandomNumbers(){
  Hero h=hero(HeroClass.WARRIOR);
  Talent selected=new ArrayList<>(h.talents.get(1).keySet()).get(2);int expected;
  Random.pushGenerator(7737);try{expected=Random.Int();}finally{Random.popGenerator();}
  Random.pushGenerator(7737);try{List<MetamorphosisChoices.Choice> choices=MetamorphosisChoices.atPosition(HeroClass.WARRIOR,2,h.talents.get(1),selected);assertTrue(MetamorphosisChoices.apply(h,2,selected,choices.get(0).talent));assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
 }
 private static Hero hero(HeroClass cls){Hero h=new Hero();h.heroClass=cls;Talent.initClassTalents(h);return h;}
 @Test public void eachChoiceTransfersPointsAtTheSamePositionAndKeepsUnrelatedTalents(){
  for(HeroClass cls:HeroClass.values())for(int tier=1;tier<=3;tier++) {
   Hero template=hero(cls);List<Talent> initial=new ArrayList<>(template.talents.get(tier-1).keySet());
   for(int pos=0;pos<initial.size();pos++)for(MetamorphosisChoices.Choice choice:MetamorphosisChoices.atPosition(cls,tier,template.talents.get(tier-1),initial.get(pos))) {
    Hero h=hero(cls);Talent old=initial.get(pos);h.talents.get(tier-1).put(old,2);
    assertTrue(MetamorphosisChoices.apply(h,tier,old,choice.talent));
    List<Talent> expected=new ArrayList<>(initial);expected.set(pos,choice.talent);
    assertEquals(expected,new ArrayList<>(h.talents.get(tier-1).keySet()));
    assertEquals(Integer.valueOf(2),h.talents.get(tier-1).get(choice.talent));
    assertEquals(choice.talent,h.metamorphedTalents.get(old));
   }
  }
 }
 @Test public void alreadyOwnedAlternativesStayVisibleButCannotOverwritePoints(){
  Hero h=hero(HeroClass.WARRIOR);LinkedHashMap<Talent,Integer> row=h.talents.get(1);
  List<Talent> original=new ArrayList<>(row.keySet());Talent replacing=original.get(0);
  Talent owned=MetamorphosisChoices.atPosition(h.heroClass,2,row,replacing).get(0).talent;
  LinkedHashMap<Talent,Integer> legacy=new LinkedHashMap<>();
  for(int i=0;i<original.size();i++)legacy.put(i==1?owned:original.get(i),i==1?2:1);
  h.talents.set(1,legacy);LinkedHashMap<Talent,Integer> before=new LinkedHashMap<>(legacy);
  List<MetamorphosisChoices.Choice> choices=MetamorphosisChoices.atPosition(h.heroClass,2,legacy,replacing);
  assertEquals(5,choices.size());assertTrue(choices.get(0).alreadyOwned);
  assertFalse(MetamorphosisChoices.apply(h,2,replacing,owned));assertEquals(before,h.talents.get(1));assertTrue(h.metamorphedTalents.isEmpty());
 }
 @Test public void repeatedMetamorphosisSimplifiesAliasesAndSurvivesExistingSaveFormat(){
  Hero h=hero(HeroClass.WARRIOR);Talent original=new ArrayList<>(h.talents.get(0).keySet()).get(2);
  h.talents.get(0).put(original,2);
  Talent first=MetamorphosisChoices.atPosition(h.heroClass,1,h.talents.get(0),original).get(0).talent;
  assertTrue(MetamorphosisChoices.apply(h,1,original,first));
  Talent second=MetamorphosisChoices.atPosition(h.heroClass,1,h.talents.get(0),first).get(1).talent;
  assertTrue(MetamorphosisChoices.apply(h,1,first,second));assertEquals(1,h.metamorphedTalents.size());assertEquals(second,h.metamorphedTalents.get(original));
  com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();Talent.storeTalentsInBundle(saved,h);
  Hero restored=new Hero();restored.heroClass=h.heroClass;Talent.restoreTalentsFromBundle(saved,restored);
  assertEquals(h.metamorphedTalents,restored.metamorphedTalents);assertEquals(h.talents,restored.talents);
 }
 @Test public void staleOrWrongPositionSelectionCannotMutateTheTalentRow(){
  Hero h=hero(HeroClass.WARRIOR);Talent selected=h.talents.get(0).keySet().iterator().next();
  LinkedHashMap<Talent,Integer> before=new LinkedHashMap<>(h.talents.get(0));
  ArrayList<LinkedHashMap<Talent,Integer>> other=new ArrayList<>();Talent.initClassTalents(HeroClass.MAGE,other);
  Talent wrong=new ArrayList<>(other.get(0).keySet()).get(1);
  assertFalse(MetamorphosisChoices.apply(h,1,selected,wrong));assertFalse(MetamorphosisChoices.apply(h,0,selected,wrong));
  assertEquals(before,h.talents.get(0));assertTrue(h.metamorphedTalents.isEmpty());
 }
 @Test public void anAlreadyModifiedRowWithAllFiveAlternativesOwnedRemainsIntact(){
  Hero h=hero(HeroClass.WARRIOR);Talent original=h.talents.get(1).keySet().iterator().next();
  List<MetamorphosisChoices.Choice> initial=MetamorphosisChoices.atPosition(h.heroClass,2,h.talents.get(1),original);
  LinkedHashMap<Talent,Integer> row=new LinkedHashMap<>();for(MetamorphosisChoices.Choice c:initial)row.put(c.talent,1);
  h.talents.set(1,row);Talent replacing=row.keySet().iterator().next();
  assertEquals(5,MetamorphosisChoices.atPosition(h.heroClass,2,row,replacing).size());
  for(MetamorphosisChoices.Choice c:MetamorphosisChoices.atPosition(h.heroClass,2,row,replacing)) {
   assertTrue(c.alreadyOwned);assertFalse(MetamorphosisChoices.apply(h,2,replacing,c.talent));
  }
  assertEquals(row,h.talents.get(1));assertTrue(h.metamorphedTalents.isEmpty());
 }
}
