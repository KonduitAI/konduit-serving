import base64
import sys
import cv2
import numpy as np


def default_python_path(work_dir):
    python_path = ':'.join(sys.path)
    python_path += ':' + work_dir
    return python_path


def to_base_64(file_name):
    with open(file_name, "rb") as f:
        img_base64 = base64.b64encode(f.read())
    return str(img_base64)[2:-1]


def base64_to_ndarray(img_base64):
    img_data = base64.b64decode(img_base64)
    img_np = np.fromstring(img_data, np.uint8)
    image = cv2.imdecode(img_np, cv2.COLOR_BGR2RGB)
    return image
