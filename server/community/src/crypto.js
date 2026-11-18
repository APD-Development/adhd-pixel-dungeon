// Small crypto helpers on Web Crypto, which both Cloudflare Workers and Node 20+ provide.

const enc = new TextEncoder();

export function hex(bytes) {
  return [...new Uint8Array(bytes)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

export function randomHex(byteCount) {
  const b = new Uint8Array(byteCount);
  crypto.getRandomValues(b);
  return hex(b);
}

export function newId() {
  return randomHex(12);
}

// 6-digit code for email verification, uniformly random.
export function newCode() {
  const b = new Uint32Array(1);
  let n;
  do { crypto.getRandomValues(b); n = b[0]; } while (n >= 4294000000); // drop the biased tail
  return String(n % 1000000).padStart(6, '0');
}

export async function sha256(text) {
  return hex(await crypto.subtle.digest('SHA-256', enc.encode(text)));
}

// Workers cap PBKDF2 at 100,000 iterations.
const ITERATIONS = 100000;

async function pbkdf2(password, saltHex, iterations) {
  const key = await crypto.subtle.importKey('raw', enc.encode(password), 'PBKDF2', false, ['deriveBits']);
  const salt = new Uint8Array(saltHex.match(/../g).map((h) => parseInt(h, 16)));
  return hex(await crypto.subtle.deriveBits({ name: 'PBKDF2', hash: 'SHA-256', salt, iterations }, key, 256));
}

export async function hashPassword(password) {
  const salt = randomHex(16);
  return `pbkdf2$${ITERATIONS}$${salt}$${await pbkdf2(password, salt, ITERATIONS)}`;
}

export async function verifyPassword(password, stored) {
  if (!stored) return false;
  const [kind, iter, salt, want] = stored.split('$');
  if (kind !== 'pbkdf2') return false;
  const got = await pbkdf2(password, salt, Number(iter));
  return constantTimeEqual(got, want);
}

export function constantTimeEqual(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}
