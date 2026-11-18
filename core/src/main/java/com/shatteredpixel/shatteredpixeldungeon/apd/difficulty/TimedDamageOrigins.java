package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
/** Ordered causal duration contributions; no RNG and no attacker object retention. */
public final class TimedDamageOrigins {
 private static final class Part {float duration;final OffensiveDamageOrigin role;Part(float d,OffensiveDamageOrigin r){duration=d;role=r;}}
 private final ArrayList<Part> parts=new ArrayList<>();
 public static TimedDamageOrigins single(float duration,OffensiveDamageOrigin role){TimedDamageOrigins o=new TimedDamageOrigins();o.parts.add(new Part(Math.max(0,duration),role));return o;}
 /** Repeats one mixed ownership sample for every damage tick without pre-scaling damage. */
 public static TimedDamageOrigins repeating(float duration,double[] shares){
  if(shares==null||shares.length!=4||!Float.isFinite(duration)||duration<0)throw new IllegalArgumentException("Invalid mixed timed cause");
  double sum=0;for(double share:shares){if(!Double.isFinite(share)||share<0)throw new IllegalArgumentException("Invalid mixed timed share");sum+=share;}
  if(Math.abs(sum-1)>1e-9)throw new IllegalArgumentException("Invalid mixed timed total");
  TimedDamageOrigins o=new TimedDamageOrigins();float remaining=duration;OffensiveDamageOrigin[] roles=OffensiveDamageOrigin.values();
  while(remaining>0){float tick=Math.min(1f,remaining);for(int i=0;i<4;i++)if(shares[i]>0)o.parts.add(new Part((float)(tick*shares[i]),roles[i]));remaining-=tick;}
  return o;
 }
 public void extend(float duration,OffensiveDamageOrigin role){if(duration<=0)return;if(parts.isEmpty())parts.add(new Part(duration,role));else{Part last=parts.get(parts.size()-1);if(last.role==role)last.duration+=duration;else parts.add(new Part(duration,role));}}
 public void consume(float duration){while(duration>0&&!parts.isEmpty()){Part first=parts.get(0);float use=Math.min(duration,first.duration);first.duration-=use;duration-=use;if(first.duration<=0)parts.remove(0);else break;}}
 public void shorten(float duration){while(duration>0&&!parts.isEmpty()){Part last=parts.get(parts.size()-1);float use=Math.min(duration,last.duration);last.duration-=use;duration-=use;if(last.duration<=0&&parts.size()>1)parts.remove(parts.size()-1);else break;}}
 public TimedDamageOrigins copy(){TimedDamageOrigins c=new TimedDamageOrigins();for(Part p:parts)c.parts.add(new Part(p.duration,p.role));return c;}
 public boolean modifiesDamage(){for(Part p:parts)if(p.role.percentage()!=100)return true;return false;}
 /** Normalized ownership mixture used by the next damage tick. */
 public double[] currentShares(){
  double[] shares=new double[4];double used=0;OffensiveDamageOrigin[] roles=OffensiveDamageOrigin.values();
  for(Part part:parts){double amount=Math.min(1-used,part.duration);if(amount>0){shares[part.role.ordinal()]+=amount;used+=amount;}if(used>=1)break;}
  if(used<=0){shares[OffensiveDamageOrigin.UNATTRIBUTED.ordinal()]=1;return shares;}
  for(int i=0;i<roles.length;i++)shares[i]/=used;return shares;
 }
 public int scale(int raw){
  if(raw<=0||parts.isEmpty())return raw;
  double used=0,weighted=0;boolean neutral=true;
  for(Part part:parts){double amount=Math.min(1-used,part.duration);if(amount>0){int pct=part.role.percentage();used+=amount;weighted+=amount*pct;if(pct!=100)neutral=false;}if(used>=1)break;}
  if(used==0){int pct=parts.get(0).role.percentage();return pct==100?raw:(int)Math.min(Integer.MAX_VALUE,Math.round(raw*(pct/100.0)));}
  if(neutral)return raw;return(int)Math.min(Integer.MAX_VALUE,Math.round(raw*weighted/(used*100)));
 }
 public void store(Bundle b,String key){b.put(key+"_version",1);float[] d=new float[parts.size()];String[] r=new String[d.length];for(int i=0;i<d.length;i++){d[i]=parts.get(i).duration;r[i]=parts.get(i).role.name();}b.put(key+"_durations",d);b.put(key+"_roles",r);}
 public static void clear(Bundle b,String key){b.remove(key+"_version");b.remove(key+"_durations");b.remove(key+"_roles");}
 public static TimedDamageOrigins restore(Bundle b,String key,float remaining){
  boolean v=b.contains(key+"_version"),d=b.contains(key+"_durations"),r=b.contains(key+"_roles");if(!v&&!d&&!r)return null;
  if(!v||!d||!r||b.getInt(key+"_version")!=1)throw new IllegalArgumentException("Unsupported timed damage metadata; preserve save");
  float[] ds=b.getFloatArray(key+"_durations");String[] rs=b.getStringArray(key+"_roles");if(ds.length!=rs.length||ds.length==0)throw new IllegalArgumentException("Invalid timed damage metadata");
  TimedDamageOrigins o=new TimedDamageOrigins();double sum=0;for(int i=0;i<ds.length;i++){if(!Float.isFinite(ds[i])||ds[i]<0)throw new IllegalArgumentException("Invalid timed cause duration");o.parts.add(new Part(ds[i],OffensiveDamageOrigin.valueOf(rs[i])));sum+=ds[i];}
  if(!Float.isFinite(remaining)||Math.abs(sum-remaining)>Math.max(.0001,Math.ulp(remaining)*(ds.length+2)))throw new IllegalArgumentException("Timed cause duration mismatch");return o;
 }
}
