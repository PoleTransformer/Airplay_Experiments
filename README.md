# Airplay_Experiments
Reverse engineering airplay protocol for non apple devices. I will upload test sketches here.

# Project Update June 18, 2022:
After reverse engineering the protocol for a while I know a couple of things:  

PUT requests for photos don't work on Apple TV generation 4 or higher. It seems Apple has removed that feature, which is annoying.  

Latest sketch `airplay_http.py` implements video streaming from a local http server or from an existing live streaming server online and photos for Apple TV generation 3 and lower.  

I am currently working on screen mirroring, but I need to learn how to implement RTSP and RTP in python for audio and video data.  

After capturing network data using wireshark on a Macbook Air running MacOS Monterey, it seems like standard screen mirroring first sets up two rtsp streams for the left and right audio channel, then uses ntp to transmit the master clock and sync data, then finally sends the stream data over tcp.  

Thats all I have for now, please stay tuned for updates.
