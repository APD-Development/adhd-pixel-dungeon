package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;
public class OffensiveSummonOwnershipTest {
 static class Ally extends Char {Ally(){alignment=Alignment.ALLY;HP=HT=10;}}
 @Before public void init(){HeadlessTestSupport.start();}
 @Test public void creatorOwnershipSurvivesMultipleSummonGenerationsAndRealBundleRestoration(){
  Char[] creators={new Hero(),new Goo(),new Rat()};
  OffensiveDamageOrigin[] origins={OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.BOSS,OffensiveDamageOrigin.ENEMY};
  for(int i=0;i<creators.length;i++){
   Ally child=new Ally();child.apdInheritOffensiveOrigin(creators[i]);assertEquals(origins[i],OffensiveDamageOrigin.capture(child,null));
   Bundle b=new Bundle();child.storeInBundle(b);Ally loaded=new Ally();loaded.restoreFromBundle(b);
   assertEquals(origins[i],OffensiveDamageOrigin.capture(loaded,null));
   Ally grandchild=new Ally();grandchild.apdInheritOffensiveOrigin(loaded);assertEquals(origins[i],OffensiveDamageOrigin.capture(grandchild,null));
   assertEquals(10,loaded.HP);assertEquals(10,loaded.HT);
  }
 }
 @Test public void legacyAlliesRemainUnattributedAndUnownedSavesDoNotAcquireNewFields(){
  Ally original=new Ally();Bundle b=new Bundle();original.storeInBundle(b);assertFalse(b.contains("apd_damage_creator_origin"));
  Ally loaded=new Ally();loaded.restoreFromBundle(b);assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveDamageOrigin.capture(loaded,null));
  original.apdInheritOffensiveOrigin(new Hero());original.apdInheritOffensiveOrigin(null);original.storeInBundle(b);
  assertFalse(b.contains("apd_damage_creator_origin"));
 }
 @Test public void attributionIsCapturedBeforeCreatorChangesOrDisappearsAndConsumesNoRng(){
  Random.pushGenerator(17832);long reference;try{reference=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(17832);try{
   Goo boss=new Goo();Ally child=new Ally();child.apdInheritOffensiveOrigin(boss);boss.alignment=Char.Alignment.ALLY;
   assertEquals(OffensiveDamageOrigin.BOSS,OffensiveDamageOrigin.capture(child,null));assertEquals(reference,Random.Long());
  }finally{Random.popGenerator();}
 }
 @Test public void unsupportedOwnershipMetadataRejectsWithoutRewritingTheEvidence(){
  Ally original=new Ally();Bundle b=new Bundle();original.storeInBundle(b);b.put("apd_damage_creator_origin","future-origin");
  assertThrows(IllegalArgumentException.class,()->new Ally().restoreFromBundle(b));
  assertEquals("future-origin",b.getString("apd_damage_creator_origin"));
 }
}
