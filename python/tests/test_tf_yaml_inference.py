import konduit
from konduit.load import server_from_file, client_from_file
import numpy as np
import pytest


@pytest.mark.integration
def test_yaml_server_python_prediction():
    try:
        konduit_yaml_path = "yaml/konduit_tf_inference.yaml"
        img = np.load("../data/input_layer.npy")
        server = server_from_file(konduit_yaml_path, start_server=True)
        client = client_from_file(konduit_yaml_path)
        predicted = client.predict(data_input={"input_layer": img})
        result = dict(zip(np.arange(10), predicted[0].round(3)))
        # {0: 0.0, 1: 0.0, 2: 0.001, 3: 0.001, 4: 0.0, 5: 0.0, 6: 0.0, 7: 0.998, 8: 0.0, 9: 0.0}
        assert round(result.get(7) * 1000) == 998
        server.stop()
    finally:
        server.stop()
