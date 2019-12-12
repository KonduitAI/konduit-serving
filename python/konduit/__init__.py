import os

USER_PATH = os.path.expanduser("~")
KONDUIT_BASE_DIR = os.path.join(USER_PATH, ".konduit")
KONDUIT_DIR = os.path.join(KONDUIT_BASE_DIR, "konduit-serving")

jar = os.getenv("KONDUIT_JAR_PATH", os.path.join(KONDUIT_DIR, "konduit.jar"))

try:
    import pydl4j

    pydl4j.add_classpath(jar)
except Exception as e:
    print("VM already running from previous test")
    print(e)

from .inference import *
from .server import *
from .client import *
