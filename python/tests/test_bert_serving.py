import numpy as np
import pytest
from konduit import (
    ParallelInferenceConfig,
    ServingConfig,
    TensorFlowConfig,
    ModelConfigType,
)
from konduit import TensorDataTypesConfig, ModelStep, InferenceConfiguration
from konduit.client import Client
from konduit.server import Server
from konduit.utils import is_port_in_use


@pytest.mark.integration
def test_server_start():
    input_names = ["IteratorGetNext:0", "IteratorGetNext:1", "IteratorGetNext:4"]
    output_names = ["loss/Softmax"]
    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(
        output_data_format="NUMPY",
        log_timings=True,
    )

    tensorflow_config = TensorFlowConfig(
        model_config_type=ModelConfigType(
            model_type="TENSORFLOW", model_loading_path="bert_mrpc_frozen.pb"
        ),
        tensor_data_types_config=TensorDataTypesConfig(
            input_data_types={
                "IteratorGetNext:0": "INT32",
                "IteratorGetNext:1": "INT32",
                "IteratorGetNext:4": "INT32",
            }
        ),
    )

    model_pipeline_step = ModelStep(
        model_config=tensorflow_config,
        parallel_inference_config=parallel_inference_config,
        input_names=input_names,
        output_names=output_names,
    )

    inference = InferenceConfiguration(
        serving_config=serving_config, steps=[model_pipeline_step]
    )

    server = Server(
        inference_config=inference, extra_start_args="-Xmx8g", jar_path="konduit.jar"
    )
    _, port, started = server.start()

    data_input = {
        "IteratorGetNext:0": np.load("../data/input-0.npy"),
        "IteratorGetNext:1": np.load("../data/input-1.npy"),
        "IteratorGetNext:4": np.load("../data/input-4.npy"),
    }

    assert started  # will be true if the server was started
    assert is_port_in_use(port)

    client = Client(input_data_format="NUMPY", prediction_type="RAW", port=port)

    try:
        predicted = client.predict(data_input)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()
