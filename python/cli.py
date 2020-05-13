import os
import sys
import subprocess
from cli_init import KONDUIT_JAR_PATH


def cli():
    if not os.path.exists(KONDUIT_JAR_PATH):
        print("No konduit binaries found. See 'konduit-init --help' or just run 'konduit-init'"
              " to initialize a konduit jar binary.")
    else:
        arguments = ["java", "-jar", "-Dvertx.cli.usage.prefix=konduit", KONDUIT_JAR_PATH]
        arguments.extend(sys.argv[1:])

        subprocess.call(
            arguments,
            shell=sys.platform.startswith("win")
        )
