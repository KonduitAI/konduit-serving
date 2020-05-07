import json
import pytest
from jnius import autoclass
from konduit import *
from konduit.json_utils import config_to_dict_with_type

StringJava = autoclass("java.lang.String")
InferenceConfigurationJava = autoclass("ai.konduit.serving.InferenceConfiguration")


@pytest.mark.unit
@pytest.mark.skip("Can't seem to find the toJson method inside the inference configuration. This might be due to the "
                  "TextConfig interface addition")
# TODO: fix this test
def test_json_compare():
    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(
        http_port=1300, output_data_format="NUMPY"
    )

    model_pipeline_step = TensorFlowStep(
        path="model.pb",
        parallel_inference_config=parallel_inference_config,
        input_names=["IteratorGetNext:0", "IteratorGetNext:1", "IteratorGetNext:4"],
        output_names=["loss/Softmax"],
    )

    inference = InferenceConfiguration(
        serving_config=serving_config, steps=[model_pipeline_step]
    )

    assert_config_works(inference)


@pytest.mark.unit
@pytest.mark.skip("Can't seem to find the toJson method inside the inference configuration. This might be due to the "
                  "TextConfig interface addition")
# TODO: fix this test
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