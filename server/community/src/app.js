// APD Community server: one Cloudflare Worker, D1 database.
// Everything is JSON over HTTPS. No private messages exist. The client is untrusted: every rule is enforced here.
//
// env.DB      D1 database (or the Node test adapter with the same prepare/bind/first/all/run API)
// env.MAILER  { send(to, subject, text) } for verification codes
// env.GOOGLE  { verify(idToken) -> {sub, email} | null } checks a Google ID token
// env.ADMIN_EMAILS  comma-separated emails that get the admin role (Chris)
// env.now     optional clock () => ms, for tests

import { newId, newCode, sha256, hashPassword, verifyPassword, randomHex, constantTimeEqual } from './crypto.js';
import { clean } from './filter.js';
import { googleVerifier } from './google.js';
import { cloudflareMailer } from './mailer.js';
import { qualifiers, weekOneSeeds, bracket, ADVANCE } from './tournament.js';

const SESSION_MS = 30 * 24 * 3600 * 1000;
const CODE_MS = 15 * 60 * 1000;
const MAX_CODE_ATTEMPTS = 5;
// Anti-spam caps per account (Claude's call, Nov 18; Chris can change them): how many of each in a rolling window.
const RATE = {
  submissions: { table: 'submissions', col: 'author_id', max: 5, ms: 24 * 3600 * 1000 },
  comments: { table: 'comments', col: 'author_id', max: 10, ms: 10 * 60 * 1000 },
  reports: { table: 'reports', col: 'reporter_id', max: 30, ms: 24 * 3600 * 1000 },
};

async function rateLimit(env, account, kind) {
  const r = RATE[kind];
  const row = await env.DB.prepare(`SELECT COUNT(*) AS n FROM ${r.table} WHERE ${r.col} = ? AND created_at > ?`)
    .bind(account.id, now(env) - r.ms).first();
  if (row.n >= r.max) fail(429, `too_many_${kind}`);
}

// Guessing and spam limits (Claude's call, Nov 18): failed sign-ins per email and per network address, sign-ups and
// code requests per network address, counted over a window.
const GUARD = {
  login_email: { max: 10, ms: 15 * 60 * 1000 },
  login_ip: { max: 30, ms: 15 * 60 * 1000 },
  register_ip: { max: 5, ms: 60 * 60 * 1000 },
  code_ip: { max: 10, ms: 60 * 60 * 1000 },
};

function ipOf(req) { return req.headers.get('cf-connecting-ip') || 'unknown'; }

async function guarded(env, kind, who) {
  const g = GUARD[kind];
  const row = await env.DB.prepare('SELECT COUNT(*) AS n FROM throttle WHERE key = ? AND at > ?').bind(`${kind}:${who}`, now(env) - g.ms).first();
  if (row.n >= g.max) fail(429, 'slow_down');
}

async function strike(env, kind, who) {
  await env.DB.prepare('INSERT INTO throttle (key, at) VALUES (?, ?)').bind(`${kind}:${who}`, now(env)).run();
  if (Math.random() < 0.02) await env.DB.prepare('DELETE FROM throttle WHERE at < ?').bind(now(env) - 24 * 3600 * 1000).run();
}

const LIMITS = { name: 24, title: 80, body: 4000, comment: 2000, reason: 200, password: 8 };

class HttpError extends Error {
  constructor(status, code) { super(code); this.status = status; this.code = code; }
}
const fail = (status, code) => { throw new HttpError(status, code); };

function json(status, body) {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
}

async function body(request) {
  try { return await request.json(); } catch { fail(400, 'bad_json'); }
}

function text(v, max, field) {
  if (typeof v !== 'string') fail(400, `missing_${field}`);
  const t = v.trim();
  if (!t) fail(400, `missing_${field}`);
  if (t.length > max) fail(400, `too_long_${field}`);
  return t;
}

function filtered(v, max, field) {
  const t = clean(text(v, max, field));
  if (t === null) fail(422, 'not_allowed_words');
  return t;
}

function isAdminEmail(env, email) {
  return !!email && String(env.ADMIN_EMAILS || '').split(',').map((s) => s.trim().toLowerCase()).includes(email);
}

// Chris's account becomes admin whenever it signs in, even if it was made before his email was configured.
async function promoteIfAdmin(env, accountId) {
  const a = await env.DB.prepare('SELECT email, role FROM accounts WHERE id = ?').bind(accountId).first();
  if (a && a.role !== 'admin' && isAdminEmail(env, a.email)) {
    await env.DB.prepare("UPDATE accounts SET role = 'admin' WHERE id = ?").bind(accountId).run();
  }
}

async function startSession(env, accountId) {
  await promoteIfAdmin(env, accountId);
  const token = randomHex(32);
  await env.DB.prepare('INSERT INTO sessions (token_hash, account_id, expires_at) VALUES (?, ?, ?)')
    .bind(await sha256(token), accountId, now(env) + SESSION_MS).run();
  return token;
}

