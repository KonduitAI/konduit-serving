import requests
import re
import numpy as np
import io
import json
from pyarrow.ipc import RecordBatchFileReader
from requests_toolbelt.multipart import decoder, encoder


class Client(object):
    def __init__(self,
                 timeout=60,
                 input_type='JSON',
                 endpoint_output_type='JSON',
                 return_output_type=None,
                 input_names=[],
                 output_names=[],
                 url=''):
        assert input_names is not None and len(input_names) > 0, 'Input names must not be empty!'
        assert isinstance(input_names, list), 'Input names should be a list!'
        assert output_names is not None and len(output_names) > 0, 'Output names must not be empty!'
        assert isinstance(output_names, list), 'Output names should be a list!'
        if return_output_type is None:
            self.return_output_type = input_type
        else:
            self.return_output_type = return_output_type
        self.timeout = timeout
        self.input_type = input_type
        self.output_type = endpoint_output_type
        self.input_names = input_names
        self.output_names = output_names
        self.url = url

    def predict(self, data_input={}):
        if self.input_type.upper() == 'JSON':
            resp = requests.post(self.url + '/' + self.output_type.lower() + '/' + self.input_type.lower(),
                                 json=data_input, timeout=self.timeout)

        else:
            self._validate_multi_part(data_input)
            data_input = self._convert_multi_part_inputs(data_input=data_input)
            resp = requests.post(self.url + '/' + self.output_type.lower() + '/' + self.input_type.lower(),
                                 files=data_input,
                                 timeout=self.timeout)
        if 'content-type' not in resp.headers.keys():
            resp.headers['content-type'] = None
        return self._parse_response(resp.content, resp.headers['content-type'])

    def _parse_response(self, content, content_type):
        try:
            if self.output_names is None or len(self.output_names) < 2:
                bytes_content = io.BytesIO(content)
                bytes_content.seek(0)
                if self.return_output_type.upper() == 'NUMPY':
                    return np.load(bytes_content)
                elif self.return_output_type.upper() == 'ARROW':
                    reader = RecordBatchFileReader(bytes_content)
                    return reader.read_pandas()
                elif self.return_output_type.upper() == 'JSON':
                    return json.load(bytes_content)
                elif self.return_output_type.upper() == 'ND4J':
                    raise NotImplementedError('Nd4j not implemented yet.')
                elif self.return_output_type.upper() == 'RAW':
                    return content
            else:
                return self._convert_multi_part_output(content, content_type)
        except Exception as e:
            print(e)
            return None

    @staticmethod
    def _convert_numpy_to_binary(input_arr):
        bytes_io = io.BytesIO()
        np.save(bytes_io, input_arr)
        bytes_io.seek(0)
        return bytes_io

    @staticmethod
    def _convert_binary_to_numpy(input_arr_bytes):
        bytes_io = io.BytesIO(input_arr_bytes)
        ret = np.load(bytes_io)
        return ret

    @staticmethod
    def _encode_multi_part_input(parts=[]):
        if type(parts) is dict:
            new_parts = []
            for key, value in parts.items():
                new_parts.append((key, value))
            parts = new_parts

        encoded_parts = []
        for key, value in parts:
            add = (key, value if type(value) is tuple else value)
            encoded_parts.append(add)
        ret = encoder.MultipartEncoder(encoded_parts)
        return ret.to_string(), ret.content_type

    @staticmethod
    def _convert_multi_part_inputs(data_input={}):
        ret = {}
        for key, value in data_input.items():
            if isinstance(value, np.ndarray):
                bytes_io = Client._convert_numpy_to_binary(value)
                ret[key] = (key, bytes_io)
            else:
                ret[key] = value
        return ret

    def _convert_multi_part_output(self, content, content_type):
        multipart_data = decoder.MultipartDecoder(content=content
                                                  , content_type=content_type)
        ret = {}
        for part in multipart_data.parts:
            # typically something like: b'form-data; name="input1"'
            name_with_form_data = str(part.headers[b'Content-Disposition'])
            name_str = re.sub(r'([";\\\']|name=|form-data|b\\)', '', name_with_form_data).replace('b ', '')
            if self.output_type.upper() == 'NUMPY':
                ret[name_str] = Client._convert_binary_to_numpy(part.content)
            elif self.output_type.upper() == 'ARROW':
                reader = RecordBatchFileReader(part.content)
                ret[name_str] = reader.read_pandas()
            elif self.output_type.upper() == 'JSON':
                return json.load(part.content)
            elif self.output_type.upper() == 'ND4J':
                raise NotImplementedError('Nd4j not implemented yet.')
            else:
                ret[name_str] = part.content
        return ret

    def _validate_multi_part(self, data_input={}):

        if self.input_type.capitalize() == 'JSON':
            raise ValueError('Attempting to execute multi part request with input type specified as json.')

        for key, value in data_input.items():
            root_name = re.sub('\\[[0-9]+\\]', '', key)
            if root_name not in self.input_names:
                raise ValueError('Specified root name ' + root_name + ' not found in input names.')
