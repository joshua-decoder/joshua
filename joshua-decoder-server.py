#! /usr/bin/env python

"""
This sever is based on the code at
http://kmkeen.com/socketserver/

USAGE:
    1) Start the server with the command:
        ./joshua-decoder-server.py [optional joshua-decoder arguments]
    2) Use a client to send source segments and receive decoded outputs
    (translations). E.g.:
        echo hola | nc localhost 2000

TODO:
* Figure out a way around starting the joshua-decoder subprocess as a global
  object.
* command-line options
  + first argument is required positional port number
  + rest of arguments are optional, passed directly to joshua-decoder
* multithreading
"""

import os
import SocketServer
import sys
from subprocess import PIPE, Popen
from threading import Thread
from Queue import Queue, Empty  # Python 2.x

ON_POSIX = 'posix' in sys.builtin_module_names
JOSHUA_PATH = os.environ['JOSHUA']
JOSHUA_CMD = os.path.join(JOSHUA_PATH, 'joshua-decoder')
HOST = 'localhost'
PORT = 2000


class JoshuaDecoderSubprocess(object):

    def _enqueue_output(self, out, queue):
        for line in iter(out.readline, b''):
            queue.put(line)
        out.close()

    def __init__(self, joshua_args):
        cmd = [JOSHUA_CMD] + joshua_args
        self.proc = Popen(cmd, stdin=PIPE, stdout=PIPE, bufsize=1, close_fds=ON_POSIX)

        # Use Queue and Thread to read output without blocking.
        self.q_stdout = Queue()
        self.t_stdout = Thread(target=self._enqueue_output,
                               args=(self.proc.stdout, self.q_stdout))
        self.t_stdout.daemon = True  # thread dies with the program
        self.t_stdout.start()

    def translate(self, input_text):
        """Process the input text with the Joshua decoder"""
        self.proc.stdin.write(input_text.encode('utf-8'))
        # Read the decoded output.
        # Read line without blocking.
        while True:
            try:
                return self.q_stdout.get().decode('utf-8')
            except Empty:
                print('no output yet')


# Dirty, dirty global object.
JOSHUA_SUBPROCESS = JoshuaDecoderSubprocess(sys.argv[1:])


class SingleTCPHandler(SocketServer.BaseRequestHandler):
    "One instance per connection.  Override handle(self) to customize action."
    def handle(self):
        # self.request is the client connection
        data = self.request.recv(1024)  # clip input at 1Kb
        reply = JOSHUA_SUBPROCESS.translate(data)
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