const now = (env) => (env.now ? env.now() : Date.now());

async function currentAccount(env, request) {
  const auth = request.headers.get('authorization') || '';
  if (!auth.startsWith('Bearer ')) return null;
  const row = await env.DB.prepare(
    'SELECT a.* FROM sessions s JOIN accounts a ON a.id = s.account_id WHERE s.token_hash = ? AND s.expires_at > ?')
    .bind(await sha256(auth.slice(7)), now(env)).first();
  return row || null;
}

async function need(env, request) {
  const a = await currentAccount(env, request);
  if (!a) fail(401, 'signed_out');
  if (a.banned) fail(403, 'banned');
  return a;
}

// Posting, commenting, voting and reporting need a verified, 16+, unbanned account.
async function needMember(env, request) {
  const a = await need(env, request);
  if (!a.email_verified) fail(403, 'email_not_verified');
  if (!a.age_ok) fail(403, 'age_gate');
  return a;
}

async function needAdmin(env, request) {
  const a = await need(env, request);
  if (a.role !== 'admin') fail(403, 'not_admin');
  return a;
}

async function sendCode(env, account) {
  const code = newCode();
  await env.DB.prepare(
    'INSERT INTO verification_codes (account_id, code_hash, expires_at, attempts) VALUES (?, ?, ?, 0) ' +
    'ON CONFLICT(account_id) DO UPDATE SET code_hash = excluded.code_hash, expires_at = excluded.expires_at, attempts = 0')
    .bind(account.id, await sha256(code), now(env) + CODE_MS).run();
  await env.MAILER.send(account.email, 'Your ADHD Pixel Dungeon code',
    `Your ADHD Pixel Dungeon Community code is ${code}. It expires in 15 minutes.`);
}

function publicAccount(a) {
  return { id: a.id, displayName: a.display_name, emailVerified: !!a.email_verified, ageOk: !!a.age_ok, admin: a.role === 'admin' };
}

async function blockedIds(env, viewer) {
  if (!viewer) return new Set();
  const { results } = await env.DB.prepare('SELECT blocked_id FROM blocks WHERE blocker_id = ?').bind(viewer.id).all();
  return new Set(results.map((r) => r.blocked_id));
}

async function audit(env, admin, action, target) {
  await env.DB.prepare('INSERT INTO audit (admin_id, action, target, created_at) VALUES (?, ?, ?, ?)')
    .bind(admin.id, action, target, now(env)).run();
}

const routes = [];
const route = (method, pattern, fn) => routes.push({ method, re: new RegExp(`^${pattern}$`), fn });

// ---- accounts ----

route('POST', '/v1/auth/register', async (env, req) => {
  await guarded(env, 'register_ip', ipOf(req));
  await strike(env, 'register_ip', ipOf(req));
  const b = await body(req);
  const email = text(b.email, 254, 'email').toLowerCase();
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) fail(400, 'bad_email');
  if (typeof b.password !== 'string' || b.password.length < LIMITS.password) fail(400, 'weak_password');
  if (b.password.length > 200) fail(400, 'too_long_password');
  const name = filtered(b.displayName, LIMITS.name, 'displayName');
  const existing = await env.DB.prepare('SELECT * FROM accounts WHERE email = ?').bind(email).first();
  if (existing) {
    if (existing.email_verified) fail(409, 'email_taken');
    // Unverified: let the owner of the inbox start over (new password, new code).
    await env.DB.prepare('UPDATE accounts SET password_hash = ?, display_name = ? WHERE id = ?')
      .bind(await hashPassword(b.password), name, existing.id).run();
    await sendCode(env, existing);
    return json(201, { accountId: existing.id });
  }
  const a = { id: newId(), email };
  await env.DB.prepare(
    'INSERT INTO accounts (id, display_name, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(a.id, name, email, await hashPassword(b.password), isAdminEmail(env, email) ? 'admin' : 'player', now(env)).run();
  await sendCode(env, a);
  return json(201, { accountId: a.id });
});

route('POST', '/v1/auth/resend', async (env, req) => {
  await guarded(env, 'code_ip', ipOf(req));
  await strike(env, 'code_ip', ipOf(req));
  const b = await body(req);
  const email = text(b.email, 254, 'email').toLowerCase();
  const a = await env.DB.prepare('SELECT * FROM accounts WHERE email = ? AND email_verified = 0').bind(email).first();
  if (a) await sendCode(env, a);
  return json(200, { ok: true }); // same answer either way: doesn't reveal which emails exist
});

