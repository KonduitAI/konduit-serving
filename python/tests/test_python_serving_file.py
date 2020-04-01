import numpy as np
import os
import pytest
from konduit import *
from konduit.client import Client
from konduit.server import Server
from konduit.utils import is_port_in_use, default_python_path


@pytest.mark.integration
def test_python_script_prediction():
    work_dir = os.path.abspath(".")

    python_config = PythonConfig(
        python_path=default_python_path(work_dir),
        python_code_path=os.path.join(work_dir, "simple.py"),
        python_inputs={"first": "NDARRAY"},
        python_outputs={"second": "NDARRAY"},
    )

    step = PythonStep().step(python_config)
    server = Server(steps=step, serving_config=ServingConfig())
    _, port, started = server.start()

    assert started
    assert is_port_in_use(port)

    client = Client(port=port)

    try:
        input_array = np.load("../data/input-0.npy")
        predicted = client.predict(input_array)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()
