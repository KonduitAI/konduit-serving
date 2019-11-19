from konduit import *
from konduit.json_utils import config_to_dict_with_type
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import random
import time
import json
from jnius import autoclass


def test_build_tp():
    TransformProcessBuilder = autoclass(
        'org.datavec.api.transform.TransformProcess$Builder')
    TransformProcess = autoclass('org.datavec.api.transform.TransformProcess')
    StringJava = autoclass("java.lang.String")

    SchemaBuilder = autoclass(
        'org.datavec.api.transform.schema.Schema$Builder')
    schema = SchemaBuilder().addColumnString(StringJava('first')).build()
    tp = TransformProcessBuilder(schema).appendStringColumnTransform(StringJava("first"), StringJava("two")).build()

    tp_json = tp.toJson()
    TransformProcess.fromJson(StringJava(tp_json))
    json.dumps(tp_json)
    as_python_json = json.loads(tp_json)
    transform_process = TransformProcessPipelineStep()\
        .set_input(None, ['first'], ['String'])\
        .set_output(None, ['first'], ['String'])\
        .transform_process(as_python_json)

    port = random.randint(1000, 65535)
    serving_config = ServingConfig(http_port=port,
                                   input_data_type='JSON',
                                   output_data_type='JSON',
                                   log_timings=True)

    inference_config = InferenceConfiguration(serving_config=serving_config,
                                              pipeline_steps=[transform_process])
    as_json = config_to_dict_with_type(inference_config)
    inference_configuration_java_class = autoclass(
        'ai.konduit.serving.InferenceConfiguration')
    config = inference_configuration_java_class.fromJson(
        StringJava(json.dumps(as_json)))

    server = Server(inference_config=inference_config,
                    extra_start_args='-Xmx8g',
                    jar_path='konduit.jar')
    server.start()
    print('Process started. Sleeping 10 seconds.')
    client = Client(return_output_type='JSON',
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
