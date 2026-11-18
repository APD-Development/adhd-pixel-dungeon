package com.shatteredpixel.shatteredpixeldungeon.apd.navigation;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.watabou.utils.PathFinder;

/** Transient safe stair routing. Only an explicitly opted-in shortcut may activate the target transition. */
public final class StairNavigation {
    private StairNavigation() {}

    public enum Route {
        UP,
        DOWN,
        RETURN_TO_MAIN_DUNGEON
    }

    public static final class Walk extends HeroAction.Move {
        public final Level level;
        public final boolean up;
        public final Route route;
        public final boolean enterOnArrival;
        // Preserve the original pure-walk constructor for telemetry and existing callers.
        public Walk(int destination, Level level) { this(destination,level,Route.DOWN,false); }
        public Walk(int destination, Level level, boolean up, boolean enterOnArrival) {
            this(destination, level, up ? Route.UP : Route.DOWN, enterOnArrival);
        }
        public Walk(int destination, Level level, Route route, boolean enterOnArrival) {
            super(destination, HeroAction.Move.Mode.STAIRS);
            this.level = level;
            this.route = route;
            this.up = route == Route.UP;
            this.enterOnArrival = enterOnArrival;
        }
    }

    /**
     * Branch areas have one logical way out: their branch entrance returns to the main dungeon.
     * Main-dungeon branch exits are deliberately ignored here so ordinary up/down controls return
     * as soon as the hero is back on the main floor.
     */
    public static Route[] availableRoutes(Level level) {
        if (hasTransition(level, Route.RETURN_TO_MAIN_DUNGEON)) {
            return new Route[]{Route.RETURN_TO_MAIN_DUNGEON};
        }
        return new Route[]{Route.UP, Route.DOWN};
    }

    public static int discoveredStair(Level level, boolean up) {
        return discoveredTarget(level, up ? Route.UP : Route.DOWN);
    }

    public static int discoveredTarget(Level level, Route route) {
        if (level==null || level.transitions==null || route==null) return -1;
        for (LevelTransition transition:level.transitions) {
            // Check the route before accessing the cell: an incomplete transition
            // collection must not crash the read-only stair shortcut.
            if (!matchesRoute(transition, route)) continue;
            int cell=transition.cell();
            if (cell>=0 && cell<level.length()
                    && (level.visited[cell] || level.mapped[cell])) return cell;
        }
        return -1;
    }

    private static boolean hasTransition(Level level, Route route) {
        if (level==null || level.transitions==null) return false;
        for (LevelTransition transition:level.transitions) {
            if (matchesRoute(transition, route)) return true;
        }
        return false;
    }

    private static boolean matchesRoute(LevelTransition transition, Route route) {
        if (transition == null || route == null) return false;
        switch (route) {
            case UP:
                return transition.type == LevelTransition.Type.REGULAR_ENTRANCE
                        || transition.type == LevelTransition.Type.SURFACE;
            case DOWN:
                return transition.type == LevelTransition.Type.REGULAR_EXIT;
            case RETURN_TO_MAIN_DUNGEON:
                return transition.type == LevelTransition.Type.BRANCH_ENTRANCE;
            default:
                return false;
        }
    }

    // Validate again after walking: foes, floor seals, obstacles and transition identity can change.
    public static boolean canEnterOnArrival(Hero hero, Walk walk) {
        Level level = Dungeon.level;
        if (walk == null || !walk.enterOnArrival || hero == null || !hero.isAlive()
                || level == null || walk.level != level || hero.pos != walk.dst
                || hero.visibleEnemies() > 0 || hero.rooted || level.locked
                || !safeStep(level, hero, walk.dst)) return false;
        LevelTransition transition = level.getTransition(walk.dst);
        return transition != null && transition.cell() == walk.dst
                && matchesRoute(transition, walk.route) && transition.inside(hero.pos);
    }

    public static boolean safeStep(Level level, Hero hero, int cell) {
        if (level==null || hero==null || cell<0 || cell>=level.length()
                || !(level.visited[cell] || level.mapped[cell])
                || !level.passable[cell] || level.avoid[cell] || level.pit[cell]) return false;
        // A partially restored level or shorter-than-map FOV must not turn a
        // shortcut into an unsafe move or crash before ordinary play resumes.
        // Fully initialized levels keep the exact prior routing checks.
        if (level.traps==null || (hero.fieldOfView!=null && cell>=hero.fieldOfView.length)) return false;
        Trap trap=level.traps.get(cell);
        if (trap!=null && trap.visible && trap.active) return false;
        if (hero.fieldOfView!=null && hero.fieldOfView[cell]) {
            if (Actor.findChar(cell)!=null && cell!=hero.pos) return false;
            if (level.blobs!=null) for (Class<? extends Blob> type: DANGERS) {
                Blob blob=level.blobs.get(type);
                // Respect current immunity (e.g. Purity), never a potion merely carried in inventory.
                // Rechecked on each turn, so an expiring protection cannot authorize later steps.
                if (blob!=null && blob.cur!=null && cell<blob.cur.length && blob.cur[cell]>0
                        && !hero.isImmune(type)) return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked") private static final Class<? extends Blob>[] DANGERS = new Class[]{Fire.class,ToxicGas.class,CorrosiveGas.class,Electricity.class,Freezing.class};

    public static String begin(Hero hero, boolean up) {
        return begin(hero, up ? Route.UP : Route.DOWN);
    }

    public static String begin(Hero hero, Route route) {
        Level level=Dungeon.level;
        if (hero==null || !hero.isAlive() || !hero.ready || level==null) return "Wait until you can move.";
        int target=discoveredTarget(level,route);
        if (target<0) return route == Route.RETURN_TO_MAIN_DUNGEON
                ? "The return to the main dungeon has not been discovered."
                : "Those stairs have not been discovered.";
        boolean enterOnArrival = SPDSettings.autoEnterStairs();
        if (hero.pos==target && !enterOnArrival) return route == Route.RETURN_TO_MAIN_DUNGEON
                ? "You are already at the return to the main dungeon."
                : "You are already at the stairs.";
        if (hero.visibleEnemies()>0) return "An enemy is nearby. Stair navigation stopped.";
        if (hero.rooted || level.locked) return "You cannot navigate to those stairs right now.";
        if (!safeStep(level,hero,target)) return "No safe discovered route reaches those stairs.";
        if (hero.pos==target) {
            LevelTransition transition=level.getTransition(target);
            if (transition==null || transition.cell()!=target || !matchesRoute(transition,route))
                return "The requested stairs are no longer available.";
            hero.interrupt();
            hero.lastAction=null;
            hero.curAction=new HeroAction.LvlTransition(target);
            return null;
        }
        boolean[] passable=new boolean[level.length()];
        for(int i=0;i<passable.length;i++) passable[i]=safeStep(level,hero,i);
        passable[hero.pos]=true;
        PathFinder.Path path=Dungeon.findPath(hero,target,passable,hero.fieldOfView,true);
        if(path==null || path.isEmpty()) return "No safe discovered route reaches those stairs.";
        hero.beginStairWalk(new Walk(target,level,route,enterOnArrival),path);
        return null;
    }
}
