"""Summarize preserved real-game evidence; this tool does not simulate gameplay."""
import argparse
import hashlib
import json
from pathlib import Path
import re


def summarize(root, first_seed, count, allowed_sources):
    if not 1 <= count <= 100:
        raise ValueError('Expected 1–100 seeds')
    expected = set(range(first_seed, first_seed + count))
    attempts = {}
    mirrored = 0
    for file in sorted(Path(root).rglob('simulation-results.jsonl')):
        manifest_files = list(file.parent.rglob('manifest.json'))
        if len(manifest_files) != 1:
            raise ValueError(f'Missing or ambiguous source manifest: {file}')
        manifest = json.loads(manifest_files[0].read_text())
        source = manifest.get('sourceCommit')
        if source not in allowed_sources or not re.fullmatch('[0-9a-f]{40}', source):
            raise ValueError('Unapproved simulator source')
        if not str(manifest.get('verificationRunId', '')).isdigit():
            raise ValueError('Missing baseline verification run')
        for line in file.read_text().splitlines():
            if not line.strip():
                continue
            row = json.loads(line)
            if type(row.get('seed')) is not int or row['seed'] not in expected:
                raise ValueError('Unexpected seed')
            if type(row.get('completed')) is not bool:
                raise ValueError('Completion must be a boolean')
            if 'profile' in row and (not isinstance(row['profile'],str) or not row['profile']):
                raise ValueError('Invalid recorded play profile')
            if not isinstance(row.get('trace'), list) or not row['trace'] or not all(isinstance(t, str) for t in row['trace']):
                raise ValueError('Missing actual action trace')
            for key in ('elapsedMillis', 'startedEpochMillis', 'finishedEpochMillis'):
                if type(row.get(key)) is not int or row[key] < 0:
                    raise ValueError('Missing or invalid measured timing')
            for key in ('observedHealingBeforeNormalFlyHealing', 'normalFlyHealingObserved'):
                if type(row.get(key)) is not bool:
                    raise ValueError('Invalid healing observation')
            if row['completed'] and row.get('stopReason') not in ('death', 'scenario finished'):
                raise ValueError('Failed or capped episode cannot be complete')
            if not row['completed'] and row.get('stopReason') not in ('game adapter failure', 'no state progress', 'action limit'):
                raise ValueError('Unknown failure reason')
            if row.get('stopReason') == 'death' and (type(row.get('terminalHp')) is not int or row['terminalHp'] > 0):
                raise ValueError('Death episode has positive or missing HP')
            key = (source, row['seed'], row['startedEpochMillis'])
            if key in attempts:
                if attempts[key]['result'] != row:
                    raise ValueError('Conflicting copies of one attempt')
                mirrored += 1
                continue
            attempts[key] = dict(sourceCommit=source, verificationRunId=manifest['verificationRunId'],
                                 result=row, traceSha256=hashlib.sha256(json.dumps(row['trace']).encode()).hexdigest())
    records = sorted(attempts.values(), key=lambda a: (a['result']['seed'], a['result']['startedEpochMillis'], a['sourceCommit']))
    completed_seeds = {a['result']['seed'] for a in records if a['result']['completed']}
    sources = sorted({a['sourceCommit'] for a in records})
    groups = []
    for source in sources:
        source_records = [a for a in records if a['sourceCommit'] == source]
        # Repeated executions are retained; statistics use the first completed attempt per seed.
        selected = {}
        for attempt in source_records:
            row = attempt['result']
            if row['completed']:
                selected.setdefault(row['seed'], row)
        rows = list(selected.values())
        # One compiled source can eventually support different play strategies.
        # Preserve all attempts, but never combine those strategies into one cohort.
        selected_by_profile = {}
        for attempt in source_records:
            row = attempt['result']
            if row['completed']:
                selected_by_profile.setdefault(row.get('profile','legacy: unrecorded'), {}).setdefault(row['seed'],row)
        profile_counts = {profile:len(seeds) for profile,seeds in sorted(selected_by_profile.items())}
        profile_results = [dict(profile=profile, completedDistinctSeeds=len(seeds),
                                consumedHealingBeforeNormalFlyHealing=sum(r['observedHealingBeforeNormalFlyHealing'] for r in seeds.values()),
                                normalFlyHealingObserved=sum(r['normalFlyHealingObserved'] for r in seeds.values()),
                                deathEpisodes=sum(r['stopReason']=='death' for r in seeds.values()))
                           for profile,seeds in sorted(selected_by_profile.items())]
        all_encounters, completed_encounters = {}, {}
        for attempt in source_records:
            counts = attempt['result'].get('encounterCounts', {})
            if not isinstance(counts, dict) or any(not isinstance(k,str) or type(v) is not int or v < 0 for k,v in counts.items()):
                raise ValueError('Invalid encounter diagnostics')
            for name, value in counts.items():
                all_encounters[name] = all_encounters.get(name,0) + value
        for row in rows:
            for name, value in row.get('encounterCounts', {}).items():
                completed_encounters[name] = completed_encounters.get(name,0) + value
        groups.append(dict(sourceCommit=source, attempts=len(source_records), completedDistinctSeeds=len(rows),
                           profiles=sorted({a['result'].get('profile','legacy: unrecorded') for a in source_records}),
                           completedDistinctSeedsByProfile=profile_counts, perProfile=profile_results,
                           consumedHealingBeforeNormalFlyHealing=sum(r['observedHealingBeforeNormalFlyHealing'] for r in rows),
                           normalFlyHealingObserved=sum(r['normalFlyHealingObserved'] for r in rows),
                           deathEpisodes=sum(r['stopReason'] == 'death' for r in rows),
                           attemptsWithoutEncounterBreakdown=sum('encounterCounts' not in a['result'] for a in source_records),
                           encounterCountsAllAttempts=all_encounters, encounterCountsCompletedAttempts=completed_encounters))
    report = dict(requested=count, actualAttempts=len(records), mirroredCopiesIgnored=mirrored,
                  completedDistinctSeeds=len(completed_seeds), unresolvedSeeds=sorted(expected-completed_seeds),
                  allRequestedSeedsCompleted=len(completed_seeds) == count, perSource=groups,
                  singleSourceComplete=any(n == count for g in groups for n in g['completedDistinctSeedsByProfile'].values()),
                  summedEpisodeElapsedMillis=sum(a['result']['elapsedMillis'] for a in records),
                  interpretation='Early-game bot episodes, not whole-run wins or causal survival evidence. '
                                 'Use perProfile cohorts; perSource totals are diagnostics, not pooled balance evidence. '
                                 'Do not pool different adapters or play profiles for balance analysis. Parallel episode time is not wall time, '
                                 'developer active time, or downtime.')
    return report, records


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('evidence')
    parser.add_argument('--first-seed', type=int, default=1000000)
    parser.add_argument('--count', type=int, default=100)
    parser.add_argument('--allowed-source', action='append', required=True)
    parser.add_argument('--output', default='simulation-summary.json')
    args = parser.parse_args()
    report, records = summarize(args.evidence, args.first_seed, args.count, set(args.allowed_source))
    Path(args.output).write_text(json.dumps(report, indent=2)+'\n')
    Path(args.output).with_suffix('.attempts.jsonl').write_text(''.join(json.dumps(a)+'\n' for a in records))
    print(json.dumps(report, indent=2))
    if not report['allRequestedSeedsCompleted'] or not report['singleSourceComplete']:
        raise SystemExit(1)
