from konduit import *
import pytest
import random


@pytest.mark.integration
def test_client_from_server():
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

    try:
        server.get_client()
    finally:
        server.stop()


@pytest.mark.unit
def test_multipart_regex():
    client = Client(
        port=1337,
        input_data_format="NUMPY",
        output_data_format="NUMPY",
        input_names=["partname"],
        output_names=["nobody_cares"],
    )

    test_data = {"partname[0]": "foo", "partname[1]": "bar"}

    client._validate_multi_part(test_data)

    test_data["foo"] = "baz"

    with pytest.raises(Exception):
        client._validate_multi_part(test_data)
