#! /usr/bin/env python

"""
This sever is based on the code at
http://kmkeen.com/socketserver/
"""

import os
import SocketServer
import sys
from subprocess import PIPE, Popen


JOSHUA_PATH = os.environ['JOSHUA']
my_unix_command = os.path.join(JOSHUA_PATH, 'joshua-decoder')
HOST = 'localhost'
PORT = 2000


def pipe_command(arg_list, standard_input=False):
    "arg_list is [command, arg1, ...], standard_input is string"
    pipe = PIPE if standard_input else None
    subp = Popen(arg_list, stdin=pipe, stdout=PIPE)
    if not standard_input:
        return subp.communicate()[0]
    return subp.communicate(standard_input)[0]


class SingleTCPHandler(SocketServer.BaseRequestHandler):
    "One instance per connection.  Override handle(self) to customize action."
    def handle(self):
        # self.request is the client connection
        data = self.request.recv(1024)  # clip input at 1Kb
        reply = pipe_command(my_unix_command, data)
        if reply is not None:
            self.request.send(reply)
        self.request.close()


class SimpleServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    # Ctrl-C will cleanly kill all spawned threads
    daemon_threads = True
    # much faster rebinding
    allow_reuse_address = True

    def __init__(self, server_address, RequestHandlerClass):
        SocketServer.TCPServer.__init__(self, server_address, RequestHandlerClass)

if __name__ == "__main__":
    server = SimpleServer((HOST, PORT), SingleTCPHandler)
    # terminate with Ctrl-C
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        sys.exit(0)
