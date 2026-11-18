package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.io.File;

//Writes small, realistic run logs for the dataset tests, through the real writer. Like the game, hidden (INTERNAL) events happen
//on the turn they belong to and do not move the clock: only things the player could see advance it.
final class SyntheticLog {

	final RunLogWriter w;
	final File file;
	long clock = 1;
	int depth = 1;

	SyntheticLog( File root, String logId, TelemetryVersion v, long createdAt ){
		file = new File(new File(root, "runs"), logId + ".apdlog");
		w = RunLogWriter.create(file, v, logId, "test", createdAt);
	}

	SyntheticLog start( String runId, int segment, String heroClass, String ruleset, int challenges, int exotic, boolean partial ){
		LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
		s.runId = runId; s.segment = segment; s.heroClass = heroClass; s.rulesetId = ruleset; s.challenges = challenges; s.exoticChance = exotic;
		s.seed = 4242; s.apdVersionName = "4.0.8-apd"; s.realTimeMillis = 1_700_000_000_000L;
		LifecycleEvents.emitRunStart(w, clock++, partial, s);
		return this;
	}

	SyntheticLog enter( int d ){
		depth = d;
		LifecycleEvents.LevelEnter l = new LifecycleEvents.LevelEnter();
		l.depth = d;
		LifecycleEvents.emitLevelEnter(w, clock++, l);
		return this;
	}

	//leaves the current floor (the game writes this when the hero goes up or down, saves aside)
	SyntheticLog leave(){
		LifecycleEvents.LevelExit x = new LifecycleEvents.LevelExit();
		x.depth = depth;
		x.reason = LifecycleEvents.EXIT_LEAVING;
		LifecycleEvents.emitLevelExit(w, clock++, x);
		return this;
	}

	//one generic fact observation of a registered kind, on the next clock tick
	SyntheticLog fact( com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind k, String subject, long... values ){
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents.emit(w, k.eventType(), clock,
				new com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents.Observation(k.key, subject, "", clock, values, new String[k.tags().size()]));
		clock++;
		return this;
	}

	//many observations of a series kind, one per tick, as one event
	SyntheticLog factSeries( com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind k, String subject, long[][] rows ){
		long[] clocks = new long[rows.length];
		for (int i = 0; i < rows.length; i++) clocks[i] = clock++;
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents.emitSeries(w, k.eventType(), k.key, subject, "", new String[k.tags().size()],
				k.values().size(), clocks, rows, rows.length);
		return this;
	}

