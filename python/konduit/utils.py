from contextlib import closing
import socket
import os
import sys
import requests
import logging


def is_port_in_use(port):
    """is this port already in use for a Konduit server?"""
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        return s.connect_ex(('localhost', port)) == 0


def default_python_path(work_dir='.'):
    """set and return the Python path by appending this work_dir to it."""
    work_dir = os.path.abspath(work_dir)
    python_path = os.pathsep.join(sys.path)
    python_path += os.pathsep + work_dir
    return python_path


def validate_server(url):
    try:
        r = requests.get("{}/healthcheck".format(url))
        if r.status_code != 204:
            logging.error("The server health checks failed. Please verify that the server is running without any "
                          "issues...")
        else:
            return True
    except Exception as ex:
        logging.error("{}\nUnable to connect to the server or the server health checks have failed. Please "
                      "verify that the server is running without any issues...".format(str(ex)))
        return False
