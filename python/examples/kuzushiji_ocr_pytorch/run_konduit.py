from konduit import *
from konduit.server import Server
from konduit.client import Client
from konduit.utils import default_python_path
import cv2

import os
#from utils import to_base_64

img = cv2.cvtColor(cv2.imread('./test_b07a37e9.jpg', 1), cv2.COLOR_BGR2RGB)
print(img.shape)

# Set the working directory to this folder and register the "detect_image.py" script as code to be executed by konduit.
work_dir = os.path.abspath('.')
python_config = PythonConfig(
    python_path=default_python_path(work_dir), python_code_path=os.path.join(work_dir, 'inference.py'),
    python_inputs={'image': 'NDARRAY'}, python_outputs={'results': 'STR'},
)

# Configure a Python pipeline step for your Python code. Internally, konduit will take Strings as input and output
# for this example.
python_pipeline_step = PythonStep().step(python_config)
serving_config = ServingConfig(http_port=1337, input_data_type='NUMPY', output_data_type='JSON')

# Start a konduit server and wait for it to start
server = Server(serving_config=serving_config, steps=[python_pipeline_step])
server.start(sleep=10)

# Initialize a konduit client that takes in and outputs JSON
client = Client(input_type='NUMPY', return_output_type='JSON',
                endpoint_output_type='RAW',url='http://localhost:1337', timeout=120)

# encode the image from a file to base64 and get back a prediction from the konduit server
#encoded_image = to_base_64(os.path.abspath('./test_b07a37e9.jpg'))
predicted = client.predict(img.astype('int16'))  # since java does not have uint8 type
print(predicted)
# print(predicted.get('image') == encoded_image)

server.stop()

