from konduit import *
from konduit.client import Client
import pytest


def test_multipart_regex():

    client = Client(input_names=['partname'], output_names=['nobody_cares'])

    test_data = {
        'partname[0]': 'foo',
        "partname[1]": 'bar'
    }

    client._validate_multi_part(test_data)

    test_data['foo'] = 'baz'

    with pytest.raises(Exception):
        client._validate_multi_part(test_data)
