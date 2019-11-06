import os as opos
import click
import subprocess

USER_PATH = opos.path.expanduser('~')
KONDUIT_BASE_DIR = opos.path.join(USER_PATH, '.konduit')
KONDUIT_DIR = opos.path.join(KONDUIT_BASE_DIR, 'konduit-serving')


def mkdir(x):
    if not opos.path.isdir(x):
        opos.mkdir(x)


mkdir(KONDUIT_BASE_DIR)


def git_clone_konduit(use_https=True):
    """Clone the konduit-serving git repo, if it doesn't already exist locally."""
    if not opos.path.exists(KONDUIT_DIR):
        if use_https:
            repo = 'https://github.com/KonduitAI/konduit-serving.git'
        else:
            repo = 'git@github.com:KonduitAI/konduit-serving.git'
        try:
            subprocess.call(["git", "clone", repo, KONDUIT_DIR])
        except:
            RuntimeError('Could not clone konduit-serving repopository. Make sure to have git installed. Type' +
                         'konduit-python --help for help resolving this')


def build_jar(operating_sys):
    """Build the actual JAR, using our mvnw wrapper under the hood."""
    # try:
    #     subprocess.call(['python3', '--version'])
    # except:
    #     RuntimeError(
    #         'No python3 found on your system. Make sure to install python3 first, then run konduit-python again')
    try:
        subprocess.call(['python3', opos.path.join(KONDUIT_DIR, 'build_jar.py'), '--os', operating_sys,
                         '--source', KONDUIT_DIR])
    except:
        RuntimeError('Failed to build jar')


def export_jar_path():
    """Export the environment variable KONDUIT_JAR_PATH so that the Python package will automatically
    pick it up."""
    jar_path = opos.path.join(KONDUIT_DIR, 'konduit.jar')
    subprocess.call(['export', 'KONDUIT_JAR_PATH=' + jar_path])


@click.command()
@click.option('--os', help='Your operating system. Choose from  `windows-x86_64`,`linux-x86_64`,`linux-x86_64-gpu`,' +
                           '`macosx-x86_64`, `linux-armhf` and `windows-x86_64-gpu`')
@click.option('--https', default=True, help='If True, use HTTPS to clone konduit-serving, else SSH.')
def init(os, https):
    """Initialize the konduit-python CLI. You can also use this to build a new konduit-serving JAR."""
    git_clone_konduit(https)
    build_jar(os)
    export_jar_path()


@click.command()
@click.option('--os', help='Your operating system. Choose from  `windows-x86_64`,`linux-x86_64`,`linux-x86_64-gpu`,' +
                           '`macosx-x86_64`, `linux-armhf` and `windows-x86_64-gpu`')
def build(os):
    """Build the underlying konduit.jar (again)."""
    build_jar(os)


@click.group()
def cli():
    pass


cli.add_command(init)
cli.add_command(build)
