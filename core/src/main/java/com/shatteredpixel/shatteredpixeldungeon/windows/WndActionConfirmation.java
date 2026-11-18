package com.shatteredpixel.shatteredpixeldungeon.windows;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.ConfirmationDecision;
/** Existing Pixel Dungeon dialog; Android Back has exactly the same result as Cancel. */
public class WndActionConfirmation extends WndOptions {
    private final ConfirmationDecision decision;
    public WndActionConfirmation(String title,String message,ConfirmationDecision decision){
        super(title,message,"Confirm","Cancel");this.decision=decision;
    }
    @Override protected void onSelect(int index){decision.choose(index==0);}
    @Override public void onBackPressed(){decision.choose(false);super.onBackPressed();}
}
