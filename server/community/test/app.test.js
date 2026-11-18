import { test } from 'node:test';
import assert from 'node:assert/strict';
import { handle } from '../src/app.js';
import { testDb } from './d1-node.js';

function setup() {
  const mail = [];
  let clock = 1_800_000_000_000;
  const db = testDb();
  // An open season so posting works; season tests make their own.
  db.raw.prepare('INSERT INTO seasons VALUES (?, ?, ?, ?, ?, ?, ?)').run('s0', 'Test season', clock - 1, clock + 1e12, clock + 2e12, clock + 3e12, clock);
  const env = {
    DB: db,
    MAILER: { async send(to, subject, text) { mail.push({ to, subject, text }); } },
    GOOGLE: { async verify(t) { return t.startsWith('good:') ? { sub: t.slice(5), email: `${t.slice(5)}@gmail.com` } : null; } },
    ADMIN_EMAILS: 'chris@example.com',
    now: () => clock,
  };
  let ipCounter = 0;
  // Each test request comes from its own address unless a test sets one (the sign-up limit is per address).
  const call = async (method, path, body, token, ip) => {
    const headers = { 'content-type': 'application/json', 'cf-connecting-ip': ip || `10.0.0.${++ipCounter}` };
    if (token) headers.authorization = `Bearer ${token}`;
    const res = await handle(new Request(`https://x${path}`, { method, headers, body: body ? JSON.stringify(body) : undefined }), env);
    return { status: res.status, body: await res.json() };
  };
  const lastCode = (to) => [...mail].reverse().find((m) => m.to === to).text.match(/\d{6}/)[0];
  // A verified, 16+ member, ready to post.
  const member = async (email, name = email.split('@')[0]) => {
    await call('POST', '/v1/auth/register', { email, password: 'hunter2hunter2', displayName: name });
    const { body } = await call('POST', '/v1/auth/verify', { email, code: lastCode(email) });
    await call('POST', '/v1/account/age', { passed: true }, body.token);
    const me = await call('GET', '/v1/me', null, body.token);
    return { token: body.token, id: me.body.id };
  };
  return { env, mail, call, lastCode, member, tick: (ms) => { clock += ms; } };
}

test('register sends a code; the code verifies the email and signs in', async () => {
  const { call, mail, lastCode } = setup();
  const r = await call('POST', '/v1/auth/register', { email: 'A@Example.com', password: 'longenough', displayName: 'Ana' });
  assert.equal(r.status, 201);
  assert.equal(mail.length, 1);
  assert.equal(mail[0].to, 'a@example.com');
  const login = await call('POST', '/v1/auth/login', { email: 'a@example.com', password: 'longenough' });
  assert.deepEqual([login.status, login.body.error], [403, 'email_not_verified']);
  const v = await call('POST', '/v1/auth/verify', { email: 'a@example.com', code: lastCode('a@example.com') });
  assert.equal(v.status, 200);
  const me = await call('GET', '/v1/me', null, v.body.token);
  assert.deepEqual([me.body.displayName, me.body.emailVerified, me.body.ageOk, me.body.admin], ['Ana', true, false, false]);
  const again = await call('POST', '/v1/auth/login', { email: 'a@example.com', password: 'longenough' });
  assert.equal(again.status, 200);
});

test('wrong codes are limited, and codes expire', async () => {
  const { call, lastCode, tick } = setup();
  await call('POST', '/v1/auth/register', { email: 'b@x.com', password: 'longenough', displayName: 'B' });
  const good = lastCode('b@x.com');
  const wrong = good === '000000' ? '111111' : '000000';
  for (let i = 0; i < 5; i++) assert.equal((await call('POST', '/v1/auth/verify', { email: 'b@x.com', code: wrong })).body.error, 'bad_code');
  assert.equal((await call('POST', '/v1/auth/verify', { email: 'b@x.com', code: good })).body.error, 'code_expired');
  await call('POST', '/v1/auth/resend', { email: 'b@x.com' });
  tick(16 * 60 * 1000);
  assert.equal((await call('POST', '/v1/auth/verify', { email: 'b@x.com', code: lastCode('b@x.com') })).body.error, 'code_expired');
});

