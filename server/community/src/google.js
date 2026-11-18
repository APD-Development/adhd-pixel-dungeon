// Checks a Google Sign-In ID token (a JWT signed by Google) without any library: RS256 signature against Google's
// published keys, issuer, audience (our OAuth client id) and expiry.
const CERTS = 'https://www.googleapis.com/oauth2/v3/certs';
const ISSUERS = ['accounts.google.com', 'https://accounts.google.com'];

function b64urlBytes(s) {
  const b = atob(s.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - (s.length % 4)) % 4));
  return Uint8Array.from(b, (c) => c.charCodeAt(0));
}
const b64urlJson = (s) => JSON.parse(new TextDecoder().decode(b64urlBytes(s)));

// clientId: the Android/web OAuth client id(s) the token must be issued for (comma-separated allowed).
// fetchKeys: () => {keys:[JWK]}; injectable for tests. Keys are cached for an hour.
export function googleVerifier({ clientId, fetchKeys = async () => (await fetch(CERTS)).json(), now = () => Date.now() }) {
  const audiences = String(clientId || '').split(',').map((s) => s.trim()).filter(Boolean);
  let cache = null;
  async function keyFor(kid) {
    if (!cache || cache.at + 3600e3 < now() || !cache.keys.some((k) => k.kid === kid)) {
      cache = { at: now(), keys: (await fetchKeys()).keys || [] };
    }
    return cache.keys.find((k) => k.kid === kid) || null;
  }
  return {
    async verify(token) {
      try {
        const [h, p, sig] = String(token).split('.');
        if (!h || !p || !sig) return null;
        const header = b64urlJson(h);
        if (header.alg !== 'RS256') return null;
        const jwk = await keyFor(header.kid);
        if (!jwk) return null;
        const key = await crypto.subtle.importKey('jwk', { kty: jwk.kty, n: jwk.n, e: jwk.e, alg: 'RS256', ext: true },
          { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['verify']);
        const ok = await crypto.subtle.verify('RSASSA-PKCS1-v1_5', key, b64urlBytes(sig), new TextEncoder().encode(`${h}.${p}`));
        if (!ok) return null;
        const c = b64urlJson(p);
        if (!ISSUERS.includes(c.iss) || !audiences.includes(c.aud) || !(c.exp * 1000 > now()) || !c.sub) return null;
        return { sub: String(c.sub), email: c.email_verified ? c.email : null };
      } catch {
        return null;
      }
    },
  };
}
