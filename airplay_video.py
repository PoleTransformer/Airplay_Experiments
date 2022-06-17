import socket
import logging

logging.basicConfig(level=logging.DEBUG)

host = "Apple-TV-2.local"
link = b'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'

plist = b'<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">\n<plist version="1.0">\n<dict>\n\t<key>Content-Location</key>\n\t<string>'+link+b'</string>\n\t<key>Start-Position</key>\n\t<real>0</real>\n</dict>\n</plist>\n'

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

s.connect((host , 7000))
output = 'POST /play HTTP/1.1\r\nHost: '+host+'\r\nContent-Type: application/x-apple-binary-plist\r\nContent-Length: '+str(len(plist))+'\r\nUser-Agent: Arcs\r\n\r\n'
status = 'GET /scrub HTTP/1.1\r\nHost: '+host+'\r\nContent-Length: 0\r\nUser-Agent: Arcs\r\n\r\n'

while True:
  s.sendall(output.encode('utf-8')+plist)
  print(s.recv(4096))
  while True:
    i = input()
    if(i == 'q'):
      s.close()
      quit()
    elif(i == 's'):
      pos = input()
      scrub = 'POST /scrub?position='+pos+' HTTP/1.1\r\nHost: '+host+'\r\nContent-Length: 0\r\nUser-Agent: Arcs\r\n\r\n'
      s.sendall(scrub.encode('utf-8'))
      print(s.recv(4096))
    elif(i == 't'):
      s.sendall(status.encode('utf-8'))
      print(s.recv(4096))