test('passwords are hashed, short ones refused, wrong ones rejected, verified emails cannot be re-registered', async () => {
  const { env, call, member } = setup();
  assert.equal((await call('POST', '/v1/auth/register', { email: 'c@x.com', password: 'short', displayName: 'C' })).body.error, 'weak_password');
  await member('c@x.com');
  const row = env.DB.raw.prepare("SELECT password_hash FROM accounts WHERE id != 'deleted'").get();
  assert.ok(row.password_hash.startsWith('pbkdf2$') && !row.password_hash.includes('hunter2'));
  assert.equal((await call('POST', '/v1/auth/login', { email: 'c@x.com', password: 'nope-nope-nope' })).status, 401);
  assert.equal((await call('POST', '/v1/auth/register', { email: 'c@x.com', password: 'longenough', displayName: 'C' })).body.error, 'email_taken');
});

test('Google sign-in makes its own account and signs back into it', async () => {
  const { call } = setup();
  assert.equal((await call('POST', '/v1/auth/google', { idToken: 'bad' })).status, 401);
  const first = await call('POST', '/v1/auth/google', { idToken: 'good:123', displayName: 'Gee' });
  const second = await call('POST', '/v1/auth/google', { idToken: 'good:123' });
  const a = await call('GET', '/v1/me', null, first.body.token);
  const b = await call('GET', '/v1/me', null, second.body.token);
  assert.equal(a.body.id, b.body.id);
  assert.equal(a.body.emailVerified, true);
});

test('posting needs a verified, 16+ account; the server only ever records a pass', async () => {
  const { call, lastCode } = setup();
  await call('POST', '/v1/auth/register', { email: 'd@x.com', password: 'longenough', displayName: 'D' });
  const { body } = await call('POST', '/v1/auth/verify', { email: 'd@x.com', code: lastCode('d@x.com') });
  const post = () => call('POST', '/v1/submissions', { title: 'Idea', body: 'A new boss' }, body.token);
  assert.equal((await post()).body.error, 'age_gate');
  assert.equal((await call('POST', '/v1/account/age', { passed: false }, body.token)).status, 400);
  assert.equal((await post()).body.error, 'age_gate');
  await call('POST', '/v1/account/age', { passed: true }, body.token);
  assert.equal((await post()).status, 201);
  assert.equal((await call('POST', '/v1/submissions', { title: 'x', body: 'y' })).status, 401);
});

test('threaded comments: replies point at their parent; a removed comment stays as an empty placeholder', async () => {
  const { call, member } = setup();
  const ana = await member('chris@example.com', 'Chris');
  const bo = await member('bo@x.com', 'Bo');
  const s = (await call('POST', '/v1/submissions', { title: 'Shield rework', body: 'Shields should have worked' }, bo.token)).body.id;
  const top = (await call('POST', `/v1/submissions/${s}/comments`, { body: 'Love it' }, ana.token)).body.id;
  const reply = (await call('POST', `/v1/submissions/${s}/comments`, { body: 'Thanks', parentId: top }, bo.token)).body.id;
  assert.equal((await call('POST', `/v1/submissions/${s}/comments`, { body: 'x', parentId: 'ffff' }, bo.token)).body.error, 'bad_parent');
  let view = (await call('GET', `/v1/submissions/${s}`)).body;
  assert.deepEqual(view.comments.map((c) => [c.id, c.parentId]), [[top, null], [reply, top]]);
  await call('POST', '/v1/admin/remove', { kind: 'comment', id: top }, ana.token);
  view = (await call('GET', `/v1/submissions/${s}`)).body;
  assert.equal(view.comments[0].body, null);
  assert.equal(view.comments[1].body, 'Thanks');
  const list = (await call('GET', '/v1/submissions')).body.submissions;
  assert.deepEqual([list[0].title, list[0].comments], ['Shield rework', 1]);
});

