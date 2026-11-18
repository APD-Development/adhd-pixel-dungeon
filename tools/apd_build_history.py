#!/usr/bin/env python3
"""Validate reviewed APK deltas and embed offline notes before compiling. No generated prose."""
import argparse,json,pathlib,os,re,zipfile
def validate(delta,ledger):
    if delta.get("status") not in ("ahead","identical"): raise ValueError("Previous source is not an ancestor")
    files=delta.get("files",[])
    if len(files)>=300 and not delta.get("files_complete", False): raise ValueError("Comparison may be truncated; obtain a complete diff")
    # Every changed source/test/resource/configuration/document path is covered.
    # The CI-provided git list is complete even when GitHub's API caps its diff.
    meaningful={f["filename"] for f in files}
    covered={p for c in ledger["changes"] for p in c["paths"]}
    missing=meaningful-covered
    if missing: raise ValueError("Unreviewed build changes: "+", ".join(sorted(missing)))
    if meaningful and not ledger["changes"]: raise ValueError("A changed build cannot have empty history")
    icon_source=pathlib.Path(__file__).resolve().parents[1]/"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/changelist/ChangeIcons.java"
    icons=set(re.findall(r"(?m)^\s*([A-Z][A-Z0-9_]*)\s*[,;]",icon_source.read_text()))
    ui_source=icon_source.parent.parent/"Icons.java"
    icons.update("ICON_"+n for n in re.findall(r"(?m)^\s*([A-Z][A-Z0-9_]*)\s*[,;]",ui_source.read_text()))
    item_source=icon_source.parents[2]/"sprites/ItemSpriteSheet.java"
    icons.update("ITEM_"+n for n in re.findall(r"public static final int\s+([A-Z][A-Z0-9_]*)",item_source.read_text().split("public static class Icons",1)[0]))
    seen=set()
    for change in ledger["changes"]:
        if not change["text"] or any("\t" in change[k] or "\n" in change[k] for k in ("category","sprite","title","text")) or len(change["text"].split())>100: raise ValueError("Invalid or excessive description")
        key=(change["category"],change["text"])
        if key in seen: raise ValueError("Duplicate change description")
        seen.add(key)
        if not change["title"] or not change["sprite"]: raise ValueError("Missing title or sprite")
        if change["sprite"] not in icons: raise ValueError("Unknown changelog icon: "+change["sprite"])
    return meaningful
def merge_previous_apk(root,apk,expected_label):
    """Preserve compiled history between APKs; reviewed corrections already in source win."""
    assets=root/"core/src/main/assets/apd/history"
    assets.mkdir(parents=True,exist_ok=True)
    index=assets/"index.tsv"
    current=index.read_text().splitlines() if index.exists() else []
    existing={line.split("\t")[0] for line in current if line}
    with zipfile.ZipFile(apk) as archive:
        previous=archive.read("assets/apd/history/index.tsv").decode("utf-8").splitlines()
        if not any(line.split("\t")[1]==expected_label for line in previous if len(line.split("\t"))==4):
            raise ValueError("Previous APK does not contain the expected build record")
        missing=[]
        for line in previous:
            fields=line.split("\t")
            if len(fields)!=4 or not re.fullmatch(r"[a-z0-9-]+",fields[0]):
                raise ValueError("Invalid previous APK history identifier")
            ident=fields[0]
            canonical={'test-1':'private-test-1','test-2':'private-test-2','test-3':'private-test-3'}.get(ident)
            if canonical in existing and (assets/(canonical+'.tsv')).is_file():
                continue # reviewed canonical private history supersedes the old identifier
            if ident not in existing: missing.append(line);existing.add(ident)
            target=assets/(ident+".tsv")
            member="assets/apd/history/"+ident+".tsv"
            if not target.exists() and member in archive.namelist():
                target.write_bytes(archive.read(member))
            if not target.is_file():
                raise ValueError('Previous history points to a missing record: '+ident)
    lines=missing+current
    families=[]
    for line in current+missing:
        family=line.split("\t",1)[0].rsplit("-",1)[0]
        if family not in families:families.append(family)
    lines.sort(key=lambda line:(families.index(line.split("\t",1)[0].rsplit("-",1)[0]),-int(line.split("\t",1)[0].rsplit("-",1)[1])))
    index.write_text("\n".join(lines)+"\n")

