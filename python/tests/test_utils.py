import pytest
from konduit.utils import *


@pytest.mark.unit
def test_unix_replacement():
    file_path = "C:\\foo\\bar"
    unix_path = to_unix_path(file_path)
    assert unix_path == "C:/foo/bar"

    step_config = {"python_path": file_path, "bar": 42, "keep_this": file_path}
    unix_step_config = update_dict_with_unix_paths(step_config)
    assert unix_step_config["python_path"] == "C:/foo/bar"
    assert unix_step_config["bar"] == 42
    assert unix_step_config["keep_this"] == file_path
