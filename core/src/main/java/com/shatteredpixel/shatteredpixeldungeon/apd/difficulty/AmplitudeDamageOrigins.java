package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.watabou.utils.Bundle;

/** Cause shares for an amplitude-based effect. Decay does not change the cause proportions. */
public final class AmplitudeDamageOrigins {
    private static final OffensiveDamageOrigin[] ROLES={OffensiveDamageOrigin.UNATTRIBUTED,OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS};
    private final double[] shares=new double[4];
    private static int index(OffensiveDamageOrigin role){for(int i=0;i<ROLES.length;i++)if(ROLES[i]==role)return i;throw new IllegalArgumentException("Missing damage origin");}
    public static AmplitudeDamageOrigins single(OffensiveDamageOrigin role){AmplitudeDamageOrigins o=new AmplitudeDamageOrigins();o.shares[index(role)]=1;return o;}
    public void add(float oldAmplitude,float extra,OffensiveDamageOrigin role){
        if(extra<=0)return;
        if(oldAmplitude<=0){java.util.Arrays.fill(shares,0);shares[index(role)]=1;return;}
        double total=(double)oldAmplitude+extra;
        for(int i=0;i<4;i++)shares[i]*=oldAmplitude/total;
        shares[index(role)]+=extra/total;
    }
    public int scale(int raw){
        if(raw<=0)return raw;
        double factor=0;boolean neutral=true;
        for(int i=0;i<4;i++){int percent=ROLES[i].percentage();if(shares[i]!=0&&percent!=100)neutral=false;factor+=shares[i]*percent/100.0;}
        if(neutral)return raw;
        return (int)Math.min(Integer.MAX_VALUE,Math.round(raw*factor));
    }
    public void store(Bundle b,String key){
        b.put(key+"_version",1);String[] values=new String[4];for(int i=0;i<4;i++)values[i]=Double.toString(shares[i]);b.put(key+"_shares",values);
    }
    public static void clear(Bundle b,String key){b.remove(key+"_version");b.remove(key+"_shares");}
    public static AmplitudeDamageOrigins restore(Bundle b,String key){
        if(!b.contains(key+"_version")&&!b.contains(key+"_shares"))return null;
        if(!b.contains(key+"_version")||!b.contains(key+"_shares")||b.getInt(key+"_version")!=1)throw new IllegalArgumentException("Unsupported damage provenance; preserve save for recovery");
        String[] values=b.getStringArray(key+"_shares");if(values.length!=4)throw new IllegalArgumentException("Invalid damage provenance");
        AmplitudeDamageOrigins o=new AmplitudeDamageOrigins();double sum=0;
        for(int i=0;i<4;i++){o.shares[i]=Double.parseDouble(values[i]);if(!Double.isFinite(o.shares[i])||o.shares[i]<0||o.shares[i]>1)throw new IllegalArgumentException("Invalid damage cause share");sum+=o.shares[i];}
        if(Math.abs(sum-1)>1e-9)throw new IllegalArgumentException("Invalid damage cause total");return o;
    }
}
