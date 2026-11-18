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

import json,os,pathlib,runpy,tempfile,unittest
from unittest.mock import patch
from apd_owner_delivery_policy import require_apk_delivery_allowed

class OwnerDeliveryPolicyTest(unittest.TestCase):
    def policy(self,root,value):
        path=root/"docs/APD-OWNER-DELIVERY-HOLD.json";path.parent.mkdir(exist_ok=True);path.write_text(json.dumps(value))
    def test_active_hold_blocks(self):
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder);self.policy(root,{"active":True})
            with self.assertRaisesRegex(RuntimeError,"hold is active"):require_apk_delivery_allowed(root)
    def test_recorded_clear_state_allows_later_verified_delivery(self):
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder);self.policy(root,{"active":False});require_apk_delivery_allowed(root)
    def test_missing_policy_blocks(self):
        with tempfile.TemporaryDirectory() as folder:
            with self.assertRaisesRegex(RuntimeError,"missing"):require_apk_delivery_allowed(folder)
    def test_malformed_or_ambiguous_policy_blocks(self):
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder)
            for value in [{"active":"false"},{"active":0},{},None]:
                self.policy(root,value)
                with self.assertRaisesRegex(RuntimeError,"invalid"):require_apk_delivery_allowed(root)
            (root/"docs/APD-OWNER-DELIVERY-HOLD.json").write_text("not json")
            with self.assertRaisesRegex(RuntimeError,"invalid"):require_apk_delivery_allowed(root)
    def test_actual_publisher_stops_before_environment_or_network_use(self):
        publisher=pathlib.Path(__file__).resolve().with_name("apd_qol_publish_prepared.py")
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder);self.policy(root,{"active":True});previous=pathlib.Path.cwd()
            try:
                os.chdir(root)
                with patch.dict(os.environ,{},clear=True),patch("subprocess.check_output") as api,patch("subprocess.run") as call:
                    with self.assertRaisesRegex(RuntimeError,"hold is active"):runpy.run_path(str(publisher),run_name="__main__")
                    api.assert_not_called();call.assert_not_called()
            finally:os.chdir(previous)
