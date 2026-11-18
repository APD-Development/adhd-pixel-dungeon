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

"""Enforce the recorded owner APK-delivery hold before any publication request."""
import json
import pathlib


def require_apk_delivery_allowed(root):
    policy = pathlib.Path(root) / "docs/APD-OWNER-DELIVERY-HOLD.json"
    if not policy.is_file():
        raise RuntimeError("APK delivery policy is missing; stop before publishing.")
    try:
        if policy.stat().st_size > 16384:
            raise ValueError("policy too large")
        state = json.loads(policy.read_text())
        active = state["active"]
        if type(active) is not bool:
            raise ValueError("active must be a boolean")
    except (OSError, ValueError, KeyError, TypeError) as error:
        raise RuntimeError("APK delivery policy is invalid; stop before publishing.") from error
    if active:
        raise RuntimeError("Owner APK delivery hold is active: finish and verify the approved work before sending another APK.")


if __name__ == "__main__":
    require_apk_delivery_allowed(pathlib.Path.cwd())
