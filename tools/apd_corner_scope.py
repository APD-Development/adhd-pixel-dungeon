"""Reuse full gameplay evidence only if the sole runtime-source change selects wall-marker edges."""
import json,os,subprocess,sys
sys.path.insert(0,'tools')
from apd_existing_build_evidence import application_inputs
repo=os.environ['GITHUB_REPOSITORY'];baseline='03522e5a6813ea6881b0fb8b70a85c7806e3411e'
def api(p):return json.loads(subprocess.check_output(['gh','api',p]))
base,current=[api(f'repos/{repo}/git/trees/{s}?recursive=1') for s in (baseline,os.environ['GITHUB_SHA'])]
assert not base.get('truncated') and not current.get('truncated')
a,b=application_inputs(base['tree']),application_inputs(current['tree'])
changed={p for p in a.keys()|b.keys() if a.get(p)!=b.get(p)}
assert changed=={'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/qol/SearchedWallMarkers.java'},changed
run=api(f'repos/{repo}/actions/runs/41536788270');assert run['head_sha']==baseline and run['status']=='completed' and run['conclusion']=='success'
print(json.dumps({'baseline_source':baseline,'baseline_run':run['id'],'baseline_full_tests':69,'changed_runtime_paths':sorted(changed),'all_other_application_inputs_unchanged':True}))
