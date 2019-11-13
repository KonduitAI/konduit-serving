from contextlib import closing
import socket
import os
import sys


def is_port_in_use(port):
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        return s.connect_ex(('localhost', port)) == 0


def default_python_path(work_dir='.'):
    work_dir = os.path.abspath(work_dir)
    python_path = ':'.join(sys.path)
    python_path += ':' + work_dir
    return python_path

