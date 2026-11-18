package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;

/** Read-only current-state preview. Never rolls damage or invokes mutating damage handlers. */
public final class ChaliceDamagePreview {
    public final int baseMin, baseMax, damageMin, damageMax, hpMin, hpMax, hp, shield;
    public final boolean linked, delayedDeath;
    public static int baseMin(int level){return (int)Math.ceil(3+2.5f*level*level);}
    public static int baseMax(int level){return (int)Math.floor(7+3.5f*level*level);}
    private ChaliceDamagePreview(int level, Hero hero, int dmin, int dmax, int hmin, int hmax){
        baseMin=baseMin(level);baseMax=baseMax(level);damageMin=dmin;damageMax=dmax;
        hpMin=hmin;hpMax=hmax;hp=hero.HP;shield=hero.shielding();
        linked=hero.buff(LifeLink.class)!=null;delayedDeath=hero.deathMarked;
    }
    public boolean couldBeFatal(){return linked || hpMax>=hp;}
    private static String range(int min,int max){return min==max ? Integer.toString(min) : min+"–"+max;}
    public String description(){
        String text="_Next prick_\nBase damage: "+range(baseMin,baseMax)+"\n";
        if(linked) text+="Life Link: damage and HP loss cannot be reliably predicted because linked characters can return damage.\n";
        else text+="After reductions: "+range(damageMin,damageMax)+"\nHP loss after shields: "+range(hpMin,hpMax)+"\n";
        text+="Current HP: "+hp+"; shielding: "+shield;
        if(couldBeFatal()) text+="\n_WARNING: This prick could kill you!_";
        if(!linked && hpMin>=hp && hp>0) text+="\nEvery outcome can exhaust your HP.";
        if(delayedDeath) text+="\nDeath Mark can delay death; zero HP is not a survival guarantee.";
        return text;
    }
    public static ChaliceDamagePreview calculate(Hero hero,int level){
        if(hero.isInvulnerable(ChaliceOfBlood.class) || hero.buff(TimeStasis.class)!=null
                || hero.buff(TimekeepersHourglass.timeStasis.class)!=null)
            return new ChaliceDamagePreview(level,hero,0,0,0,0);
        Earthroot.Armor earth=hero.buff(Earthroot.Armor.class);
        WandOfLivingEarth.RockArmor rock=hero.buff(WandOfLivingEarth.RockArmor.class);
        int low=baseMin(level),high=baseMax(level);
        if(earth!=null){low=earth.previewAbsorb(low);high=earth.previewAbsorb(high);}
        if(hero.buff(MagicImmune.class)!=null && hero.buff(HolyWard.HolyArmBuff.class)!=null){
            int ward=hero.subClass==HeroSubClass.PALADIN?3:1;low-=ward;high-=ward;
        }
        if(rock!=null){low=rock.previewAbsorb(low);high=rock.previewAbsorb(high);}
        int drLow=0,drHigh=Barkskin.currentLevel(hero);
        Armor armor=hero.belongings.armor();
        if(armor!=null){int penalty=Math.max(0,armor.STRReq()-hero.STR())*2;
            drLow+=Math.max(0,Math.min(armor.DRMin(),armor.DRMax())-penalty);
            drHigh+=Math.max(0,Math.max(armor.DRMin(),armor.DRMax())-penalty);}
        if(hero.belongings.weapon()!=null && !RingOfForce.fightingUnarmed(hero)){
            Weapon weapon=(Weapon)hero.belongings.weapon();
            int defense=weapon.defenseFactor(hero),penalty=Math.max(0,weapon.STRReq()-hero.STR())*2;
            drLow+=Math.max(0,Math.min(0,defense)-penalty);drHigh+=Math.max(0,Math.max(0,defense)-penalty);
        }
        HoldFast hold=hero.buff(HoldFast.class);
        if(hold!=null && hold.pos==hero.pos){
            int cap=hero.heroClass==HeroClass.WARRIOR?6:armor==null?0:armor.tier+armor.buffedLvl();
            int points=hero.pointsInTalent(Talent.HOLD_FAST);
            drLow+=Math.min(points,cap);drHigh+=Math.min(2*points,cap);
        }
        drLow=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDefenseHooks.scale(hero,drLow);
        drHigh=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDefenseHooks.scale(hero,drHigh);
        low=Math.max(1,low-drHigh);high=Math.max(1,high-drLow);
        float early=1f;
        Endure.EndureTracker endure=hero.buff(Endure.EndureTracker.class);
        if(endure!=null && endure.enduring){early*=0.5f;
            if(hero.hasTalent(Talent.SHRUG_IT_OFF)) early*=Math.pow(0.8f,hero.pointsInTalent(Talent.SHRUG_IT_OFF));}
        // Keep sequential float rounding identical to Hero.damage, rather than multiplying factors together.
        CapeOfThorns.Thorns cape=hero.buff(CapeOfThorns.Thorns.class);
        float tenacity=RingOfTenacity.damageMultiplier(hero);
        float resistance=hero.isImmune(ChaliceOfBlood.class)?0:hero.resist(ChaliceOfBlood.class);
        int glyph=hero.glyphLevel(AntiMagic.class),magicLow=0,magicHigh=0;
        if(glyph>=0){float power=Armor.Glyph.genericProcChanceMultiplier(hero);
            magicLow=Math.round(glyph*power);magicHigh=Math.round((3+glyph*1.5f)*power);}
        ArcaneArmor arcane=hero.buff(ArcaneArmor.class);
        if(arcane!=null) magicHigh+=arcane.level();
        BrokenSeal.WarriorShield seal=hero.buff(BrokenSeal.WarriorShield.class);
        boolean sealReady=seal!=null && !seal.coolingDown();int sealAmount=sealReady?seal.maxShield():0;
        boolean arena=hero.buff(ScrollOfChallenge.ChallengeArena.class)!=null;
        boolean meditate=hero.buff(MonkEnergy.MonkAbility.Meditate.MeditateResistance.class)!=null;
        int food=hero.buff(Talent.WarriorFoodImmunity.class)==null?0:hero.pointsInTalent(Talent.IRON_STOMACH);
        float aura=hero.buff(AuraOfProtection.AuraBuff.class)==null?1f:0.9f-0.1f*hero.pointsInTalent(Talent.AURA_OF_PROTECTION);
        float power=hero.buff(PowerOfMany.PowerBuff.class)==null?1f:hero.buff(LifeLinkSpell.LifeLinkSpellBuff.class)!=null?
                0.70f-0.05f*hero.pointsInTalent(Talent.LIFE_LINK):0.75f;
        boolean doom=hero.buff(Doom.class)!=null && !hero.isImmune(Doom.class);
        boolean mark=hero.alignment!=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY && hero.buff(DeathMark.DeathMarkTracker.class)!=null;
        java.util.ArrayList<Float> championFactors=new java.util.ArrayList<>();
        for(ChampionEnemy champion:hero.buffs(ChampionEnemy.class))championFactors.add(champion.damageTakenFactor());
        int shield=hero.shielding();int dmin=Integer.MAX_VALUE,dmax=0,hmin=Integer.MAX_VALUE,hmax=0;
        // Deflection makes every integer from zero through the hit reachable. Work on that
        // union once, instead of re-enumerating the same deflections for every base roll.
        int first=low,last=high;
        if(cape!=null){
            float smallest=low*early,largest=high*early;
            if(arena){smallest*=0.67f;largest*=0.67f;}
            if(meditate){smallest*=0.2f;largest*=0.2f;}
            last=(int)largest;
            first=cape.previewMinimumDamage(last)==0?0:(int)smallest;
        }
        for(int input=first;input<=last;input++){
                float taken=input;
                if(cape==null){taken*=early;if(arena)taken*=0.67f;if(meditate)taken*=0.2f;}
                if(food==1)taken/=4f;else if(food==2)taken=0;
                taken=(int)Math.ceil(Math.round(taken)*tenacity);
                // Life Link is explicitly labelled unpredictable: do not promise survivability from this direct-hit range.
                taken*=aura;taken*=power;
                if(doom)taken*=1.67f;
                if(mark)taken*=1.25f;
                int reduced=Math.round(taken*resistance);
                for(float factor:championFactors)reduced=(int)Math.ceil(reduced*factor);
                int least=Math.max(0,reduced-magicHigh),most=Math.max(0,reduced-magicLow);
                dmin=Math.min(dmin,least);dmax=Math.max(dmax,most);
                // Shield activation makes HP loss non-monotonic. Check both sides of its
                // threshold as well as the endpoints; do not enumerate huge antimagic ranges.
                int threshold=Math.max(1,hero.HP+shield-hero.HT/2);
                if(hero.HP<=hero.HT/2)threshold=1;
                for(int corner=0;corner<(sealReady?4:2);corner++){
                    int result=corner==0?least:corner==1?most:corner==2?threshold-1:threshold;
                    result=Math.max(least,Math.min(most,result));
                    int available=shield;
                    if(sealReady && result>0 && (hero.HP<=hero.HT/2 || hero.HP+shield-result<=hero.HT/2))available+=sealAmount;
                    int loss=Math.max(0,result-available);
                    hmin=Math.min(hmin,loss);hmax=Math.max(hmax,loss);
                }
        }
        return new ChaliceDamagePreview(level,hero,dmin,dmax,hmin,hmax);
    }
}

