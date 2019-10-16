from jnius_config import set_classpath
try:
    set_classpath('konduit.jar')
except:
    print("VM already running from previous test")


from konduit import *
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import time
import random

def test_server_start():
    port = random.randint(1000,65535)
    parallel_inference_config = ParallelInferenceConfig(workers=1)
    serving_config = ServingConfig(http_port=port,
                                   input_data_type='NUMPY',
                                   output_data_type='NUMPY',
                                   log_timings=True,
                                   parallel_inference_config=parallel_inference_config)

    tensorflow_config = TensorFlowConfig(model_config_type=ModelConfigType(
        model_type='TENSORFLOW',
        model_loading_path='./bert_mrpc_frozen.pb'),
        tensor_data_types_config=TensorDataTypesConfig(
        input_data_types={'IteratorGetNext:0': 'INT32',
                          'IteratorGetNext:1': 'INT32',
                          'IteratorGetNext:4': 'INT32'
                          }))

    model_pipeline_step = ModelPipelineStep(model_config=tensorflow_config,
                                            serving_config=serving_config,
                                            input_names=["IteratorGetNext:0",
                                                         "IteratorGetNext:1",
                                                         "IteratorGetNext:4"],
                                            output_names=["loss/Softmax"])

    inference_config = InferenceConfiguration(serving_config=serving_config,
                                              pipeline_steps=[model_pipeline_step])

    server = Server(config=inference_config, extra_start_args='-Xmx8g', jar_path='konduit.jar')
    server.start()
    print('Process started. Sleeping 30 seconds.')

    time.sleep(30)
    assert is_port_in_use(port)
    print('Done sleeping. Assuming server alive. Killing process.')
    server.stop()

