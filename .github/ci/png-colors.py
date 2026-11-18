"""Prints how many distinct colors a PNG has (stdlib only), sampling every 4th pixel of 8-bit RGB/RGBA images."""
import struct, sys, zlib
data = open(sys.argv[1], 'rb').read()
pos, idat, w = 8, b'', 0
while pos < len(data):
    n, t = struct.unpack('>I4s', data[pos:pos + 8])
    body = data[pos + 8:pos + 8 + n]
    if t == b'IHDR': w, h, depth, ctype = struct.unpack('>IIBB', body[:10])
    if t == b'IDAT': idat += body
    pos += 12 + n
bpp = {2: 3, 6: 4}.get(ctype, 0)
if depth != 8 or not bpp: print(0); sys.exit()
raw, stride, prev, colors, i = zlib.decompress(idat), w * bpp, bytearray(w * bpp), set(), 0
for y in range(h):
    f, line = raw[i], bytearray(raw[i + 1:i + 1 + stride]); i += 1 + stride
    for x in range(stride):
        a = line[x - bpp] if x >= bpp else 0; b = prev[x]; c = prev[x - bpp] if x >= bpp else 0
        if f == 1: line[x] = (line[x] + a) & 255
        elif f == 2: line[x] = (line[x] + b) & 255
        elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
        elif f == 4:
            p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
            line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
    if y % 4 == 0:
        for x in range(0, w, 4): colors.add(bytes(line[x * bpp:x * bpp + 3]))
    prev = line
print(len(colors))
