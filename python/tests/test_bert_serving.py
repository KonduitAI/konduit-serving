import numpy as np
import pytest
from konduit import (
    ParallelInferenceConfig,
    ServingConfig,
    TensorFlowStep,
    InferenceConfiguration
)
from konduit.client import Client
from konduit.server import Server
from konduit.utils import is_port_in_use


@pytest.mark.integration
def test_server_start():
    server_id = "tensorflow_server"
    input_names = ["IteratorGetNext:0", "IteratorGetNext:1", "IteratorGetNext:4"]
    output_names = ["loss/Softmax"]
    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(
        output_data_format="NUMPY",
        log_timings=True,
    )

    model_pipeline_step = TensorFlowStep(
        path="bert_mrpc_frozen.pb",
        input_data_types={
            input_names[0]: "INT32",
            input_names[1]: "INT32",
            input_names[2]: "INT32",
        },
        parallel_inference_config=parallel_inference_config,
        input_names=input_names,
        output_names=output_names,
    )

    inference = InferenceConfiguration(
        serving_config=serving_config, steps=[model_pipeline_step]
    )

    server = Server(
        inference_config=inference, extra_start_args="-Xmx8g"
    )
    _, port, started = server.start(server_id)

    data_input = {
        input_names[0]: np.load("../data/input-0.npy"),
        input_names[1]: np.load("../data/input-1.npy"),
        input_names[2]: np.load("../data/input-4.npy"),
    }

    assert started  # will be true if the server was started
    assert is_port_in_use(port)

    client = Client(input_data_format="NUMPY", prediction_type="RAW", port=port)

    try:
        predicted = client.predict(data_input)
        print(predicted)
    except Exception as e:
        print(e)
    finally:
        server.stop(server_id)