test('filter: slurs are refused, swears are masked, harmless words pass', async () => {
  const { call, member } = setup();
  const m = await member('e@x.com');
  const post = (body) => call('POST', '/v1/submissions', { title: 'T', body }, m.token);
  assert.equal((await post('you f.a.g.g.0.t')).body.error, 'not_allowed_words');
  const id = (await post('this boss is fucking hard, like Sh1t')).body.id;
  assert.equal((await call('GET', `/v1/submissions/${id}`)).body.body, 'this boss is ****ing hard, like ****');
  const id2 = (await post('Cockatrice in Scunthorpe, a classic assassin')).body.id;
  assert.equal((await call('GET', `/v1/submissions/${id2}`)).body.body, 'Cockatrice in Scunthorpe, a classic assassin');
  assert.equal((await call('POST', '/v1/auth/register', { email: 'f@x.com', password: 'longenough', displayName: 'n1gger' })).body.error, 'not_allowed_words');
});

test('report and block: blocking hides that player from me; reports reach the admin queue once per reporter', async () => {
  const { call, member } = setup();
  const chris = await member('chris@example.com', 'Chris');
  const ana = await member('ana@x.com', 'Ana');
  const troll = await member('troll@x.com', 'Troll');
  const s = (await call('POST', '/v1/submissions', { title: 'bait', body: 'bait' }, troll.token)).body.id;
  await call('POST', `/v1/submissions/${s}/comments`, { body: 'hi' }, troll.token);
  for (let i = 0; i < 2; i++) await call('POST', '/v1/reports', { kind: 'submission', id: s, reason: 'spam' }, ana.token);
  await call('POST', '/v1/reports', { kind: 'submission', id: s, reason: 'rude' }, chris.token);
  assert.equal((await call('GET', '/v1/admin/reports', null, ana.token)).status, 403);
  const q = (await call('GET', '/v1/admin/reports', null, chris.token)).body.reports;
  assert.deepEqual([q.length, q[0].reports, q[0].author_id, q[0].preview], [1, 2, troll.id, 'bait']);
  assert.equal((await call('POST', '/v1/blocks', { accountId: ana.id }, ana.token)).body.error, 'cannot_block_self');
  await call('POST', '/v1/blocks', { accountId: troll.id }, ana.token);
  assert.equal((await call('GET', '/v1/submissions', null, ana.token)).body.submissions.length, 0);
  assert.equal((await call('GET', `/v1/submissions/${s}`, null, ana.token)).status, 404);
  assert.equal((await call('GET', '/v1/submissions', null, chris.token)).body.submissions.length, 1);
  await call('DELETE', `/v1/blocks/${troll.id}`, null, ana.token);
  assert.equal((await call('GET', '/v1/submissions', null, ana.token)).body.submissions.length, 1);
});

test('admin: hide, remove and restore posts, ban and unban accounts; banned players are signed out and hidden', async () => {
  const { env, call, member } = setup();
  const chris = await member('chris@example.com', 'Chris');
  const bad = await member('bad@x.com', 'Bad');
  const s = (await call('POST', '/v1/submissions', { title: 't', body: 'b' }, bad.token)).body.id;
  assert.equal((await call('POST', '/v1/admin/hide', { kind: 'submission', id: s }, bad.token)).status, 403);
  await call('POST', '/v1/admin/hide', { kind: 'submission', id: s }, chris.token);
  assert.equal((await call('GET', '/v1/submissions')).body.submissions.length, 0);
  await call('POST', '/v1/admin/restore', { kind: 'submission', id: s }, chris.token);
  assert.equal((await call('GET', '/v1/submissions')).body.submissions.length, 1);
  await call('POST', '/v1/admin/ban', { accountId: bad.id }, chris.token);
  assert.equal((await call('GET', '/v1/me', null, bad.token)).status, 401);
  assert.equal((await call('POST', '/v1/auth/login', { email: 'bad@x.com', password: 'hunter2hunter2' })).body.error, 'banned');
  assert.equal((await call('GET', '/v1/submissions')).body.submissions.length, 0);
  assert.equal((await call('POST', '/v1/admin/ban', { accountId: chris.id }, chris.token)).body.error, 'cannot_ban_self');
  await call('POST', '/v1/admin/unban', { accountId: bad.id }, chris.token);
  assert.equal((await call('GET', '/v1/submissions')).body.submissions.length, 1);
  const log = env.DB.raw.prepare('SELECT action FROM audit ORDER BY id').all().map((r) => r.action);
  assert.deepEqual(log, ['hidden', 'visible', 'ban', 'unban']);
});

