import http.client
import plistlib

host = '172.20.3.14'
port = 7000
method = 'POST'
h = {
    'X-Apple-Client-Name': 'Subwoofer',
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
h3 = {
    'Content-Type': 'application/x-apple-binary-plist'
}
h1.request(method,'/pair-setup-pin',body=auth_plist,headers=h3)
r = h1.getresponse()
print(r.status)
print(r.read().decode('ISO-8859-1'))