route('POST', '/v1/auth/verify', async (env, req) => {
  const b = await body(req);
  const email = text(b.email, 254, 'email').toLowerCase();
  const code = text(b.code, 6, 'code');
  const a = await env.DB.prepare('SELECT * FROM accounts WHERE email = ?').bind(email).first();
  const v = a && await env.DB.prepare('SELECT * FROM verification_codes WHERE account_id = ?').bind(a.id).first();
  if (!v) fail(400, 'bad_code');
  if (v.expires_at < now(env) || v.attempts >= MAX_CODE_ATTEMPTS) fail(400, 'code_expired');
  if (!constantTimeEqual(await sha256(code), v.code_hash)) {
    await env.DB.prepare('UPDATE verification_codes SET attempts = attempts + 1 WHERE account_id = ?').bind(a.id).run();
    fail(400, 'bad_code');
  }
  await env.DB.prepare('UPDATE accounts SET email_verified = 1 WHERE id = ?').bind(a.id).run();
  await env.DB.prepare('DELETE FROM verification_codes WHERE account_id = ?').bind(a.id).run();
  return json(200, { token: await startSession(env, a.id) });
});

route('POST', '/v1/auth/login', async (env, req) => {
  const b = await body(req);
  const email = text(b.email, 254, 'email').toLowerCase();
  await guarded(env, 'login_email', email);
  await guarded(env, 'login_ip', ipOf(req));
  const a = await env.DB.prepare('SELECT * FROM accounts WHERE email = ?').bind(email).first();
  if (!a || !(await verifyPassword(String(b.password || ''), a.password_hash))) {
    await strike(env, 'login_email', email);
    await strike(env, 'login_ip', ipOf(req));
    fail(401, 'wrong_login');
  }
  if (a.banned) fail(403, 'banned');
  if (!a.email_verified) fail(403, 'email_not_verified');
  return json(200, { token: await startSession(env, a.id) });
});

route('POST', '/v1/auth/google', async (env, req) => {
  const b = await body(req);
  const who = await env.GOOGLE.verify(text(b.idToken, 4096, 'idToken'));
  if (!who || !who.sub) fail(401, 'bad_google_token');
  let a = await env.DB.prepare('SELECT * FROM accounts WHERE google_sub = ?').bind(who.sub).first();
  if (!a) {
    const name = clean(String(b.displayName || '').trim().slice(0, LIMITS.name)) || 'Adventurer';
    a = { id: newId() };
    const email = who.email ? String(who.email).toLowerCase() : null;
    const taken = email && await env.DB.prepare('SELECT id FROM accounts WHERE email = ?').bind(email).first();
    // Never merge into an existing email account silently: a Google account gets its own account.
    await env.DB.prepare(
      'INSERT INTO accounts (id, display_name, email, google_sub, email_verified, role, created_at) VALUES (?, ?, ?, ?, 1, ?, ?)')
      .bind(a.id, name, taken ? null : email, who.sub, isAdminEmail(env, email) ? 'admin' : 'player', now(env)).run();
    a = await env.DB.prepare('SELECT * FROM accounts WHERE id = ?').bind(a.id).first();
  }
  if (a.banned) fail(403, 'banned');
  return json(200, { token: await startSession(env, a.id) });
});

route('POST', '/v1/auth/logout', async (env, req) => {
  const auth = req.headers.get('authorization') || '';
  if (auth.startsWith('Bearer ')) {
    await env.DB.prepare('DELETE FROM sessions WHERE token_hash = ?').bind(await sha256(auth.slice(7))).run();
  }
  return json(200, { ok: true });
});

route('GET', '/v1/me', async (env, req) => json(200, publicAccount(await need(env, req))));

// The device decides the age gate (Play Age Signals, else birth month/year). The server stores only that it passed.
// A failed check is never sent: the device locks Community itself, and nothing about the age is stored here.
route('POST', '/v1/account/age', async (env, req) => {
  const a = await need(env, req);
  const b = await body(req);
  if (b.passed !== true) fail(400, 'only_pass_is_recorded');
  await env.DB.prepare('UPDATE accounts SET age_ok = 1 WHERE id = ?').bind(a.id).run();
  return json(200, { ok: true });
});

// Deletes the account and everything it posted, voted or reported. Replies by others under deleted comments go too.
async function deleteAccount(env, a) {
  // My comments that others replied to stay as empty placeholders owned by [deleted], so their replies aren't lost.
  // Everything else I wrote, voted, reported or blocked goes with the account.
  await env.DB.prepare(
    `UPDATE comments SET author_id = 'deleted', body = '', status = 'removed'
      WHERE author_id = ? AND id IN (SELECT parent_id FROM comments WHERE parent_id IS NOT NULL AND author_id != ?)`)
    .bind(a.id, a.id).run();
  await env.DB.prepare('DELETE FROM accounts WHERE id = ?').bind(a.id).run();
}

route('DELETE', '/v1/account', async (env, req) => {
  await deleteAccount(env, await need(env, req));
  return json(200, { deleted: true });
});

// ---- seasons ----