test('account deletion removes the account, its sessions, posts, comments, reports and blocks', async () => {
  const { env, call, member } = setup();
  const a = await member('gone@x.com');
  const b = await member('stay@x.com');
  const s = (await call('POST', '/v1/submissions', { title: 't', body: 'b' }, a.token)).body.id;
  await call('POST', `/v1/submissions/${s}/comments`, { body: 'c' }, a.token);
  const s2 = (await call('POST', '/v1/submissions', { title: 't2', body: 'b2' }, b.token)).body.id;
  await call('POST', `/v1/submissions/${s2}/comments`, { body: 'mine' }, a.token);
  await call('POST', '/v1/reports', { kind: 'submission', id: s2, reason: 'x' }, a.token);
  await call('POST', '/v1/blocks', { accountId: b.id }, a.token);
  assert.equal((await call('DELETE', '/v1/account', null, a.token)).status, 200);
  assert.equal((await call('GET', '/v1/me', null, a.token)).status, 401);
  const count = (t) => env.DB.raw.prepare(`SELECT COUNT(*) AS n FROM ${t}`).get().n;
  assert.deepEqual(['accounts', 'submissions', 'comments', 'reports', 'blocks'].map(count), [2, 1, 0, 0, 0]); // + the [deleted] placeholder
  assert.equal((await call('POST', '/v1/auth/login', { email: 'gone@x.com', password: 'hunter2hunter2' })).status, 401);
});

test('there is no private-message route', async () => {
  const { call, member } = setup();
  const m = await member('pm@x.com');
  for (const p of ['/v1/messages', '/v1/dm', '/v1/inbox']) assert.equal((await call('POST', p, { to: 'x', body: 'hi' }, m.token)).status, 404);
});

test('seasons: submissions only during Month 1, discussion continues, the list shows the current season', async () => {
  const { env, call, member, tick } = setup();
  env.DB.raw.prepare('DELETE FROM seasons').run();
  const chris = await member('chris@example.com', 'Chris');
  const ana = await member('ana@x.com', 'Ana');
  assert.equal((await call('GET', '/v1/season')).body.season, null);
  assert.equal((await call('POST', '/v1/submissions', { title: 't', body: 'b' }, ana.token)).body.error, 'submissions_closed');
  const min = 60 * 1000;
  assert.equal((await call('POST', '/v1/admin/seasons', { title: 'x' }, ana.token)).status, 403);
  const r = await call('POST', '/v1/admin/seasons', { title: 'Season 1', submissionMs: 10 * min, votingMs: 10 * min, tournamentMs: 20 * min }, chris.token);
  assert.equal(r.status, 201);
  assert.equal((await call('POST', '/v1/admin/seasons', { title: 'Clash' }, chris.token)).body.error, 'season_overlaps');
  assert.equal((await call('GET', '/v1/season')).body.season.phase, 'submissions');
  const s = (await call('POST', '/v1/submissions', { title: 'Idea', body: 'b' }, ana.token)).body.id;
  tick(10 * min);
  assert.equal((await call('GET', '/v1/season')).body.season.phase, 'voting');
  assert.equal((await call('POST', '/v1/submissions', { title: 'Late', body: 'b' }, ana.token)).body.error, 'submissions_closed');
  assert.equal((await call('POST', `/v1/submissions/${s}/comments`, { body: 'still talking' }, chris.token)).status, 201);
  tick(10 * min);
  assert.equal((await call('GET', '/v1/season')).body.season.phase, 'tournament');
  tick(20 * min);
  assert.equal((await call('GET', '/v1/season')).body.season.phase, 'finished');
  assert.equal((await call('GET', '/v1/submissions')).body.submissions.length, 1);
  await call('POST', '/v1/admin/seasons', { title: 'Season 2' }, chris.token);
  assert.equal((await call('GET', '/v1/season')).body.season.title, 'Season 2');
  assert.equal((await call('GET', '/v1/submissions')).body.submissions.length, 0);
});

