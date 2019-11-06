import random
import re
import sys
import os

from konduit import ServingConfig, PythonPipelineStep
from konduit import InferenceConfiguration
from konduit.server import Server


def cleanup(value, key):
    return value.replace("<{}:".format(key), "").replace(":>", "")


def separate(value, delimiter):
    separated = value.split(delimiter)
    return separated[0].strip(), separated[1].strip()


def get_individuals(read_input, key):
    matches = re.findall('<{}:.*:>'.format(key), read_input)

    if "rest" in key:
        # Since only one rest input and output type makes sense
        ret = cleanup(matches[0], key)
    else:
        ret = []
        for match in matches:
            ret.extend([separate(split, "|") for split in cleanup(match, key).split(",")])

    return ret


with open('script.py', 'r') as script_file:
    content = script_file.read()

    inputs = get_individuals(content, "in")
    outputs = get_individuals(content, "out")

    rest_input_type = get_individuals(content, "rest_in")
    rest_output_type = get_individuals(content, "rest_out")

    # ----------------------------------------------------------------------------------------

    port = random.randint(1000, 65535)

    serving_config = ServingConfig(http_port=port,
                                   input_data_type=rest_input_type,
                                   output_data_type=rest_output_type,
                                   log_timings=True)

    pythonConfig = {
        "pythonPath": ";".join(path.strip() for path in sys.path if path.strip()),
        "pythonCodePath": os.path.abspath("script.py"),
        "pythonInputs": dict(inputs),
        "pythonOutputs": dict(outputs)
    }

    pythonPipelineStep = PythonPipelineStep(input_names=["default"],
                                            input_column_names={"default": [key for key, _ in inputs]},
                                            input_schemas={"default": [value for _, value in inputs]},
                                            output_names=["default"],
                                            output_column_names={"default": [key for key, _ in outputs]},
                                            output_schemas={"default": [value for _, value in outputs]},
                                            python_configs={"default": pythonConfig})

    inference = InferenceConfiguration(serving_config=serving_config,
                                       pipeline_steps=[pythonPipelineStep])

    server = Server(config=inference,
                    extra_start_args='-Xmx8g',
                    jar_path='konduit.jar')

    server.start()
