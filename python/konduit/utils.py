from contextlib import closing
import socket


def is_port_in_use(port):
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        return s.connect_ex(('localhost', port)) == 0
