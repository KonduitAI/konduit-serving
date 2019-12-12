import logging
import os
import requests
import socket
import sys
from contextlib import closing


def is_port_in_use(port, base_url="localhost"):
    """is this port already in use for a Konduit server?"""
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        return s.connect_ex((base_url, port)) == 0


def default_python_path(work_dir="."):
    """set and return the Python path by appending this work_dir to it."""
    work_dir = os.path.abspath(work_dir)
    python_path = os.pathsep.join(sys.path)
    python_path += os.pathsep + work_dir
    return python_path


def validate_server(url):
    """
    Validates if a konduit.Server is running under the specified url. Returns True if
    the server is running, False otherwise.

    :param url: host and port of the server as str
    :return: boolean
    """
    try:
        r = requests.get("{}/healthcheck".format(url))
        if r.status_code != 204:
            logging.error(
                "The server health checks failed. Please verify that the server is running without any "
                "issues..."
            )
        else:
            return True
    except Exception as ex:
        logging.error(
            "{}\nUnable to connect to the server or the server health checks have failed. Please "
            "verify that the server is running without any issues...".format(str(ex))
        )
        return False


def to_unix_path(file_path):
    """Utility function to turn a Windows-style path into Unix-style. Windows
    can handle the latter, so we use this representation internally. Python can
    handle this in any case. We also replace Windows' ";" for path separation by
    os.pathsep to make sure this runs on every system.

    :param file_path: path to your file
    :return: Unix-style version of your file
    """
    return file_path.replace("\\", "/").replace(";", os.pathsep)


def update_dict_with_unix_paths(config):
    """Replace all str values in Python dictionary that correspond to paths
    by their Unix-style equivalent.

    :param config: any Python dictionary
    :return: updated dict
    """
    for k, v in config.items():
        if "_path" in k and isinstance(v, str):
            config[k] = to_unix_path(v)
    return config
