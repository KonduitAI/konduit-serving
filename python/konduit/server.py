from konduit.inference import InferenceConfiguration
from konduit.json_utils import config_to_dict_with_type

import json
import subprocess
import os


class Server(object):
    def __init__(self, config=InferenceConfiguration(),
                 extra_start_args=None, config_path='config.json',
                 jar_path='konduit.jar'):
        """Konduit Server

        Start and stop a server from a given inference configuration.
        Extra arguments with JVM options can be passed.

        :param config: InferenceConfiguration instance
        :param extra_start_args: list of string arguments to start the process with
        :param config_path: path to write the config object to (as json)
        :param jar_path: path to the konduit uberjar
        """
        self.config = config
        self.config_path = config_path
        self.jar_path = jar_path
        self.process = None
        if extra_start_args is None:
            extra_start_args = []

        # Handle singular element case
        if type(extra_start_args) is not list:
            self.extra_start_args = [extra_start_args]
        else:
            self.extra_start_args = extra_start_args

    def start(self):
        """Start the server"""
        json_config = config_to_dict_with_type(self.config)
        with open(self.config_path, 'w') as f:
            abs_path = os.path.abspath(self.config_path)
            print('Wrote config.json to path ' + abs_path)
            json.dump(json_config, f)

        args = self._process_extra_args(abs_path)
        process = subprocess.Popen(args=args)
        self.process = process
        return process

    def stop(self):
        """Stop the server"""
        if self.process is None:
            if os.path.exists(self.config_path):
                os.remove(self.config_path)
            raise Exception('Server is not started!')
        else:
            if os.path.exists(self.config_path):
                os.remove(self.config_path)
            self.process.kill()

    def _process_extra_args(self, absolute_path):
        """Process submitted extra arguments list.

        :param absolute_path: absolute path of the configuration file
        :return: concatenated string arguments
        """
        args = ['java']
        # Pass extra jvm arguments such as memory.
        if self.extra_start_args:
            args.extend(self.extra_start_args)
        args.append('-cp')
        args.append(self.jar_path)
        args.append('ai.konduit.serving.configprovider.KonduitServingMain')
        args.append('--configPath')
        args.append(absolute_path)
        args.append('--verticleClassName')
        args.append('ai.konduit.serving.verticles.inference.InferenceVerticle')
        print('Running with args\n' + ' '.join(args))
        return args
