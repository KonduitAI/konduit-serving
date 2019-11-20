from konduit import *
from konduit.json_utils import config_to_dict_with_type
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import json
import random
import time
from jnius import autoclass
import pydatavec


def load_java_tp(tp_json):
    """Just used for testing, users won't need this."""
    StringJava = autoclass("java.lang.String")
    JTransformProcess = autoclass('org.datavec.api.transform.TransformProcess')
    JTransformProcess.fromJson(StringJava(tp_json))


def test_build_tp():
    schema = pydatavec.Schema()
    schema.add_string_column('first')
    tp = pydatavec.TransformProcess(schema)
    tp.append_string('first', 'two')
    java_tp = tp.to_java()

    tp_json = java_tp.toJson()
    load_java_tp(tp_json)
    json_tp = json.dumps(tp_json)

    as_python_json = json.loads(tp_json)
    transform_process = TransformProcessStep()\
        .set_input(None, ['first'], ['String'])\
        .set_output(None, ['first'], ['String'])\
        .transform_process(as_python_json)

    input_names = ['default']
    output_names = ['default']
    port = random.randint(1000, 65535)

    serving_config = ServingConfig(http_port=port,
                                   input_data_type='JSON',
                                   output_data_type='ARROW',
                                   log_timings=True)

    inference_config = InferenceConfiguration(serving_config=serving_config,
                                              pipeline_steps=[transform_process])
    as_json = config_to_dict_with_type(inference_config)
    inference_configuration_java = autoclass(
        'ai.konduit.serving.InferenceConfiguration')

    unused_config = inference_configuration_java.fromJson(
        StringJava(json.dumps(as_json)))

    server = Server(inference_config=inference_config,
                    extra_start_args='-Xmx8g',
                    jar_path='konduit.jar')
    server.start()
    print('Process started. Sleeping 10 seconds.')
    client = Client(input_names=input_names,
                    output_names=output_names,
                    return_output_type='ARROW',
                    input_type='JSON',
                    endpoint_output_type='RAW',
                    url='http://localhost:' + str(port))

    data_input = {'first': 'value'}

    time.sleep(30)
    assert is_port_in_use(port)

    try:
        predicted = client.predict(data_input)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()
