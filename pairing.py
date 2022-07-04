import http.client
import plistlib

host = '172.20.3.18'
port = 7000
method = 'POST'
name = 'Makus\'s Thinkpad'.encode('utf-8')
x = "\U0001f600".encode('utf-8')
h = {
    'X-Apple-Client-Name': name+x,
    'User-Agent': 'Arcs'
}
h1 = http.client.HTTPConnection(host,port)
h1.request(method,'/pair-pin-start',headers=h)
r = h1.getresponse()
print(r.status)
h1.close()

h2 = dict(
    user = "00:00:00:00:00:00",
    method = "pin"
)
auth_plist = plistlib.dumps(h2)
h['Content-Type'] = 'application/x-apple-binary-plist'
h1.request(method,'/pair-setup-pin',body=auth_plist,headers=h)
r = h1.getresponse()
print(plistlib.loads(r.read()))