	SyntheticLog tick( long n ){
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

	SyntheticLog registered( String uid, String trueClass, int trueLevel, boolean cursed, int origin ){
		ItemEvents.Registered r = new ItemEvents.Registered();
		r.uid = uid; r.trueClass = trueClass; r.trueLevel = trueLevel; r.cursed = cursed; r.origin = origin; r.where = iw();
		ItemEvents.emitRegistered(w, clock, 0, r);
		return this;
	}

	SyntheticLog generated( String uid, int container, String room ){
		GenerationEvents.ItemGenerated g = new GenerationEvents.ItemGenerated();
		g.uid = uid; g.container = container; g.room = room; g.drawKind = GenerationEvents.DRAW_CATEGORY; g.requestedCategory = "WEAPON";
		g.where = new GenerationEvents.Where();
		g.where.depth = depth;
		GenerationEvents.emitItemGenerated(w, clock, g);
		return this;
	}

	SyntheticLog seen( String uid, String name, int category ){
		ItemEvents.Seen s = new ItemEvents.Seen();
		s.uid = uid; s.where = iw(); s.state = state(name, category, false, 0, false);
		ItemEvents.emitSeen(w, clock++, 0, s);
		return this;
	}

	SyntheticLog acquired( String uid, String name, int category, boolean levelKnown, int level ){
		ItemEvents.Acquired a = new ItemEvents.Acquired();
		a.uid = uid; a.where = iw(); a.state = state(name, category, levelKnown, level, levelKnown);
		ItemEvents.emitAcquired(w, clock++, 0, a);
		return this;
	}

	SyntheticLog equip( String uid, String name, int category ){
		ItemEvents.Equip q = new ItemEvents.Equip();
		q.uid = uid; q.where = iw(); q.state = state(name, category, false, 0, false);
		ItemEvents.emitEquip(w, clock++, 0, true, q);
		return this;
	}

	SyntheticLog unequip( String uid, String name, int category ){
		ItemEvents.Equip q = new ItemEvents.Equip();
		q.uid = uid; q.where = iw(); q.state = state(name, category, false, 0, false);
		ItemEvents.emitEquip(w, clock++, 0, false, q);
		return this;
	}

	SyntheticLog classKnown( String label, String className, int category ){
		ItemEvents.ClassKnown c = new ItemEvents.ClassKnown();
		c.where = iw();
		c.category = category; c.label = label; c.className = className;
		ItemEvents.emitClassKnown(w, clock++, c);
		return this;
	}

	SyntheticLog truthChanged( String uid, int newLevel, boolean newCursed ){
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
	SyntheticLog heroAttacks( String enemyName, String enemyClass, boolean hit ){
		CombatEvents.Attack a = new CombatEvents.Attack();
		a.where = cw(); a.attacker.kind = CombatEvents.KIND_HERO; a.attacker.name = "you";
		a.defender.kind = CombatEvents.KIND_ENEMY; a.defender.name = enemyName; a.hit = hit;
		CombatEvents.emitAttack(w, clock++, a);
		CombatEvents.AttackDetail d = new CombatEvents.AttackDetail();
		d.where = cw(); d.attackerClass = "Hero"; d.defenderClass = enemyClass; d.hit = hit; d.publicEmitted = true;
		CombatEvents.emitAttackDetail(w, clock - 1, d);
		return this;
	}

	SyntheticLog enemyAttacks( String enemyName, String enemyClass, boolean hit ){
		CombatEvents.Attack a = new CombatEvents.Attack();
		a.where = cw(); a.defender.kind = CombatEvents.KIND_HERO; a.defender.name = "you";
		a.attacker.kind = CombatEvents.KIND_ENEMY; a.attacker.name = enemyName; a.hit = hit;
		CombatEvents.emitAttack(w, clock++, a);
		CombatEvents.AttackDetail d = new CombatEvents.AttackDetail();
		d.where = cw(); d.attackerClass = enemyClass; d.defenderClass = "Hero"; d.hit = hit; d.publicEmitted = true;
		CombatEvents.emitAttackDetail(w, clock - 1, d);
		return this;
	}

	SyntheticLog damageToHero( String enemyName, String enemyClass, int shown ){
		CombatEvents.Damage dm = new CombatEvents.Damage();
		dm.where = cw(); dm.target.kind = CombatEvents.KIND_HERO; dm.source.kind = CombatEvents.SRC_CHAR;
		dm.source.fighterKind = CombatEvents.KIND_ENEMY; dm.source.label = enemyName; dm.shown = shown;
		CombatEvents.emitDamage(w, clock++, dm);
		CombatEvents.DamageDetail d = new CombatEvents.DamageDetail();
		d.where = cw(); d.targetClass = "Hero"; d.srcClass = enemyClass; d.publicEmitted = true;
		CombatEvents.emitDamageDetail(w, clock - 1, d);
		return this;
	}

	SyntheticLog damageFromHero( String enemyName, String enemyClass, int shown ){
		CombatEvents.Damage dm = new CombatEvents.Damage();
		dm.where = cw(); dm.target.kind = CombatEvents.KIND_ENEMY; dm.target.name = enemyName; dm.source.kind = CombatEvents.SRC_CHAR;
		dm.source.fighterKind = CombatEvents.KIND_HERO; dm.shown = shown;
		CombatEvents.emitDamage(w, clock++, dm);
		CombatEvents.DamageDetail d = new CombatEvents.DamageDetail();
		d.where = cw(); d.targetClass = enemyClass; d.srcClass = "Hero"; d.publicEmitted = true;
		CombatEvents.emitDamageDetail(w, clock - 1, d);
		return this;
	}

	SyntheticLog heroKills( String enemyName, String enemyClass, int exp ){
		CombatEvents.Kill k = new CombatEvents.Kill();
		k.where = cw(); k.victim.kind = CombatEvents.KIND_ENEMY; k.victim.name = enemyName;
		k.killer.kind = CombatEvents.SRC_CHAR; k.killer.fighterKind = CombatEvents.KIND_HERO;
		CombatEvents.emitKill(w, clock++, k);
		CombatEvents.KillDetail d = new CombatEvents.KillDetail();
		d.where = cw(); d.victimClass = enemyClass; d.expValue = exp; d.publicEmitted = true;
		CombatEvents.emitKillDetail(w, clock - 1, d);
		return this;
	}

	//the hero heals: one HEAL, and for an exact heal (telemetry 1.6) the hidden HEAL_DETAIL with the true source beside it
	SyntheticLog heal( int amount, boolean exact, String source ){
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

	SyntheticLog mobGenerated( String mobClass ){
		GenerationEvents.MobGenerated m = new GenerationEvents.MobGenerated();
		m.mobClass = mobClass; m.where = new GenerationEvents.Where();
		m.where.depth = depth;
		GenerationEvents.emitMobGenerated(w, clock, m);
		return this;
	}

	SyntheticLog heroDies( String cause, int kind ){
		CombatEvents.HeroDeath d = new CombatEvents.HeroDeath();
		d.where = cw(); d.cause.label = cause; d.cause.kind = kind;
		if (kind == CombatEvents.SRC_CHAR) d.cause.fighterKind = CombatEvents.KIND_ENEMY;
		CombatEvents.emitHeroDeath(w, clock++, d);
		return this;
	}

	SyntheticLog revived(){
		CombatEvents.HeroRevived r = new CombatEvents.HeroRevived();
		r.where = cw();
		CombatEvents.emitHeroRevived(w, clock++, r);
		return this;
	}

	SyntheticLog rulesetChange( String from, String to ){
		LifecycleEvents.RulesetChange c = new LifecycleEvents.RulesetChange();
		c.oldId = from; c.newId = to; c.depth = depth;
		LifecycleEvents.emitRulesetChange(w, clock++, c);
		return this;
	}

	//a hidden event that happens LATER than anything the player has seen (the clock itself does not move)
	SyntheticLog hiddenAt( long ahead ){
		GenerationEvents.MobGenerated m = new GenerationEvents.MobGenerated();
		m.mobClass = "actors.mobs.Goo"; m.where = new GenerationEvents.Where();
		m.where.depth = depth;
		GenerationEvents.emitMobGenerated(w, clock + ahead, m);
		return this;
	}

	SyntheticLog gap(){
		w.event(EventTypes.GAP).clock(clock++).unsigned(EventTypes.GAP_DROPPED_EVENTS).unsigned(3).emit();
		return this;
	}

	SyntheticLog rankingLink( String id ){
		LegacyEvents.RankingLink l = new LegacyEvents.RankingLink();
		l.gameId = id;
		LegacyEvents.emitRankingLink(w, clock++, l);
		return this;
	}

	SyntheticLog end( int outcome, String cause, int deepest, int level, long score ){
		LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
		e.outcome = outcome; e.cause = cause; e.depth = depth; e.deepest = deepest; e.heroLevel = level; e.score = score; e.turns = clock;
		LifecycleEvents.emitRunEnd(w, clock++, e);
		return this;
	}

	//closes the log; sealed only if the run ended
	File close( boolean seal ){
		w.close(seal);
		return file;
	}
}