test('full season on a fast clock: upvotes to qualify, week 1 picks to 8, then the 1v1 bracket to a champion', async () => {
  const { env, call, member, tick } = setup();
  env.DB.raw.prepare('DELETE FROM seasons').run();
  const min = 60 * 1000;
  const chris = await member('chris@example.com', 'Chris');
  const authors = [];
  for (let i = 0; i < 10; i++) authors.push(await member(`author${i}@x.com`, `A${i}`));
  const voters = [];
  for (let i = 0; i < 4; i++) voters.push(await member(`voter${i}@x.com`, `V${i}`));
  await call('POST', '/v1/admin/seasons', { title: 'S', submissionMs: 10 * min, votingMs: 10 * min, tournamentMs: 40 * min }, chris.token);
  const subs = [];
  for (let i = 0; i < 10; i++) { subs.push((await call('POST', '/v1/submissions', { title: `Idea ${i}`, body: 'b' }, authors[i].token)).body.id); tick(1); }
  assert.equal((await call('POST', `/v1/submissions/${subs[0]}/upvote`, null, voters[0].token)).body.error, 'voting_closed');
  tick(10 * min); // Month 2
  assert.equal((await call('POST', `/v1/submissions/${subs[0]}/upvote`, null, authors[0].token)).body.error, 'own_submission');
  // Idea 9 gets 4 upvotes, Idea 8 gets 3, ... ; one upvote taken back
  for (let i = 0; i < 4; i++) for (let k = 9; k >= 9 - i; k--) await call('POST', `/v1/submissions/${subs[k]}/upvote`, null, voters[i].token);
  await call('POST', `/v1/submissions/${subs[0]}/upvote`, null, voters[0].token);
  await call('DELETE', `/v1/submissions/${subs[0]}/upvote`, null, voters[0].token);
  let r = (await call('GET', '/v1/season/results', null, voters[0].token)).body;
  assert.equal(r.standings[0].title, 'Idea 9');
  assert.equal(r.standings[0].upvotes, 4);
  assert.ok(!r.myUpvotes.includes(subs[0]));
  // A banned voter's upvotes stop counting
  await call('POST', '/v1/admin/ban', { accountId: voters[3].id }, chris.token);
  r = (await call('GET', '/v1/season/results')).body;
  assert.equal(r.standings.find((x) => x.title === 'Idea 6').upvotes, 0);
  await call('POST', '/v1/admin/unban', { accountId: voters[3].id }, chris.token);
  voters[3].token = (await call('POST', '/v1/auth/login', { email: 'voter3@x.com', password: 'hunter2hunter2' })).body.token; // the ban signed them out
  tick(10 * min); // Tournament week 1
  r = (await call('GET', '/v1/season/results')).body;
  assert.deepEqual([r.season.phase, r.week, r.qualifiers.length], ['tournament', 1, 10]);
  assert.equal((await call('POST', '/v1/tournament/picks', { submissionIds: subs.slice(0, 9) }, voters[0].token)).body.error, 'too_many_picks');
  assert.equal((await call('POST', '/v1/tournament/picks', { submissionIds: [subs[1]] }, authors[1].token)).body.error, 'own_submission');
  // Everyone picks Ideas 0-7, so Ideas 8 and 9 (the Month 2 leaders) are cut
  for (const v of voters) assert.equal((await call('POST', '/v1/tournament/picks', { submissionIds: subs.slice(0, 8) }, v.token)).status, 200);
  tick(10 * min); // Week 2: 8 seeds, all tied on picks, so Month 2 score then age decides the seeds
  r = (await call('GET', '/v1/season/results')).body;
  assert.equal(r.week, 2);
  assert.deepEqual(r.seeds.map((s) => s.title), ['Idea 7', 'Idea 6', 'Idea 0', 'Idea 1', 'Idea 2', 'Idea 3', 'Idea 4', 'Idea 5']);
  const m0 = r.rounds[0].matchups[0];
  assert.deepEqual([m0.a.title, m0.b.title], ['Idea 7', 'Idea 5']);
  assert.equal((await call('POST', '/v1/tournament/vote', { matchup: 0, submissionId: subs[8] }, voters[0].token)).body.error, 'not_in_matchup');
  // Upset: seed 8 (Idea 5) beats seed 1; a voter changes their mind and only the latest vote counts
  await call('POST', '/v1/tournament/vote', { matchup: 0, submissionId: m0.a.id }, voters[0].token);
  await call('POST', '/v1/tournament/vote', { matchup: 0, submissionId: m0.b.id }, voters[0].token);
  await call('POST', '/v1/tournament/vote', { matchup: 0, submissionId: m0.b.id }, voters[1].token);
  r = (await call('GET', '/v1/season/results')).body;
  assert.deepEqual([r.rounds[0].matchups[0].a.votes, r.rounds[0].matchups[0].b.votes, r.rounds[0].matchups[0].winner], [0, 2, null]);
  tick(10 * min); // Week 3
  r = (await call('GET', '/v1/season/results')).body;
  assert.equal(r.rounds[0].matchups[0].winner, m0.b.id);
  assert.equal(r.rounds[1].matchups[0].a.title, 'Idea 5');
  tick(10 * min); // Week 4: final, no votes -> higher seed
  tick(10 * min); // finished
  r = (await call('GET', '/v1/season/results')).body;
  assert.equal(r.season.phase, 'finished');
  assert.equal(r.rounds.length, 3);
  assert.ok(r.champion && r.champion.title);
  assert.equal((await call('POST', '/v1/tournament/vote', { matchup: 0, submissionId: m0.a.id }, voters[0].token)).body.error, 'no_matchups_open');
});

