from .inference import *
from jnius_config import set_classpath


def init(jar='konduit.jar'):
    try:
        set_classpath(jar)
    except Exception as e:
        print("VM already running from previous test")
        print(e)
