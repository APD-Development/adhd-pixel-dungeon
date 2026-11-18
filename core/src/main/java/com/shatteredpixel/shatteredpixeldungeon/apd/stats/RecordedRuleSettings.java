package com.shatteredpixel.shatteredpixeldungeon.apd.stats;
import java.util.*;
/** Frozen wire schema; historical records never use mutable game defaults or slider bounds. */
public final class RecordedRuleSettings{
 public static final int VERSION=1;
 public static final List<String> KEYS=Collections.unmodifiableList(Arrays.asList("ENEMY_HP","ENEMY_DAMAGE","ENEMY_DEFENSE","ENEMY_ACCURACY","ENEMY_EVASION","ENEMY_SPAWN","BOSS_HP","BOSS_DAMAGE","PLAYER_HP","PLAYER_DAMAGE","PLAYER_ACCURACY","PLAYER_EVASION","PLAYER_DEFENSE","FOOD","HUNGER","HEALING","EQUIPMENT","UPGRADE_SCROLLS"));
 private static final int[] MIN={25,25,0,25,0,25,25,25,25,25,25,0,0,25,25,0,25,1};
 private static final int[] MAX={500,500,500,500,500,300,500,500,500,500,500,500,500,200,400,200,200,60};
 private final int[] values;
 private RecordedRuleSettings(int[] v){values=v.clone();}
 public static RecordedRuleSettings parse(String text){String[] parts=text.split(",",-1);if(parts.length!=KEYS.size()+1||!parts[0].equals("1"))throw new IllegalArgumentException("Unsupported recorded rules schema");int[] v=new int[KEYS.size()];for(int i=0;i<v.length;i++){v[i]=Integer.parseInt(parts[i+1]);int step=i==17?1:25;if(v[i]<MIN[i]||v[i]>MAX[i]||(v[i]-MIN[i])%step!=0)throw new IllegalArgumentException("Invalid recorded setting");}return new RecordedRuleSettings(v);}
 public int get(String key){int i=KEYS.indexOf(key);if(i<0)throw new IllegalArgumentException("Unknown recorded setting");return values[i];}
 public boolean isNeutral(){for(int i=0;i<values.length;i++)if(values[i]!=(i==17?15:100))return false;return true;}
 public String identityText(){StringBuilder s=new StringBuilder("custom-v1");for(int i=0;i<values.length;i++)s.append(';').append(KEYS.get(i)).append('=').append(values[i]);return s.toString();}
 public static final class Scoring{
  public final boolean neutral;public final String status;public final String calculationVersion,multiplierExact;
  private Scoring(boolean n,String c,String m){this(n,c,m,n?"neutral identity":"legacy unadjusted");}
  private Scoring(boolean n,String c,String m,String status){this.status=status;neutral=n;calculationVersion=c;multiplierExact=m;}
  public String displayStatus(){return status;}
 }
 public Scoring scoring(int version,String status,String calculation,String multiplier){
  if(status==null||calculation==null||multiplier==null)throw new IllegalArgumentException("Missing recorded scoring metadata");
  if(version==0&&status.isEmpty()&&calculation.isEmpty()&&multiplier.isEmpty())return null;
  if(version==2){
   if(!status.equals("EXPERIMENTAL")||!calculation.equals(ProvisionalScoreLookup.VERSION)||!multiplier.equals(ProvisionalScoreLookup.combined(values).toPlainString()))throw new IllegalArgumentException("Invalid provisional scoring metadata");
   return new Scoring(isNeutral(),calculation,multiplier,"experimental provisional");
  }
  if(version!=1)throw new IllegalArgumentException("Unsupported recorded scoring schema");
  if(status.equals("NEUTRAL_IDENTITY")&&isNeutral()&&calculation.equals("neutral-identity-v1")&&multiplier.equals("1.00"))return new Scoring(true,calculation,multiplier);
  if(status.equals("PENDING_CALIBRATION")&&!isNeutral()&&calculation.equals("pending-calibration-v1")&&multiplier.isEmpty())return new Scoring(false,calculation,multiplier);
  throw new IllegalArgumentException("Inconsistent recorded scoring metadata");
 }
}
