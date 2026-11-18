#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Apply the bounded, owner-approved APD432 follow-up description correction.

All three original files must match before any write. No gameplay handler,
formatting control, save schema, or gameplay value is changed.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROPERTIES = 'core/src/main/assets/messages/actors/actors.properties'
HELPER = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/mechanics/TalentExplanation.java'
NATIVE = 'android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/QolItemFeedbackDeviceTest.java'
EXPECTED = {
    PROPERTIES: 'a799817d609b2e15278a134c4dd97cecc6daa9e224c581b5248a01394255e6d7',
    HELPER: 'd4ce960d5bb39fbb43d4fee01cbaf6713f46e86221897e16209b2fecbb5bd9e9',
    NATIVE: '530d256940c1b0b75fad44e8b1f2c06a279b9ef7172cea0f21c99232120cafc4',
}
RATIONS = (
    '_+1:_ Up to _2 supply rations_ can be placed in special-room chests as new floors are generated.\\n\\n'
    '_+2:_ Raises that run-wide total to _3 rations_, including any already placed.\\n\\n'
    'Rations are quick to eat, satisfy hunger and add 1 charge to a carried Cloak of Shadows, up to capacity. '
    'They normally restore up to 5 HP; healing rules and missing health determine the actual amount. '
    'They restore no HP when healing is disabled. Learning this talent does not add chests to floors already generated.'
)
FORESIGHT = (
    '_+1:_ On an eligible floor with enough secret rooms, there is a _75% chance_ to get a hint.\\n\\n'
    '_+2:_ Raises that chance to _100%_.\\n\\n'
    'Applies to regular main-dungeon floors with at least one secret room, or at least two on a secrets-feeling floor. '
    'The roll is fixed for that floor; revisiting does not reroll it. A hint does not reveal an entrance, '
    'and no hint does not prove that no secrets exist.'
)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def replace_property(text, key, value):
    lines = text.splitlines(keepends=True)
    matches = [i for i, line in enumerate(lines) if line.startswith(key + '=')]
    if len(matches) != 1:
        raise ValueError(f'Expected exactly one property: {key}')
    lines[matches[0]] = key + '=' + value + '\n'
    return ''.join(lines)


def replace_case(text, name, next_name, replacement):
    start = text.index('            case ' + name + ':\n')
    end = text.index('            case ' + next_name + ':\n', start)
    return text[:start] + replacement + text[end:]


def main():
    original = {}
    for name, expected in EXPECTED.items():
        data = (ROOT / name).read_bytes()
        if digest(data) != expected:
            raise ValueError(f'Source changed; reconcile before applying: {name}')
        original[name] = data.decode('utf-8')
    edited = dict(original)
    edited[PROPERTIES] = replace_property(edited[PROPERTIES], 'actors.hero.talent.cached_rations.desc', RATIONS)
    edited[PROPERTIES] = replace_property(edited[PROPERTIES], 'actors.hero.talent.rogues_foresight.desc', FORESIGHT)
    edited[HELPER] = replace_case(edited[HELPER], 'CACHED_RATIONS', 'ROGUES_FORESIGHT', '''            case CACHED_RATIONS:
                return "_Cached Rations_\\nUp to _"+(1+rank)+" supply rations_ across the run, including rations already placed. New chests are attempted only when eligible floors are first generated and a suitable special-room tile is available. Learning this talent does not refill old floors.\\n\\nRations satisfy hunger and add 1 charge to a carried Cloak of Shadows, up to capacity. Their normal 5 HP healing is adjusted by healing rules and missing health; they restore no HP when healing is disabled.";
''')
    edited[HELPER] = replace_case(edited[HELPER], 'ROGUES_FORESIGHT', 'NATURES_BOUNTY', '''            case ROGUES_FORESIGHT:
                return "_Rogue's Foresight_\\nA _"+(25*(2+rank))+"% chance_ to hint that a regular main-dungeon floor contains secrets. Requires at least one secret room, or at least two on a secrets-feeling floor.\\n\\nThe floor's seeded roll is reused when you return; revisiting does not reroll it. A hint does not reveal an entrance. No hint does not prove that no secrets exist. Secret-room generation is unchanged.";
''')
    anchor = '        final HeroClass[] beforeRechargeClass=new HeroClass[1];'
    if edited[NATIVE].count(anchor) != 1:
        raise ValueError('Expected unique native talent insertion point')
    addition = '''        calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.CACHED_RATIONS,2,null),"no HP when healing is disabled","qol-reviewed-talent-clear-CACHED_RATIONS-native.png");
        calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.ROGUES_FORESIGHT,2,null),"two on a secrets-feeling floor","qol-reviewed-talent-clear-ROGUES_FORESIGHT-native.png");
'''
    edited[NATIVE] = edited[NATIVE].replace(anchor, addition + anchor)
    for name, text in edited.items():
        if text == original[name]:
            raise ValueError(f'Missing intended edit: {name}')
    receipt = {'base': 'c368927e18c18014b91c5be4abb2f3e37e3e7e79',
               'scope': 'Two existing Rogue talent descriptions and existing native windows only',
               'files': {name: {'before_sha256': EXPECTED[name],
                                'after_sha256': digest(text.encode('utf-8'))}
                         for name, text in edited.items()}}
    for name, text in edited.items():
        (ROOT / name).write_text(text, encoding='utf-8')
    (ROOT / 'rogue-text-patch.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))


if __name__ == '__main__':
    main()
