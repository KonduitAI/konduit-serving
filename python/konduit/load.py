from . import *
from .server import Server
from .client import Client
from .config import KONDUIT_PID_STORAGE
from .utils import create_konduit_folders

import yaml
import os
import json

MODEL_TYPES = ['TENSORFLOW', 'KERAS', 'COMPUTATION_GRAPH', 'MULTI_LAYER_NETWORK', 'PMML', 'SAMEDIFF']


# This creates all base folders under the hood and will be run once you import this module
create_konduit_folders()


def store_pid(file_path, pid):
    """ Store the process ID for a running Konduit Server, given a configuration file that
    the server has been started from.

    :param file_path: path to your Konduit configuration file
    :param pid: process ID of the Konduit server you created before.
    """
    yaml_path = os.path.abspath(file_path)
    pid_dict = {yaml_path: pid}
    if os.path.isfile(KONDUIT_PID_STORAGE):
        with open(KONDUIT_PID_STORAGE, 'w') as f:
            previous = json.load(f)
            new = previous.copy()
            new.update(pid_dict)
            json.dump(new, f)
    else:
        with open(KONDUIT_PID_STORAGE, 'w') as f:
            json.dump(pid_dict, f)


def pop_pid(file_path):
    """Remove a process ID from the list of running processes. Use this if you want
    to kill that process immediately after.

    :param file_path: path to the Konduit configuration file that your Konduit server has been created from.
    :return: the process ID belonging to that Konduit server instance.
    """
    pid = None
    with open(KONDUIT_PID_STORAGE, 'w') as f:
        yaml_path = os.path.abspath(file_path)
        previous = json.load(f)
        if yaml_path in previous:
            pid = previous.pop(yaml_path)
        json.dump(previous, f)
    return pid


def load_data(file_path):
    """Load data from a given YAML file into a Python dictionary.

    :param file_path: path to your YAML file.
    :return: contents of the file as Python dict.
    """
    with open(file_path, 'r') as f:
        data = yaml.safe_load(f)
    return data


def pop_data(serving_dictionary, dict_key):
    """Return a value from a Python dictionary for a given key, if it exists,
    and also remove the key-value pair afterwards.
    
    :param serving_dictionary: 
    :param dict_key:
    :return: 
    """
    result = None
    if dict_key in serving_dictionary:
        result = serving_dictionary.pop(dict_key)
    return result


def from_file(file_path, start_server=True):
    """Create Konduit Server and Client from file

    :param file_path: path to your konduit.yaml
    :param start_server: whether to start the server instance or not (if not you can start it later).
    :return: konduit.server.Server and konduit.client.Client instances
    """
    return server_from_file(file_path, start_server), client_from_file(file_path)


def server_from_file(file_path, start_server=True):
    """Create a Konduit Server from a from a configuration file.

    :param file_path: path to your konduit.yaml
    :param start_server: whether to start the server instance or not (if not you can start it later).
    :return: konduit.server.Server instance
    """
    data = load_data(file_path)
    serving_data = data.get('serving', None)

    extra_start_args = pop_data(serving_data, 'extra_start_args')
    sleep = pop_data(serving_data, 'sleep')
    jar_path = pop_data(serving_data, 'jar_path')
    config_path = pop_data(serving_data, 'config_path')
    if not config_path:
        config_path = 'config.json'

    serving_config = ServingConfig(**serving_data)

    step_data = data.get('steps', None)
    steps = []
    for step_config in step_data.values():
        steps.append(get_step(step_config))

    server = Server(
        serving_config=serving_config,
        steps=steps,
        extra_start_args=extra_start_args,
        jar_path=jar_path,
        config_path=config_path
    )
    if start_server:
        server.start(sleep=sleep)
    return server


def client_from_file(file_path):
    """Create a Konduit client instance from a configuration file

    :param file_path: path to your konduit.yaml
    :return: konduit.client.Client instance
    """
    data = load_data(file_path)
    client_data = data.get('client', None)
    client = Client(**client_data)
    return client


def get_step(step_config):
    """Get a PipelineStep from a step configuration.

    :param step_config: python dictionary with properties to create a PipelineStep
    :return: konduit.inference.PipelineStep instance.
    """
    step_type = step_config.pop('type')
    if step_type == 'PYTHON':
        step = get_python_step(step_config)
    elif step_type in MODEL_TYPES:  # TODO other types
        step = get_model_step(step_config, step_type)
    else:
        raise Exception('Step type of type ' + step_type + ' currently not supported.')
    return step


def get_python_step(step_config):
    """Get a PythonPipelineStep from a configuration object

    :param step_config: python dictionary with properties to create a PipelineStep
    :return: konduit.inference.PythonPipelineStep instance.
    """
    python_config = PythonConfig(**step_config)
    step = PythonPipelineStep().step(python_config)
    return step


def get_model_step(step_config, step_type):
    """Get a ModelPipelineStep from a configuration object

    :param step_config: python dictionary with properties to create a PipelineStep
    :param step_type: type of the step (TENSORFLOW, KERAS, COMPUTATION_GRAPH, MULTI_LAYER_NETWORK, PMML or SAMEDIFF)
    :return: konduit.inference.ModelPipelineStep instance.
    """
    model_config_type = ModelConfigType(
        model_type=step_type,
        model_loading_path=pop_data(step_config, 'model_loading_path')
    )

    if step_type == 'TENSORFLOW':  # TF has to extra properties, all others are identical
        model_config = TensorFlowConfig(
            config_proto_path=pop_data(step_config, 'config_proto_path'),
            saved_model_config=pop_data(step_config, 'saved_model_config'),
            model_config_type=model_config_type,
            tensor_data_types_config=get_tensor_data_types(step_config)
        )
    else:
        model_config = ModelConfig(
            model_config_type=model_config_type,
            tensor_data_types_config=get_tensor_data_types(step_config)
        )
    step_config['model_config'] = model_config
    step_config = extract_parallel_inference(step_config)
    step = ModelPipelineStep(**step_config)
    return step


def get_tensor_data_types(step_config):
    input_data_types = pop_data(step_config, 'input_data_types')
    output_data_types = pop_data(step_config, 'output_data_types')
    return TensorDataTypesConfig(
        input_data_types=input_data_types,
        output_data_types=output_data_types
    )


def extract_parallel_inference(step_config):
    pi_config = pop_data(step_config, 'parallel_inference_config')
    if pi_config:
        pic = ParallelInferenceConfig(**pi_config)
        step_config['parallel_inference_config'] = pic
    return step_config