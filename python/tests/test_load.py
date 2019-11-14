from konduit.load import create_server_from_file, create_client_from_file


def test_yaml_server_loading():
    file_path = 'yaml/konduit.yaml'
    server = create_server_from_file(file_path, start_server=False)

    running_server = create_server_from_file(file_path, start_server=True)
    running_server.stop()


def test_yaml_client_loading():
    file_path = 'yaml/konduit.yaml'
    client = create_client_from_file(file_path)
