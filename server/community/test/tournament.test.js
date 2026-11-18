import { test } from 'node:test';
import assert from 'node:assert/strict';
import { qualifiers, weekOneSeeds, bracket } from '../src/tournament.js';

const e = (id, score, created_at = 0) => ({ id, score, created_at });

test('Month 2: top 16 qualify, and everything tied with 16th place gets in too', () => {
  const few = [e('a', 1), e('b', 0)];
  assert.deepEqual(qualifiers(few).map((x) => x.id), ['a', 'b']);
  const many = [];
  for (let i = 0; i < 15; i++) many.push(e(`top${i}`, 100 - i));
  many.push(e('tieA', 10, 1), e('tieB', 10, 2), e('tieC', 10, 3), e('low', 9));
  const q = qualifiers(many);
  assert.equal(q.length, 18);
  assert.deepEqual(q.slice(15).map((x) => x.id), ['tieA', 'tieB', 'tieC']);
  assert.ok(!q.some((x) => x.id === 'low'));
});

test('Week 1: top 8 by picks advance; a tie at the cut goes to the higher Month 2 score, then the earlier submission', () => {
  const q = [];
  for (let i = 0; i < 16; i++) q.push(e(`s${i}`, 50 - i, i));
  const picks = new Map();
  for (let i = 0; i < 7; i++) picks.set(`s${i}`, 100 - i);
  picks.set('s9', 5); // tied for 8th with s12, but s9 has the higher Month 2 score
  picks.set('s12', 5);
  const seeds = weekOneSeeds(q, picks);
  assert.equal(seeds.length, 8);
  assert.equal(seeds[7].id, 's9');
  // Full tie: same picks, same Month 2 score -> earlier submission
  const q2 = [e('late', 5, 9), e('early', 5, 1)];
  assert.deepEqual(weekOneSeeds(q2, new Map()).map((x) => x.id), ['early', 'late']);
});

test('Bracket: 1v8, 4v5, 2v7, 3v6, winners meet; a tied matchup goes to the higher seed', () => {
  const seeds = ['s1', 's2', 's3', 's4', 's5', 's6', 's7', 's8'].map((id) => ({ id }));
  const v = (pairs) => new Map(pairs.map(([slot, a, va, b, vb]) => [slot, new Map([[a, va], [b, vb]])]));
  const votes = new Map([
    [2, v([[0, 's1', 3, 's8', 5], [1, 's4', 2, 's5', 2], [2, 's2', 9, 's7', 1], [3, 's3', 0, 's6', 1]])],
    [3, v([[0, 's8', 1, 's4', 1], [1, 's2', 4, 's6', 6]])],
    [4, v([[0, 's4', 2, 's6', 7]])],
  ]);
  const rounds = bracket(seeds, votes, 4);
  assert.deepEqual(rounds[0].matchups.map((m) => [m.a, m.b]), [['s1', 's8'], ['s4', 's5'], ['s2', 's7'], ['s3', 's6']]);
  assert.deepEqual(rounds[0].matchups.map((m) => m.winner), ['s8', 's4', 's2', 's6']); // s4 v s5 tied: s4 is the higher seed
  assert.deepEqual(rounds[1].matchups.map((m) => [m.a, m.b, m.winner]), [['s8', 's4', 's4'], ['s2', 's6', 's6']]);
  assert.deepEqual(rounds[2].matchups.map((m) => [m.a, m.b, m.winner]), [['s4', 's6', 's6']]);
  // No votes at all: every matchup goes to the higher seed, so seed 1 wins
  const quiet = bracket(seeds, new Map(), 4);
  assert.equal(quiet[2].matchups[0].winner, 's1');
});

test('Bracket with fewer than 8 qualifiers: missing opponents are byes and those seeds go through', () => {
  const seeds = ['a', 'b', 'c', 'd', 'e'].map((id) => ({ id }));
  const rounds = bracket(seeds, new Map([[2, new Map([[1, new Map([['e', 3]])]])]]), 4);
  assert.deepEqual(rounds[0].matchups.map((m) => [m.a, m.b, m.winner, !!m.bye]),
    [['a', null, 'a', true], ['d', 'e', 'e', false], ['b', null, 'b', true], ['c', null, 'c', true]]);
  assert.deepEqual(rounds[1].matchups.map((m) => [m.a, m.b, m.winner]), [['a', 'e', 'a'], ['b', 'c', 'b']]);
  assert.equal(rounds[2].matchups[0].winner, 'a');
  assert.deepEqual(bracket([{ id: 'solo' }], new Map(), 4), []);
});
