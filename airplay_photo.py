import socket
import logging

logging.basicConfig(level=logging.DEBUG)

host = '192.168.1.71'

with open("transformer.jpg", "rb") as image:
  f = image.read()
  b = bytearray(f)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)                 

s.connect((host , 7000))
output = 'PUT /photo HTTP/1.1\r\nHost: '+host+'\r\nContent-Length: '+str(len(b))+'\r\nUser-Agent: Arcs\r\n\r\n'

while True:
  s.sendall(output.encode('utf-8')+b)
  print(s.recv(4096))
  while True:
    i = input()
    if(i == 'q'):
      s.close()
      quit()