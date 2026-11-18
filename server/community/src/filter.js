// Text filter for everything players post (names, submissions, comments, report reasons).
// Slurs: the post is refused. Profanity: allowed but masked (Community is 16+ and the game has an Adult humor mode),
// so a swear doesn't block a post. Both lists are short starting points; extend them here.
// Matching ignores case, accents, spacing tricks and common letter substitutions (sh1t, f.u.c.k, $lur).

const SLURS = [
  'nigger', 'nigga', 'faggot', 'fag', 'retard', 'tranny', 'chink', 'spic', 'kike', 'wetback',
  'gook', 'coon', 'dyke', 'raghead', 'towelhead', 'beaner', 'paki',
];

const PROFANITY = [
  'fuck', 'shit', 'cunt', 'bitch', 'asshole', 'dick', 'cock', 'pussy', 'bastard', 'motherfucker', 'whore', 'slut',
];

const SUBS = { '0': 'o', '1': 'i', '!': 'i', '3': 'e', '4': 'a', '@': 'a', '5': 's', '$': 's', '7': 't', '+': 't', '8': 'b' };

// One character per input character, so match positions map straight back to the original text.
function fold(text) {
  let out = '';
  for (const ch of text.normalize('NFD')) {
    if (/[̀-ͯ]/.test(ch)) continue;
    const c = ch.toLowerCase();
    out += SUBS[c] || c;
  }
  return out;
}

// Letters only, remembering where each came from, so "f.u.c.k" and "f u c k" still match.
function squeeze(folded) {
  let letters = '';
  const at = [];
  for (let i = 0; i < folded.length; i++) {
    if (/[a-z]/.test(folded[i])) { letters += folded[i]; at.push(i); }
  }
  return { letters, at };
}

// Words that contain a listed word but are fine (Scunthorpe problem). Checked on the squeezed text.
const ALLOWED = ['scunthorpe', 'cockatrice', 'cocktail', 'peacock', 'hancock', 'dickens', 'shitake', 'shiitake',
  'assassin', 'bass', 'class', 'grass', 'pass', 'mass', 'spicy', 'spice', 'raccoon', 'cocoon', 'tycoon',
  'pakistan', 'fagin', 'snigger', 'retardant', 'scrapbook', 'cockpit', 'cockroach', 'documents'];

function hits(letters, words) {
  const found = [];
  const masked = new Array(letters.length).fill(false);
  for (const ok of ALLOWED) {
    let i = letters.indexOf(ok);
    while (i >= 0) { for (let k = i; k < i + ok.length; k++) masked[k] = true; i = letters.indexOf(ok, i + 1); }
  }
  for (const w of words) {
    let i = letters.indexOf(w);
    while (i >= 0) {
      let allowed = true;
      for (let k = i; k < i + w.length; k++) if (!masked[k]) { allowed = false; break; }
      if (!allowed) found.push([i, i + w.length]);
      i = letters.indexOf(w, i + 1);
    }
  }
  return found;
}

export function containsSlur(text) {
  return hits(squeeze(fold(text)).letters, SLURS).length > 0;
}

// Replaces each profane word's letters with '*', keeping everything else as typed.
export function maskProfanity(text) {
  const chars = [...text.normalize('NFD')].filter((c) => !/[̀-ͯ]/.test(c));
  const { letters, at } = squeeze(fold(text));
  for (const [a, b] of hits(letters, PROFANITY)) for (let k = a; k < b; k++) chars[at[k]] = '*';
  return chars.join('').normalize('NFC');
}

// What the server does with player text: null = refused (slur), otherwise the text to store.
export function clean(text) {
  if (containsSlur(text)) return null;
  return maskProfanity(text);
}
