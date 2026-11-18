package com.shatteredpixel.shatteredpixeldungeon.apd.hotbar;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import java.io.IOException;

/** Commit a manual shortcut edit at a stable input boundary, using the existing safe save format. */
public final class HotbarPersistence {
    private HotbarPersistence(){}
    public static boolean saveManualAssignment(){
        if(Dungeon.hero==null || !Dungeon.hero.isAlive() || !Dungeon.hero.ready || Dungeon.level==null || GamesInProgress.curSlot<1)return false;
        try{Dungeon.saveAll();return true;}
        catch(IOException failure){ShatteredPixelDungeon.reportException(failure);GLog.w("The hotbar change could not be saved. Keep APD open and try saving again.");return false;}
    }
}
