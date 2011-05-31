all: sentserver sentclient

sentserver:
	gcc -g -O2 -o sentserver -static -pthread -L/usr/local/lib -Wl,--rpath -Wl,/usr/local/lib sentserver.c

sentclient:
	gcc -g -O2 -o sentclient -static -pthread -L/usr/local/lib -Wl,--rpath -Wl,/usr/local/lib sentclient.c