const DAY = 24 * 3600 * 1000;

export function phaseOf(season, t) {
  if (!season || t < season.starts_at) return 'none';
  if (t < season.voting_at) return 'submissions';
  if (t < season.tournament_at) return 'voting';
  if (t < season.ends_at) return 'tournament';
  return 'finished';
}

// The season players see: the latest one that has started.
async function currentSeason(env) {
  return env.DB.prepare('SELECT * FROM seasons WHERE starts_at <= ? ORDER BY starts_at DESC LIMIT 1').bind(now(env)).first();
}

function publicSeason(s, t) {
  return s && { id: s.id, title: s.title, phase: phaseOf(s, t), startsAt: s.starts_at, votingAt: s.voting_at,
    tournamentAt: s.tournament_at, endsAt: s.ends_at };
}

route('GET', '/v1/season', async (env) => json(200, { season: publicSeason(await currentSeason(env), now(env)) }));

// Chris opens a season. Default: 30 days per phase from the given start. Phases may not overlap another season.
route('POST', '/v1/admin/seasons', async (env, req) => {
  const admin = await needAdmin(env, req);
  const b = await body(req);
  const title = filtered(b.title, LIMITS.title, 'title');
  const start = Number.isFinite(b.startsAt) ? b.startsAt : now(env);
  const len = (k, d) => (Number.isFinite(b[k]) && b[k] > 0 ? b[k] : d);
  const voting = start + len('submissionMs', 30 * DAY);
  const tournament = voting + len('votingMs', 30 * DAY);
  const ends = tournament + len('tournamentMs', 28 * DAY);
  const clash = await env.DB.prepare('SELECT id FROM seasons WHERE starts_at < ? AND ends_at > ?').bind(ends, start).first();
  if (clash) fail(409, 'season_overlaps');
  const id = newId();
  await env.DB.prepare('INSERT INTO seasons (id, title, starts_at, voting_at, tournament_at, ends_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)')
    .bind(id, title, start, voting, tournament, ends, now(env)).run();
  await audit(env, admin, 'season', id);
  return json(201, { id });
});

// ---- account deletion on the web (Google Play requires a link that works without the app) ----

const PAGE = (msg) => `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Delete your ADHD Pixel Dungeon Community account</title>
<body style="font-family:system-ui,sans-serif;max-width:32rem;margin:2rem auto;padding:0 1rem;line-height:1.5">
<h1 style="font-size:1.4rem">Delete your ADHD Pixel Dungeon Community account</h1>
${msg}
<p>Deleting removes your account, your ideas, comments, votes, reports and blocks. Comments other players replied to
are kept as empty "[deleted]" placeholders so their replies still make sense. This can't be undone. Your game saves
and run history stay on your phone and are not affected.</p>
<form method="post" action="/delete">
<p><label>Email<br><input name="email" type="email" required style="width:100%"></label></p>
<p><label>Password<br><input name="password" type="password" required style="width:100%"></label></p>
<p><button type="submit">Delete my account</button></p>
</form>
<p>Signed up with Google? Open Community in the game, go to Account, then Delete my account.</p>
</body></html>`;

const html = (status, body) => new Response(body, { status, headers: { 'content-type': 'text/html; charset=utf-8' } });

route('GET', '/delete', async () => html(200, PAGE('')));

route('POST', '/delete', async (env, req) => {
  const form = await req.formData().catch(() => null);
  const email = String(form?.get('email') || '').trim().toLowerCase();
  const password = String(form?.get('password') || '');
  try {
    await guarded(env, 'login_email', email);
    await guarded(env, 'login_ip', ipOf(req));
  } catch (e) {
    return html(429, PAGE('<p style="color:#b00"><b>Too many tries. Wait a few minutes and try again.</b></p>'));
  }
  const a = email && await env.DB.prepare('SELECT * FROM accounts WHERE email = ?').bind(email).first();
  if (!a || !(await verifyPassword(password, a.password_hash))) {
    await strike(env, 'login_email', email);
    await strike(env, 'login_ip', ipOf(req));
    return html(401, PAGE('<p style="color:#b00"><b>That email and password don\'t match.</b></p>'));
  }
  await deleteAccount(env, a);
  return html(200, '<!doctype html><meta charset="utf-8"><body style="font-family:system-ui,sans-serif;max-width:32rem;margin:2rem auto"><h1>Your account is deleted.</h1><p>Everything you posted in ADHD Pixel Dungeon Community is gone.</p></body>');
});

// ---- submissions and comments ----

