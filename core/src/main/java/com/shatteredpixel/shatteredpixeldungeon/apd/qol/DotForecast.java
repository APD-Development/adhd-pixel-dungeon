package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import java.util.Locale;

/** Informational only. Never advances actors or samples RNG. */
public final class DotForecast {
    public final String label;
    public final String next;
    public final String remaining;
    public final String duration;
    public final String spent;
    public final String caveat;

    private DotForecast(String name,String next,String remaining,String duration,String spent,String caveat){
        this.label=name;this.next=next;this.remaining=remaining;this.duration=duration;this.spent=spent;this.caveat=caveat;
    }

    public static DotForecast of(Buff buff) {
        if (buff == null || !(buff instanceof Buff.DOTbuff))return null;
        String next="unknown",rem="not predictable",duration="variable";
        String caveat="Other buffs, resists, immunities, shielding and changing conditions can alter HP loss.";
        if (buff instanceof Poison) {
            Poison p=(Poison)buff;
            duration=approx(p.apdRemaining());
            next=p.apdNextInput()+" before target defense";
            rem=p.totalIncomingDMG()+" before target defense (current conditions)";
        } else if (buff instanceof Corrosion) {
            Corrosion c=(Corrosion)buff;
            duration=approx(c.apdRemaining());
            next=c.apdNextRaw()+" raw, increasing each tick";
            rem=c.totalIncomingDMG()+" before target defense (if unchanged)";
        } else if (buff instanceof Ooze) {
            Ooze o=(Ooze)buff;
            duration=approx(o.apdRemaining());
            next=o.apdNextRaw()+" raw (water can remove Ooze)";
            rem=o.totalIncomingDMG()+" before target defense (if unchanged)";
        } else if (buff instanceof Burning) {
            Burning fire=(Burning)buff;
            int max=fire.apdRandomMaximum();
            int ticks=Math.max(0,(int)Math.ceil(fire.apdRemaining()));
            int first=fire.apdNextRaw();
            duration=approx(fire.apdRemaining());
            next=first>0 ? first+" raw (pre-rolled)" : "1-"+max+" raw (random)";
            double estimated=(first>0?first:(max+1)/2d)+Math.max(0,ticks-1)*(max+1)/2d;
            rem="~"+Math.round(estimated)+" raw expected over "+ticks+" tick(s)";
            caveat="Random future ticks; water/immunity may extinguish it. Do not treat estimate as guaranteed HP loss.";
        } else if (buff instanceof Bleeding) {
            Bleeding bleed=(Bleeding)buff;
            next=bleed.apdNextRaw()+" raw (current strength)";
            rem="~"+bleed.totalIncomingDMG()+" before target defense";
            duration="random decay; no fixed end turn";
            caveat="Bleeding loses a random amount of strength each tick; total and expiry are estimates.";
        } else {
            rem="not recorded precisely";
        }
        Char target=buff.target;
        if (target != null && target.isImmune(buff.getClass())) {
            caveat="Target currently immune. Future changes can alter the result.";
        }
        String spent="Applied so far: "+buff.apdDotRaw()+" input, "+
                buff.apdDotShield()+" shielding consumed, "+buff.apdDotHp()+" HP lost";
        float ready=buff.visualcooldown();
        String when="Next scheduled action: "+(Float.isFinite(ready)?approx(Math.max(0,ready)):"unknown");
        return new DotForecast(buff.name(),next,rem,duration+" | "+when,spent,caveat);
    }

    private static String approx(float f){
        if(!Float.isFinite(f)||f<0)return "unknown";
        return String.format(Locale.ROOT,"~%.1f turn(s)",f);
    }
    public String details(){
        return label+"\nNext: "+next+"\nRemaining: "+remaining+"\nDuration: "+duration+
                "\n"+spent+"\n"+caveat;
    }
}
