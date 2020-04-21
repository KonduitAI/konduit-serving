import os
import sys
import requests
import subprocess
import click

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
KONDUIT_SOURCE_DIR = os.path.join(KONDUIT_BASE_DIR, "source")
KONDUIT_JAR_DIR = os.path.join(KONDUIT_BASE_DIR, "jar")
KONDUIT_JAR_PATH = os.path.join(KONDUIT_JAR_DIR, "konduit.jar")

DEFAULT_KONDUIT_COMMIT_HASH = "512bb17"

os_choices = [
    "windows-x86_64",
    "linux-x86_64",
    "linux-x86_64-gpu",
    "macosx-x86_64",
    "linux-armhf",
    "windows-x86_64-gpu",
]


def get_platform():
    if sys.platform.startswith("win32"):
        return "windows-x86_64"
    elif sys.platform.startswith("darwin"):
        return "macosx-x86_64"
    elif sys.platform.startswith("linux"):
        return "linux-x86_64"
    else:
        raise RuntimeError("Please specify '--os'. Possible values are: " + os_choices.__str__())


def download_if_required(url, save_path):
    if not os.path.exists(KONDUIT_JAR_DIR):
        os.makedirs(KONDUIT_JAR_DIR)

    response = requests.get(url, stream=True)
    status_code = response.status_code
    if status_code != 200:
        print("Failed with a status code of {}".format(status_code))
        return

    total = response.headers.get('content-length')

    if os.path.exists(save_path) and os.path.getsize(KONDUIT_JAR_PATH) == total:
        print("The required CLI binary has already been downloaded.")
        return
    else:
        print("Downloading command line binaries")

    with open(save_path, 'wb') as f:
        if total is None:
            f.write(response.content)
        else:
            downloaded = 0
            total = int(total)
            for data in response.iter_content(chunk_size=max(int(total / 1000), 1024 * 1024)):
                downloaded += len(data)
                f.write(data)
                done = int(50 * downloaded / total)
                sys.stdout.write('\r[{}{}]'.format('█' * done, '.' * (50 - done)))
                sys.stdout.flush()
    sys.stdout.write('\n')


def git_clone_konduit(use_https=True, commit_hash=DEFAULT_KONDUIT_COMMIT_HASH):
    """Clone the konduit-serving git repo, if it doesn't already exist locally."""

    if not os.path.exists(KONDUIT_SOURCE_DIR):
        os.makedirs(KONDUIT_SOURCE_DIR)

    if use_https:
        repo = "https://github.com/KonduitAI/konduit-serving.git"
    else:
        repo = "git@github.com:KonduitAI/konduit-serving.git"
    try:
        if not os.listdir(KONDUIT_SOURCE_DIR):
            subprocess.call(["git", "clone", repo, KONDUIT_SOURCE_DIR],
                            shell=sys.platform.startswith("win"))
        subprocess.call(["git", "checkout", commit_hash], cwd=KONDUIT_SOURCE_DIR,
                        shell=sys.platform.startswith("win"))
    except Exception as e:
        raise RuntimeError(">>> Could not clone konduit-serving repository and switch to commit hash {}"
                           "Make sure to have git installed. Type 'konduit-init --help' for help .\n"
                           .format(commit_hash), e)


def build_jar(operating_sys, spin):
    """Build the actual JAR, using our mvnw wrapper under the hood."""

    if not os.path.exists(KONDUIT_JAR_DIR):
        os.makedirs(KONDUIT_JAR_DIR)

    if operating_sys is None:
        operating_sys = get_platform()

    try:
        subprocess.call(
            [
                "python",
                os.path.join(KONDUIT_SOURCE_DIR, "build_jar.py"),
                "--os",
                operating_sys,
                "--source",
                KONDUIT_SOURCE_DIR,
                "--spin",
                spin,
                "--target",
                KONDUIT_JAR_PATH
            ],
            shell=sys.platform.startswith("win")
        )
    except Exception as e:
        RuntimeError("Failed to build jar.\n", e)


@click.command()
@click.option(
    "--platform",
    help="Your operating system. Choose from {}. "
         "Defaults to the cpu version of the "
         "current OS platform in use."
        .format(os_choices.__str__()),
)
@click.option(
    "--https",
    type=bool,
    default=True,
    show_default=True,
    help="If True, use HTTPS to clone konduit-serving, else SSH.",
)
@click.option(
    "--commit_hash",
    default=DEFAULT_KONDUIT_COMMIT_HASH,
    show_default=True,
    help="The commit hash to build the source code from.",
)
@click.option(
    "--spin",
    default="all",
    show_default=True,
    help="Whether to bundle Python ('python'), PMML ('pmml'), both ('all') " +
         "or neither ('minimal'). Python bundling is not encouraged with ARM, " +
         "and PMML bundling is not encouraged if agpl license is an issue.",
)
@click.option(
    "--download",
    type=bool,
    default=False,
    show_default=True,
    help="Set to True if you want to download the pre-built jar file instead of building it.",
)
def init(platform, https, commit_hash, spin, download):
    if download:
        download_if_required(KONDUIT_JAR_URL, KONDUIT_JAR_PATH)
    else:
        git_clone_konduit(https, commit_hash)
        build_jar(platform, spin)


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
