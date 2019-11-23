import os as opos
import click
import subprocess
import numpy as np
import logging

USER_PATH = opos.path.expanduser('~')
KONDUIT_BASE_DIR = opos.path.join(USER_PATH, '.konduit')
KONDUIT_DIR = opos.path.join(KONDUIT_BASE_DIR, 'konduit-serving')
KONDUIT_PID_STORAGE = opos.path.join(KONDUIT_DIR, 'pid.json')


def mkdir(x):
    """Create a directory if it doesn't exist already"""
    if not opos.path.isdir(x):
        opos.mkdir(x)


def create_konduit_folders():
    mkdir(KONDUIT_BASE_DIR)
    mkdir(KONDUIT_DIR)


def git_clone_konduit(use_https=True):
    """Clone the konduit-serving git repo, if it doesn't already exist locally."""
    if not opos.path.exists(KONDUIT_DIR):
        if use_https:
            repo = 'https://github.com/KonduitAI/konduit-serving.git'
        else:
            repo = 'git@github.com:KonduitAI/konduit-serving.git'
        try:
            subprocess.call(["git", "clone", repo, KONDUIT_DIR])
        except Exception as e:
            raise RuntimeError('>>> Could not clone konduit-serving repopository. Make sure to have '
                               'git installed. Type' +
                               'konduit-python --help for help resolving this.\n', e)


def build_jar(operating_sys):
    """Build the actual JAR, using our mvnw wrapper under the hood."""
    try:
        subprocess.call(["git", "-C", KONDUIT_DIR, "pull"])
    except Exception as e:
        raise RuntimeError('>>> Could not clone konduit-serving repository. Make sure to have '
                           'git installed. Type ' +
                           'konduit-python --help for help resolving this.\n', e)

    try:
        subprocess.call(['python3', '--version'])
    except Exception as e:
        raise RuntimeError('>>> No python3 found on your system. Make sure to install python3 first, '
                           'then run konduit-python again.\n', e)
    try:
        subprocess.call(['python3', opos.path.join(KONDUIT_DIR, 'build_jar.py'), '--os', operating_sys,
                         '--source', KONDUIT_DIR])
    except Exception as e:
        RuntimeError('Failed to build jar.\n', e)


def export_jar_path():
    """Export the environment variable KONDUIT_JAR_PATH so that the Python package will automatically
    pick it up."""
    jar_path = opos.path.join(KONDUIT_DIR, 'konduit.jar')
    opos.environ['KONDUIT_JAR_PATH'] = jar_path


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
@click.option('--os', required=True, help='Your operating system. Choose from  `windows-x86_64`,`linux-x86_64`,'
                                          '`linux-x86_64-gpu`,' +
                                          '`macosx-x86_64`, `linux-armhf` and `windows-x86_64-gpu`')
def build(os):
    """Build the underlying konduit.jar (again)."""
    build_jar(os)


@click.command()
@click.option('--config', required=True, help='Relative or absolute path to your konduit serving config file.')
@click.option('--start_server', default=True, help='Whether to start the server instance after initialization.')
def serve(config, start_server):
    """Serve a pipeline from a konduit configuration"""
    from konduit.load import store_pid
    from konduit.load import server_from_file

    server = server_from_file(file_path=config, start_server=start_server)
    store_pid(config, server.process.pid)
    logging.info(">>> Started a Konduit server with PID " + str(server.process.pid))


@click.command()
@click.option('--config', required=True, help='Relative or absolute path to your konduit serving config file.')
@click.option('--numpy_data', help='Path to your numpy data')
def predict_numpy(config, numpy_data):
    """Get predictions for your pipeline from numpy input."""
    # TODO: Note that this is a very limited use case for demo purposes only. we need a more reliable
    #  system going forward.
    from konduit.load import client_from_file

    client = client_from_file(file_path=config)
    print(client.predict(np.load(numpy_data)))


@click.command()
@click.option('--config', required=True, help='Relative or absolute path to your konduit serving config file.')
def stop_server(config):
    """Stop the Konduit server associated with a given config file."""
    from konduit.server import stop_server_by_pid
    from konduit.load import pop_pid

    pid = pop_pid(config)
    stop_server_by_pid(pid)
    logging.info(">>> Stopped running Konduit server with PID " + str(pid))


@click.group()
def cli():
    pass


cli.add_command(init)
cli.add_command(build)
cli.add_command(serve)
cli.add_command(predict_numpy)
cli.add_command(stop_server)
