package com.shatteredpixel.shatteredpixeldungeon.apd.demo;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.io.File;

//Writes realistic run logs for the INDEV-only demo data, through the real writer (see DemoData). Like the game, hidden (INTERNAL) events happen
//on the turn they belong to and do not move the clock: only things the player could see advance it.
public final class DemoLog {

	public final RunLogWriter w;
	public final File file;
	final long createdAt;
	public long clock = 1;
	//telemetry 1.7 runs only: the difficulty written into the next run start (0 = not recorded)
	public int difficulty;
	public int difficultyBalance;
	public int depth = 1;

	public DemoLog( File root, String logId, TelemetryVersion v, long createdAt ){
		this.createdAt = createdAt;
		file = new File(new File(root, "runs"), logId + ".apdlog");
		w = RunLogWriter.create(file, v, logId, DemoData.BUILD_MARK, createdAt);
	}

	public DemoLog start( String runId, int segment, String heroClass, String ruleset, int challenges, int exotic, boolean partial ){
		LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
		s.runId = runId; s.segment = segment; s.heroClass = heroClass; s.rulesetId = ruleset; s.challenges = challenges; s.exoticChance = exotic;
		s.seed = 4242; s.apdVersionName = DemoData.VERSION_MARK; s.platform = DemoData.BUILD_MARK; s.customSeedText = DemoData.BUILD_MARK;
		s.rulesetText = DemoData.BUILD_MARK; s.realTimeMillis = createdAt;
		s.difficulty = difficulty;
		s.difficultyBalance = difficultyBalance;
		LifecycleEvents.emitRunStart(w, clock++, partial, s);
		return this;
	}

	public DemoLog enter( int d ){
		depth = d;
		LifecycleEvents.LevelEnter l = new LifecycleEvents.LevelEnter();
		l.depth = d;
		LifecycleEvents.emitLevelEnter(w, clock++, l);
		return this;
	}

	public DemoLog tick( long n ){
		clock += n;
		return this;
	}

	private ItemEvents.Where iw(){
		ItemEvents.Where x = new ItemEvents.Where();
		x.depth = depth;
		return x;
	}

	private static ItemEvents.VisibleState state( String name, int category, boolean levelKnown, int level, boolean identified ){
		ItemEvents.VisibleState s = new ItemEvents.VisibleState();
		s.name = name; s.category = category; s.levelKnown = levelKnown; s.visibleLevel = level; s.identified = identified;
		return s;
	}

	public DemoLog registered( String uid, String trueClass, int trueLevel, boolean cursed, int origin ){
		ItemEvents.Registered r = new ItemEvents.Registered();
		r.uid = uid; r.trueClass = trueClass; r.trueLevel = trueLevel; r.cursed = cursed; r.origin = origin; r.where = iw();
		ItemEvents.emitRegistered(w, clock, 0, r);
		return this;
	}

	public DemoLog generated( String uid, int container, String room ){
		GenerationEvents.ItemGenerated g = new GenerationEvents.ItemGenerated();
		g.uid = uid; g.container = container; g.room = room; g.drawKind = GenerationEvents.DRAW_CATEGORY; g.requestedCategory = "WEAPON";
		g.where = new GenerationEvents.Where();
		g.where.depth = depth;
		GenerationEvents.emitItemGenerated(w, clock, g);
		return this;
	}

	public DemoLog seen( String uid, String name, int category ){
		ItemEvents.Seen s = new ItemEvents.Seen();
		s.uid = uid; s.where = iw(); s.state = state(name, category, false, 0, false);
		ItemEvents.emitSeen(w, clock++, 0, s);
		return this;
	}

	public DemoLog acquired( String uid, String name, int category, boolean levelKnown, int level ){
		ItemEvents.Acquired a = new ItemEvents.Acquired();
		a.uid = uid; a.where = iw(); a.state = state(name, category, levelKnown, level, levelKnown);
		ItemEvents.emitAcquired(w, clock++, 0, a);
		return this;
	}

	public DemoLog equip( String uid, String name, int category ){
		ItemEvents.Equip q = new ItemEvents.Equip();
		q.uid = uid; q.where = iw(); q.state = state(name, category, false, 0, false);
		ItemEvents.emitEquip(w, clock++, 0, true, q);
		return this;
	}