def validate_player_notes(root):
    """Keep curated player releases separate from the unabridged technical ledger."""
    import datetime
    assets=root/'core/src/main/assets/apd/releases'
    index=assets/'index.tsv'
    if not index.exists():
        scene=root/'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/APDTextScene.java'
        if scene.exists(): raise ValueError('Missing player release notes catalogue')
        return # Minimal internal-history unit-test fixtures do not contain a game.
    internal=re.compile(r'\bcodex\b|\bTEST\s*\d+\b',re.I)
    categories={'New Content','Gameplay','Quality of Life','Bug Fixes','Statistics / UIMSD','Buffs','Nerfs','Developer Commentary'}
    ids=set()
    for line in index.read_text().splitlines():
        if not line.strip() or line.startswith('#'):continue
        fields=line.split('\t')
        if len(fields)!=4:raise ValueError('Invalid player release record')
        ident,label,date,status=fields
        if not re.fullmatch(r'[a-z0-9-]+',ident) or ident in ids or not label.startswith('APD ') or internal.search(label+' '+status):
            raise ValueError('Player release records require unique IDs and APD branding')
        ids.add(ident)
        if date:datetime.date.fromisoformat(date)
        if ident.endswith('-preview') and date:raise ValueError('An unreleased preview cannot have a publication date')
        descriptions=set()
        for row in (assets/(ident+'.tsv')).read_text().splitlines():
            if not row.strip() or row.startswith('#'):continue
            cells=row.split('\t')
            if len(cells)!=4:raise ValueError('Invalid player change')
            category,sprite,title,text=cells
            if category not in categories or not title or not sprite or sprite=='V061_PAGE' or not text or len(text.split())>100 or internal.search(title+' '+text) or text in descriptions:
                raise ValueError('Player notes must be concise, categorized and free of internal build labels')
            descriptions.add(text)
        if not descriptions:raise ValueError('Empty player release notes')
    if not ids:raise ValueError('Empty player release catalogue')

def embed(root,ledger,number):
    validate_player_notes(root)
    assets=root/"core/src/main/assets/apd/history"
    assets.mkdir(parents=True,exist_ok=True)
    ident="codex-test-"+str(number)
    rows=["\t".join([c["category"],c["sprite"],c["title"],c["text"]]) for c in ledger["changes"]]
    (assets/(ident+".tsv")).write_text("\n".join(rows)+"\n")
    index=assets/"index.tsv"
    lines=[l for l in index.read_text().splitlines() if not l.startswith(ident+"\t")]
    index.write_text(ident+"\tCodex TEST "+str(number)+"\t"+ledger["date"]+"\tAPK change record"+"\n"+"\n".join(lines)+"\n")
def verify_compiled(root,apk):
    validate_player_notes(root)
    assets=root/"core/src/main/assets/apd/history"
    with zipfile.ZipFile(apk) as archive:
        for path in assets.glob("*.tsv"):
            member="assets/apd/history/"+path.name
            if archive.read(member)!=path.read_bytes():
                raise ValueError("Compiled history differs from reviewed source: "+path.name)
        releases=root/"core/src/main/assets/apd/releases"
        for path in releases.glob("*.tsv"):
            if archive.read("assets/apd/releases/"+path.name)!=path.read_bytes():
                raise ValueError("Compiled player release notes differ from reviewed source: "+path.name)

def complete_comparison(delta,previous,current):
    """Obtain every changed path from pinned Git trees, with no rename ambiguity."""
    import subprocess
    data=subprocess.check_output(["git","diff","--name-status","--no-renames","-z",previous,current])
    fields=data.decode("utf-8").split("\0")
    if fields[-1]=="":fields.pop()
    if len(fields)%2:raise ValueError("Incomplete git diff output")
    delta=dict(delta)
    delta["files"]=[{"status":fields[i],"filename":fields[i+1]} for i in range(0,len(fields),2)]
    delta["files_complete"]=True
    return delta

if __name__=="__main__":
    p=argparse.ArgumentParser();p.add_argument("--comparison",required=True);p.add_argument("--ledger",required=True);p.add_argument("--build",required=True);p.add_argument("--root",default=".");p.add_argument("--previous-apk");p.add_argument("--verify-apk");a=p.parse_args()
    ledger=json.loads(pathlib.Path(a.ledger).read_text());delta=json.loads(pathlib.Path(a.comparison).read_text())
    validate(delta,ledger)
    if a.previous_apk: merge_previous_apk(pathlib.Path(a.root),a.previous_apk,ledger["previous_label"].replace("TEST", "TEST ") if "TEST " not in ledger["previous_label"] else ledger["previous_label"])
    embed(pathlib.Path(a.root),ledger,a.build)
    if a.verify_apk:
        verify_compiled(pathlib.Path(a.root),pathlib.Path(a.verify_apk))
    print("Complete reviewed build changes embedded; descriptions are curated, not inferred from commit titles.")


