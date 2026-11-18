package com.shatteredpixel.shatteredpixeldungeon.apd;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.watabou.utils.Random;
public class ArmorStudy {
 public static void main(String[] args)throws Exception{
  HeadlessTestSupport.start();SPDSettings.apdDifficulty(args.length>1?Integer.parseInt(args[1]):3);SPDSettings.apdExoticChance(0);SPDSettings.intro(false);
  int n=Integer.parseInt(args[0]);long start=System.nanoTime();
  for(int region=0;region<5;region++){
   long[] tiers=new long[6];Random.pushGenerator(2026101200L+region);
   try{for(int i=0;i<100000;i++)tiers[Generator.randomArmor(region).tier]++;}finally{Random.popGenerator();}
   System.out.println("REGION "+region+" "+java.util.Arrays.toString(tiers));
  }
  int noHigh=0,noFreeHigh=0,noReachableFreeHigh=0;long[] totals=new long[6];int[] noByDepth=new int[23];
  for(int s=0;s<n;s++){
   Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.seed=202610120000L+s;
   GamesInProgress.selectedClass=HeroClass.WARRIOR;GamesInProgress.randomizedClass=false;Dungeon.init();
   boolean high=false,freeHigh=false,reachableHigh=false;
   for(int d=1;d<=22;d++){
    Dungeon.depth=d;Dungeon.branch=0;Dungeon.level=Dungeon.newLevel();
    boolean[] reach=new boolean[Dungeon.level.length()];java.util.ArrayDeque<Integer> queue=new java.util.ArrayDeque<>();int entrance=Dungeon.level.entrance();reach[entrance]=true;queue.add(entrance);
    while(!queue.isEmpty()){int cell=queue.remove();for(int off:com.watabou.utils.PathFinder.NEIGHBOURS8){int next=cell+off;if(next>=0&&next<reach.length&&!reach[next]&&(Dungeon.level.passable[next]||Dungeon.level.avoid[next])){reach[next]=true;queue.add(next);}}}
    for(Heap h:Dungeon.level.heaps.valueList())for(Item item:h.items)if(item instanceof Armor){int tier=((Armor)item).tier;totals[tier]++;if(tier>=3){high=true;if(h.type!=Heap.Type.FOR_SALE){freeHigh=true;if(reach[h.pos]&&h.type!=Heap.Type.LOCKED_CHEST&&h.type!=Heap.Type.CRYSTAL_CHEST)reachableHigh=true;}}}
    if(!high)noByDepth[d]++;
   }
   if(!high){noHigh++;System.out.println("NO_HIGH_SEED "+Dungeon.seed);}if(!freeHigh){noFreeHigh++;System.out.println("NO_FREE_HIGH_SEED "+Dungeon.seed);}if(!reachableHigh)noReachableFreeHigh++;
   if((s+1)%50==0)System.out.println("PROGRESS "+(s+1)+" noHigh="+noHigh);
  }
  System.out.println("RESULT n="+n+" noHigh="+noHigh+" noFreeHigh="+noFreeHigh+" noReachableFreeHigh="+noReachableFreeHigh+" tiers="+java.util.Arrays.toString(totals)+" noByDepth="+java.util.Arrays.toString(noByDepth)+" seconds="+(System.nanoTime()-start)/1e9);
 }
}
