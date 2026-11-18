#!/usr/bin/env python3
# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>

"""Reject desktop JSON float overloads in production bytecode (absent on Android)."""
import pathlib,struct,sys

def method_refs(data):
    if data[:4]!=b'\xca\xfe\xba\xbe':raise ValueError('Not a Java class')
    count=struct.unpack_from('>H',data,8)[0];cp=[None]*count;at=10;i=1
    while i<count:
        tag=data[at];at+=1
        if tag==1:
            n=struct.unpack_from('>H',data,at)[0];at+=2;cp[i]=(tag,data[at:at+n].decode('utf-8',errors='replace'));at+=n
        elif tag in (7,8,16,19,20):cp[i]=(tag,struct.unpack_from('>H',data,at)[0]);at+=2
        elif tag in (9,10,11,12,17,18):cp[i]=(tag,*struct.unpack_from('>HH',data,at));at+=4
        elif tag in (3,4):at+=4
        elif tag in (5,6):at+=8;i+=1
        elif tag==15:at+=3
        else:raise ValueError('Unknown constant-pool tag '+str(tag))
        i+=1
    for item in cp:
        if item and item[0] in (10,11):
            owner=cp[cp[item[1]][1]][1];name_type=cp[item[2]]
            yield owner,cp[name_type[1]][1],cp[name_type[2]][1]

def check(root):
    root=pathlib.Path(root);failures=[];seen=set();classes=0
    for module in ('core','SPD-classes','services','android','desktop'):
        for p in (root/module/'build').rglob('*.class'):
            # Test code may deliberately inspect newer desktop-only APIs; it never ships.
            if '/test/' in p.as_posix() or '/androidTest/' in p.as_posix() or '/testFixtures/' in p.as_posix():continue
            classes+=1
            for owner,name,descriptor in method_refs(p.read_bytes()):
                if owner.startswith('org/json/'):
                    seen.add((owner,name,descriptor))
                    if 'F' in descriptor:failures.append((str(p),owner,name,descriptor))
    if not classes or not seen:raise AssertionError('Missing compiled production JSON callers')
    return classes,seen,failures

if __name__=='__main__':
    classes,seen,failures=check(sys.argv[1] if len(sys.argv)>1 else '.')
    print('Checked production classes:',classes,'distinct JSON references:',len(seen))
    for failure in failures:print('UNSUPPORTED_ANDROID_JSON_FLOAT_ABI',*failure)
    raise SystemExit(bool(failures))