	public DemoLog unequip( String uid, String name, int category ){
		ItemEvents.Equip q = new ItemEvents.Equip();
		q.uid = uid; q.where = iw(); q.state = state(name, category, false, 0, false);
		ItemEvents.emitEquip(w, clock++, 0, false, q);
		return this;
	}

	//the player learned what a kind of unidentified item is (public)
	public DemoLog classKnown( String label, String className, int category ){
		ItemEvents.ClassKnown c = new ItemEvents.ClassKnown();
		c.where = iw();
		c.category = category; c.label = label; c.className = className;
		ItemEvents.emitClassKnown(w, clock++, c);
		return this;
	}

	public DemoLog truthChanged( String uid, int newLevel, boolean newCursed ){
		ItemEvents.TruthChanged t = new ItemEvents.TruthChanged();
		t.uid = uid; t.where = iw(); t.newLevel = newLevel; t.newCursed = newCursed;
		ItemEvents.emitTruthChanged(w, clock, 0, t);
		return this;
	}

	private CombatEvents.Where cw(){
		CombatEvents.Where x = new CombatEvents.Where();
		x.depth = depth;
		return x;
	}

	//the hero attacks an enemy: ATTACK then its detail, as the recorder writes them
	public DemoLog heroAttacks( String enemyName, String enemyClass, boolean hit ){
		CombatEvents.Attack a = new CombatEvents.Attack();
		a.where = cw(); a.attacker.kind = CombatEvents.KIND_HERO; a.attacker.name = "you";
		a.defender.kind = CombatEvents.KIND_ENEMY; a.defender.name = enemyName; a.hit = hit;
		CombatEvents.emitAttack(w, clock++, a);
		CombatEvents.AttackDetail d = new CombatEvents.AttackDetail();
		d.where = cw(); d.attackerClass = "Hero"; d.defenderClass = enemyClass; d.hit = hit; d.publicEmitted = true;
		CombatEvents.emitAttackDetail(w, clock - 1, d);
		return this;
	}

	//the hero heals: one HEAL, and for an exact heal (telemetry 1.6) the hidden HEAL_DETAIL with the true source beside it
	public DemoLog heal( int amount, boolean exact, String source ){
		CombatEvents.Heal h = new CombatEvents.Heal();
		h.where = cw(); h.amount = amount; h.hpAfter = 10 + amount; h.hpMax = 30;
		CombatEvents.emitHeal(w, clock++, h, exact);
		if (exact){
			CombatEvents.HealDetail d = new CombatEvents.HealDetail();
			d.source = source; d.requested = amount; d.applied = amount;
			CombatEvents.emitHealDetail(w, clock - 1, d);
		}
		return this;
	}

	public DemoLog enemyAttacks( String enemyName, String enemyClass, boolean hit ){
		CombatEvents.Attack a = new CombatEvents.Attack();
		a.where = cw(); a.defender.kind = CombatEvents.KIND_HERO; a.defender.name = "you";
		a.attacker.kind = CombatEvents.KIND_ENEMY; a.attacker.name = enemyName; a.hit = hit;
		CombatEvents.emitAttack(w, clock++, a);
		CombatEvents.AttackDetail d = new CombatEvents.AttackDetail();
		d.where = cw(); d.attackerClass = enemyClass; d.defenderClass = "Hero"; d.hit = hit; d.publicEmitted = true;
		CombatEvents.emitAttackDetail(w, clock - 1, d);
		return this;
	}

	public DemoLog damageToHero( String enemyName, String enemyClass, int shown ){
		CombatEvents.Damage dm = new CombatEvents.Damage();
		dm.where = cw(); dm.target.kind = CombatEvents.KIND_HERO; dm.source.kind = CombatEvents.SRC_CHAR;
		dm.source.fighterKind = CombatEvents.KIND_ENEMY; dm.source.label = enemyName; dm.shown = shown;
		CombatEvents.emitDamage(w, clock++, dm);
		CombatEvents.DamageDetail d = new CombatEvents.DamageDetail();
		d.where = cw(); d.targetClass = "Hero"; d.srcClass = enemyClass; d.publicEmitted = true;
		CombatEvents.emitDamageDetail(w, clock - 1, d);
		return this;
	}

