/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import android.os.SystemClock;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.OozeTrap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.noosa.*;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/** Disposable-emulator checks. CI force-stops the app between a and b to prove disk persistence. */
@RunWith(AndroidJUnit4.class) @FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TrapDiscoveryDeviceTest {
    private static void waitFor(Callable<Boolean> condition)throws Exception{long until=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<until){if(condition.call())return;Thread.sleep(100);}fail("Trap-discovery fixture timed out");}
    private static void title()throws Exception{waitFor(()->Game.scene()!=null&&PixelScene.uiCamera!=null);TopScoresDeviceTest.render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static WndAPDSettings settings()throws Exception{AtomicReference<WndAPDSettings> ref=new AtomicReference<>();TopScoresDeviceTest.render(()->{ref.set(new WndAPDSettings(ApdSettingsSection.GAMEPLAY));Game.scene().addToFront(ref.get());});Thread.sleep(500);return ref.get();}
    private static CheckBox checkbox(WndAPDSettings window)throws Exception{APDSettingsPane pane=TopScoresDeviceTest.find(window,APDSettingsPane.class).get(0);assertEquals(ApdSettingsSection.GAMEPLAY,pane.section());return (CheckBox)field(pane,"chkContinueTrapDiscovery");}
    private static void revealControl(WndAPDSettings window,CheckBox box)throws Exception{
        ScrollPane scroll=TopScoresDeviceTest.find(window,ScrollPane.class).get(0);
        TopScoresDeviceTest.render(()->{((APDSettingsPane)box.parent).reveal(box);scroll.scrollTo(0,Math.max(0,box.top()-20));});Thread.sleep(400);
    }
    private static void touchControl(WndAPDSettings window,CheckBox box)throws Exception{
        revealControl(window,box);AtomicReference<Point> point=new AtomicReference<>();
        TopScoresDeviceTest.render(()->point.set(box.camera().cameraToScreen(box.left()+box.width()/2,box.top()+box.height()/2)));
        android.app.Instrumentation in=InstrumentationRegistry.getInstrumentation();long down=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point.get().x,point.get().y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);in.sendPointerSync(event);event.recycle();Thread.sleep(120);}Thread.sleep(500);
    }
    private static final class SeerShotKnife extends ThrowingKnife {
        void land(Hero user,int cell){
            Hero previousUser=curUser;
            // Exercise the real empty-cell handler; omit only the unrelated heap drawing tail.
            spawnedForEffect=true;curUser=user;
            try{super.onThrow(cell);}finally{curUser=previousUser;}
        }
    }
    private static final class SeerShotProbe {
        final Hero hero=Dungeon.hero;
        final Level level=Dungeon.level;
        final int branch=Dungeon.branch,depth=Dungeon.depth,vision=hero.viewDistance,hp=hero.HP;
        final float time=hero.cooldown();
        final HeroClass heroClass=hero.heroClass;
        final ArrayList<LinkedHashMap<Talent,Integer>> talents=hero.talents;
        final LinkedHashMap<Talent,Talent> metamorphs=hero.metamorphedTalents;
        final ArrayList<Mob> mindEnemies=hero.mindVisionEnemies;
        final boolean[] fov=level.heroFOV,visited=level.visited,mapped=level.mapped,heroFov=hero.fieldOfView;
        final PointF cameraScroll=Camera.main.scroll.clone();
        final Field mindFovField;
        final Object mindFov;
        // The earlier trap detour can move the hero off-center; stay inside its cleared 19x19 arena.
        final int target=level.width()*(level.height()/2)+level.width()/2+6;
        final HashSet<Buff> originalBuffs=new HashSet<>(hero.buffs());
        RevealedArea reveal;
        Talent.SeerShotCooldown cooldown;
        SeerShotProbe()throws Exception{
            mindFovField=Level.class.getDeclaredField("heroMindFov");mindFovField.setAccessible(true);mindFov=mindFovField.get(null);
        }
        void prepare()throws Exception{
            assertTrue("The retained trap arena has no mobs",level.mobs.isEmpty());
            assertNull(hero.buff(RevealedArea.class));assertNull(hero.buff(Talent.SeerShotCooldown.class));
            hero.heroClass=HeroClass.WARRIOR;hero.metamorphedTalents=new LinkedHashMap<>();
            hero.metamorphedTalents.put(Talent.HOLD_FAST,Talent.SEER_SHOT);
            hero.talents=new ArrayList<>();Talent.initClassTalents(hero);
            assertTrue(hero.talents.get(2).containsKey(Talent.SEER_SHOT));hero.talents.get(2).put(Talent.SEER_SHOT,1);
            hero.viewDistance=0;hero.mindVisionEnemies=new ArrayList<>();Dungeon.branch=1;
            level.heroFOV=fov.clone();level.visited=visited.clone();level.mapped=mapped.clone();hero.fieldOfView=level.heroFOV;
            mindFovField.set(null,new boolean[level.length()]);
            assertNull("The throw target is empty",Actor.findChar(target));
            for(int offset:PathFinder.NEIGHBOURS9){
                int cell=target+offset;assertTrue(cell>=0&&cell<level.length());
                assertEquals("The whole reveal footprint is inside the cleared arena",Terrain.EMPTY,level.map[cell]);
                assertTrue(level.discoverable[cell]);level.visited[cell]=false;level.mapped[cell]=false;
            }
            view(false,"before");
        }
        void view(boolean visible,String stage){
            level.updateFieldOfView(hero,level.heroFOV);
            for(int offset:PathFinder.NEIGHBOURS9)assertEquals(stage+" reveal cell "+(target+offset),visible,level.heroFOV[target+offset]);
            assertEquals("Probe keeps the same depth",depth,Dungeon.depth);
            assertEquals("Probe does not spend hero time",time,hero.cooldown(),0);assertEquals("Probe does not change HP",hp,hero.HP);
            // Keep the same actual map location centered in all three proof frames.
            PointF center=DungeonTilemap.tileCenterToWorld(target);Camera.main.snapTo(center);
            Point screen=Camera.main.cameraToScreen(center.x,center.y);
            assertTrue("Reveal target is visibly inside the viewport",screen.x>0&&screen.x<Game.width&&screen.y>0&&screen.y<Game.height);
            GameScene.updateFog();
            System.out.println("SEER_SHOT_BRANCH_PROBE stage="+stage+" depth="+Dungeon.depth+" branch="+Dungeon.branch+" target="+target+" visible="+visible+" screen="+screen.x+","+screen.y);
        }
        void throwAndReveal(){
            new SeerShotKnife().land(hero,target);reveal=hero.buff(RevealedArea.class);cooldown=hero.buff(Talent.SeerShotCooldown.class);
            assertNotNull(reveal);assertNotNull(cooldown);assertEquals(target,reveal.pos);assertEquals(depth,reveal.depth);assertEquals(1,reveal.branch);
            assertEquals(5f,reveal.cooldown(),0);assertEquals(20f,cooldown.cooldown(),0);
            view(true,"side-branch-revealed");
        }
        void foreignBranch(){
            Dungeon.branch=0;view(false,"foreign-branch-hidden");
            assertSame(reveal,hero.buff(RevealedArea.class));assertSame(cooldown,hero.buff(Talent.SeerShotCooldown.class));
            assertEquals(1,reveal.branch);assertEquals(5f,reveal.cooldown(),0);assertEquals(20f,cooldown.cooldown(),0);
        }
        void restore()throws Exception{
            for(Buff buff:hero.buffs())if(!originalBuffs.contains(buff)&&(buff instanceof RevealedArea||buff instanceof Talent.SeerShotCooldown))buff.detach();
            Dungeon.branch=branch;hero.viewDistance=vision;hero.heroClass=heroClass;hero.talents=talents;hero.metamorphedTalents=metamorphs;hero.mindVisionEnemies=mindEnemies;
            level.heroFOV=fov;level.visited=visited;level.mapped=mapped;hero.fieldOfView=heroFov;mindFovField.set(null,mindFov);
            Camera.main.snapTo(hero.sprite.center());Camera.main.scroll.set(cameraScroll);GameScene.updateFog();
        }
    }
    private static void alternateSeerShotHonorsBranch()throws Exception{
        AtomicReference<SeerShotProbe> ref=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{try{ref.set(new SeerShotProbe());}catch(Exception e){throw new RuntimeException(e);}});
        try{
            TopScoresDeviceTest.render(()->{try{ref.get().prepare();}catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);TopScoresDeviceTest.screenshot("trap-discovery-seer-shot-side-branch-before.png");
            TopScoresDeviceTest.render(()->ref.get().throwAndReveal());
            Thread.sleep(400);TopScoresDeviceTest.screenshot("trap-discovery-seer-shot-side-branch-revealed.png");
            TopScoresDeviceTest.render(()->ref.get().foreignBranch());
            Thread.sleep(400);TopScoresDeviceTest.screenshot("trap-discovery-seer-shot-foreign-branch-hidden.png");
        }finally{TopScoresDeviceTest.render(()->{try{ref.get().restore();}catch(Exception e){throw new RuntimeException(e);}});}
    }
    /** The native scheduler reaches this ordinary-priority mob after the real projectile lands. */
    private static final class LiquidAgilityObserver extends Rat {
        volatile boolean observed;
        int defense,heroHp,mobHp;
        float observedTime,heroDeadline,evasionDeadline;
        boolean evasionPresent;
        @Override protected boolean act(){
            Hero hero=Dungeon.hero;
            observedTime=Actor.now();heroDeadline=hero.cooldown();
            Talent.LiquidAgilEVATracker evasion=hero.buff(Talent.LiquidAgilEVATracker.class);
            evasionPresent=evasion!=null;evasionDeadline=evasion==null?-1f:evasion.cooldown();
            defense=hero.defenseSkill(this);heroHp=hero.HP;mobHp=HP;
            observed=true;
            // Hold this real creature turn for its capture; no attack or replacement scheduler.
            return false;
        }
        void finishTurn(){spend(TICK);next();}
    }
    private static final class LiquidAgilityProbe {
        final Hero hero=Dungeon.hero;
        final Level level=Dungeon.level;
        final int oldPos=hero.pos,oldHp=hero.HP,oldStrength=hero.STR;
        final HeroAction oldAction=hero.curAction,oldLastAction=hero.lastAction;
        final boolean oldResting=hero.resting;
        final ArrayList<LinkedHashMap<Talent,Integer>> oldTalents=hero.talents;
        final LinkedHashMap<Talent,Talent> oldMetamorphs=hero.metamorphedTalents;
        final ArrayList<Mob> oldMindEnemies=hero.mindVisionEnemies;
        final boolean[] oldFov=level.heroFOV,oldVisited=level.visited,oldMapped=level.mapped,oldHeroFov=hero.fieldOfView;
        final PointF oldCameraScroll=Camera.main.scroll.clone();
        final Char oldQuickTarget=QuickSlotButton.lastTarget,oldInventoryTarget=InventoryPane.lastTarget;
        final HashSet<Buff> originalBuffs=new HashSet<>(hero.buffs());
        final Ooze oldOoze=hero.buff(Ooze.class);
        final float oldOozeTime=Actor.now();
        final Field mindFovField;
        final Object oldMindFov;
        final Field userField,itemField;
        final Object oldUser,oldItem;
        int anchor;
        final ArrayList<Item> addedItems=new ArrayList<>();
        LiquidAgilityObserver observer;
        Potion potion;
        float startTime;
        int rank,heroHp,mobHp;
        boolean eligible;
        LiquidAgilityProbe()throws Exception{
            mindFovField=Level.class.getDeclaredField("heroMindFov");mindFovField.setAccessible(true);oldMindFov=mindFovField.get(null);
            userField=Item.class.getDeclaredField("curUser");userField.setAccessible(true);oldUser=userField.get(null);
            itemField=Item.class.getDeclaredField("curItem");itemField.setAccessible(true);oldItem=itemField.get(null);
        }
        void prepare()throws Exception{
            assertTrue("The retained arena has no mobs",level.mobs.isEmpty());
            assertNull(hero.buff(Talent.LiquidAgilEVATracker.class));assertNull(hero.buff(Talent.LiquidAgilACCTracker.class));
            assertNull(hero.buff(Talent.LethalMomentumTracker.class));
            assertNull(hero.buff(Slow.class));assertNull(hero.buff(Chill.class));assertNull(hero.buff(Speed.class));
            assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass.timeFreeze.class));
            assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.TimeBubble.class));
            // The preceding case already proved Ooze. Pause that effect while measuring potion HP neutrality.
            if(oldOoze!=null)oldOoze.detach();
            assertEquals(HeroClass.WARRIOR,hero.heroClass);
            hero.metamorphedTalents=new LinkedHashMap<>();
            hero.metamorphedTalents.put(Talent.LIQUID_WILLPOWER,Talent.LIQUID_AGILITY);
            hero.talents=new ArrayList<>();Talent.initClassTalents(hero);
            assertTrue(hero.talents.get(1).containsKey(Talent.LIQUID_AGILITY));
            hero.mindVisionEnemies=new ArrayList<>();
            level.heroFOV=oldFov.clone();level.visited=oldVisited.clone();level.mapped=oldMapped.clone();hero.fieldOfView=level.heroFOV;
            mindFovField.set(null,new boolean[level.length()]);
            int center=level.width()*(level.height()/2)+level.width()/2;
            anchor=-1;
            for(int attempt=0;attempt<17&&anchor<0;attempt++){
                int row=(attempt+11)%17-8,candidate=center+row*level.width()-2;
                boolean clear=true;
                for(int x=0;x<=4;x++){
                    int cell=candidate+x;
                    clear&=level.map[cell]==Terrain.EMPTY&&level.traps.get(cell)==null
                            &&level.plants.get(cell)==null&&level.heaps.get(cell)==null&&Actor.findChar(cell)==null;
                }
                if(clear)anchor=candidate;
            }
            assertTrue("A five-cell potion path exists inside the cleared arena",anchor>=0);
            hero.pos=anchor;hero.sprite.place(anchor);hero.curAction=null;hero.lastAction=null;hero.resting=false;
            Dungeon.observe();Camera.main.snapTo(DungeonTilemap.tileCenterToWorld(anchor+2));
            // Drain only the earlier fixture's already-spent movement before taking a new action.
            hero.next();
        }
        void begin(int requestedRank,boolean validPotion){
            assertTrue(hero.ready);assertEquals(0f,hero.cooldown(),0f);assertFalse(hero.sprite.isMoving);
            assertNull(hero.buff(Talent.LiquidAgilEVATracker.class));assertNull(hero.buff(Talent.LiquidAgilACCTracker.class));
            rank=requestedRank;eligible=validPotion;hero.talents.get(1).put(Talent.LIQUID_AGILITY,rank);
            observer=new LiquidAgilityObserver();observer.pos=anchor+4;GameScene.add(observer);
            assertSame(observer,Actor.findChar(observer.pos));assertEquals(0f,observer.cooldown(),0f);
            assertEquals("Fresh arena has the ordinary unmodified defense",5,hero.defenseSkill(observer));
            potion=eligible?new PotionOfPurity():new PotionOfStrength();potion.quantity(2);
            hero.belongings.backpack.items.add(potion);addedItems.add(potion);potion.setCurrent(hero);potion.setKnown();
            assertEquals(observer.pos,potion.throwPos(hero,observer.pos));
            heroHp=hero.HP;mobHp=observer.HP;startTime=Actor.now();
            Dungeon.observe();Camera.main.snapTo(DungeonTilemap.tileCenterToWorld(anchor+2));
            assertTrue(level.heroFOV[hero.pos]);assertTrue(level.heroFOV[observer.pos]);
            assertTrue(observer.sprite.visible);
            for(int cell:new int[]{hero.pos,observer.pos}){
                PointF center=DungeonTilemap.tileCenterToWorld(cell);Point screen=Camera.main.cameraToScreen(center.x,center.y);
                assertTrue("Both ends of the actual flight are inside the capture",screen.x>0&&screen.x<Game.width&&screen.y>0&&screen.y<Game.height);
            }
        }
        void cast(){
            assertFalse(observer.observed);assertEquals(2,potion.quantity());
            potion.cast(hero,observer.pos);
            // No callback is invoked by the test. The native MissileSprite must finish its flight.
            assertEquals(2,potion.quantity());assertEquals(0f,hero.cooldown(),0f);assertFalse(hero.ready);
        }
        void verifyImpact(){
            assertTrue(observer.observed);assertEquals(startTime,observer.observedTime,0f);
            assertEquals("Exactly one ordinary throw turn was paid",1f,observer.heroDeadline,0f);
            assertEquals(1,potion.quantity());assertTrue(hero.belongings.backpack.items.contains(potion));
            assertEquals(heroHp,observer.heroHp);assertEquals(mobHp,observer.mobHp);assertEquals(oldStrength,hero.STR);
            assertEquals(eligible,observer.evasionPresent);
            assertEquals(eligible?1f:-1f,observer.evasionDeadline,0f);
            assertEquals(eligible?(rank==1?15:Char.INFINITE_EVASION):5,observer.defense);
            Talent.LiquidAgilACCTracker accuracy=hero.buff(Talent.LiquidAgilACCTracker.class);
            if(eligible){assertNotNull(accuracy);assertEquals(1,accuracy.uses);assertEquals(5f,accuracy.cooldown(),0f);}
            else assertNull(accuracy);
            assertTrue(potion.isKnown());assertFalse(hero.ready);
            System.out.println("LIQUID_AGILITY_NATIVE rank="+rank+" potion="+potion.getClass().getSimpleName()+" occupied=true start="+startTime+" enemy_time="+observer.observedTime+" paid="+observer.heroDeadline+" evasion="+observer.evasionDeadline+" defense="+observer.defense+" quantity="+potion.quantity()+" hero_hp="+observer.heroHp+" target_hp="+observer.mobHp);
        }
        void finishTurn(){observer.finishTurn();}
        void verifyBoundary(){
            assertTrue(hero.ready);assertEquals(startTime+1f,Actor.now(),0f);assertEquals(0f,hero.cooldown(),0f);
            assertNull("Evasion expires at the next ordinary hero turn",hero.buff(Talent.LiquidAgilEVATracker.class));
            assertEquals(5,hero.defenseSkill(observer));
            if(eligible)assertNotNull("The separate attack bonus remains available",hero.buff(Talent.LiquidAgilACCTracker.class));
            clearCase();
        }
        void clearCase(){
            if(observer!=null){Actor.remove(observer);level.mobs.remove(observer);if(observer.sprite!=null)observer.sprite.killAndErase();observer=null;}
            for(Buff buff:hero.buffs())if(!originalBuffs.contains(buff)
                    &&(buff instanceof Talent.LiquidAgilEVATracker||buff instanceof Talent.LiquidAgilACCTracker))buff.detach();
            for(Item added:addedItems)hero.belongings.backpack.items.remove(added);
            addedItems.clear();potion=null;Item.updateQuickslot();
        }
        void restore()throws Exception{
            clearCase();hero.talents=oldTalents;hero.metamorphedTalents=oldMetamorphs;hero.mindVisionEnemies=oldMindEnemies;
            hero.pos=oldPos;hero.sprite.place(oldPos);hero.HP=oldHp;hero.STR=oldStrength;
            hero.curAction=oldAction;hero.lastAction=oldLastAction;hero.resting=oldResting;
            if(oldOoze!=null&&hero.buff(Ooze.class)==null){oldOoze.fixTime(oldOozeTime);assertTrue(oldOoze.attachTo(hero));}
            level.heroFOV=oldFov;level.visited=oldVisited;level.mapped=oldMapped;hero.fieldOfView=oldHeroFov;mindFovField.set(null,oldMindFov);
            userField.set(null,oldUser);itemField.set(null,oldItem);
            hero.checkVisibleMobs();QuickSlotButton.lastTarget=oldQuickTarget;InventoryPane.lastTarget=oldInventoryTarget;
            Camera.main.snapTo(hero.sprite.center());Camera.main.scroll.set(oldCameraScroll);GameScene.updateFog();Item.updateQuickslot();
        }
    }
    private static void liquidAgilitySurvivesActualCreatureThrow()throws Exception{
        AtomicReference<LiquidAgilityProbe> ref=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{try{ref.set(new LiquidAgilityProbe());}catch(Exception e){throw new RuntimeException(e);}});
        try{
            TopScoresDeviceTest.render(()->{try{ref.get().prepare();}catch(Exception e){throw new RuntimeException(e);}});
            waitFor(()->Dungeon.hero.ready&&Dungeon.hero.cooldown()==0f&&!Dungeon.hero.sprite.isMoving);
            for(int scenario=0;scenario<3;scenario++){
                final int rank=scenario==0?1:2;final boolean eligible=scenario<2;
                final String name=eligible?"rank"+rank:"excluded-strength";
                TopScoresDeviceTest.render(()->ref.get().begin(rank,eligible));
                Thread.sleep(200);TopScoresDeviceTest.screenshot("trap-discovery-liquid-agility-"+name+"-before.png");
                TopScoresDeviceTest.render(()->ref.get().cast());
                waitFor(()->ref.get().observer.observed);
                TopScoresDeviceTest.render(()->ref.get().verifyImpact());
                TopScoresDeviceTest.screenshot("trap-discovery-liquid-agility-"+name+"-after.png");
                TopScoresDeviceTest.render(()->ref.get().finishTurn());
                waitFor(()->Dungeon.hero.ready&&Dungeon.hero.cooldown()==0f);
                TopScoresDeviceTest.render(()->ref.get().verifyBoundary());
            }
        }finally{TopScoresDeviceTest.render(()->{try{ref.get().restore();}catch(Exception e){throw new RuntimeException(e);}});}
    }
    private static void captureActualOoze()throws Exception{
        Field activeFade=Class.forName(PixelScene.class.getName()+"$Fader").getDeclaredField("INSTANCE");
        activeFade.setAccessible(true);AtomicReference<Boolean> settled=new AtomicReference<>(false);
        waitFor(()->{
            TopScoresDeviceTest.render(()->{try{
                settled.set(activeFade.get(null)==null&&!Dungeon.hero.sprite.isMoving);
            }catch(IllegalAccessException e){throw new RuntimeException(e);}});
            return settled.get();
        });
        float[] time=new float[1];int[] hp=new int[1];
        TopScoresDeviceTest.render(()->{
            Hero hero=Dungeon.hero;assertNotNull("The actual trap effect remains present for its capture",hero.buff(Ooze.class));
            time[0]=hero.cooldown();hp[0]=hero.HP;
            // Reframe the disposable arena after its real movement and normal scene fade have finished.
            Camera.main.snapTo(hero.sprite.center());GameScene.updateFog();
        });
        Thread.sleep(200);
        TopScoresDeviceTest.render(()->{
            Hero hero=Dungeon.hero;assertNotNull(hero.buff(Ooze.class));assertTrue(hero.sprite.visible);
            assertEquals("Framing spends no game time",time[0],hero.cooldown(),0f);assertEquals(hp[0],hero.HP);
            PointF center=hero.sprite.center();Point screen=Camera.main.cameraToScreen(center.x,center.y);
            assertTrue("The affected hero is inside the actual capture",screen.x>0&&screen.x<Game.width&&screen.y>0&&screen.y<Game.height);
            System.out.println("OOZE_CAPTURE hero="+hero.pos+" screen="+screen.x+","+screen.y+" hp="+hero.HP+" effect=true normal_fade_finished=true");
        });
        TopScoresDeviceTest.screenshot("trap-discovery-runtime-actual-ooze.png");
    }
    @Test public void a_controlWritesEnabledPreference()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){title();assertFalse("Fresh disposable emulator keeps the original default",SPDSettings.continueOnTrapDiscovery());WndAPDSettings window=settings();CheckBox box=checkbox(window);assertFalse(box.checked());touchControl(window,box);assertTrue(box.checked());assertTrue(SPDSettings.continueOnTrapDiscovery());System.out.println("TRAP_PERSIST_SEED_PID="+android.os.Process.myPid());TopScoresDeviceTest.screenshot("trap-discovery-setting-on.png");TopScoresDeviceTest.render(window::hide);}
        // Intentionally leaves On for b, which CI runs after an actual force-stop/new process.
    }
    @Test public void b_freshProcessReadsEnabledAndCanDisable()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){title();assertTrue("Enabled state must come from the previous process's saved preferences",SPDSettings.continueOnTrapDiscovery());WndAPDSettings window=settings();CheckBox box=checkbox(window);assertTrue(box.checked());System.out.println("TRAP_PERSIST_VERIFY_PID="+android.os.Process.myPid());revealControl(window,box);TopScoresDeviceTest.screenshot("trap-discovery-setting-restarted.png");touchControl(window,box);assertFalse(box.checked());assertFalse(SPDSettings.continueOnTrapDiscovery());TopScoresDeviceTest.render(()->{window.hide();GameSettings.set(null);});assertFalse("Fresh Android preference wrapper reads the saved Off value",SPDSettings.continueOnTrapDiscovery());WndAPDSettings reopened=settings();CheckBox offBox=checkbox(reopened);assertFalse(offBox.checked());revealControl(reopened,offBox);TopScoresDeviceTest.screenshot("trap-discovery-setting-off.png");TopScoresDeviceTest.render(reopened::hide);}
    }
    @Test public void c_runtimeDiscoveryRouteAndActualTrapEffect()throws Exception{
        final boolean oldContinue=SPDSettings.continueOnTrapDiscovery(),oldConfirm=SPDSettings.confirmKnownTraps(),oldIntro=SPDSettings.intro();final int oldSlot=GamesInProgress.curSlot;final HeroClass oldClass=GamesInProgress.selectedClass;final int[] created={-1};
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            title();TopScoresDeviceTest.render(()->{SPDSettings.intro(false);created[0]=GamesInProgress.firstEmpty(HeroClass.WARRIOR);assertTrue(created[0]>0);GamesInProgress.curSlot=created[0];GamesInProgress.selectedClass=HeroClass.WARRIOR;Dungeon.hero=null;Dungeon.daily= false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.initSeed();InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);});
            waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);
            TopScoresDeviceTest.render(()->{try{
                Hero hero=Dungeon.hero;Level level=Dungeon.level;int width=level.width(),anchor=width*(level.height()/2)+width/2;
                for(Mob mob:new ArrayList<>(level.mobs)){Actor.remove(mob);if(mob.sprite!=null)mob.sprite.killAndErase();}level.mobs.clear();
                for(int y=-9;y<=9;y++)for(int x=-9;x<=9;x++){int cell=anchor+y*width+x;assertTrue(cell>=0&&cell<level.length());Level.set(cell,Terrain.EMPTY);level.visited[cell]=true;level.heroFOV[cell]=true;level.discoverable[cell]=true;}
                hero.pos=anchor;hero.sprite.place(anchor);hero.fieldOfView=level.heroFOV;hero.curAction=null;Buff.affect(hero,Foresight.class);SPDSettings.confirmKnownTraps(false);
                Method closer=Hero.class.getDeclaredMethod("getCloser",int.class);closer.setAccessible(true);Field path=Char.class.getDeclaredField("path");path.setAccessible(true);
                for(boolean enabled:new boolean[]{false,true}){
                    hero.pos=anchor;hero.sprite.place(anchor);hero.ready=true;SPDSettings.continueOnTrapDiscovery(enabled);OozeTrap trap=new OozeTrap();trap.set(anchor+width+2);level.traps.put(trap.pos,trap);Level.set(trap.pos,Terrain.SECRET_TRAP);HeroAction.Move action=new HeroAction.Move(anchor+6,HeroAction.Move.Mode.TRAVEL);hero.curAction=action;hero.lastAction=null;path.set(hero,null);
                    assertTrue((Boolean)closer.invoke(hero,action.dst));assertEquals(anchor+1,hero.pos);assertTrue(trap.visible);assertTrue(trap.active);assertNull(hero.buff(Ooze.class));if(enabled)assertSame(action,hero.curAction);else assertNull(hero.curAction);hero.curAction=null;hero.lastAction=null;
                }
                hero.pos=anchor;hero.sprite.place(anchor);OozeTrap routeTrap=new OozeTrap();routeTrap.set(anchor+2);level.traps.put(routeTrap.pos,routeTrap);Level.set(routeTrap.pos,Terrain.SECRET_TRAP);HeroAction.Move walk=new HeroAction.Move(anchor+6,HeroAction.Move.Mode.TRAVEL);hero.curAction=walk;path.set(hero,null);assertTrue((Boolean)closer.invoke(hero,walk.dst));assertSame(walk,hero.curAction);assertTrue(routeTrap.visible);assertFalse(level.passable[routeTrap.pos]);assertTrue((Boolean)closer.invoke(hero,walk.dst));assertNotEquals(routeTrap.pos,hero.pos);assertTrue(routeTrap.active);assertSame(walk,hero.curAction);
                OozeTrap stepped=new OozeTrap();stepped.set(hero.pos+1);level.traps.put(stepped.pos,stepped);Level.set(stepped.pos,Terrain.SECRET_TRAP);level.discover(stepped.pos);hero.curAction=new HeroAction.Move(stepped.pos);path.set(hero,null);assertTrue((Boolean)closer.invoke(hero,stepped.pos));assertEquals(stepped.pos,hero.pos);assertNull(hero.curAction);assertFalse(stepped.active);assertNotNull("The actual Ooze trap must apply its actual effect",hero.buff(Ooze.class));hero.lastAction=null;GameScene.updateMap();
            }catch(Exception error){throw new RuntimeException(error);}});
            captureActualOoze();
            alternateSeerShotHonorsBranch();
            liquidAgilitySurvivesActualCreatureThrow();
        }finally{SPDSettings.continueOnTrapDiscovery(oldContinue);SPDSettings.confirmKnownTraps(oldConfirm);SPDSettings.intro(oldIntro);if(created[0]>0)Dungeon.deleteGame(created[0],true);GamesInProgress.curSlot=oldSlot;GamesInProgress.selectedClass=oldClass;}
    }
}
