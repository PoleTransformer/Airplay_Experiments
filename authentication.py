import hashlib

username = 'Airplay:'
realm = 'airplay:'
password = '3863'
method = 'PUT:'
uri = '/photo'
nonce = 'MTY1NTMzMTQ3NyBx3nHB/bn5VJG9A0jp/vuq'

ha1 = username+realm+password
ha2 = method+uri
  
result = hashlib.md5(ha1.encode()).hexdigest()
result2 = hashlib.md5(ha2.encode()).hexdigest()

ha3 = result+':'+nonce+':'+result2
result3 = hashlib.md5(ha3.encode()).hexdigest()

print(result)
print(result2)
print(result3)