from konduit import *
from konduit.json_utils import config_to_dict_with_type

from jnius import autoclass
import json
import pytest


StringJava = autoclass("java.lang.String")
InferenceConfigurationJava = autoclass("ai.konduit.serving.InferenceConfiguration")


@pytest.mark.unit
def test_json_compare():
    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(
        http_port=1300, input_data_format="NUMPY", output_data_format="NUMPY"
    )

    tensorflow_config = TensorFlowConfig(
        model_config_type=ModelConfigType(
            model_type="TENSORFLOW", model_loading_path="model.pb"
        )
    )

    model_pipeline_step = ModelStep(
        model_config=tensorflow_config,
        parallel_inference_config=parallel_inference_config,
        input_names=["IteratorGetNext:0", "IteratorGetNext:1", "IteratorGetNext:4"],
        output_names=["loss/Softmax"],
    )

    inference = InferenceConfiguration(
        serving_config=serving_config, steps=[model_pipeline_step]
    )

    assert_config_works(inference)


@pytest.mark.unit
def test_python_serde():
    input_names = ["default"]
    output_names = ["default"]

    python_config = PythonConfig(
        python_code="first += 2",
        python_inputs={"first": "NDARRAY"},
        python_outputs={"first": "NDARRAY"},
    )

    port = 1300
    serving_config = ServingConfig(
        http_port=port,
        input_data_format="NUMPY",
        output_data_format="NUMPY",
        log_timings=True,
    )

    python_pipeline_step = PythonStep(
        input_names=input_names,
        output_names=output_names,
        python_configs=DictWrapper({"default": python_config}),
    )

    inference = InferenceConfiguration(
        serving_config=serving_config, steps=[python_pipeline_step]
    )

    assert_config_works(inference)


def assert_config_works(config):
    config_json = config_to_dict_with_type(config)
    json_str = json.dumps(config_json)
    config = InferenceConfigurationJava.fromJson(StringJava(json_str))
    test_json = config.toJson()
    test_json_dict = json.loads(test_json)
    config_json_dict = config_json
    test_pipeline_steps = test_json_dict["steps"]
    config_pipeline_steps = config_json_dict["steps"]
    assert len(test_pipeline_steps) == len(config_pipeline_steps)
