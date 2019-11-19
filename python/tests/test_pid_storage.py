from konduit.load import server_from_file
from konduit.load import store_pid, pop_pid
from konduit.server import stop_server_by_pid


def test_pid_storage():
    file_path = 'yaml/konduit.yaml'
    store_pid(file_path, 123)
    pid = pop_pid(file_path)
    assert pid == 123


def test_pid_creation_removal():
    file_path = 'yaml/konduit.yaml'
    running_server = server_from_file(file_path, start_server=True)

    # store the pid of this server and forget the Python object
    pid = running_server.process.pid
    store_pid(file_path=file_path, pid=running_server.process.pid)
    del running_server

    # retrieve the pid internally and kill the process
    recov_pid = pop_pid(file_path=file_path)
    assert pid == recov_pid
    stop_server_by_pid(recov_pid)
