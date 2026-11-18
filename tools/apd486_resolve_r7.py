#!/usr/bin/env python3
"""Resolve the reviewed eight C466R7/C485 conflicts without changing other code."""
import pathlib, re
ROOT = pathlib.Path('.')
PREFIX = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/'
TEST = 'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/qol/SearchControlContextTest.java'
pattern = re.compile(r'^<<<<<<< HEAD\n(.*?)^=======\n(.*?)^>>>>>>> 34a1471ccd2f56ee21bb8c2579cb154ceb5e21c7\n', re.M|re.S)
def modify(path, operation, count):
    p=ROOT/path;s=p.read_text();matches=list(pattern.finditer(s))
    assert len(matches)==count, (path,len(matches),count)
    s=pattern.sub(lambda m:operation(m.group(1),m.group(2)),s)
    assert not re.search(r'^(<<<<<<<|=======|>>>>>>>)',s,re.M), path
    p.write_text(s)
def once(s,old,new):
    assert s.count(old)==1, (old[:90],s.count(old));return s.replace(old,new,1)
# R7 positions the healing emitter over the hero sprite while retaining the 1x layer.
modify(PREFIX+'actors/blobs/WaterOfHealth.java',lambda a,b:b,4)
# Identical method; retain R7's description of the approved five exempt categories.
modify(PREFIX+'apd/PlaybackGroup.java',lambda a,b:b,1)
def search(a,b):
    if 'boolean wallResume' in a:
        a=once(a,'boolean wallResume = saved && DoorSearchEligibility.supportsDoors(level);',
               'boolean wallResume = saved && hero.wallSearch.matches(level) && hasMeaningfulWall(hero, level);')
        start=a.index('        // On floors')
        end=a.index('        // Allocate only')
        a=a[:start]+('        // R7 requires a current, meaningful wall context, not just saved memory.\n'
                     '        // Keep C485\'s one-array allocation and saved-position controls.\n')+a[end:]
        return a
    # Retain the guarded legacy geometry helper, but not as an R7 eligibility gate.
    a=once(a,'/** A known, open part of a generated room is not a wall-search position.',
           '/** Legacy geometry query, no longer a wall-search eligibility gate.')
    return a+'    }\n\n'+b
modify(PREFIX+'apd/qol/SearchControlContext.java',search,2)
def wall(a,b):
    b=once(b,'cell>=l.length()||!l.heroFOV[cell]',
           'cell>=l.length()||l.heroFOV==null||cell>=l.heroFOV.length||!l.heroFOV[cell]')
    return b
modify(PREFIX+'apd/qol/WallSearchSession.java',wall,1)
def cell(a,b):
    return a+('\t\temitter.pos( p.x, p.y, DungeonTilemap.SIZE, DungeonTilemap.SIZE );\n'
              '\t\treturn emitter;\n\t}\n\n')+b
modify(PREFIX+'effects/CellEmitter.java',cell,1)
def scene(a,b):
    return ('\t/** Compatibility entry point retained for C485 diagnostics and tests. */\n'
            '\tpublic static synchronized Emitter normalSpeedEmitter() {\n'
            '\t\treturn wellEmitter();\n\t}\n\n')+b
modify(PREFIX+'scenes/GameScene.java',scene,1)
# R7 composes its message below from the actual eligible choices.
modify(PREFIX+'windows/WndWallSearchResume.java',lambda a,b:b,1)
# Preserve both independently introduced regression methods.
modify(TEST,lambda a,b:a+'        }\n    }\n\n'+b,1)
p=ROOT/TEST;s=p.read_text()
s=once(s,'expected[0] = new Action[]{Action.SEARCH, Action.WALL_SEARCH, Action.SHOW_SAVED,',
         'expected[0] = new Action[]{Action.SEARCH, Action.SHOW_SAVED,')
s=once(s,'expected[1] = new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH, Action.WALL_SEARCH,',
         'expected[1] = new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH,')
s=once(s,'expected[3] = expected[1];',
         'expected[3] = new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH, Action.WALL_SEARCH,\n'
         '                    Action.SHOW_SAVED, Action.FORGET, Action.CANCEL};')
s=once(s,'assertArrayEquals("eligible floors retain the existing explicit wall resume",\n'
         '                new Action[]{Action.SEARCH, Action.WALL_SEARCH, Action.SHOW_SAVED,\n'
         '                        Action.FORGET, Action.CANCEL}, SearchControlContext.choices(hero, level));',
         'assertArrayEquals("door-capable floor alone does not establish a meaningful wall",\n'
         '                new Action[]{Action.SEARCH, Action.SHOW_SAVED, Action.FORGET, Action.CANCEL},\n'
         '                SearchControlContext.choices(hero, level));\n'
         '        candidate(1, 0);\n'
         '        assertArrayEquals("a matching saved session and actual wall restore wall resume",\n'
         '                new Action[]{Action.WALL_SEARCH, Action.SEARCH, Action.SHOW_SAVED,\n'
         '                        Action.FORGET, Action.CANCEL}, SearchControlContext.choices(hero, level));')
s=once(s,'assertArrayEquals(new Action[]{Action.SEARCH, Action.WALL_SEARCH, Action.SHOW_SAVED,\n'
         '                    Action.FORGET, Action.CANCEL}, SearchControlContext.choices(hero, level));',
         'assertArrayEquals(new Action[]{Action.SEARCH, Action.SHOW_SAVED,\n'
         '                    Action.FORGET, Action.CANCEL}, SearchControlContext.choices(hero, level));')
p.write_text(s)
print('Resolved eight reviewed conflicts; R7 semantics and C485 safety/API compatibility retained.')
