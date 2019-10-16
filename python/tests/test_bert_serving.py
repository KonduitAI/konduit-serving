from jnius_config import set_classpath
try:
    set_classpath('konduit.jar')
except:
    print("VM already running from previous test")


from konduit import ParallelInferenceConfig, ServingConfig, TensorFlowConfig, ModelConfigType
from konduit import TensorDataTypesConfig, ModelPipelineStep, InferenceConfiguration
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import numpy as np
import time
import random

def test_server_start():

    input_names = ["IteratorGetNext:0", "IteratorGetNext:1", "IteratorGetNext:4"]
    output_names = ["loss/Softmax"]
    port = random.randint(1000,65535)
    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(http_port=port,
                                   input_data_type='NUMPY',
                                   output_data_type='NUMPY',
                                   log_timings=True,
                                   parallel_inference_config=parallel_inference_config)

    tensorflow_config = TensorFlowConfig(
        model_config_type=
        ModelConfigType(model_type='TENSORFLOW',
                        model_loading_path='bert_mrpc_frozen.pb'),
        tensor_data_types_config=TensorDataTypesConfig(
            input_data_types={'IteratorGetNext:0': 'INT32',
                              'IteratorGetNext:1': 'INT32',
                              'IteratorGetNext:4': 'INT32'
                              }))

    model_pipeline_step = ModelPipelineStep(model_config=tensorflow_config,
                                            serving_config=serving_config,
                                            input_names=input_names,
                                            output_names=output_names)

    inference = InferenceConfiguration(serving_config=serving_config,
                                       pipeline_steps=[model_pipeline_step])

    server = Server(config=inference,
                    extra_start_args='-Xmx8g',
                    jar_path='konduit.jar')
    server.start()
    client = Client(input_names=input_names,
                    output_names=output_names,
                    input_type='NUMPY',
                    endpoint_output_type='NUMPY',
                    url='http://localhost:' + str(port))

    data_input = {
        'IteratorGetNext:0': np.load('../data/input-0.npy'),
        'IteratorGetNext:1': np.load('../data/input-1.npy'),
        'IteratorGetNext:4': np.load('../data/input-4.npy')
    }

    print('Process started. Sleeping 30 seconds.')
    time.sleep(30)
    assert is_port_in_use(port)

    try:
        predicted = client.predict(data_input)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()

