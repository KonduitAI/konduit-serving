from konduit import *
from konduit.json_utils import config_to_dict_with_type
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import json
import random
import time
import pydatavec
from .utils import load_java_tp, inference_from_json
import pytest


@pytest.mark.integration
def test_build_tp():
    schema = pydatavec.Schema()
    schema.add_string_column("first")
    tp = pydatavec.TransformProcess(schema)
    tp.append_string("first", "two")
    java_tp = tp.to_java()

    tp_json = java_tp.toJson()
    load_java_tp(tp_json)
    _ = json.dumps(tp_json)

    as_python_json = json.loads(tp_json)
    transform_process = (
        TransformProcessStep()
        .set_input(None, ["first"], ["String"])
        .set_output(None, ["first"], ["String"])
        .transform_process(as_python_json)
    )

    input_names = ["default"]
    output_names = ["default"]
    port = random.randint(1000, 65535)

    serving_config = ServingConfig(
        http_port=port,
        input_data_format="JSON",
        output_data_format="ARROW",
        log_timings=True,
    )

    inference_config = InferenceConfiguration(
        serving_config=serving_config, steps=[transform_process]
    )
    as_json = config_to_dict_with_type(inference_config)
    inference_from_json(as_json)

    server = Server(
        inference_config=inference_config,
        extra_start_args="-Xmx8g",
        jar_path="konduit.jar",
    )
    server.start()
    print("Process started. Sleeping 10 seconds.")
    client = Client(
        convert_to_format="ARROW",
        input_data_format="JSON",
        output_data_format="RAW",
        port=port,
    )

    data_input = {"first": "value"}

    assert is_port_in_use(port)

    try:
        predicted = client.predict(data_input)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()
