import plistlib
b = b'bplist00\xd2\x01\x02\x03\x04RpkTsaltO\x11\x01\x00\x96\xb8\'i\xb4\xde<\x81 x\x8f\xae\x82\xa8\x0e\xaa\xfc\xb8\xda\xb5$fG\x9c\xd8\x87\x14\xb9\x84wU\xf5R\x17VW\x1dZ\xe0\xb3/\x81\x82\x12rX#\x7f\x12i\xd2mK\xa60\xfbg \xf2"\xa1\xf8\xa3`\xd1u(\x13=+\x13U.\x00\nK\xb9\xd0\xd49\x7fL\xbdk8\xe4\x06\x08\x0f3\xe2\r<c\x8e\'\x95N\xf86\x9aD\xdb\x96=\x1c\xbe0\xfa\xc8\xae\x83\xdar-:\xa3eNZbGZ\x8f\xb2\x94E\xc7\xac`\x86P\xff\x83\xf1\xead\xb1\xc3\xf0\xf5\\\xf0\xe3&\xfc.U\x00)\x1c\xed\x97m\xdf\x88Zf8\xf2~\xfe\xe24\xb3#7\xdd\x03:\x08\xc1/\x8b?\xafx\xcc\x11@\xe8\xce\x02\x96\xc5\x9dv\x06M\xa6\x95\xc7\xb6D\x10#U\x1f\xdd\x15\xfd\xff\xfeN\xb9<\xbb\xc9\xec?\x06\x0c\x8e\x98\x91"\x9aS\x06\x06\xe5[v\x1e\xde\xe3\xc5\x14\xa9\xaeu*\x16\xbc;\xad\x08\xbb(\xbf\xbcG\x91\xa2\n\xc0\x05R\x9e\xe4\xa1{\xfd\xfc\x95\xe2O\x10\x10\xaf\x0c4\xe2cz\xfeqd\x85Sf\r\xb4\xd9\xcc\x00\x08\x00\r\x00\x10\x00\x15\x01\x19\x00\x00\x00\x00\x00\x00\x02\x01\x00\x00\x00\x00\x00\x00\x00\x05\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x01,'

p = str(plistlib.loads(b))
print(p)