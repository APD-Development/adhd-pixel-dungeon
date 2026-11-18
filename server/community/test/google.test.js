import { test } from 'node:test';
import assert from 'node:assert/strict';
import { googleVerifier } from '../src/google.js';

const b64url = (bytes) => Buffer.from(bytes).toString('base64url');

async function signer() {
  const kp = await crypto.subtle.generateKey({ name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' }, true, ['sign', 'verify']);
  const jwk = { ...(await crypto.subtle.exportKey('jwk', kp.publicKey)), kid: 'k1' };
  const sign = async (claims, header = { alg: 'RS256', kid: 'k1' }) => {
    const h = b64url(JSON.stringify(header)), p = b64url(JSON.stringify(claims));
    const s = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', kp.privateKey, new TextEncoder().encode(`${h}.${p}`));
    return `${h}.${p}.${b64url(new Uint8Array(s))}`;
  };
  return { jwk, sign };
}

test('accepts a real signed token for our client; rejects wrong audience, issuer, expiry, signature and algorithm', async () => {
  const t = 1_800_000_000_000;
  const { jwk, sign } = await signer();
  const other = await signer();
  let fetches = 0;
  const v = googleVerifier({ clientId: 'apd.apps.googleusercontent.com', now: () => t, fetchKeys: async () => { fetches++; return { keys: [jwk] }; } });
  const good = { iss: 'https://accounts.google.com', aud: 'apd.apps.googleusercontent.com', sub: '42', email: 'p@gmail.com', email_verified: true, exp: t / 1000 + 600 };
  assert.deepEqual(await v.verify(await sign(good)), { sub: '42', email: 'p@gmail.com' });
  assert.deepEqual(await v.verify(await sign({ ...good, email_verified: false })), { sub: '42', email: null });
  assert.equal(await v.verify(await sign({ ...good, aud: 'someone-else' })), null);
  assert.equal(await v.verify(await sign({ ...good, iss: 'evil.com' })), null);
  assert.equal(await v.verify(await sign({ ...good, exp: t / 1000 - 1 })), null);
  assert.equal(await v.verify(await other.sign(good)), null); // signed by a key Google didn't publish
  assert.equal(await v.verify(await sign(good, { alg: 'none', kid: 'k1' })), null);
  assert.equal(await v.verify('garbage'), null);
  assert.equal(fetches <= 2, true); // keys are cached
});
