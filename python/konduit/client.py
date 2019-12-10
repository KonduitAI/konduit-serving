import io
import json
import logging
import numpy as np
import re
import requests
from konduit.utils import validate_server
from pyarrow.ipc import RecordBatchFileReader
from requests_toolbelt.multipart import decoder, encoder


class Client(object):
    def __init__(
        self,
        port,
        host="http://localhost",
        output_data_format=None,
        input_data_format=None,
        prediction_type=None,
        timeout=60,
        input_names=None,
        output_names=None,
    ):
        """Konduit Client

        This client is used to connect to a Konduit Server instance. You usually create a Client instance
        just by specifying host and port. If you want to convert the result of your endpoint to another data
        format on return, use `convert_to_format` to do so.

        :param port: The port on which the server is listening to. e.g. '1337' or 42, i.e. accepts int and str.
        :param host: The server host, defaults to 'http://localhost'.
        :param output_data_format: The output format returned by the `predict` method of this client. If not
               specified, we assume the output format is the same as the input format. This output format can
               be 'NUMPY', 'JSON', 'ARROW' and 'RAW'. 'IMAGE' will be implemented at a later stage.
        :param input_data_format: The format in which the input data is accepted by endpoints. Defaults to 'NUMPY',
               but can be 'JSON', 'ND4J', 'IMAGE' and 'ARROW' as well.
        :param prediction_type: The prediction type of the Konduit server. If not specified, this format
               will default to 'RAW'.
        :param timeout: Request time-out in seconds.
        :param input_names: The names of all inputs of the Konduit pipeline deployed for the Server corresponding to
               this client.
        :param output_names: The names of all inputs of the Konduit pipeline deployed for the Server corresponding to
               this client.
        """

        url = "{}:{}".format(host, port)

        server_running = validate_server(url)
        insufficient_data = input_data_format is None and output_data_format is None
        if server_running:
            try:
                response = requests.get("{}/config".format(url))
                config = response.json()
                logging.info("Retrieved config is".format(json.dumps(config)))
                steps = config["steps"]
                config = config["servingConfig"]
                input_names = []
                for step in steps:
                    input_names += step["inputNames"]
                if output_names is None:
                    output_names = []
                    for step in steps:
                        output_names += step["outputNames"]
                if input_data_format is None:
                    input_data_format = config["inputDataFormat"]
                if output_data_format is None:
                    output_data_format = config["outputDataFormat"]
                if prediction_type is None:
                    prediction_type = config["predictionType"]
            except Exception as ex:
                logging.error(
                    "{}\nUnable to get configuration from the server. Please verify that the server is "
                    "running without any issues...".format(str(ex))
                )
                raise RuntimeError(ex)
        elif not server_running and insufficient_data:
            raise RuntimeError(
                "Unable to connect to the server at {}, not enough data provided to initialize "
                "the Client".format(url)
            )
        else:
            logging.info(
                "No server-side validation needed, enough information available to create Client."
            )

        if input_names is None:
            input_names = ["default"]
        assert isinstance(input_names, list), "Input names should be a list!"
        assert len(input_names) > 0, "Input names must not be empty!"

        if output_names is None:
            output_names = ["default"]
        assert isinstance(output_names, list), "Output names should be a list!"
        assert len(output_names) > 0, "Output names must not be empty!"

        if output_data_format:
            self.convert_to_format = output_data_format
        else:
            self.convert_to_format = input_data_format

        if prediction_type is None:
            prediction_type = "RAW"

        self.timeout = timeout
        self.input_format = input_data_format
        self.prediction_type = prediction_type
        self.input_names = input_names
        self.output_names = output_names
        self.url = url

    def predict(self, data_input=None):
        if isinstance(data_input, np.ndarray):
            data_input = {"default": data_input}
        if data_input is None:
            data_input = {}
        if self.input_format.upper() == "JSON":
            resp = requests.post(
                "{}/{}/{}".format(
                    self.url, self.prediction_type.lower(), self.input_format.lower()
                ),
                json=data_input,
                timeout=self.timeout,
            )

        else:
            self._validate_multi_part(data_input=data_input)
            converted_input = self._convert_multi_part_inputs(data_input=data_input)
            resp = requests.post(
                "{}/{}/{}".format(
                    self.url, self.prediction_type.lower(), self.input_format.lower()
                ),
                files=converted_input,
                timeout=self.timeout,
            )
        if "content-type" not in resp.headers.keys():
            resp.headers["content-type"] = None
        return self._parse_response(resp.content, resp.headers["content-type"])

    def _parse_response(self, content, content_type):
        try:
            if self.output_names is None or len(self.output_names) < 2:
                bytes_content = io.BytesIO(content)
                bytes_content.seek(0)
                if self.convert_to_format.upper() == "NUMPY":
                    return np.load(bytes_content)
                elif self.convert_to_format.upper() == "ARROW":
                    reader = RecordBatchFileReader(bytes_content)
                    return reader.read_pandas()
                elif self.convert_to_format.upper() == "JSON":
                    return json.load(bytes_content)
                elif self.convert_to_format.upper() == "IMAGE":
                    raise NotImplementedError("Image not implemented yet.")
                elif self.convert_to_format.upper() == "RAW":
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
            content=content, content_type=content_type
        )
        ret = {}
        for part in multipart_data.parts:
            # typically something like: b'form-data; name="input1"'
            name_with_form_data = str(part.headers[b"Content-Disposition"])
            name_str = re.sub(
                r'([";\\\']|name=|form-data|b\\)', "", name_with_form_data
            ).replace("b ", "")
            if self.prediction_type.upper() == "NUMPY":
                ret[name_str] = Client._convert_binary_to_numpy(part.content)
            elif self.prediction_type.upper() == "ARROW":
                reader = RecordBatchFileReader(part.content)
                ret[name_str] = reader.read_pandas()
            elif self.prediction_type.upper() == "JSON":
                return json.load(part.content)
            elif self.prediction_type.upper() == "ND4J":
                raise NotImplementedError("Nd4j not implemented yet.")
            else:
                ret[name_str] = part.content
        return ret

    def _validate_multi_part(self, data_input):
        if self.input_format.capitalize() == "JSON":
            raise ValueError(
                "Attempting to execute multi part request with input type specified as JSON."
            )

        for key, value in data_input.items():
            root_name = re.sub("\\[[0-9]+\\]", "", key)
            if root_name not in self.input_names:
                raise ValueError(
                    "Specified root name " + root_name + " not found in input names."
                )
