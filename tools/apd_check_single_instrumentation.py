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
"""Reject incomplete, skipped, failed or unexpected single-case instrumentation output."""
import pathlib,re,sys
text=pathlib.Path(sys.argv[1]).read_text()
method=sys.argv[2]
expected_class=sys.argv[3] if len(sys.argv)>3 else "com.shatteredpixel.shatteredpixeldungeon.android.TrapDiscoveryDeviceTest"
assert re.fullmatch(r"[A-Za-z_$][A-Za-z0-9_$]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)+",expected_class)
assert 'OK (1 test)' in text, text[-4000:]
assert 'INSTRUMENTATION_CODE: -1' in text, text[-4000:]
assert re.search(r'INSTRUMENTATION_STATUS: class='+re.escape(expected_class)+r'(?:\r?\n|$)',text), text[-4000:]
assert len(re.findall(r'INSTRUMENTATION_STATUS_CODE: 0(?:\r?\n|$)',text))==1, text[-4000:]
assert re.search(r'INSTRUMENTATION_STATUS: test='+re.escape(method)+r'(?:\r?\n|$)',text), text[-4000:]
assert not re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|AssumptionFailure|\bFAILURE\b',text), text[-4000:]
print('Verified actual terminal instrumentation result:',method)
