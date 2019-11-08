from konduit import *
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import numpy as np
import time
import random


def test_server_start():
    port = random.randint(1000, 65535)
    serving_config = ServingConfig(http_port=port,
                                   input_data_type='NUMPY',
                                   output_data_type='NUMPY',
                                   log_timings=True)

    python_config = PythonConfig(
        python_code='first += 2',
        python_inputs={'first': 'NDARRAY'},
        python_outputs={'first': 'NDARRAY'},
    )

    python_pipeline_step = PythonPipelineStep().step(
        python_config=python_config, step_name="default", input_types=['NDArray'],
        output_types=['NDArray'], input_column_names=['first'], output_column_names=['first']
    )

    inference_config = InferenceConfiguration(serving_config=serving_config,
                                              pipeline_steps=[python_pipeline_step])

    server = Server(config=inference_config,
                    extra_start_args='-Xmx8g',
                    jar_path='konduit.jar')
    server.start()
    print('Process started. Sleeping 10 seconds.')
    client = Client(input_names=['default'],
                    output_names=['default'],
                    input_type='NUMPY',
                    endpoint_output_type='NUMPY',
                    url='http://localhost:' + str(port))

    data_input = {'default': np.load('../data/input-0.npy')}

    time.sleep(10)
    assert is_port_in_use(port)

    try:
        predicted = client.predict(data_input)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()
