import os,re,json,pathlib,xml.etree.ElementTree as E
from PIL import Image
text=pathlib.Path('title-instrumentation.txt').read_text()
expected=int(os.environ['APD_EXPECTED_METHODS'])
assert re.search(r'OK \('+str(expected)+r' tests?\)',text),text[-5000:]
parts=re.split(r'INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)',text)
started=[];finished=[]
for i in range(1,len(parts),2):
    block=parts[i-1];code=int(parts[i])
    cls=re.findall(r'^INSTRUMENTATION_STATUS: class=(.+)$',block,re.M)
    name=re.findall(r'^INSTRUMENTATION_STATUS: test=(.+)$',block,re.M)
    if not cls or not name:continue
    key=(cls[-1].strip(),name[-1].strip())
    if code==1:started.append(key)
    else:
        assert code==0,(key,code,block)
        finished.append(key)
assert len(started)==len(set(started))==len(finished)==len(set(finished))==expected
assert set(started)==set(finished)
root=E.Element('testsuite',name='native-android-confirmations',tests=str(expected),failures='0',errors='0',skipped='0')
for cls,name in finished:E.SubElement(root,'testcase',classname=cls,name=name)
E.ElementTree(root).write('title-evidence/TEST-native.xml',encoding='utf-8',xml_declaration=True)
images={}
for name in os.environ['APD_EXPECTED_IMAGES'].split():
    p=pathlib.Path('title-evidence')/name
    with Image.open(p)as im:im.verify()
    with Image.open(p)as im:images[name]=list(im.size)
pathlib.Path('title-evidence/device-acceptance-result.json').write_text(json.dumps({'methods':expected,'failures':0,'errors':0,'skipped':0,'images':images,'raw_instrumentation_retained':True,'xml_derived_from_actual_android_status_codes':True,'physical_device':False},indent=2))
print('Verified actual Android method identities:',len(finished),'images:',len(images))
