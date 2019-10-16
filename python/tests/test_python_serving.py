from jnius_config import set_classpath
try:
    set_classpath('konduit.jar')
except:
    print("VM already running from previous test")

from konduit import *
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import numpy as np
import time
import random


def test_server_start():
    input_names = ['default']
    output_names = ['default']
    port = random.randint(1000, 65535)
    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(http_port=port,
                                   input_data_type='NUMPY',
                                   output_data_type='NUMPY',
                                   log_timings=True,
                                   parallel_inference_config=parallel_inference_config)

    python_config = PythonConfig(
        python_code='first += 2'
        , python_inputs={'first': 'NDARRAY'},
        python_outputs={'first': 'NDARRAY'},
    )

    python_pipeline_step = PythonPipelineStep(input_names=input_names,
                                              output_names=output_names,
                                              input_schemas=({'default': ['NDArray']}),
                                              output_schemas=({'default': ['NDArray']}),
                                              input_column_names={'default': ['first']},
                                              output_column_names={'default': ['first']},
                                              python_configs={'default': python_config})

    inference = InferenceConfiguration(serving_config=serving_config,
                                       pipeline_steps=[python_pipeline_step])

    server = Server(config=inference,
                    extra_start_args='-Xmx8g',
                    jar_path='konduit.jar')
    server.start()
    print('Process started. Sleeping 10 seconds.')
    client = Client(input_names=input_names,
                    output_names=output_names,
                    input_type='NUMPY',
                    endpoint_output_type='NUMPY',
                    url='http://localhost:' + str(port))

    data_input = {
        'default': np.load('../data/input-0.npy'),
    }

    time.sleep(4)
    assert is_port_in_use(port)

    try:
        predicted = client.predict(data_input)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()
