from jnius import autoclass
import json


def load_java_tp(tp_json):
    """Just used for testing, users won't need this."""
    string_java = autoclass("java.lang.String")
    java_transform_process = autoclass("org.datavec.api.transform.TransformProcess")
    java_transform_process.fromJson(string_java(tp_json))


def inference_from_json(as_json):
    string_java = autoclass("java.lang.String")
    inference_configuration_java = autoclass(
        "ai.konduit.serving.InferenceConfiguration"
    )
    inference_configuration_java.fromJson(string_java(json.dumps(as_json)))
