from contextlib import closing
import socket
import os
import sys


def is_port_in_use(port, base_url='localhost'):
    """is this port already in use for a Konduit server?"""
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        return s.connect_ex((base_url, port)) == 0


def default_python_path(work_dir='.'):
    """set and return the Python path by appending this work_dir to it."""
    work_dir = os.path.abspath(work_dir)
    python_path = os.pathsep.join(sys.path)
    python_path += os.pathsep + work_dir
    return python_path