test('anti-spam caps: 5 submissions a day, 10 comments per 10 minutes', async () => {
  const { call, member, tick } = setup();
  const m = await member('spam@x.com');
  for (let i = 0; i < 5; i++) assert.equal((await call('POST', '/v1/submissions', { title: `t${i}`, body: 'b' }, m.token)).status, 201);
  assert.equal((await call('POST', '/v1/submissions', { title: 'six', body: 'b' }, m.token)).body.error, 'too_many_submissions');
  const s = (await call('GET', '/v1/submissions')).body.submissions[0].id;
  for (let i = 0; i < 10; i++) await call('POST', `/v1/submissions/${s}/comments`, { body: `c${i}` }, m.token);
  assert.equal((await call('POST', `/v1/submissions/${s}/comments`, { body: 'more' }, m.token)).status, 429);
  tick(10 * 60 * 1000 + 1);
  assert.equal((await call('POST', `/v1/submissions/${s}/comments`, { body: 'later' }, m.token)).status, 201);
  tick(24 * 3600 * 1000);
  assert.equal((await call('POST', '/v1/submissions', { title: 'next day', body: 'b' }, m.token)).status, 201);
});

test('deleting an account keeps other players\' replies; the deleted comment becomes an empty placeholder', async () => {
  const { call, member } = setup();
  const a = await member('leaving@x.com', 'Leaving');
  const b = await member('staying@x.com', 'Staying');
  const s = (await call('POST', '/v1/submissions', { title: 't', body: 'b' }, b.token)).body.id;
  const parent = (await call('POST', `/v1/submissions/${s}/comments`, { body: 'my take' }, a.token)).body.id;
  await call('POST', `/v1/submissions/${s}/comments`, { body: 'lonely' }, a.token);
  await call('POST', `/v1/submissions/${s}/comments`, { body: 'good point', parentId: parent }, b.token);
  await call('DELETE', '/v1/account', null, a.token);
  const view = (await call('GET', `/v1/submissions/${s}`)).body;
  assert.equal(view.comments.length, 2);
  assert.deepEqual([view.comments[0].id, view.comments[0].body, view.comments[0].author], [parent, null, null]);
  assert.deepEqual([view.comments[1].body, view.comments[1].parentId], ['good point', parent]);
});

test('guessing limits: 10 wrong passwords per email in 15 minutes, 5 sign-ups per address per hour', async () => {
  const { call, member, tick } = setup();
  await member('target@x.com');
  for (let i = 0; i < 10; i++) assert.equal((await call('POST', '/v1/auth/login', { email: 'target@x.com', password: `guess${i}xx` })).status, 401);
  const blocked = await call('POST', '/v1/auth/login', { email: 'target@x.com', password: 'hunter2hunter2' });
  assert.deepEqual([blocked.status, blocked.body.error], [429, 'slow_down']);
  tick(15 * 60 * 1000 + 1);
  assert.equal((await call('POST', '/v1/auth/login', { email: 'target@x.com', password: 'hunter2hunter2' })).status, 200);
  for (let i = 0; i < 5; i++) assert.equal((await call('POST', '/v1/auth/register', { email: `s${i}@x.com`, password: 'longenough', displayName: 'S' }, null, '9.9.9.9')).status, 201);
  assert.equal((await call('POST', '/v1/auth/register', { email: 's6@x.com', password: 'longenough', displayName: 'S' }, null, '9.9.9.9')).status, 429);
  assert.equal((await call('POST', '/v1/auth/register', { email: 's7@x.com', password: 'longenough', displayName: 'S' }, null, '8.8.8.8')).status, 201);
});

