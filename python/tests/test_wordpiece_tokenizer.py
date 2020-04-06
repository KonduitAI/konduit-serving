import pytest
from konduit import *
from konduit.load import server_from_file, client_from_file
import numpy as np

@pytest.mark.integration
def test_wordpiece_tokenizer_serving_minimal():

    file_path = "yaml/konduit_wordpiece_tokenizer_minimal.yaml"
    server = server_from_file(file_path)
    try:
        running_server = server_from_file(file_path, start_server=True)
    finally:
        running_server.stop()


@pytest.mark.integration
def test_wordpiece_tokenizer_serving_two_steps():

    file_path = "yaml/konduit_wordpiece_tokenizer_two_steps.yaml"
    server = server_from_file(file_path)
    try:
        running_server = server_from_file(file_path, start_server=True)
    finally:
        running_server.stop()
