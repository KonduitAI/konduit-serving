from jnius_config import set_classpath
try:
    set_classpath('konduit.jar')
except:
    print("VM already running from previous test")


from konduit import *
from konduit.json_utils import json_with_type
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import json
import random
import time
from jnius import autoclass



def test_build_tp():
    TransformProcessBuilder = autoclass('org.datavec.api.transform.TransformProcess$Builder')
    TransformProcess = autoclass('org.datavec.api.transform.TransformProcess')

    SchemaBuilder = autoclass('org.datavec.api.transform.schema.Schema$Builder')
    schema = SchemaBuilder().addColumnString('first').build()
    tp = TransformProcessBuilder(schema) \
        .appendStringColumnTransform("first", "two") \
    .build()

    tp_json = tp.toJson()
    from_json = TransformProcess.fromJson(tp_json)
    json_tp = json.dumps(tp_json)
    as_python_json = json.loads(tp_json)
    transform_process = TransformProcessPipelineStep(
        transform_processes={'default': as_python_json},
        input_names=['default'],
        output_names=['default'],
        input_schemas={'default': ['String']},
        output_schemas={'default': ['String']},
        input_column_names={'default': ['first']},
        output_column_names={'default': ['first']}
    )

    input_names = ['default']
    output_names = ['default']
    port = random.randint(1000, 65535)

    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(http_port=port,
                                   input_data_type='JSON',
                                   output_data_type='ARROW',
                                   log_timings=True,
                                   parallel_inference_config=parallel_inference_config)

    inference = InferenceConfiguration(serving_config=serving_config,
                                       pipeline_steps=[transform_process])
    as_json = json_with_type(inference)
    InferenceConfigurationJava = autoclass('ai.konduit.serving.InferenceConfiguration')
    config = InferenceConfigurationJava.fromJson(json.dumps(as_json))

    server = Server(config=inference,
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