test('blocked players list shows who I blocked, and unblocking removes them', async () => {
  const { call, member } = setup();
  const me = await member('me@x.com', 'Me');
  const pest = await member('pest@x.com', 'Pest');
  assert.deepEqual((await call('GET', '/v1/blocks', null, me.token)).body.blocked, []);
  await call('POST', '/v1/blocks', { accountId: pest.id }, me.token);
  assert.deepEqual((await call('GET', '/v1/blocks', null, me.token)).body.blocked, [{ id: pest.id, displayName: 'Pest' }]);
  await call('DELETE', `/v1/blocks/${pest.id}`, null, me.token);
  assert.deepEqual((await call('GET', '/v1/blocks', null, me.token)).body.blocked, []);
  assert.equal((await call('GET', '/v1/blocks')).status, 401);
});

test('web deletion page (for Google Play): form, wrong password refused, right password deletes everything', async () => {
  const { env, member, call } = setup();
  await member('web@x.com', 'Web');
  const page = await handle(new Request('https://x/delete'), env);
  assert.equal(page.status, 200);
  assert.match(await page.text(), /<form method="post" action="\/delete">/);
  const post = (pw) => handle(new Request('https://x/delete', { method: 'POST', body: new URLSearchParams({ email: 'Web@x.com', password: pw }) }), env);
  assert.equal((await post('wrong-wrong')).status, 401);
  assert.equal(env.DB.raw.prepare("SELECT COUNT(*) AS n FROM accounts WHERE email = 'web@x.com'").get().n, 1);
  const ok = await post('hunter2hunter2');
  assert.equal(ok.status, 200);
  assert.match(await ok.text(), /Your account is deleted/);
  assert.equal(env.DB.raw.prepare("SELECT COUNT(*) AS n FROM accounts WHERE email = 'web@x.com'").get().n, 0);
  assert.equal((await call('POST', '/v1/auth/login', { email: 'web@x.com', password: 'hunter2hunter2' })).status, 401);
});

test('an account made before its email was set as admin becomes admin at the next sign-in', async () => {
  const { env, call, member } = setup();
  env.ADMIN_EMAILS = '';
  await member('later@x.com', 'Later');
  env.ADMIN_EMAILS = 'later@x.com';
  const t = (await call('POST', '/v1/auth/login', { email: 'later@x.com', password: 'hunter2hunter2' })).body.token;
  assert.equal((await call('GET', '/v1/me', null, t)).body.admin, true);
});

test('admin can find players by part of their name or exact email, banned ones included', async () => {
  const { call, member } = setup();
  const chris = await member('chris@example.com', 'Chris');
  const t = await member('troll99@x.com', 'TrollFace');
  await call('POST', '/v1/admin/ban', { accountId: t.id }, chris.token);
  const byName = (await call('GET', '/v1/admin/accounts?q=troll', null, chris.token)).body.accounts;
  assert.deepEqual(byName.map((a) => [a.displayName, a.banned]), [['TrollFace', true]]);
  assert.equal((await call('GET', '/v1/admin/accounts?q=troll99@x.com', null, chris.token)).body.accounts[0].id, t.id);
  assert.equal((await call('GET', '/v1/admin/accounts?q=%25%25', null, chris.token)).body.accounts.length, 0); // % is literal
  assert.equal((await call('GET', '/v1/admin/accounts?q=t', null, chris.token)).body.error, 'search_too_short');
  assert.equal((await call('GET', '/v1/admin/accounts?q=troll', null, (await member('x@x.com')).token)).status, 403);
});
