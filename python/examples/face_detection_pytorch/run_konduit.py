from konduit import *
from konduit.server import Server
from konduit.client import Client
from konduit.utils import default_python_path

import os
import cv2

# Set the working directory to this folder and register
# the "detect_image.py" script as code to be executed by konduit.
work_dir = os.path.abspath('.')
python_config = PythonConfig(
    python_path=default_python_path(work_dir), python_code_path=os.path.join(work_dir, 'detect_image.py'),
    python_inputs={'image': 'NDARRAY'}, python_outputs={'num_boxes': 'STR'},
)

# Configure a Python pipeline step for your Python code. Internally, konduit will take Strings as input and output
# for this example.
python_pipeline_step = PythonStep().step(python_config)
serving_config = ServingConfig(http_port=1337, input_data_format='NUMPY', output_data_format='JSON')

# Start a konduit server and wait for it to start
server = Server(serving_config=serving_config, steps=[python_pipeline_step])
server.start(sleep=10)

# Initialize a konduit client that takes in and outputs JSON
client = Client(input_data_format='NUMPY', return_output_data_format='JSON',
                output_data_format='RAW', url='http://localhost:1337', timeout=30)

encoded_image = cv2.cvtColor(cv2.imread('./Ultra-Light-Fast-Generic-Face-Detector-1MB/imgs/1.jpg'), cv2.COLOR_BGR2RGB)

try:
    predicted = client.predict({'default': encoded_image.astype('int16')})
    print(predicted)
finally:
    server.stop()
