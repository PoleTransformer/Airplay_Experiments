import http.client
import hashlib

host = 'Apple-TV-2.local'
port = 7000
method = ''
url = ''
data = ''
password = ''
photo = ''
video_url = ''
video_plist = ''
h = {
    'User-Agent': 'Arcs'
}

def makeAuthorization(password,method,url):
    ha1 = 'Airplay:airplay:'+password
    ha2 = method+':'+url
    
    result = hashlib.md5(ha1.encode()).hexdigest()
    result2 = hashlib.md5(ha2.encode()).hexdigest()

    ha3 = result+':'+f[3]+':'+result2
    result3 = hashlib.md5(ha3.encode()).hexdigest()
    return result3

a = input()
if a == 'v':
    method = 'POST'
    url = '/play'
    h['Content-Type'] = 'application/x-apple-binary-plist'
    video_url = input("Enter video url or enter for default") or "https://devstreaming-cdn.apple.com/videos/wwdc/2019/502gzyuhh8p2r8g8/502/502_hd_introducing_lowlatency_hls.mp4?dl=1"
    video_plist = '<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">\n<plist version="1.0">\n<dict>\n\t<key>Content-Location</key>\n\t<string>'+video_url+'</string>\n\t<key>Start-Position</key>\n\t<real>0</real>\n</dict>\n</plist>\n'
    data = video_plist
elif a == 'p':
    method = 'PUT'
    url = '/photo'
    photo = input("Enter photo path or defaulting to test.jpg") or "test.jpg"
    with open(photo, "rb") as image:
        f = image.read()
        b = bytearray(f)
    data = b
else:
    print('Invalid')
    quit()

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
    r1 = h1.getresponse()
    r1.read()
    print(r1.status)

while True:
    i = input()
    if i == 'q':
        h1.close()
        quit()
    elif i == 't' and a == 'v':
        h1.request('GET','/scrub')
        r1 = h1.getresponse()
        print(r1.read())
    elif i == 's' and a == 'v':
        time = input("Enter time in seconds")
        h1.request('POST','/scrub?position='+time+'')
        r1 = h1.getresponse()
        r1.read()
        print(r1.status)