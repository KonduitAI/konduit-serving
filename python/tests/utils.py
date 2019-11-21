from jnius import autoclass
import json


def load_java_tp(tp_json):
    """Just used for testing, users won't need this."""
    StringJava = autoclass("java.lang.String")
    JTransformProcess = autoclass('org.datavec.api.transform.TransformProcess')
    JTransformProcess.fromJson(StringJava(tp_json))


def inference_from_json(as_json):
    StringJava = autoclass("java.lang.String")
    inference_configuration_java = autoclass('ai.konduit.serving.InferenceConfiguration')
    inference_configuration_java.fromJson(StringJava(json.dumps(as_json)))

