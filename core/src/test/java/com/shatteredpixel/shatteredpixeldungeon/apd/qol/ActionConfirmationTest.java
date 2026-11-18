package com.shatteredpixel.shatteredpixeldungeon.apd.qol;
import static org.junit.Assert.*;
import org.junit.*;
import java.lang.reflect.*;
import java.util.*;
import com.badlogic.gdx.Preferences;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;

public class ActionConfirmationTest {
    private final Map<String,Object> values=new HashMap<>();
    private Hero previousHero;private Level previousLevel;
    private Preferences preferences(){return (Preferences)Proxy.newProxyInstance(Preferences.class.getClassLoader(),new Class[]{Preferences.class},(p,m,a)->{
        if(m.getName().startsWith("put") && a!=null && a.length==2){values.put((String)a[0],a[1]);return p;}
        if(m.getName().startsWith("get") && a!=null && a.length==2)return values.getOrDefault(a[0],a[1]);
        if(m.getName().equals("contains"))return values.containsKey(a[0]);
        if(m.getName().equals("get"))return values;
        return null;
    });}
    @Before public void setup(){previousHero=Dungeon.hero;previousLevel=Dungeon.level;SPDSettings.set(preferences());SPDSettings.confirmWaterskin(true);SPDSettings.confirmKnownTraps(true);}
    @After public void cleanup(){Dungeon.hero=previousHero;Dungeon.level=previousLevel;SPDSettings.set(null);}
    static class Empty extends Level {protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}}
    static class Skin extends Waterskin {
        ConfirmationDecision decision;int executions;
        @Override protected void requestDrinkConfirmation(ConfirmationDecision decision){this.decision=decision;}
        @Override protected void executeConfirmed(Hero hero,String action){executions++;}
    }
    private String volume(Waterskin skin){try{Field f=Waterskin.class.getDeclaredField("volume");f.setAccessible(true);return Integer.toString(f.getInt(skin));}catch(Exception e){throw new AssertionError(e);}}
    private Skin skin(){Hero hero=new Hero();hero.ready=true;hero.HP=10;Dungeon.hero=hero;Skin skin=new Skin();skin.fill();hero.belongings.backpack.items.add(skin);return skin;}
    private Level trapLevel(){Level level=new Empty();level.setSize(5,5);level.traps=new com.watabou.utils.SparseArray<>();Dungeon.level=level;return level;}
    @Test public void bothPreferencesPersistIndependently(){
        assertTrue(SPDSettings.confirmWaterskin());assertTrue(SPDSettings.confirmKnownTraps());
        SPDSettings.confirmWaterskin(false);SPDSettings.set(preferences());
        assertFalse(SPDSettings.confirmWaterskin());assertTrue(SPDSettings.confirmKnownTraps());
        SPDSettings.confirmKnownTraps(false);SPDSettings.confirmWaterskin(true);SPDSettings.set(preferences());
        assertTrue(SPDSettings.confirmWaterskin());assertFalse(SPDSettings.confirmKnownTraps());
    }
    @Test public void confirmCallbackRunsExactlyOnceEvenWithRepeatedInput(){int[] calls={0,0};ConfirmationDecision d=new ConfirmationDecision(()->calls[0]++,()->calls[1]++);d.choose(true);d.choose(true);d.choose(false);assertArrayEquals(new int[]{1,0},calls);}
    @Test public void cancelAndBackCannotLaterConfirm(){int[] calls={0,0};ConfirmationDecision d=new ConfirmationDecision(()->calls[0]++,()->calls[1]++);d.choose(false);d.choose(true);assertArrayEquals(new int[]{0,1},calls);}
    @Test public void waterskinCancelDoesNotDispatchOrChangeHpDewOrTime(){Skin s=skin();String dew=volume(s);float time=Dungeon.hero.cooldown();s.execute(Dungeon.hero,"DRINK");assertNotNull(s.decision);assertEquals(0,s.executions);s.decision.choose(false);assertEquals(0,s.executions);assertEquals(dew,volume(s));assertEquals(10,Dungeon.hero.HP);assertEquals(time,Dungeon.hero.cooldown(),0);}
    @Test public void waterskinConfirmDispatchesExistingActionOnce(){Skin s=skin();s.execute(Dungeon.hero,"DRINK");s.decision.choose(true);s.decision.choose(true);assertEquals(1,s.executions);}
    @Test public void disabledWaterskinConfirmationUsesImmediatePath(){Skin s=skin();SPDSettings.confirmWaterskin(false);s.execute(Dungeon.hero,"DRINK");assertNull(s.decision);assertEquals(1,s.executions);}
    @Test public void staleWaterskinConfirmationCannotActAfterHeroMovesOrItemIsRemoved(){Skin s=skin();s.execute(Dungeon.hero,"DRINK");Dungeon.hero.pos++;s.decision.choose(true);assertEquals(0,s.executions);s.execute(Dungeon.hero,"DRINK");Dungeon.hero.belongings.backpack.items.remove(s);s.decision.choose(true);assertEquals(0,s.executions);}
    @Test public void trapWarningRequiresEveryLegitimateKnowledgeCondition(){
        Level l=trapLevel();Hero h=new Hero();WornDartTrap t=new WornDartTrap();t.set(13);t.visible=true;l.traps.put(13,t);l.map[13]=Terrain.TRAP;l.heroFOV[13]=true;
        assertTrue(TrapStepPermit.needsWarning(true,l,h,13));
        assertFalse(TrapStepPermit.needsWarning(false,l,h,13));
        l.heroFOV[13]=false;assertFalse(TrapStepPermit.needsWarning(true,l,h,13));l.heroFOV[13]=true;
        t.visible=false;assertFalse(TrapStepPermit.needsWarning(true,l,h,13));t.visible=true;
        l.map[13]=Terrain.SECRET_TRAP;assertFalse(TrapStepPermit.needsWarning(true,l,h,13));l.map[13]=Terrain.TRAP;
        t.active=false;assertFalse(TrapStepPermit.needsWarning(true,l,h,13));t.active=true;
        h.flying=true;assertFalse(TrapStepPermit.needsWarning(true,l,h,13));
    }
    @Test public void trapPermitIsSingleUseAndBoundToItsActualAction(){Level l=trapLevel();WornDartTrap t=new WornDartTrap();HeroAction action=new HeroAction.Move(14);TrapStepPermit p=new TrapStepPermit();p.allow(l,t,action,12,13);assertTrue(p.consume(l,t,action,12,13));assertFalse(p.consume(l,t,action,12,13));}
    @Test public void changedPathOriginTrapOrLevelCannotReusePermission(){
        Level l=trapLevel();WornDartTrap t=new WornDartTrap();HeroAction a=new HeroAction.Move(14);TrapStepPermit p=new TrapStepPermit();
        p.allow(l,t,a,12,13);assertFalse(p.consume(l,t,a,11,13));
        p.allow(l,t,a,12,13);assertFalse(p.consume(l,new WornDartTrap(),a,12,13));
        p.allow(l,t,a,12,13);assertFalse(p.consume(new Empty(),t,a,12,13));
        p.allow(l,t,a,12,13);assertFalse(p.consume(l,t,new HeroAction.Move(14),12,13));
        p.allow(l,t,a,12,13);p.clear();assertFalse(p.consume(l,t,a,12,13));
    }

    static class WalkingHero extends Hero {
        ConfirmationDecision decision;int nextCalls,moves;
        @Override protected void requestKnownTrapConfirmation(com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap trap,ConfirmationDecision decision){this.decision=decision;}
        @Override public void next(){nextCalls++;}
        @Override public boolean search(boolean intentional){return false;}
        @Override public void move(int cell,boolean travelling){pos=cell;moves++;}
    }
    static class NoopSprite extends com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite {
        @Override public void move(int from,int to){}
        @Override public void sprint(float speed){}
    }
    private Object selector;
    private WalkingHero walking()throws Exception{
        Field sf=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector");sf.setAccessible(true);selector=sf.get(null);
        Field uf=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");uf.setAccessible(true);Object u=uf.get(null);
        Method alloc=u.getClass().getMethod("allocateInstance",Class.class);
        sf.set(null,alloc.invoke(u,com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.class));
        Level l=trapLevel();Arrays.fill(l.map,Terrain.EMPTY);Arrays.fill(l.passable,true);Arrays.fill(l.visited,true);
        WornDartTrap t=new WornDartTrap();t.set(13);t.visible=true;l.traps.put(13,t);l.map[13]=Terrain.TRAP;l.heroFOV[13]=true;
        WalkingHero h=new WalkingHero();h.pos=12;h.ready=true;h.sprite=(NoopSprite)alloc.invoke(u,NoopSprite.class);h.fieldOfView=l.heroFOV;Dungeon.hero=h;return h;
    }
    private boolean closer(Hero h,int cell)throws Exception{Method m=Hero.class.getDeclaredMethod("getCloser",int.class);m.setAccessible(true);return (Boolean)m.invoke(h,cell);}
    private void restoreSelector()throws Exception{Field f=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector");f.setAccessible(true);f.set(null,selector);}
    @Test public void realMovementStopsBeforeTrapAndCancelCostsNoTurn()throws Exception{
        WalkingHero h=walking();try{h.curAction=new HeroAction.Move(13);float time=h.cooldown();assertFalse(closer(h,13));assertNotNull(h.decision);assertEquals(12,h.pos);assertEquals(time,h.cooldown(),0);h.decision.choose(false);assertEquals(0,h.nextCalls);assertEquals(0,h.moves);}finally{restoreSelector();}
    }
    @Test public void realPathConfirmationResumesOnceAndPaysOnlyTheNormalStep()throws Exception{
        WalkingHero h=walking();try{
            HeroAction action=new HeroAction.Move(14);h.curAction=action;
            Field p=com.shatteredpixel.shatteredpixeldungeon.actors.Char.class.getDeclaredField("path");p.setAccessible(true);com.watabou.utils.PathFinder.Path path=new com.watabou.utils.PathFinder.Path();path.add(13);path.add(14);p.set(h,path);
            float time=h.cooldown();assertFalse(closer(h,14));assertEquals(12,h.pos);assertNull(h.curAction);
            h.decision.choose(true);h.decision.choose(true);assertEquals(1,h.nextCalls);assertSame(action,h.curAction);
            // The actual queued action still uses getCloser; a fresh route may be recalculated.
            p.set(h,path=new com.watabou.utils.PathFinder.Path());path.add(13);path.add(14);
            assertTrue(closer(h,14));assertEquals(13,h.pos);assertEquals(1,h.moves);assertEquals(time+1f/h.speed(),h.cooldown(),0.00001f);
        }finally{restoreSelector();}
    }
    @Test public void disabledTrapWarningAndForcedMovementNeverPrompt()throws Exception{
        WalkingHero h=walking();try{SPDSettings.confirmKnownTraps(false);h.curAction=new HeroAction.Move(13);assertTrue(closer(h,13));assertNull(h.decision);h.pos=12;SPDSettings.confirmKnownTraps(true);h.move(13,false);assertNull(h.decision);}finally{restoreSelector();}
    }
}
