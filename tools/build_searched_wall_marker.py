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
# along with this program. If not, see <http://www.gnu.org/licenses/>
"""Editable original 16px overlay tile; no external artwork or font inputs."""
from pathlib import Path
from PIL import Image
# Bitmask atlas: visible front face, southern overhang, left edge, right edge.
im=Image.new('RGBA',(16*16,16))
shapes={1:[(5,9),(6,10),(7,11),(8,10),(9,9),(10,8)],
        2:[(5,9),(6,10),(7,11),(8,10),(9,9),(10,8)],
        4:[(1,6),(2,7),(1,8),(1,9),(2,10)],
        8:[(14,6),(13,7),(14,8),(14,9),(13,10)]}
for mask in range(1,16):
 for bit,points in shapes.items():
  if mask&bit:
   for x,y in points:
    if x+1<16 and y+1<16 and (bit!=2 or y+1<=11):im.putpixel((mask*16+x+1,y+1),(0,0,0,255))
   for x,y in points:im.putpixel((mask*16+x,y),(255,255,255,255))
im.save(Path(__file__).resolve().parents[1]/'core/src/main/assets/interfaces/apd_searched_wall.png')
