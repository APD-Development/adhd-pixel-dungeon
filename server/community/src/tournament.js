// Season results, worked out from the votes every time they're asked for (nothing stored that could drift).
// Rules (Chris, Nov 18 2026):
//  Month 2: upvote any number of submissions, one upvote each, never your own. The 16 most-upvoted qualify;
//           everything tied with 16th place also qualifies, so there can be more than 16.
//  Tournament week 1: every account picks up to 8 of the qualifiers. The top 8 by picks advance.
//           A tie at the cut goes to the higher Month 2 score.
//  Weeks 2-4: seeded 1v1 bracket from week 1's order (1v8, 4v5, 2v7, 3v6, then winners meet), one vote per account
//           per matchup. A tied matchup goes to the higher seed.
// Decisions made for Chris (revert if he says no): a full tie (same score and same tiebreak) goes to the earlier
// submission; you can't vote for your own submission in the tournament either; votes from banned accounts and
// hidden/removed submissions don't count.

export const QUALIFY = 16;
export const ADVANCE = 8;

// entries: [{id, created_at, score}] -> sorted best first, ties broken by the tiebreak function then by age.
function rank(entries, tiebreak = () => 0) {
  return [...entries].sort((a, b) => b.score - a.score || tiebreak(a, b) || a.created_at - b.created_at || (a.id < b.id ? -1 : 1));
}

// Month 2 result: everyone at or above 16th place's score.
export function qualifiers(entries) {
  const ranked = rank(entries);
  if (ranked.length <= QUALIFY) return ranked;
  const line = ranked[QUALIFY - 1].score;
  return ranked.filter((e, i) => i < QUALIFY || e.score === line);
}

// Week 1 result: top 8 by picks; Month 2 score breaks ties. Returned order is the bracket seeding.
export function weekOneSeeds(qualified, picks) {
  const m2 = new Map(qualified.map((e) => [e.id, e.score]));
  const scored = qualified.map((e) => ({ ...e, month2: m2.get(e.id), score: picks.get(e.id) || 0 }));
  return rank(scored, (a, b) => b.month2 - a.month2).slice(0, ADVANCE);
}

// Bracket order so 1 and 2 can only meet in the final.
const FIRST_ROUND = [[0, 7], [3, 4], [1, 6], [2, 5]];

// seeds: week 1 order. votesByWeek: week -> Map(matchupIndex -> Map(submissionId -> votes)).
// Returns the rounds for weeks 2..upToWeek: each matchup {a, b, votesA, votesB, winner}.
export function bracket(seeds, votesByWeek, upToWeek) {
  if (seeds.length < 2) return [];
  const seedOf = new Map(seeds.map((e, i) => [e.id, i]));
  // Fewer than 8 qualifiers: a missing opponent is a bye and the seed goes through without a vote.
  let pairs = FIRST_ROUND.map(([x, y]) => [seeds[x]?.id ?? null, seeds[y]?.id ?? null]);
  const rounds = [];
  for (let week = 2; week <= Math.min(upToWeek, 4); week++) {
    const votes = votesByWeek.get(week) || new Map();
    const round = pairs.map(([a, b], i) => {
      if (!a || !b) return { a, b, votesA: 0, votesB: 0, winner: a || b, bye: true };
      const v = votes.get(i) || new Map();
      const va = v.get(a) || 0, vb = v.get(b) || 0;
      return { a, b, votesA: va, votesB: vb, winner: va > vb ? a : vb > va ? b : (seedOf.get(a) < seedOf.get(b) ? a : b) };
    });
    rounds.push({ week, matchups: round });
    const winners = round.map((m) => m.winner);
    if (winners.length < 2) break;
    pairs = [];
    for (let i = 0; i + 1 < winners.length; i += 2) pairs.push([winners[i], winners[i + 1]]);
  }
  return rounds;
}
