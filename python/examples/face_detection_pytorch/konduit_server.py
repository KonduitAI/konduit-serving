from konduit import *
from konduit.server import Server
from konduit.client import Client

import os
import time
from utils import  default_python_path, to_base_64

# Set the working directory to this folder and register the "detect_image.py" script as code to be executed by konduit.
work_dir = os.path.abspath('.')
python_config = PythonConfig(
    python_path=default_python_path(work_dir), python_code_path=os.path.join(work_dir, 'detect_image.py'),
    python_inputs={'default': 'STR'}, python_outputs={'default': 'STR'},
)

# Configure a Python pipeline step for your Python code. Internally, konduit will take Strings as input and output
# for this example.
python_pipeline_step = PythonPipelineStep().step(
    python_config=python_config, input_types=['String'], input_column_names=['default'],
    output_types=['String'], output_column_names=['default'], step_name='default'
)
serving_config = ServingConfig(http_port=1337, input_data_type='JSON', output_data_type='JSON')
inference = InferenceConfiguration(serving_config=serving_config, pipeline_steps=[python_pipeline_step])

# Start a konduit server and wait for it to start
server = Server(config=inference, extra_start_args='-Xmx8g')
server.start()
time.sleep(10)

# Initialize a konduit client that takes in and outputs JSON
client = Client(input_names=['default'], output_names=['default'],
                input_type='JSON', return_output_type='JSON',
                endpoint_output_type='RAW',url='http://localhost:1337')

# encode the image from a file to base64 and get back a prediction from the konduit server
encoded_image = to_base_64(os.path.abspath('./Ultra-Light-Fast-Generic-Face-Detector-1MB/imgs/1.jpg'))
predicted = client.predict( {'default': encoded_image})
print(predicted)
server.stop()
