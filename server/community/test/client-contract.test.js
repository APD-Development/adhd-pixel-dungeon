// Every field the Android client (CommunityController / CommunityApi) reads, checked against real server replies.
// If a field is renamed on either side, this fails instead of the app silently showing blanks.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { handle } from '../src/app.js';
import { testDb } from './d1-node.js';

const has = (obj, path) => {
  let v = obj;
  for (const k of path.split('.')) { assert.ok(v != null && k in v, `missing ${path}`); v = v[k]; }
  return v;
};

test('every field the app reads exists in the server replies, through a whole season', async () => {
  const mail = [];
  let clock = 1_800_000_000_000, ip = 0;
  const env = { DB: testDb(), ADMIN_EMAILS: 'chris@x.com', now: () => clock,
    MAILER: { async send(to, s, text) { mail.push({ to, text }); } }, GOOGLE: { async verify() { return null; } } };
  const call = async (method, path, body, token) => {
    const headers = { 'content-type': 'application/json', 'cf-connecting-ip': `10.1.0.${++ip}` };
    if (token) headers.authorization = `Bearer ${token}`;
    const r = await handle(new Request(`https://x${path}`, { method, headers, body: body ? JSON.stringify(body) : undefined }), env);
    return r.json();
  };
  const join = async (email, name) => {
    await call('POST', '/v1/auth/register', { email, password: 'longenough', displayName: name });
    const code = mail.filter((m) => m.to === email).pop().text.match(/\d{6}/)[0];
    const v = await call('POST', '/v1/auth/verify', { email, code });
    has(v, 'token');
    await call('POST', '/v1/account/age', { passed: true }, v.token);
    return v.token;
  };
  const chris = await join('chris@x.com', 'Chris');
  const bo = await join('bo@x.com', 'Bo');
  const me = await call('GET', '/v1/me', null, chris);
  for (const k of ['id', 'displayName', 'ageOk', 'admin']) has(me, k);

  const min = 60000;
  await call('POST', '/v1/admin/seasons', { title: 'S', submissionMs: 10 * min, votingMs: 10 * min, tournamentMs: 40 * min }, chris);
  const season = await call('GET', '/v1/season', null, chris);
  has(season, 'season.title'); has(season, 'season.phase');

  const sid = (await call('POST', '/v1/submissions', { title: 'Idea', body: 'Body' }, bo)).id;
  await call('POST', '/v1/submissions', { title: 'Other idea', body: 'Body' }, chris); // a bracket needs two
  const cid = (await call('POST', `/v1/submissions/${sid}/comments`, { body: 'Hi' }, chris)).id;
  await call('POST', `/v1/submissions/${sid}/comments`, { body: 'Re', parentId: cid }, bo);
  const list = await call('GET', '/v1/submissions', null, chris);
  for (const k of ['id', 'title', 'author.displayName', 'comments']) has(list.submissions[1], k);
  const detail = await call('GET', `/v1/submissions/${sid}`, null, chris);
  for (const k of ['title', 'body', 'author.id', 'author.displayName']) has(detail, k);
  for (const k of ['id', 'parentId', 'body', 'author.id', 'author.displayName']) has(detail.comments[1], k);

  clock += 10 * min;
  await call('POST', `/v1/submissions/${sid}/upvote`, null, chris);
  const voting = await call('GET', '/v1/season/results', null, chris);
  has(voting, 'season.phase'); has(voting, 'myUpvotes');
  for (const k of ['id', 'title', 'upvotes']) has(voting.standings[0], k);

  clock += 10 * min;
  const w1 = await call('GET', '/v1/season/results', null, chris);
  has(w1, 'week'); for (const k of ['id', 'title', 'picks']) has(w1.qualifiers[0], k);
  clock += 10 * min;
  const w2 = await call('GET', '/v1/season/results', null, chris);
  has(w2.rounds[0], 'week');
  for (const k of ['matchup', 'bye', 'winner']) has(w2.rounds[0].matchups[0], k);
  clock += 40 * min;
  const done = await call('GET', '/v1/season/results', null, chris);
  has(done, 'champion.title');
  const m = done.rounds.flatMap((r) => r.matchups).find((x) => !x.bye); // two entries: byes, then the final
  for (const k of ['matchup', 'bye', 'winner', 'a.id', 'a.title', 'a.votes', 'b.id', 'b.title', 'b.votes']) has(m, k);

  await call('POST', '/v1/reports', { kind: 'comment', id: cid, reason: 'test' }, bo);
  const reports = await call('GET', '/v1/admin/reports', null, chris);
  for (const k of ['target_kind', 'target_id', 'author_id', 'preview', 'reasons', 'reports']) has(reports.reports[0], k);

  const bob = (await call('GET', '/v1/me', null, bo)).id;
  await call('POST', '/v1/blocks', { accountId: bob }, chris);
  const blocks = await call('GET', '/v1/blocks', null, chris);
  for (const k of ['id', 'displayName']) has(blocks.blocked[0], k);

  const err = await call('POST', '/v1/submissions', { title: 'late', body: 'x' }, bo);
  has(err, 'error');
});
