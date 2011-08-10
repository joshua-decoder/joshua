all: sentserver sentclient

sentserver: sentserver.c
# 	gcc -g -O2 -o sentserver -static -pthread -L/usr/local/lib -Wl,--rpath -Wl,/usr/local/lib sentserver.c
	gcc -g -O2 -o sentserver -pthread -L/usr/local/lib sentserver.c

sentclient: sentclient.c
# 	gcc -g -O2 -o sentclient -static -pthread -L/usr/local/lib -Wl,--rpath -Wl,/usr/local/lib sentclient.c
	gcc -g -O2 -o sentclient -pthread -L/usr/local/lib sentclient.c

clean:
	rm -f sentserver sentclient
