import os
jar = os.getenv('KONDUIT_JAR_PATH', 'konduit.jar')

try:
    import pydl4j
    pydl4j.add_classpath(jar)
except Exception as e:
    print("VM already running from previous test")
    print(e)

from .inference import *
