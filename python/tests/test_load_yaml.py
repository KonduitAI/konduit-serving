from konduit.load import create_server_from_file, create_client_from_file
import numpy as np


def test_yaml_server_loading():
    file_path = 'yaml/konduit.yaml'
    server = create_server_from_file(file_path, start_server=False)

    try:
        running_server = create_server_from_file(file_path, start_server=True)
    finally:
        running_server.stop()


def test_yaml_client_loading():
    file_path = 'yaml/konduit.yaml'
    client = create_client_from_file(file_path)


def test_yaml_minimal_loading():
    file_path = 'yaml/konduit_minimal.yaml'
    server = create_server_from_file(file_path)
    client = create_client_from_file(file_path)
    server.stop()


def test_keras_serving():
    file_path = 'yaml/konduit_keras.yaml'
    server = create_server_from_file(file_path=file_path)
    server.stop()


def test_tf_simple_serving():
    file_path = 'yaml/konduit_tf_simple.yaml'
    server = create_server_from_file(file_path=file_path)
    server.stop()


def test_dl4j_mln_serving():
    file_path = 'yaml/konduit_dl4j_mln.yaml'
    server = create_server_from_file(file_path=file_path)
    server.stop()


def test_dl4j_cg_serving():
    file_path = 'yaml/konduit_dl4j_cg.yaml'
    server = create_server_from_file(file_path=file_path)
    server.stop()


def test_dl4j_samediff_serving():
    file_path = 'yaml/konduit_samediff.yaml'
    server = create_server_from_file(file_path=file_path)
    server.stop()


def test_tensor_flow_serving():
    file_path = 'yaml/konduit_tensorflow.yaml'
    server = create_server_from_file(file_path=file_path)
    server.stop()


# def test_yaml_server_python_prediction():
#     try:
#         file_path = 'yaml/konduit_python_code.yaml'
#         server = create_server_from_file(file_path)
#         client = create_client_from_file(file_path)
#         client.predict(np.load('../data/input-0.npy'))
#     finally:
#         server.stop()
