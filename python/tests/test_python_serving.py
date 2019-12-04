from konduit import *
from konduit.server import Server
from konduit.utils import is_port_in_use

import numpy as np
import random
import pytest


@pytest.mark.integration
def test_server_start():
    port = random.randint(1000, 65535)
    serving_config = ServingConfig(http_port=port)

    python_config = PythonConfig(
        python_code="first += 2",
        python_inputs={"first": "NDARRAY"},
        python_outputs={"first": "NDARRAY"},
    )

    step = PythonStep().step(python_config)
    server = Server(steps=step, serving_config=serving_config)
    server.start()

    client = server.get_client()

    data_input = {"default": np.load("../data/input-0.npy")}

    assert is_port_in_use(port)

    try:
        predicted = client.predict(data_input)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()
