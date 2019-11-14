from . import *
from .server import Server
from .client import Client

import yaml


def load_data(file_path):
    with open(file_path, 'r') as f:
        data = yaml.safe_load(f)
    return data


def pop_data(serving_data, name):
    result = None
    if name in serving_data:
        result = serving_data.pop(name)
    return result


def create_server_from_file(file_path, start_server=True):
    data = load_data(file_path)
    serving_data = data.get('serving', None)

    extra_start_args = pop_data(serving_data, 'extra_start_args')
    sleep = pop_data(serving_data, 'sleep')
    jar_path = pop_data(serving_data, 'jar_path')

    serving_config = ServingConfig(**serving_data)

    step_data = data.get('steps', None)
    steps = []
    for step_config in step_data.values():
        steps.append(get_step(step_config))

    server = Server(
        serving_config=serving_config,
        steps=steps,
        extra_start_args=extra_start_args,
        jar_path=jar_path
    )
    if start_server:
        server.start(sleep=sleep)
    return server


def create_client_from_file(file_path):
    data = load_data(file_path)
    client_data = data.get('client', None)
    client = Client(**client_data)
    return client


def get_step(step_config):
    step_type = step_config.pop('type')
    if step_type == 'PYTHON':
        python_config = PythonConfig(**step_config)
        step = PythonPipelineStep().step(python_config)
    elif step_type == 'TENSORFLOW':
        pass
    else:
        raise Exception('Step type of type ' + step_type + ' currently not supported.')
    return step
