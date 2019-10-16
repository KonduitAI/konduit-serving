from jnius_config import set_classpath
try:
    set_classpath('konduit.jar')
except:
    print("VM already running from previous test")

from konduit import *
from konduit.client import Client
from konduit.json_utils import json_with_type

import json
import random

import numpy as np



def test_multipart_encode():
    input_names = ["IteratorGetNext:0", "IteratorGetNext:1", "IteratorGetNext:4"]
    output_names = ["loss/Softmax"]
    port = random.randint(1000,65535)
    client = Client(input_names=input_names,
                    output_names=output_names,
                    input_type='NUMPY',
                    endpoint_output_type='NUMPY',
                    url='http://localhost:' + str(port))

    input = {
        'input1': Client._convert_numpy_to_binary(np.ones(1)),
        'input2': Client._convert_numpy_to_binary(np.ones(2))
    }

    converted = Client._convert_multi_part_inputs(input)
    body, content_type = Client._encode_multi_part_input(converted)
    output = client._convert_multi_part_output(content=body, content_type=content_type)
    print(output)

def test_python_serde():
    input_names = ['default']
    output_names = ['default']

    python_config = PythonConfig(
        python_code='first += 2'
        ,python_inputs=['first'],
        python_outputs=['first']
    )

    port = random.randint(1000,65535)
    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(http_port=port,
                                   input_data_type='NUMPY',
                                   output_data_type='NUMPY',
                                   log_timings=True,
                                   parallel_inference_config=parallel_inference_config)

    python_pipeline_step = PythonPipelineStep(input_names=input_names,
                                              output_names=output_names,
                                              python_configs={'default': python_config})

    inference = InferenceConfiguration(serving_config=serving_config,
                                       pipeline_steps=[python_pipeline_step])

    json.dumps(json_with_type(inference))
