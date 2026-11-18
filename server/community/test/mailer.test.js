import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cloudflareMailer } from '../src/mailer.js';
import worker from '../src/app.js';
import { testDb } from './d1-node.js';

test('mailer sends through the EMAIL binding from MAIL_FROM, and is absent until both exist', async () => {
  const sent = [];
  const m = cloudflareMailer({ EMAIL: { async send(msg) { sent.push(msg); } }, MAIL_FROM: 'community@charltonbornstudios.com' });
  await m.send('p@x.com', 'Your code', 'Code 123456');
  assert.deepEqual(sent, [{ to: 'p@x.com', from: 'community@charltonbornstudios.com', subject: 'Your code', text: 'Code 123456' }]);
  assert.equal(cloudflareMailer({ MAIL_FROM: 'a@b.c' }), null);
  assert.equal(cloudflareMailer({ EMAIL: {} }), null);
});

test('the deployed entry point wires the mailer: sign-up sends a code through the binding', async () => {
  const sent = [];
  const env = { DB: testDb(), EMAIL: { async send(msg) { sent.push(msg); } }, MAIL_FROM: 'community@charltonbornstudios.com', GOOGLE: { async verify() { return null; } } };
  const res = await worker.fetch(new Request('https://x/v1/auth/register', { method: 'POST', body: JSON.stringify({ email: 'n@x.com', password: 'longenough', displayName: 'N' }) }), env);
  assert.equal(res.status, 201);
  assert.equal(sent.length, 1);
  assert.match(sent[0].text, /\d{6}/);
});

test('without email set up, sign-up says so instead of pretending', async () => {
  const env = { DB: testDb(), GOOGLE: { async verify() { return null; } } };
  const res = await worker.fetch(new Request('https://x/v1/auth/register', { method: 'POST', body: JSON.stringify({ email: 'n@x.com', password: 'longenough', displayName: 'N' }) }), env);
  assert.deepEqual([res.status, (await res.json()).error], [503, 'email_not_set_up']);
});
