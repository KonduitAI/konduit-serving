from konduit import *
from konduit.server import Server
from konduit.client import client_from_server
from konduit.utils import default_python_path

import os
import cv2


work_dir = os.path.abspath('.')
python_config = PythonConfig(
    python_path=default_python_path(work_dir), python_code_path=os.path.join(work_dir, 'detect_image.py'),
    python_inputs={'image': 'NDARRAY'}, python_outputs={'num_boxes': 'NDARRAY'},
)

server = Server(serving_config=ServingConfig(http_port=1337), steps=PythonStep().step(python_config))
server.start(sleep=10)

client = client_from_server(server)

img_path = './Ultra-Light-Fast-Generic-Face-Detector-1MB/imgs/1.jpg'
img = cv2.cvtColor(cv2.imread(img_path), cv2.COLOR_BGR2RGB).astype('int16')

try:
    print(client.predict(img))
finally:
    server.stop()
