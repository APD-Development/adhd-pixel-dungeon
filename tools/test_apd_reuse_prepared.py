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

import hashlib,json,pathlib,tempfile,unittest,zipfile
from apd_reuse_prepared import verify_prepared
class PreparedReuseTest(unittest.TestCase):
    def fixture(self,root,extra=None):
        h=lambda b:hashlib.sha256(b).hexdigest()
        apk={'Developer':b'developer fixture','Alpha-Testers':b'alpha fixture'}
        source=b'key-free source fixture'
        proof={'number':368,'source_commit':'b'*40,'core_tests':2,'android_tests':1,'application_id':'fixture','version_code':920,'signing_certificate':'c'*64,'apk_hashes':{k:h(v) for k,v in apk.items()},'source_sha256':h(source),'physical_phone_verified':False}
        files={'APD-Preview-368-Verification.json':json.dumps(proof).encode(),'APD-Source-368.zip':source,'APD-Source-Scan.json':json.dumps({'supplemental':{'source_sha256':h(source),'source_findings_reviewed':True}}).encode()}
        for label,data in apk.items():files[f'APD-{label}-368.apk']=data;files[f'APD-{label}-368.apk.sha256']=(h(data)+' fixture').encode()
        if extra:files.update(extra)
        archive=root/'prepared.zip'
        with zipfile.ZipFile(archive,'w') as z:
            for name,data in files.items():z.writestr(name,data)
        spec={**proof,'source':proof['source_commit'],'artifact_sha256':h(archive.read_bytes())}
        return archive,spec
    def test_exact_pinned_package_is_accepted(self):
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder);archive,spec=self.fixture(root)
            self.assertEqual('b'*40,verify_prepared(archive,spec,root/'out')['source_commit'])
    def test_changed_outer_bytes_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder);archive,spec=self.fixture(root);spec['artifact_sha256']='0'*64
            with self.assertRaises(AssertionError):verify_prepared(archive,spec,root/'out')
    def test_changed_apk_rejected_even_if_outer_archive_digest_matches(self):
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder);archive,spec=self.fixture(root,{'APD-Developer-368.apk':b'changed'})
            with self.assertRaises(AssertionError):verify_prepared(archive,spec,root/'out')
    def test_path_escape_rejected_and_not_written(self):
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder);archive,spec=self.fixture(root,{'../escape':b'bad'})
            with self.assertRaises(AssertionError):verify_prepared(archive,spec,root/'out')
            self.assertFalse((root/'escape').exists())
    def test_wrong_source_or_signing_receipt_rejected(self):
        for key,value in [('source','d'*40),('signing_certificate','d'*64)]:
            with tempfile.TemporaryDirectory() as folder:
                root=pathlib.Path(folder);archive,spec=self.fixture(root);spec[key]=value
                with self.assertRaises(AssertionError):verify_prepared(archive,spec,root/'out')
