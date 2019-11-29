import requests
import re
import numpy as np
import io
import json
from pyarrow.ipc import RecordBatchFileReader
from requests_toolbelt.multipart import decoder, encoder
import logging
from konduit.utils import validate_server


class Client(object):
    def __init__(self, input_data_format='NUMPY', output_data_format=None,
                 return_output_data_format=None, input_names=None, output_names=None, timeout=60,
                 host="http://localhost", port=None):
        """Konduit Client

        This client is used to connect to a Konduit Server instance.

        :param input_data_format: The format in which the input data is accepted by endpoints. Defaults to 'NUMPY',
               but can be 'JSON', 'ND4J', 'IMAGE' and 'ARROW' as well.
        :param output_data_format: The output format returned from the Konduit server. If not specified, this format
               will default to the input format specified.
        :param return_output_data_format: The output format returned by the `predict` method of this client. If not
               specified, we assume the return output format is the same as the input format. This output format can
               be 'NUMPY', 'JSON', 'ARROW' and 'RAW'. 'ND4J' will be implemented at a later stage.
        :param input_names: The names of all inputs of the Konduit pipeline deployed for the Server corresponding to
               this client.
        :param output_names: The names of all inputs of the Konduit pipeline deployed for the Server corresponding to
               this client.
        :param timeout: Request time-out in seconds.
        :param host: The server host. e.g. 'http://localhost'.
        :param port: The port on which the server is listening to. e.g. '1337'.
        """

        if not host or not port:
            logging.warning("You initialized your Client instance without specifying a 'host' or 'port' argument. "
                            "The 'predict' method will fail to return valid results this way. Please "
                            "set the 'host' and 'port' to the full URL to connect against yout")

        url = "{}:{}".format(host, port)

        if input_names is None:
            if not validate_server(url):
                logging.error("Unable to connect to the server at {}".format(url))
                exit(-1)
            else:
                try:
                    response = requests.get("{}/config".format(url))
                    config = response.json()
                    logging.info("Retrieved config is".format(config))
                    steps = config['steps']
                    input_names = steps[0]['inputNames']
                    if output_names is None:
                        output_names = steps[-1]['outputNames']
                except Exception as ex:
                    logging.error("{}\nUnable to get configuration from the server. Please verify that the server is "
                                  "running without any issues...".format(str(ex)))
                    exit(-1)

        assert isinstance(input_names, list), 'Input names should be a list!'
        assert len(input_names) > 0, 'Input names must not be empty!'

        if output_names is None:
            output_names = ['default']
        assert isinstance(output_names, list), 'Output names should be a list!'
        assert len(output_names) > 0, 'Output names must not be empty!'

        # if not specified, we output the format we put in.
        if not output_data_format:
            output_data_format = input_data_format

        # the format returned to the client is identical to the input format, unless explicitly specified.
        if return_output_data_format:
            self.return_output_data_format = return_output_data_format
        else:
            self.return_output_data_format = input_data_format

        self.timeout = timeout
        self.input_format = input_data_format
        self.output_format = output_data_format
        self.input_names = input_names
        self.output_names = output_names
        self.url = url

    def predict(self, data_input=None):
        if isinstance(data_input, np.ndarray):
            # Note: this is only slightly dangerous, given the current state ;)
            data_input = {'default': data_input}
        if data_input is None:
            data_input = {}
        if self.input_format.upper() == 'JSON':
            resp = requests.post(self.url + '/' + self.output_format.lower() + '/' + self.input_format.lower(),
                                 json=data_input, timeout=self.timeout)

        else:
            self._validate_multi_part(data_input)
            data_input = self._convert_multi_part_inputs(data_input=data_input)
            resp = requests.post(self.url + '/' + self.output_format.lower() + '/' + self.input_format.lower(),
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
                if self.return_output_data_format.upper() == 'NUMPY':
                    return np.load(bytes_content)
                elif self.return_output_data_format.upper() == 'ARROW':
                    reader = RecordBatchFileReader(bytes_content)
                    return reader.read_pandas()
                elif self.return_output_data_format.upper() == 'JSON':
                    return json.load(bytes_content)
                elif self.return_output_data_format.upper() == 'ND4J':
                    raise NotImplementedError('Nd4j not implemented yet.')
                elif self.return_output_data_format.upper() == 'RAW':
                    return content
            else:
                return self._convert_multi_part_output(content, content_type)
        except Exception as e:
            logging.info(e)
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
    def _encode_multi_part_input(parts=None):
        if parts is None:
            parts = {}
        encoded_parts = [(k, v) for k, v in parts.items()]
        ret = encoder.MultipartEncoder(encoded_parts)
        return ret.to_string(), ret.content_type

    @staticmethod
    def _convert_multi_part_inputs(data_input=None):
        if data_input is None:
            data_input = {}
        ret = {}
        for key, value in data_input.items():
            if isinstance(value, np.ndarray):
                value = Client._convert_numpy_to_binary(value)
            ret[key] = value
        return ret

    def _convert_multi_part_output(self, content, content_type):
        multipart_data = decoder.MultipartDecoder(
            content=content, content_type=content_type)
        ret = {}
        for part in multipart_data.parts:
            # typically something like: b'form-data; name="input1"'
            name_with_form_data = str(part.headers[b'Content-Disposition'])
            name_str = re.sub(r'([";\\\']|name=|form-data|b\\)',
                              '', name_with_form_data).replace('b ', '')
            if self.output_format.upper() == 'NUMPY':
                ret[name_str] = Client._convert_binary_to_numpy(part.content)
            elif self.output_format.upper() == 'ARROW':
                reader = RecordBatchFileReader(part.content)
                ret[name_str] = reader.read_pandas()
            elif self.output_format.upper() == 'JSON':
                return json.load(part.content)
            elif self.output_format.upper() == 'ND4J':
                raise NotImplementedError('Nd4j not implemented yet.')
            else:
                ret[name_str] = part.content
        return ret

    def _validate_multi_part(self, data_input={}):

        if self.input_format.capitalize() == 'JSON':
            raise ValueError(
                'Attempting to execute multi part request with input type specified as json.')

        for key, value in data_input.items():
            root_name = re.sub('\\[[0-9]+\\]', '', key)
            if root_name not in self.input_names:
                raise ValueError('Specified root name ' +
                                 root_name + ' not found in input names.')
