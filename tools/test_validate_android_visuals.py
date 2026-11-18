import importlib.util,pathlib,struct,unittest,zlib
spec=importlib.util.spec_from_file_location('visual',pathlib.Path(__file__).with_name('validate_android_visuals.py'));visual=importlib.util.module_from_spec(spec);spec.loader.exec_module(visual)
def chunk(kind,payload):return struct.pack('>I',len(payload))+kind+payload+struct.pack('>I',zlib.crc32(kind+payload)&0xffffffff)
def image(w,h):return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress((b'\0'+b'\0'*(w*3))*h))+chunk(b'IEND',b'')
class ScreenshotValidation(unittest.TestCase):
 def test_adb_error_cannot_be_called_a_screenshot(self):
  with self.assertRaises(ValueError):visual.validate(b'run-as: unknown package: example\n')
 def test_full_screen_capture_accepted(self):self.assertEqual((640,480),visual.validate(image(640,480)))
 def test_placeholder_rejected(self):
  with self.assertRaises(ValueError):visual.validate(image(1,1))
 def test_bad_crc_rejected(self):
  data=bytearray(image(640,480));data[20]^=1
  with self.assertRaises(ValueError):visual.validate(data)
 def test_truncated_capture_rejected(self):
  with self.assertRaises(ValueError):visual.validate(image(640,480)[:-1])
