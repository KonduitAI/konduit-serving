import json
import logging
import os
import signal
import subprocess
import requests

from konduit.base_inference import PipelineStep
from konduit.inference import InferenceConfiguration
from konduit.json_utils import config_to_dict_with_type


def stop_server_by_pid(pid):
    """Stop a Konduit server with a given process ID

    :param pid: process ID of a running Konduit Server
    """
    if pid:
        os.kill(pid, signal.SIGTERM)


class Server(object):
    def __init__(self, inference_config=None, serving_config=None, steps=None,
                 extra_start_args='-Xmx8g', config_path='config.json',
                 jar_path='konduit.jar', pid_file_path='konduit-serving.pid',
                 start_timeout=120):
        """Konduit Server

        Start and stop a server from a given inference configuration.
        Extra arguments with JVM options can be passed.

        Example:

        >>> import konduit
        >>> server = konduit.Server()

        :param inference_config: InferenceConfiguration instance
        :param serving_config: ServingConfig instance
        :param steps: list (or single) PipelineSteps
        :param extra_start_args: list of string arguments to start the process with
        :param config_path: path to write the config object to (as json)
        :param jar_path: path to the konduit uberjar
        """

        if inference_config:
            self.config = inference_config
        elif serving_config and steps:
            if isinstance(steps, PipelineStep):
                steps = [steps]
            self.config = InferenceConfiguration(pipeline_steps=steps, serving_config=serving_config)
        else:
            self.config = InferenceConfiguration()
        self.config_path = config_path
        self.jar_path = jar_path
        self.pid_file_path = pid_file_path
        self.start_timeout = start_timeout
        self.process = None
        if extra_start_args is None:
            extra_start_args = []

        # Handle singular element case
        if type(extra_start_args) is not list:
            self.extra_start_args = [extra_start_args]
        else:
            self.extra_start_args = extra_start_args

    def start(self, kill_existing_server=True):
        """Start the Konduit server

        :param kill_existing_server: whether to kill any previously started server if it wasn't stop
        :param sleep: optional number of seconds to sleep after triggering server start.
        """
        if kill_existing_server:
            if os.path.exists(self.pid_file_path):
                with open(self.pid_file_path, 'rb') as pid_file:
                    pid = int(pid_file.readline().strip())
                    try:
                        stop_server_by_pid(pid)
                    except OSError:
                        logging.debug("Attempt to kill existing process by pid: '{}' failed. The process might not "
                                      "exist. ".format(pid))

                os.remove(self.pid_file_path)

        json_config = config_to_dict_with_type(self.config)
        with open(self.config_path, 'w') as f:
            abs_path = os.path.abspath(self.config_path)
            logging.info('Wrote config.json to path ' + abs_path)
            json.dump(json_config, f)

        args = self._process_extra_args(abs_path)
        process = subprocess.Popen(args=args)
        self.process = process

        # Check if the server is up or not.
        request_timeout = 5
        tries = int(self.start_timeout/request_timeout + 1)

        started = False

        for i in range(tries):
            # This url returns status 204 with no content when the server is up.
            try:
                print("Checking server integrity. Tries: {} of {}".format(i + 1, tries))

                r = requests.get("http://localhost:{}/healthcheck".format(self.config.serving_config.http_port),
                                 timeout=request_timeout)
                if r.status_code == 204:
                    started = True
                    break
            except Exception as ex:
                logging.debug("{}\nChecking server integrity again...".format(str(ex)))

        if started:
            print("Server has started successfully.")
        else:
            print("The server wasn't able to start.")
            self.stop()

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
        if self.jar_path:
            args.append('-cp')
            args.append(self.jar_path)
        args.append('ai.konduit.serving.configprovider.KonduitServingMain')
        args.append('--configPath')
        args.append(absolute_path)
        args.append('--verticleClassName')
        args.append('ai.konduit.serving.verticles.inference.InferenceVerticle')
        logging.info('Running with args\n' + ' '.join(args))
        return args
