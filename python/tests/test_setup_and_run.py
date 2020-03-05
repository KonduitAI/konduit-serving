import numpy as np
import pytest
from konduit import *
from konduit.server import Server
from konduit.utils import is_port_in_use


@pytest.mark.integration
def test_setup_and_run_start():
    python_config = PythonConfig(
        python_code="def setup(): pass\ndef run(input): {'output': np.array(input + 2)}",
        python_inputs={"input": "NDARRAY"},
        python_outputs={"output": "NDARRAY"},
        setup_and_run=True,
    )

    step = PythonStep().step(python_config)
    server = Server(steps=step, serving_config=ServingConfig())
    _, port, started = server.start()

    assert started
    assert is_port_in_use(port)

    client = server.get_client()

    data_input = {"default": np.asarray([42.0, 1.0])}

    try:
        predicted = client.predict(data_input)
        print(predicted)
        server.stop()
    except Exception as e:
        print(e)
        server.stop()
