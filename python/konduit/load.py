import json
import logging
import os
import yaml

from .inference import *
from .client import Client
from .server import Server
from .utils import to_unix_path, update_dict_with_unix_paths

USER_PATH = os.path.expanduser("~")
KONDUIT_BASE_DIR = os.path.join(USER_PATH, ".konduit")
KONDUIT_DIR = os.path.join(KONDUIT_BASE_DIR, "konduit-serving")
KONDUIT_PID_STORAGE = os.path.join(KONDUIT_DIR, "pid.json")
MODEL_TYPES = [
    "TENSORFLOW",
    "KERAS",
    "COMPUTATION_GRAPH",
    "MULTI_LAYER_NETWORK",
    "PMML",
    "SAMEDIFF",
]


def store_pid(file_path, pid):
    """ Store the process ID for a running Konduit Server, given a configuration file that
    the server has been started from.

    :param file_path: path to your Konduit configuration file
    :param pid: process ID of the Konduit server you created before.
    """
    yaml_path = os.path.abspath(file_path)
    pid_dict = {yaml_path: pid}
    with open(KONDUIT_PID_STORAGE, "w") as f:
        try:
            previous = json.load(f)
            new = previous.copy()
            new.update(pid_dict)
            json.dump(new, f)
        except:
            json.dump(pid_dict, f)


def pop_pid(file_path):
    """Remove a process ID from the list of running processes. Use this if you want
    to kill that process immediately after.

    :param file_path: path to the Konduit configuration file that your Konduit server has been created from.
    :return: the process ID belonging to that Konduit server instance.
    """
    pid = None
    with open(KONDUIT_PID_STORAGE, "r+") as f:
        yaml_path = os.path.abspath(file_path)
        try:
            previous = json.load(f)
            if yaml_path in previous:
                pid = previous.pop(yaml_path)
            f.seek(0)
            f.write(json.dumps(previous))
            f.truncate()
        except Exception as e:
            logging.warning("Process ID not found, file is empty.", e)
    return pid


def load_data(file_path, use_yaml=True):
    """Load data from a given YAML file into a Python dictionary.

    :param file_path: path to your YAML file.
    :param use_yaml: use yaml or json
    :return: contents of the file as Python dict.
    """
    with open(file_path, "r") as f:
        if use_yaml:
            data = yaml.safe_load(f)
        else:
            data = json.load(f)
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


def from_file(file_path, start_server=True, use_yaml=True):
    """Create Konduit Server and Client from file

    :param file_path: path to your konduit.yaml
    :param start_server: whether to start the server instance or not (if not you can start it later). Defaults to
           True so that the client can be loaded successfully.
    :param use_yaml: use yaml or json
    :return: konduit.server.Server and konduit.client.Client instances
    """
    return (
        server_from_file(file_path, start_server, use_yaml),
        client_from_file(file_path, use_yaml),
    )


def server_from_file(file_path, start_server=False, use_yaml=True):
    """Create a Konduit Server from a from a configuration file.

    :param file_path: path to your konduit.yaml
    :param start_server: whether to start the server instance or not (if not you can start it later). Defaults to
           False, meaning you have to start the server manually by calling "start()" explicitly.
    :param use_yaml: use yaml or json
    :return: konduit.server.Server instance
    """
    file_path = to_unix_path(file_path)
    data = load_data(file_path, use_yaml)
    serving_data = data.get("serving", None)

    extra_start_args = pop_data(serving_data, "extra_start_args")
    jar_path = pop_data(serving_data, "jar_path")
    config_path = pop_data(serving_data, "config_path")
    if not config_path:
        config_path = "config.json"

    serving_config = ServingConfig(**serving_data)

    step_data = data.get("steps", None)
    steps = []
    for step_config in step_data.values():
        steps.append(get_step(step_config))

    server = Server(
        serving_config=serving_config,
        steps=steps,
        extra_start_args=extra_start_args,
        jar_path=jar_path,
        config_path=config_path,
    )
    if start_server:
        server.start()
    return server


