from konduit import *
from konduit.client import Client
from konduit.json_utils import config_to_dict_with_type

import json
import random
import numpy as np
import pytest


@pytest.mark.unit
def test_multipart_encode():
    input_names = ["IteratorGetNext:0", "IteratorGetNext:1", "IteratorGetNext:4"]
    output_names = ["loss/Softmax"]
    port = random.randint(1000, 65535)
    client = Client(
        input_names=input_names,
        output_names=output_names,
        input_data_format="NUMPY",
        output_data_format="NUMPY",
        port=port,
    )

    input_data = {
        "input1": Client._convert_numpy_to_binary(np.ones(1)),
        "input2": Client._convert_numpy_to_binary(np.ones(2)),
    }

    converted = Client._convert_multi_part_inputs(input_data)
    body, content_type = Client._encode_multi_part_input(converted)

    client._convert_multi_part_output(content=body, content_type=content_type)


@pytest.mark.unit
def test_python_serde():
    python_configuration = PythonConfig(
        python_code="first += 2", python_inputs=["first"], python_outputs=["first"]
    )

    port = random.randint(1000, 65535)
    serving_config = ServingConfig(
        http_port=port,
        input_data_format="NUMPY",
        output_data_format="NUMPY",
        log_timings=True,
    )

    python_pipeline_step = PythonStep().step(python_configuration)
    inference_config = InferenceConfiguration(
        serving_config=serving_config, steps=[python_pipeline_step]
    )

    json.dumps(config_to_dict_with_type(inference_config))