	public DemoLog damageFromHero( String enemyName, String enemyClass, int shown ){
		CombatEvents.Damage dm = new CombatEvents.Damage();
		dm.where = cw(); dm.target.kind = CombatEvents.KIND_ENEMY; dm.target.name = enemyName; dm.source.kind = CombatEvents.SRC_CHAR;
		dm.source.fighterKind = CombatEvents.KIND_HERO; dm.shown = shown;
		CombatEvents.emitDamage(w, clock++, dm);
		CombatEvents.DamageDetail d = new CombatEvents.DamageDetail();
		d.where = cw(); d.targetClass = enemyClass; d.srcClass = "Hero"; d.publicEmitted = true;
		CombatEvents.emitDamageDetail(w, clock - 1, d);
		return this;
	}

	public DemoLog heroKills( String enemyName, String enemyClass, int exp ){
		CombatEvents.Kill k = new CombatEvents.Kill();
		k.where = cw(); k.victim.kind = CombatEvents.KIND_ENEMY; k.victim.name = enemyName;
		k.killer.kind = CombatEvents.SRC_CHAR; k.killer.fighterKind = CombatEvents.KIND_HERO;
		CombatEvents.emitKill(w, clock++, k);
		CombatEvents.KillDetail d = new CombatEvents.KillDetail();
		d.where = cw(); d.victimClass = enemyClass; d.expValue = exp; d.publicEmitted = true;
		CombatEvents.emitKillDetail(w, clock - 1, d);
		return this;
	}

	public DemoLog mobGenerated( String mobClass ){
		GenerationEvents.MobGenerated m = new GenerationEvents.MobGenerated();
		m.mobClass = mobClass; m.where = new GenerationEvents.Where();
		m.where.depth = depth;
		GenerationEvents.emitMobGenerated(w, clock, m);
		return this;
	}

	public DemoLog heroDies( String cause, int kind ){
		CombatEvents.HeroDeath d = new CombatEvents.HeroDeath();
		d.where = cw(); d.cause.label = cause; d.cause.kind = kind;
		if (kind == CombatEvents.SRC_CHAR) d.cause.fighterKind = CombatEvents.KIND_ENEMY;
		CombatEvents.emitHeroDeath(w, clock++, d);
		return this;
	}

	public DemoLog revived(){
		CombatEvents.HeroRevived r = new CombatEvents.HeroRevived();
		r.where = cw();
		CombatEvents.emitHeroRevived(w, clock++, r);
		return this;
	}

	public DemoLog rulesetChange( String from, String to ){
		LifecycleEvents.RulesetChange c = new LifecycleEvents.RulesetChange();
		c.oldId = from; c.newId = to; c.depth = depth;
		LifecycleEvents.emitRulesetChange(w, clock++, c);
		return this;
	}

	//a hidden event that happens LATER than anything the player has seen (the clock itself does not move)
	public DemoLog hiddenAt( long ahead ){
		GenerationEvents.MobGenerated m = new GenerationEvents.MobGenerated();
		m.mobClass = "actors.mobs.Goo"; m.where = new GenerationEvents.Where();
		m.where.depth = depth;
		GenerationEvents.emitMobGenerated(w, clock + ahead, m);
		return this;
	}

	public DemoLog gap(){
		w.event(EventTypes.GAP).clock(clock++).unsigned(EventTypes.GAP_DROPPED_EVENTS).unsigned(3).emit();
		return this;
	}

	public DemoLog rankingLink( String id ){
		LegacyEvents.RankingLink l = new LegacyEvents.RankingLink();
		l.gameId = id;
		LegacyEvents.emitRankingLink(w, clock++, l);
		return this;
	}

	public DemoLog end( int outcome, String cause, int deepest, int level, long score ){
		LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
		e.outcome = outcome; e.cause = cause; e.depth = depth; e.deepest = deepest; e.heroLevel = level; e.score = score; e.turns = clock;
		LifecycleEvents.emitRunEnd(w, clock++, e);
		return this;
	}

	//closes the log; sealed only if the run ended
	public File close( boolean seal ){
		w.close(seal);
		return file;
	}
}
