import http.client
import hashlib
import socket

host = '172.20.3.14'
port = 7100
method = 'POST'
url = '/stream'
data = ''
h = {
    'User-Agent': 'Arcs',
    'X-Apple-Client-Name': 'Subwoofer'
}

UDPClientSocket = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)

with open("tiny.png", "rb") as image:
  f = image.read()
  b = bytearray(f)

def makeAuthorization(password,method,url):
    ha1 = 'Airplay:airplay:'+password
    ha2 = method+':'+url
    
    result = hashlib.md5(ha1.encode()).hexdigest()
    result2 = hashlib.md5(ha2.encode()).hexdigest()

    ha3 = result+':'+f[3]+':'+result2
    result3 = hashlib.md5(ha3.encode()).hexdigest()
    return result3

h1 = http.client.HTTPConnection(host,port)
h1.request(method,url,body=data,headers=h)
r1 = h1.getresponse()
r1.read()
print(r1.status)

if(r1.status == 401):
    h1.close()
    f = r1.getheader('WWW-Authenticate').split('"')

    password = input("Enter password on screen")

    h['Authorization'] = 'Digest username="Airplay", realm="airplay", nonce="'+f[3]+'", uri="'+url+'", response="'+makeAuthorization(password,method,url)+'"'
    
    h1.request(method,url,body=data,headers=h)
    while True:
        UDPClientSocket.sendto(b, (host,port))

while True:
    i = input()
    if i == 'q':
        h1.close()
        quit()