import http.client
import hashlib
import threading
import time

host = '172.20.3.136'
port = 7000
method = ''
url = ''
data = ''
password = ''
photo = 'test.jpg'
video_url = 'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8'
video_plist = ''
x = "\U0001f600".encode('utf-8')
h = {
    'User-Agent': 'AirPlay/615.12.1',
    'X-Apple-Client-Name': x
}

def feedback():
    try:
        while True:
            h1.request('POST','/feedback')
            r = h1.getresponse()
            r.read()
            time.sleep(30)
    except:
        print('Feedback error')

def makeAuthorization(password,method,url):
    ha1 = 'Airplay:airplay:'+password
    ha2 = method+':'+url
    
    result = hashlib.md5(ha1.encode()).hexdigest()
    result2 = hashlib.md5(ha2.encode()).hexdigest()

    ha3 = result+':'+f[3]+':'+result2
    result3 = hashlib.md5(ha3.encode()).hexdigest()
    return result3

ip = input("Enter apple tv ip or enter for "+host) or host
a = input("Enter p for photo or enter for video") or 'v'
if a == 'v':
    method = 'POST'
    url = '/play'
    h['Content-Type'] = 'application/x-apple-binary-plist'
    video_url = input("Enter video url or enter for sync test") or video_url
    video_plist = '<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">\n<plist version="1.0">\n<dict>\n\t<key>Content-Location</key>\n\t<string>'+video_url+'</string>\n\t<key>Start-Position</key>\n\t<real>0</real>\n</dict>\n</plist>\n'
    data = video_plist
elif a == 'p':
    method = 'PUT'
    url = '/photo'
    photo = input("Enter photo path or enter for "+photo) or photo
    with open(photo, "rb") as image:
        f = image.read()
        b = bytearray(f)
    data = b
else:
    quit()

h1 = http.client.HTTPConnection(host,port)
h1.request(method,url,body=data,headers=h)
r1 = h1.getresponse()
r1.read()
print(r1.status)

while(r1.status == 401):
    h1.close()
    f = r1.getheader('WWW-Authenticate').split('"')

    password = input("Enter password on screen")

    h['Authorization'] = 'Digest username="Airplay", realm="airplay", nonce="'+f[3]+'", uri="'+url+'", response="'+makeAuthorization(password,method,url)+'"'
    
    h1.request(method,url,body=data,headers=h)
    r1 = h1.getresponse()
    r1.read()
    print(r1.status)

if(r1.status == 200):
    x = threading.Thread(target=feedback,daemon=True)
    x.start()
else:
    print('Something bad happened')
    quit()

while True:
    i = input()
    if i == 'q':
        h1.request('POST','/stop')
        quit()
    elif i == 'c' and a == 'p':
        path = input("Enter new photo path")
        with open(path, "rb") as image:
            f = image.read()
            b = bytearray(f)
        data = b
        h1.request(method,url,body=data,headers=h)
        r1 = h1.getresponse()
        r1.read()
        print(r1.status)
    elif i == 'c' and a == 'v':      
        link = input("Enter new video url or enter for current") or video_url
        video_plist = '<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">\n<plist version="1.0">\n<dict>\n\t<key>Content-Location</key>\n\t<string>'+link+'</string>\n\t<key>Start-Position</key>\n\t<real>0</real>\n</dict>\n</plist>\n'
        data = video_plist
        h1.request('POST','/stop')
        r1 = h1.getresponse()
        r1.read()  
        h1.request(method,url,body=data,headers=h)
        r1 = h1.getresponse()
        r1.read()
        print(r1.status)
    elif i == 't' and a == 'v':
        h1.request('GET','/scrub')
        r1 = h1.getresponse()
        print(r1.read())
    elif i == 's' and a == 'v':
        s = input("Enter time in seconds")
        h1.request('POST','/scrub?position='+s+'')
        r1 = h1.getresponse()
        r1.read()
        print(r1.status)
    elif i == 'p' and a == 'v':
        rate = input("Enter rate from 0-1")
        h1.request('POST','/rate?value='+rate+'')
        r1 = h1.getresponse()
        r1.read()
        print(r1.status)