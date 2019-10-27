from .inference import *
import os
jar = os.getenv('KONDUIT_JAR_PATH', 'konduit.jar')

try:
    from jnius_config import set_classpath
    set_classpath(jar)
except Exception as e:
    print("VM already running from previous test")
    print(e)
