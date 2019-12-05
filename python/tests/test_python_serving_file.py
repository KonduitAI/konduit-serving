from konduit import *
from konduit.server import Server
from konduit.client import Client
from konduit.utils import is_port_in_use, default_python_path

import numpy as np
import os
import pytest


@pytest.mark.integration
def test_python_script_prediction():
    port = 1337
    serving_config = ServingConfig(http_port=port)
    work_dir = os.path.abspath(".")

    python_config = PythonConfig(
        python_path=default_python_path(work_dir),
        python_code_path=os.path.join(work_dir, "simple.py"),
        python_inputs={"first": "NDARRAY"},
        python_outputs={"second": "NDARRAY"},
    )

    step = PythonStep().step(python_config)
    server = Server(steps=step, serving_config=serving_config)
    server.start()

    client = Client(port=port)

    if is_port_in_use(port):
        input_array = np.load("../data/input-0.npy")
        predicted = client.predict(input_array)
        print(predicted)
        server.stop()
    else:
        server.stop()
        raise Exception("Server not running on specified port")
