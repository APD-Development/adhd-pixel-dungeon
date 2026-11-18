#!/usr/bin/env python3
"""Physical source-line inventory from an exact Git commit; no estimates."""
import argparse, collections, csv, io, json, pathlib, subprocess, tarfile
EXTENSIONS={'.java','.kt','.py','.sh','.gradle','.yml','.yaml'}
PREFIX='core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/'
def component(path):
    if path.startswith('core/src/test/'): return 'Core unit tests'
    if path.startswith('android/src/androidTest/'): return 'Android regression and simulator tests'
    if path.startswith(PREFIX+'apd/'):
        return 'APD: '+path[len(PREFIX+'apd/'):].split('/')[0]
    if path.startswith(PREFIX+'windows/WndUimsd'): return 'UIMSD windows'
    if path.startswith(PREFIX):
        remainder=path[len(PREFIX):]
        return 'Core gameplay/UI: '+(remainder.split('/')[0] if '/' in remainder else 'shared classes')
    if path.startswith('SPD-classes/'): return 'Rendering, input and utility engine'
    if path.startswith('android/src/main/'): return 'Android platform'
    if path.startswith('desktop/src/main/'): return 'Desktop platform'
    if path.startswith('services/'): return 'News/update services'
    if path.startswith('tools/'): return 'Development and simulator tools'
    if path.startswith('.github/workflows/'): return 'GitHub build/test/delivery workflows'
    return 'Other source/build configuration'
def inventory(ref):
    archive=subprocess.check_output(['git','archive','--format=tar',ref])
    rows=[]
    with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
        for entry in tar:
            if not entry.isfile() or pathlib.PurePosixPath(entry.name).suffix not in EXTENSIONS: continue
            lines=tar.extractfile(entry).read().decode('utf-8').splitlines()
            rows.append((component(entry.name),entry.name,len(lines),sum(bool(line.strip()) for line in lines)))
    return sorted(rows)
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('ref'); parser.add_argument('destination')
    args=parser.parse_args()
    ref=subprocess.check_output(['git','rev-parse',args.ref+'^{commit}'],text=True).strip()
    rows=inventory(ref); totals=collections.defaultdict(lambda:[0,0,0])
    for group,path,lines,nonblank in rows:
        totals[group][0]+=1; totals[group][1]+=lines; totals[group][2]+=nonblank
    destination=pathlib.Path(args.destination); destination.mkdir(parents=True,exist_ok=True)
    with (destination/'APD-Source-Line-Counts.csv').open('w',newline='') as file:
        writer=csv.writer(file); writer.writerow(['component','path','physical_lines','nonblank_lines']); writer.writerows(rows)
    text='# Source line counts\n\nExact source commit: '+ref+'\n\nPhysical lines include comments and blank lines; nonblank lines still include comments. These are file-size measurements, not executable-statement counts. Every source file belongs to one component. Core gameplay/UI contains inherited SPD code and APD modifications; these counts do not claim to separate authorship. Binary assets, documents, generated outputs and dependencies are excluded.\n\n| Component | Files | Physical lines | Nonblank lines |\n|---|---:|---:|---:|\n'
    for group,counts in sorted(totals.items()): text+='| '+group+' | '+' | '.join(map(str,counts))+' |\n'
    text+='| TOTAL | '+str(len(rows))+' | '+str(sum(row[2] for row in rows))+' | '+str(sum(row[3] for row in rows))+' |\n'
    (destination/'APD-Source-Line-Counts.md').write_text(text)
    (destination/'APD-Source-Line-Counts.json').write_text(json.dumps({'source_commit':ref,'components':dict(totals),'files':len(rows),'physical_lines':sum(row[2] for row in rows),'nonblank_lines':sum(row[3] for row in rows)},indent=2)+'\n')
    print(text)
if __name__=='__main__': main()