route('GET', '/v1/submissions', async (env, req) => {
  const viewer = await currentAccount(env, req);
  const blocked = await blockedIds(env, viewer);
  const { results } = await env.DB.prepare(
    `SELECT s.id, s.title, s.created_at, s.author_id, a.display_name,
            (SELECT COUNT(*) FROM comments c WHERE c.submission_id = s.id AND c.status = 'visible') AS comments
       FROM submissions s JOIN accounts a ON a.id = s.author_id
      WHERE s.status = 'visible' AND a.banned = 0 AND s.season_id = ? ORDER BY s.created_at DESC LIMIT 500`)
    .bind(((await currentSeason(env)) || {}).id ?? '').all();
  return json(200, { submissions: results.filter((s) => !blocked.has(s.author_id)).map((s) => ({
    id: s.id, title: s.title, author: { id: s.author_id, displayName: s.display_name }, createdAt: s.created_at, comments: s.comments,
  })) });
});

route('POST', '/v1/submissions', async (env, req) => {
  const a = await needMember(env, req);
  const b = await body(req);
  const season = await currentSeason(env);
  if (phaseOf(season, now(env)) !== 'submissions') fail(409, 'submissions_closed');
  await rateLimit(env, a, 'submissions');
  const id = newId();
  await env.DB.prepare('INSERT INTO submissions (id, author_id, title, body, season_id, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(id, a.id, filtered(b.title, LIMITS.title, 'title'), filtered(b.body, LIMITS.body, 'body'), season.id, now(env)).run();
  return json(201, { id });
});

route('GET', '/v1/submissions/([0-9a-f]+)', async (env, req, [id]) => {
  const viewer = await currentAccount(env, req);
  const blocked = await blockedIds(env, viewer);
  const s = await env.DB.prepare(
    `SELECT s.*, a.display_name FROM submissions s JOIN accounts a ON a.id = s.author_id
      WHERE s.id = ? AND s.status = 'visible' AND a.banned = 0`).bind(id).first();
  if (!s || blocked.has(s.author_id)) fail(404, 'not_found');
  const { results } = await env.DB.prepare(
    `SELECT c.id, c.parent_id, c.body, c.status, c.created_at, c.author_id, a.display_name, a.banned
       FROM comments c JOIN accounts a ON a.id = c.author_id WHERE c.submission_id = ? ORDER BY c.created_at`).bind(id).all();
  // Threaded: a hidden/removed/blocked comment keeps its place as a placeholder so replies under it still make sense.
  const comments = results.map((c) => {
    const gone = c.status !== 'visible' || c.banned || blocked.has(c.author_id);
    return { id: c.id, parentId: c.parent_id, createdAt: c.created_at,
      body: gone ? null : c.body, author: gone ? null : { id: c.author_id, displayName: c.display_name } };
  });
  return json(200, { id: s.id, title: s.title, body: s.body, createdAt: s.created_at,
    author: { id: s.author_id, displayName: s.display_name }, comments });
});

route('POST', '/v1/submissions/([0-9a-f]+)/comments', async (env, req, [id]) => {
  const a = await needMember(env, req);
  const b = await body(req);
  const s = await env.DB.prepare("SELECT id FROM submissions WHERE id = ? AND status = 'visible'").bind(id).first();
  if (!s) fail(404, 'not_found');
  await rateLimit(env, a, 'comments');
  let parent = null;
  if (b.parentId != null) {
    parent = await env.DB.prepare("SELECT id FROM comments WHERE id = ? AND submission_id = ?").bind(String(b.parentId), id).first();
    if (!parent) fail(400, 'bad_parent');
  }
  const cid = newId();
  await env.DB.prepare('INSERT INTO comments (id, submission_id, parent_id, author_id, body, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(cid, id, parent ? parent.id : null, a.id, filtered(b.body, LIMITS.comment, 'body'), now(env)).run();
  return json(201, { id: cid });
});

// ---- voting and the tournament ----

function tournamentWeek(season, t) {
  const week = Math.floor((t - season.tournament_at) / ((season.ends_at - season.tournament_at) / 4)) + 1;
  return Math.max(1, Math.min(4, week));
}

// Everything the results depend on, read fresh. Only visible submissions by unbanned authors, and only votes by
// unbanned accounts, count.
async function seasonState(env, season) {
  const t = now(env);
  const { results: entries } = await env.DB.prepare(
    `SELECT s.id, s.title, s.created_at, s.author_id,
            (SELECT COUNT(*) FROM upvotes u JOIN accounts v ON v.id = u.account_id
              WHERE u.submission_id = s.id AND v.banned = 0) AS score
       FROM submissions s JOIN accounts a ON a.id = s.author_id
      WHERE s.season_id = ? AND s.status = 'visible' AND a.banned = 0`).bind(season.id).all();
  const phase = phaseOf(season, t);
  const state = { season, phase, entries, t };
  if (phase !== 'tournament' && phase !== 'finished') return state;
  state.week = phase === 'finished' ? 5 : tournamentWeek(season, t);
  state.qualified = qualifiers(entries);
  const { results: tv } = await env.DB.prepare(
    `SELECT tv.week, tv.slot, tv.submission_id, COUNT(*) AS n FROM tournament_votes tv JOIN accounts v ON v.id = tv.account_id
      WHERE tv.season_id = ? AND v.banned = 0 GROUP BY tv.week, tv.slot, tv.submission_id`).bind(season.id).all();
  const picks = new Map();
  const byWeek = new Map();
  for (const r of tv) {
    if (r.week === 1) { picks.set(r.submission_id, (picks.get(r.submission_id) || 0) + r.n); continue; }
    if (!byWeek.has(r.week)) byWeek.set(r.week, new Map());
    const w = byWeek.get(r.week);
    if (!w.has(r.slot)) w.set(r.slot, new Map());
    w.get(r.slot).set(r.submission_id, r.n);
  }
  state.picks = picks;
  // The seeds only exist once week 1 is over; until then week 1 standings are live.
  state.seeds = state.week >= 2 ? weekOneSeeds(state.qualified, picks) : null;
  state.rounds = state.seeds ? bracket(state.seeds, byWeek, Math.min(state.week, 4)) : [];
  if (phase === 'finished' && state.rounds.length) {
    const last = state.rounds[state.rounds.length - 1].matchups;
    if (last.length === 1) state.champion = last[0].winner;
  }
  return state;
}

function liveSeason(env, season) {
  if (!season) fail(409, 'no_season');
  return season;
}

route('POST', '/v1/submissions/([0-9a-f]+)/upvote', async (env, req, [id]) => {
  const a = await needMember(env, req);
  const season = liveSeason(env, await currentSeason(env));
  if (phaseOf(season, now(env)) !== 'voting') fail(409, 'voting_closed');
  const s = await env.DB.prepare("SELECT author_id FROM submissions WHERE id = ? AND season_id = ? AND status = 'visible'").bind(id, season.id).first();
  if (!s) fail(404, 'not_found');
  if (s.author_id === a.id) fail(403, 'own_submission');
  await env.DB.prepare('INSERT OR IGNORE INTO upvotes (account_id, submission_id, created_at) VALUES (?, ?, ?)').bind(a.id, id, now(env)).run();
  return json(200, { upvoted: true });
});

route('DELETE', '/v1/submissions/([0-9a-f]+)/upvote', async (env, req, [id]) => {
  const a = await needMember(env, req);
  const season = liveSeason(env, await currentSeason(env));
  if (phaseOf(season, now(env)) !== 'voting') fail(409, 'voting_closed');
  await env.DB.prepare('DELETE FROM upvotes WHERE account_id = ? AND submission_id = ?').bind(a.id, id).run();
  return json(200, { upvoted: false });
});

// Week 1: replace my picks with up to 8 of the qualifiers.
route('POST', '/v1/tournament/picks', async (env, req) => {
  const a = await needMember(env, req);
  const season = liveSeason(env, await currentSeason(env));
  const st = await seasonState(env, season);
  if (st.phase !== 'tournament' || st.week !== 1) fail(409, 'not_week_one');
  const b = await body(req);
  const ids = Array.isArray(b.submissionIds) ? [...new Set(b.submissionIds.map(String))] : fail(400, 'missing_submissionIds');
  if (ids.length > ADVANCE) fail(400, 'too_many_picks');
  const q = new Map(st.qualified.map((e) => [e.id, e]));
  for (const id of ids) {
    if (!q.has(id)) fail(400, 'not_a_qualifier');
    if (q.get(id).author_id === a.id) fail(403, 'own_submission');
  }
  await env.DB.prepare('DELETE FROM tournament_votes WHERE season_id = ? AND week = 1 AND account_id = ?').bind(season.id, a.id).run();
  for (let i = 0; i < ids.length; i++) {
    await env.DB.prepare('INSERT INTO tournament_votes (season_id, week, slot, account_id, submission_id, created_at) VALUES (?, 1, ?, ?, ?, ?)')
      .bind(season.id, i, a.id, ids[i], now(env)).run();
  }
  return json(200, { picks: ids });
});

// Weeks 2-4: one vote per matchup of this week's round, changeable until the week ends.
route('POST', '/v1/tournament/vote', async (env, req) => {
  const a = await needMember(env, req);
  const season = liveSeason(env, await currentSeason(env));
  const st = await seasonState(env, season);
  if (st.phase !== 'tournament' || st.week < 2) fail(409, 'no_matchups_open');
  const b = await body(req);
  const round = st.rounds.find((r) => r.week === st.week);
  const m = round && round.matchups[b.matchup];
  if (!m) fail(400, 'bad_matchup');
  if (m.bye) fail(400, 'bye_no_vote');
  const pick = String(b.submissionId || '');
  if (pick !== m.a && pick !== m.b) fail(400, 'not_in_matchup');
  const authorOf = new Map(st.entries.map((e) => [e.id, e.author_id]));
  if (authorOf.get(pick) === a.id) fail(403, 'own_submission');
  await env.DB.prepare(
    'INSERT INTO tournament_votes (season_id, week, slot, account_id, submission_id, created_at) VALUES (?, ?, ?, ?, ?, ?) ' +
    'ON CONFLICT(season_id, week, slot, account_id) DO UPDATE SET submission_id = excluded.submission_id, created_at = excluded.created_at')
    .bind(season.id, st.week, b.matchup, a.id, pick, now(env)).run();
  return json(200, { ok: true });
});

route('GET', '/v1/season/results', async (env, req) => {
  const season = await currentSeason(env);
  if (!season) return json(200, { season: null });
  const st = await seasonState(env, season);
  const viewer = await currentAccount(env, req);
  const title = new Map(st.entries.map((e) => [e.id, e.title]));
  const out = { season: publicSeason(season, st.t) };
  if (st.phase === 'voting' || st.phase === 'submissions') {
    out.standings = st.phase === 'voting'
      ? [...st.entries].sort((x, y) => y.score - x.score || x.created_at - y.created_at).map((e) => ({ id: e.id, title: e.title, upvotes: e.score }))
      : [];
    if (viewer) {
      const { results } = await env.DB.prepare('SELECT submission_id FROM upvotes WHERE account_id = ?').bind(viewer.id).all();
      out.myUpvotes = results.map((r) => r.submission_id);
    }
    return json(200, out);
  }
  out.week = Math.min(st.week, 4);
  out.qualifiers = st.qualified.map((e) => ({ id: e.id, title: e.title, upvotes: e.score, picks: st.picks.get(e.id) || 0 }));
  out.seeds = st.seeds && st.seeds.map((e, i) => ({ seed: i + 1, id: e.id, title: e.title }));
  out.rounds = st.rounds.map((r) => ({ week: r.week, final: r.week < st.week, matchups: r.matchups.map((m, i) => ({
    matchup: i, bye: !!m.bye, a: m.a && { id: m.a, title: title.get(m.a), votes: m.votesA },
    b: m.b && { id: m.b, title: title.get(m.b), votes: m.votesB },
    winner: r.week < st.week ? m.winner : null })) }));
  if (st.champion) out.champion = { id: st.champion, title: title.get(st.champion) };
  return json(200, out);
});

// ---- reports and blocks ----

const KINDS = { submission: 'submissions', comment: 'comments', account: 'accounts' };

route('POST', '/v1/reports', async (env, req) => {
  const a = await needMember(env, req);
  const b = await body(req);
  const table = KINDS[b.kind];
  if (!table) fail(400, 'bad_kind');
  await rateLimit(env, a, 'reports');
  const target = await env.DB.prepare(`SELECT id FROM ${table} WHERE id = ?`).bind(String(b.id || '')).first();
  if (!target) fail(404, 'not_found');
  const reason = clean(text(b.reason, LIMITS.reason, 'reason')) ?? '[reason contained blocked words]';
  await env.DB.prepare(
    'INSERT OR IGNORE INTO reports (id, reporter_id, target_kind, target_id, reason, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(newId(), a.id, b.kind, target.id, reason, now(env)).run();
  return json(201, { ok: true });
});

route('POST', '/v1/blocks', async (env, req) => {
  const a = await need(env, req);
  const b = await body(req);
  const other = String(b.accountId || '');
  if (other === a.id) fail(400, 'cannot_block_self');
  if (!(await env.DB.prepare('SELECT id FROM accounts WHERE id = ?').bind(other).first())) fail(404, 'not_found');
  await env.DB.prepare('INSERT OR IGNORE INTO blocks (blocker_id, blocked_id, created_at) VALUES (?, ?, ?)').bind(a.id, other, now(env)).run();
  return json(201, { ok: true });
});

route('GET', '/v1/blocks', async (env, req) => {
  const a = await need(env, req);
  const { results } = await env.DB.prepare(
    'SELECT b.blocked_id AS id, x.display_name FROM blocks b JOIN accounts x ON x.id = b.blocked_id WHERE b.blocker_id = ? ORDER BY b.created_at DESC')
    .bind(a.id).all();
  return json(200, { blocked: results.map((r) => ({ id: r.id, displayName: r.display_name })) });
});

route('DELETE', '/v1/blocks/([0-9a-f]+)', async (env, req, [other]) => {
  const a = await need(env, req);
  await env.DB.prepare('DELETE FROM blocks WHERE blocker_id = ? AND blocked_id = ?').bind(a.id, other).run();
  return json(200, { ok: true });
});

// ---- admin (Chris) ----

route('GET', '/v1/admin/reports', async (env, req) => {
  await needAdmin(env, req);
  const { results } = await env.DB.prepare(
    `SELECT r.target_kind, r.target_id, COUNT(*) AS reports, GROUP_CONCAT(r.reason, ' | ') AS reasons, MIN(r.created_at) AS first,
            CASE r.target_kind WHEN 'account' THEN r.target_id
              WHEN 'submission' THEN (SELECT author_id FROM submissions WHERE id = r.target_id)
              ELSE (SELECT author_id FROM comments WHERE id = r.target_id) END AS author_id,
            CASE r.target_kind WHEN 'account' THEN (SELECT display_name FROM accounts WHERE id = r.target_id)
              WHEN 'submission' THEN (SELECT title FROM submissions WHERE id = r.target_id)
              ELSE (SELECT body FROM comments WHERE id = r.target_id) END AS preview
       FROM reports r WHERE r.resolved = 0 GROUP BY r.target_kind, r.target_id ORDER BY reports DESC, first`).all();
  return json(200, { reports: results });
});

// Find players by name (part of it) or exact email, banned ones included, so Chris can ban or unban anyone.
route('GET', '/v1/admin/accounts', async (env, req) => {
  await needAdmin(env, req);
  const q = (new URL(req.url).searchParams.get('q') || '').trim().toLowerCase();
  if (q.length < 2) fail(400, 'search_too_short');
  const like = `%${q.replace(/[\\%_]/g, (c) => '\\' + c)}%`;
  const { results } = await env.DB.prepare(
    `SELECT id, display_name, email, banned, role, created_at FROM accounts
      WHERE id != 'deleted' AND (lower(display_name) LIKE ? ESCAPE '\\' OR email = ?) ORDER BY created_at DESC LIMIT 50`)
    .bind(like, q).all();
  return json(200, { accounts: results.map((a) => ({ id: a.id, displayName: a.display_name, email: a.email, banned: !!a.banned, admin: a.role === 'admin' })) });
});

async function setStatus(env, req, status) {
  const admin = await needAdmin(env, req);
  const b = await body(req);
  if (b.kind !== 'submission' && b.kind !== 'comment') fail(400, 'bad_kind');
  const r = await env.DB.prepare(`UPDATE ${KINDS[b.kind]} SET status = ? WHERE id = ?`).bind(status, String(b.id || '')).run();
  if (!r.meta.changes) fail(404, 'not_found');
  await env.DB.prepare('UPDATE reports SET resolved = 1 WHERE target_kind = ? AND target_id = ?').bind(b.kind, b.id).run();
  await audit(env, admin, status, `${b.kind}:${b.id}`);
  return json(200, { ok: true });
}
route('POST', '/v1/admin/hide', (env, req) => setStatus(env, req, 'hidden'));
route('POST', '/v1/admin/remove', (env, req) => setStatus(env, req, 'removed'));
route('POST', '/v1/admin/restore', (env, req) => setStatus(env, req, 'visible'));

async function setBan(env, req, banned) {
  const admin = await needAdmin(env, req);
  const b = await body(req);
  const id = String(b.accountId || '');
  if (id === admin.id) fail(400, 'cannot_ban_self');
  const r = await env.DB.prepare('UPDATE accounts SET banned = ? WHERE id = ?').bind(banned ? 1 : 0, id).run();
  if (!r.meta.changes) fail(404, 'not_found');
  if (banned) await env.DB.prepare('DELETE FROM sessions WHERE account_id = ?').bind(id).run();
  await env.DB.prepare("UPDATE reports SET resolved = 1 WHERE target_kind = 'account' AND target_id = ?").bind(id).run();
  await audit(env, admin, banned ? 'ban' : 'unban', `account:${id}`);
  return json(200, { ok: true });
}
route('POST', '/v1/admin/ban', (env, req) => setBan(env, req, true));
route('POST', '/v1/admin/unban', (env, req) => setBan(env, req, false));

export async function handle(request, env) {
  const url = new URL(request.url);
  for (const r of routes) {
    if (r.method !== request.method) continue;
    const m = url.pathname.match(r.re);
    if (!m) continue;
    try {
      return await r.fn(env, request, m.slice(1));
    } catch (e) {
      if (e instanceof HttpError) return json(e.status, { error: e.code });
      console.error(e);
      return json(500, { error: 'server_error' });
    }
  }
  return json(404, { error: 'no_route' });
}

// Real deployment: Google tokens are checked against GOOGLE_CLIENT_ID. The mail sender is a binding set up once the
// email provider is chosen; until then sign-up codes can't be delivered and registration answers 503.
let google = null;
export default {
  fetch(request, env) {
    if (!env.GOOGLE) {
      google = google || googleVerifier({ clientId: env.GOOGLE_CLIENT_ID });
      env = { ...env, GOOGLE: google };
    }
    if (!env.MAILER) env = { ...env, MAILER: cloudflareMailer(env) || { async send() { throw new HttpError(503, 'email_not_set_up'); } } };
    return handle(request, env);
  },
};