def client_from_file(file_path, use_yaml=True):
    """Create a Konduit client instance from a configuration file.
    If your konduit configuration file has a "client" section, that
    is used to create the client instance. If it doesn't, all properties
    are derived from the "serving" section.

    :param file_path: path to your konduit.yaml
    :param use_yaml: use yaml or json
    :return: konduit.client.Client instance
    """
    file_path = to_unix_path(file_path)
    data = load_data(file_path, use_yaml)
    if "client" in data.keys():
        client_data = data.get("client", None)
        client = Client(**client_data)
    else:
        client_data = data.get("serving", None)
        port = client_data.get("http_port", None)
        input_data_format = client_data.get("input_data_format", None)
        output_data_format = client_data.get("output_data_format", None)

        if not port:
            raise RuntimeError(
                "No HTTP port found in configuration file, can't proceed."
            )
        client = Client(
            port=port,
            input_data_format=input_data_format,
            output_data_format=output_data_format,
        )
    return client


def get_step(step_config):
    """Get a PipelineStep from a step configuration.

    :param step_config: python dictionary with properties to create a PipelineStep
    :return: konduit.inference.PipelineStep instance.
    """
    step_type = step_config.pop("type")
    step_config = update_dict_with_unix_paths(step_config)

    if step_type == "PYTHON":
        step = get_python_step(step_config)
    elif step_type in MODEL_TYPES:
        step = get_model_step(step_config, step_type)
    elif step_type == 'IMAGE_LOADING':
        step = get_image_load_step(step_config)
    else:
        raise Exception("Step type of type " + step_type + " currently not supported.")
    return step


def get_python_step(step_config):
    """Get a PythonStep from a configuration object

    :param step_config: python dictionary with properties to create a PipelineStep
    :return: konduit.inference.PythonStep instance.
    """
    python_config = PythonConfig(**step_config)
    step = PythonStep().step(python_config)
    return step

def get_image_load_step(step_config):
    """Get a ImageLoadingStep from a configuration object

    :param step_config: python dictionary with properties to create the ImageLoadingStep
    :return: konduit.inference.ImageLoadingStep instance.
    """
    step = ImageLoadingStep(**step_config)
    return step


def get_model_step(step_config, step_type):
    """Get a ModelStep from a configuration object

    :param step_config: python dictionary with properties to create a PipelineStep
    :param step_type: type of the step (TENSORFLOW, KERAS, COMPUTATION_GRAPH, MULTI_LAYER_NETWORK, PMML or SAMEDIFF)
    :return: konduit.inference.ModelStep instance.
    """
    model_config_type = ModelConfigType(
        model_type=step_type,
        model_loading_path=pop_data(step_config, "model_loading_path"),
    )
    if (
        step_type == "TENSORFLOW"
    ):  # TF has to extra properties, all others are identical
        model_config = TensorFlowConfig(
            config_proto_path=pop_data(step_config, "config_proto_path"),
            saved_model_config=pop_data(step_config, "saved_model_config"),
            model_config_type=model_config_type,
            tensor_data_types_config=get_tensor_data_types(step_config),
        )
    else:
        model_config = ModelConfig(
            model_config_type=model_config_type,
            tensor_data_types_config=get_tensor_data_types(step_config),
        )
    step_config["model_config"] = model_config
    step_config = extract_parallel_inference(step_config)
    step = ModelStep(**step_config)
    return step


def get_tensor_data_types(step_config):
    input_data_types = pop_data(step_config, "input_data_types")
    output_data_types = pop_data(step_config, "output_data_types")
    return TensorDataTypesConfig(
        input_data_types=input_data_types, output_data_types=output_data_types
    )


def extract_parallel_inference(step_config):
    pi_config = pop_data(step_config, "parallel_inference_config")
    if pi_config:
        pic = ParallelInferenceConfig(**pi_config)
        step_config["parallel_inference_config"] = pic
    return step_config
