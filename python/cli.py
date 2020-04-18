import os
import sys
import requests
import subprocess

KONDUIT_SERVING_VERSION = "0.1.0-20200416.141921-81"
SPIN = "all"
PLATFORM = "windows-x86_64" if sys.platform.startswith("win") else "linux-x86_64"
CHIP = "cpu"
KONDUIT_JAR_URL = "https://oss.sonatype.org/content/repositories/snapshots/" \
                  "ai/konduit/serving/konduit-serving-uberjar/" \
                  "0.1.0-SNAPSHOT/" \
                  "konduit-serving-uberjar-{}-{}-{}-{}.jar".format(KONDUIT_SERVING_VERSION, SPIN, PLATFORM, CHIP)

USER_PATH = os.path.expanduser("~")
KONDUIT_BASE_DIR = os.path.join(USER_PATH, ".konduit-serving")
KONDUIT_JAR_DIR = os.path.join(KONDUIT_BASE_DIR, "jar")
KONDUIT_JAR_PATH = os.path.join(KONDUIT_JAR_DIR, "konduit.jar")


def download_if_required(url, save_path):
    if os.path.exists(save_path):
        return
    else:
        print("Downloading command line binaries")

    with open(save_path, 'wb') as f:
        response = requests.get(url, stream=True)
        total = response.headers.get('content-length')

        if total is None:
            f.write(response.content)
        else:
            downloaded = 0
            total = int(total)
            for data in response.iter_content(chunk_size=max(int(total/1000), 1024*1024)):
                downloaded += len(data)
                f.write(data)
                done = int(50*downloaded/total)
                sys.stdout.write('\r[{}{}]'.format('█' * done, '.' * (50-done)))
                sys.stdout.flush()
    sys.stdout.write('\n')


def cli():
    if not os.path.exists(KONDUIT_JAR_DIR):
        os.makedirs(KONDUIT_JAR_DIR)

    download_if_required(KONDUIT_JAR_URL, KONDUIT_JAR_PATH)

    arguments = ["java", "-jar", "-Dvertx.cli.usage.prefix=konduit", KONDUIT_JAR_PATH]
    arguments.extend(sys.argv[1:])

    subprocess.call(
        arguments,
        shell=sys.platform.startswith("win")
    )

