from konduit import ApiClient, InferenceApi
from konduit.rest import ApiException
from konduit import Configuration
import base64
import os
import re


class KonduitServingClient:

    def __init__(self, id=None, host='0.0.0.0', port=9009):
        self.id = id
        self.host = host
        self.port = port

        self.configuration = Configuration()
        if self.id is None:
            self.configuration.host = 'http://{}:{}'.format(self.host, self.port)
        else:
            self.configuration.host = self.get_host_and_port_from_id()

        self.api_client = ApiClient(self.configuration)
        self.api_instance = InferenceApi(api_client=self.api_client)

    def get_host_and_port_from_id(self):
        logs_path = os.path.join(os.path.expanduser("~"), '.konduit-serving', 'command_logs', '{}.log'.format(self.id))

        re_host_pattern = re.compile("server is listening on host: '(.+)'")
        re_port_pattern = re.compile("server started on port (\\d+) with (\\d+) pipeline steps")

        for i, line in enumerate(open(logs_path)):
            for match in re.finditer(re_host_pattern, line):
                self.host = match.group(1)

            for match in re.finditer(re_port_pattern, line):
                self.port = match.group(1)

        return 'http://{}:{}'.format(self.host, self.port)

    def predict(self, body):
        try:
            api_response = self.api_instance.predict(body, _preload_content=False)
            print(api_response.data.decode())
            return api_response
        except ApiException as e:
            print("Exception when calling InferenceApi->predict: %s\n" % e)
            return {'error': e}

    def get_image(self, image_path):
        with open(image_path, 'rb') as image_file:
            return {
                '@ImageData': base64.b64encode(image_file.read()).decode('utf-8'),
                '@ImageFormat': "PNG"
            }

