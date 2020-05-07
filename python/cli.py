import os
import sys
import requests
import subprocess
import click
from packaging.version import parse
from hurry.filesize import size

USER_PATH = os.path.expanduser("~")
KONDUIT_BASE_DIR = os.path.join(USER_PATH, ".konduit-serving")
KONDUIT_SOURCE_DIR = os.path.join(KONDUIT_BASE_DIR, "source")
KONDUIT_JAR_DIR = os.path.join(KONDUIT_BASE_DIR, "jar")
KONDUIT_JAR_PATH = os.path.join(KONDUIT_JAR_DIR, "konduit.jar")

INCOMPATIBLE_COMPILATION_TAGS = ["cli_base", "cli_base_2", "cli_base_3", "cli_base_4"]
DOWNLOAD_TAG = "cli_base"

LAST_COMPATIBLE_KONDUIT_VERSION = "0.1.0-SNAPSHOT"
DEFAULT_KONDUIT_TAG = "cli_base_4"
KONDUIT_JAR_URL_FORMAT = "https://github.com/KonduitAI/konduit-serving/releases/download/" \
                       "{tag}/konduit-serving-uberjar-{version}-{spin}-{platform}-{chip}.jar"

os_choices = [
    "windows-x86_64",
    "linux-x86_64",
    "macosx-x86_64",
    "linux-armhf",
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
        print("Download path: {}".format(url))
        print("Failed with a status code of {}".format(status_code))
        return

    total = response.headers.get('content-length')

    if os.path.exists(save_path) and os.path.getsize(KONDUIT_JAR_PATH) == total:
        print("The required CLI binary has already been downloaded.")
        return
    else:
        print("Downloading command line binaries from " + url)

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
                sys.stdout.write('\r[{}{}]'.format('█' * done, '.' * (50 - done)) +
                                 (" ({}/{})".format(size(downloaded), size(total))))
                sys.stdout.flush()
    sys.stdout.write('\n')


def git_clone_konduit(use_https=True, tag=DEFAULT_KONDUIT_TAG):
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
        subprocess.call(["git", "checkout", tag], cwd=KONDUIT_SOURCE_DIR,
                        shell=sys.platform.startswith("win"))
    except Exception as e:
        raise RuntimeError(">>> Could not clone konduit-serving repository and switch to commit hash {}"
                           "Make sure to have git installed. Type 'konduit-init --help' for help .\n"
                           .format(tag), e)


def build_jar(operating_sys, spin, chip):
    """Build the actual JAR, using our mvnw wrapper under the hood."""

    if not os.path.exists(KONDUIT_JAR_DIR):
        os.makedirs(KONDUIT_JAR_DIR)

    if operating_sys is None:
        operating_sys = get_platform()

    # Pulling in changes if needed
    try:
        subprocess.call(["git", "-C", KONDUIT_SOURCE_DIR, "pull"])
        subprocess.call(["git", "-C", KONDUIT_SOURCE_DIR, "fetch", "--all"])
    except Exception as e:
        raise RuntimeError(">>> Could not fetch and pull changes from konduit-serving repository. Make sure to have "
                           "git installed. Type " + "konduit-init --help for help resolving this.\n", e)

    # Building the uber-jar file
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
                "--chip",
                chip,
                "--target",
                KONDUIT_JAR_PATH
            ],
            shell=sys.platform.startswith("win")
        )
    except Exception as e:
        RuntimeError("Failed to build jar.\n", e)


def get_git_tags():
    response = requests.get("https://api.github.com/repos/KonduitAI/konduit-serving/tags")
    status_code = response.status_code
    if status_code == 200:
        tags = response.json()
        return [tag['name'] for tag in tags]
    else:
        return [DEFAULT_KONDUIT_TAG]


def get_jar_url(platform, version, spin, chip):
    if not platform:
        platform = get_platform()

    return KONDUIT_JAR_URL_FORMAT.format(version=version,
                                         platform=platform,
                                         tag=DOWNLOAD_TAG,
                                         spin=spin,
                                         chip=chip)


git_tags = list(set(get_git_tags()).difference(INCOMPATIBLE_COMPILATION_TAGS))
if len(git_tags) == 0:
    git_tags = [DEFAULT_KONDUIT_TAG]
DEFAULT_KONDUIT_TAG = git_tags[0]  # Assuming the first one in the response is the most recent one


@click.command()
@click.option(
    "-p", "--platform",
    help="Your operating system. "
         "Defaults to the cpu version of the "
         "current OS platform in use.",
    type=click.Choice(os_choices),
)
@click.option(
    "--https",
    type=bool,
    default=True,
    show_default=True,
    help="If True, use HTTPS to clone konduit-serving, else SSH.",
)
@click.option(
    "-t", "--tag",
    default=DEFAULT_KONDUIT_TAG,
    show_default=True,
    help="The git tag to build the source code from.",
    type=click.Choice(git_tags)
)
@click.option(
    "-s", "--spin",
    default="all",
    show_default=True,
    help="Whether to bundle Python ('python'), PMML ('pmml'), both ('all') " +
         "or neither ('minimal'). Python bundling is not encouraged with ARM, " +
         "and PMML bundling is not encouraged if agpl license is an issue.",
)
@click.option(
    "-c", "--chip",
    default="cpu",
    show_default=True,
    help="Specifies the chip architecture which could be cpu, gpu (CUDA) or arm.",
    type=click.Choice(["cpu", "gpu", "arm"])
)
@click.option(
    "-v", "--version",
    default=LAST_COMPATIBLE_KONDUIT_VERSION,
    show_default=True,
    help="Only works with the `--download` option to specify the version of konduit-serving to be downloaded.",
)
@click.option(
    "-d", "--download",
    is_flag=True,
    help="If set, downloads the pre-built jar file instead of building it. "
         "Also works with `--platform`, `--chip`, `--version` and `--spin` options.",
)
def init(platform, https, tag, spin, chip, version, download):
    if download:
        if parse(LAST_COMPATIBLE_KONDUIT_VERSION.replace("-SNAPSHOT", "")) < parse(version.replace("-SNAPSHOT", "")):
            print("This version of Python CLI is only compatible with versions <= {}"
                  .format(LAST_COMPATIBLE_KONDUIT_VERSION))
        else:
            download_if_required(get_jar_url(platform, version, spin, chip), KONDUIT_JAR_PATH)
    else:
        git_clone_konduit(https, tag)
        build_jar(platform, spin, chip)


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
