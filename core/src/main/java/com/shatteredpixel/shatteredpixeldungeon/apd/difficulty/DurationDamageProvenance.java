package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.watabou.utils.Bundle;
import java.util.*;

/** Causal damage shares for a duration-derived effect; preserves one original damage event. */
public final class DurationDamageProvenance {
    private static final OffensiveDamageOrigin[] ROLES={OffensiveDamageOrigin.UNATTRIBUTED,OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS};
    private static class Part {
        float duration;final double[] weights;
        Part(float duration,double[] weights){this.duration=duration;this.weights=weights.clone();}
    }
    private final ArrayList<Part> parts=new ArrayList<>();
    private static double[] single(OffensiveDamageOrigin origin){double[] result=new double[4];for(int i=0;i<4;i++)if(ROLES[i]==origin){result[i]=1;return result;}throw new IllegalArgumentException("Unknown damage origin");}
    private static void duration(float value){if(!Float.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid damage duration metadata");}
    public static int raw(float left){return (int)(left/3)+1;}
    public void reset(float remaining,OffensiveDamageOrigin origin){duration(remaining);parts.clear();parts.add(new Part(remaining,single(origin)));}
    public void extend(float amount,OffensiveDamageOrigin origin){
        duration(amount);double[] weights=single(origin);
        if(!parts.isEmpty()&&Arrays.equals(parts.get(parts.size()-1).weights,weights))parts.get(parts.size()-1).duration+=amount;
        else parts.add(new Part(amount,weights));
    }
    public void consume(float amount){
        duration(amount);
        while(!parts.isEmpty()&&(amount>0||parts.get(0).duration==0)){
            Part first=parts.get(0);
            if(first.duration<=amount){amount-=first.duration;parts.remove(0);}else{first.duration-=amount;break;}
        }
    }
    public void shorten(float amount){
        duration(amount);
        while(!parts.isEmpty() && amount>0){
            Part last=parts.get(parts.size()-1);
            if(last.duration>=amount){last.duration-=amount;break;}
            amount-=last.duration;
            if(parts.size()==1){last.duration=0;break;}else parts.remove(parts.size()-1);
        }
    }
    public static void clearStored(Bundle bundle,String prefix){bundle.remove(prefix+"_version");bundle.remove(prefix+"_durations");bundle.remove(prefix+"_weights");}
    private double[] contributions(float remaining){
        double[] result=new double[4];int total=raw(remaining);if(parts.isEmpty()||total<=0){result[0]=Math.max(0,total);return result;}
        float prefix=0;int previous=0;
        for(int i=0;i<parts.size();i++){
            Part part=parts.get(i);prefix=Math.min(remaining,prefix+part.duration);
            int next=i==parts.size()-1?total:Math.min(total,Math.max(previous,raw(prefix)));
            int amount=Math.max(0,next-previous);previous=next;
            for(int role=0;role<4;role++)result[role]+=amount*part.weights[role];
        }
        return result;
    }
    public boolean modifiesDamage(){
        for(Part part:parts)for(int i=0;i<4;i++)if(part.weights[i]>0 && ROLES[i].percentage()!=100)return true;
        return false;
    }
    public int incoming(float remaining){
        DurationDamageProvenance copy=new DurationDamageProvenance();for(Part part:parts)copy.parts.add(new Part(part.duration,part.weights));
        long total=0;
        do {
            total+=Math.max(0,copy.scaled(remaining));if(total>=Integer.MAX_VALUE)return Integer.MAX_VALUE;
            copy.consume(1);float next=remaining-1;
            if(next==remaining)return Integer.MAX_VALUE; // UI sum cannot represent a duration this large.
            remaining=next;
        }while(remaining>0);
        return (int)total;
    }
    public int scaled(float remaining){
        int raw=raw(remaining);if(raw<=0)return raw;
        boolean neutral=true;for(OffensiveDamageOrigin role:ROLES)if(role.percentage()!=100)neutral=false;
        if(neutral)return raw;
        if(parts.size()==1){double factor=0;for(int i=0;i<4;i++)factor+=parts.get(0).weights[i]*ROLES[i].percentage()/100d;return (int)Math.min(Integer.MAX_VALUE,Math.round(raw*factor));}
        double[] amounts=contributions(remaining);double total=0;
        for(int i=0;i<4;i++)total+=amounts[i]*ROLES[i].percentage()/100d;
        return (int)Math.min(Integer.MAX_VALUE,Math.round(total));
    }
    /** Seeping into a new target carries original contributors, not the victim's identity. */
    public DurationDamageProvenance copyFor(float remaining,float newDuration){
        duration(newDuration);double[] values=contributions(remaining);double total=0;for(double value:values)total+=value;
        if(total==0)values=single(OffensiveDamageOrigin.UNATTRIBUTED);else for(int i=0;i<4;i++)values[i]/=total;
        DurationDamageProvenance copy=new DurationDamageProvenance();copy.parts.add(new Part(newDuration,values));return copy;
    }
    public void store(Bundle bundle,String prefix){
        bundle.put(prefix+"_version",1);float[] durations=new float[parts.size()];String[] weights=new String[parts.size()*4];
        for(int i=0;i<parts.size();i++){durations[i]=parts.get(i).duration;for(int j=0;j<4;j++)weights[i*4+j]=Double.toString(parts.get(i).weights[j]);}
        bundle.put(prefix+"_durations",durations);bundle.put(prefix+"_weights",weights);
    }
    public static DurationDamageProvenance restore(Bundle bundle,String prefix,float remaining){
        boolean version=bundle.contains(prefix+"_version"),dur=bundle.contains(prefix+"_durations"),mix=bundle.contains(prefix+"_weights");
        if(!version&&!dur&&!mix)return null;
        if(!version||!dur||!mix||bundle.getInt(prefix+"_version")!=1)throw new IllegalArgumentException("Unsupported/incomplete damage provenance; preserve save");
        duration(remaining);float[] durations=bundle.getFloatArray(prefix+"_durations");String[] values=bundle.getStringArray(prefix+"_weights");
        if((long)durations.length*4!=values.length)throw new IllegalArgumentException("Invalid damage provenance shape");
        DurationDamageProvenance result=new DurationDamageProvenance();float sum=0;
        for(int i=0;i<durations.length;i++){
            duration(durations[i]);sum+=durations[i];double[] weights=new double[4];double total=0;
            for(int j=0;j<4;j++){weights[j]=Double.parseDouble(values[i*4+j]);if(!Double.isFinite(weights[j])||weights[j]<0)throw new IllegalArgumentException("Invalid damage provenance share");total+=weights[j];}
            if(Math.abs(total-1)>1e-9)throw new IllegalArgumentException("Invalid damage provenance total");
            result.parts.add(new Part(durations[i],weights));
        }
        if(!Float.isFinite(sum)||Math.abs(sum-remaining)>Math.max(0.0001f,Math.ulp(remaining)*(durations.length+2)))throw new IllegalArgumentException("Damage provenance differs from remaining effect");
        return result;
    }
}
