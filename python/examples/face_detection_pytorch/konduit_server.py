from konduit import *
from konduit.server import Server

import os
import sys

python_path = ':'.join(sys.path)
workdir = os.path.abspath('./Ultra-Light-Fast-Generic-Face-Detector-1MB')
python_path += ':' + workdir

port = 1337
serving_config = ServingConfig(http_port=port, input_data_type='JSON', output_data_type='JSON')

python_config = PythonConfig(
    python_path=python_path, python_code_path=os.path.join(workdir, 'detect_one.py'),
    python_inputs={'default': 'STR'}, python_outputs={'default': 'STR'},
)

python_pipeline_step = PythonPipelineStep().step(
    python_config=python_config, input_types=['String'], input_column_names=['default'],
    output_types=['String'], output_column_names=['default'], step_name="default"
)

inference = InferenceConfiguration(serving_config=serving_config, pipeline_steps=[python_pipeline_step])

server = Server(config=inference, extra_start_args='-Xmx8g')
server.start()
