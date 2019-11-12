from konduit import *
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use

import numpy as np
import time
import random
import os
import sys

python_path = ':'.join(sys.path)
workdir = os.path.abspath('./Ultra-Light-Fast-Generic-Face-Detector-1MB')
python_path += ':' + workdir
print(python_path)

input_names = ['default']
output_names = ['default']
port = random.randint(1000, 65535)
port = 9233
parallel_inference_config = ParallelInferenceConfig(workers=1)
serving_config = ServingConfig(http_port=port,
                               input_data_type='JSON',
                               output_data_type='JSON',
                               log_timings=True)

python_config = PythonConfig(
    python_path=python_path,
    python_code_path=workdir+'/detect_one.py',
    python_inputs={'default': 'STR'},
    python_outputs={'default': 'STR'},
)

python_pipeline_step = PythonPipelineStep(input_names=input_names,
                                          output_names=output_names,
                                          input_schemas=({'default': ['String']}),
                                          output_schemas=({'default': ['String']}),
                                          input_column_names={'default': ['default']},
                                          output_column_names={'default': ['default']},
                                          python_configs={'default': python_config})

inference = InferenceConfiguration(serving_config=serving_config,
                                   pipeline_steps=[python_pipeline_step])

server = Server(config=inference,
                #extra_start_args='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005',  # intellij remote debug
                extra_start_args='-Xmx8g',
                jar_path='konduit.jar')
server.start()
print('Process started.')
time.sleep(10)