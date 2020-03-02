import json
import logging
import os
import re
import signal
import subprocess

import requests
from konduit import KONDUIT_DIR
from konduit.base_inference import PipelineStep
from konduit.client import Client
from konduit.inference import InferenceConfiguration
from konduit.json_utils import config_to_dict_with_type


def stop_server_by_pid(pid):
    """Stop a Konduit server with a given process ID

    :param pid: process ID of a running Konduit Server
    """
    if pid:
        os.kill(pid, signal.SIGTERM)


class Server(object):
    def __init__(
        self,
        inference_config=None,
        serving_config=None,
        steps=None,
        extra_start_args="-Xmx8g",
        extra_jar_args=None,
        config_path="config.json",
        jar_path=None,
        pid_file_path="konduit-serving.pid"
    ):
        """Konduit Server

        Start and stop a server from a given inference configuration. Alternatively,
        you can provide a serving configuration and a list of steps, instead of an
        inference config.
        Extra arguments with JVM options can be passed.

        Example:

        >>> import konduit
        >>> server = konduit.Server()

        :param inference_config: InferenceConfiguration instance
        :param serving_config: ServingConfig instance
        :param steps: list (or single) PipelineSteps
        :param extra_start_args: list of string arguments to start the process with
        :param extra_jar_args: extra jar files to add to the classpath
        :param config_path: path to write the config object to (as json)
        :param jar_path: path to the konduit uberjar
        """
        self.port = -1
        if jar_path is None:
            jar_path = os.getenv(
                "KONDUIT_JAR_PATH", os.path.join(KONDUIT_DIR, "konduit.jar")
            )

        if inference_config:
            self.config = inference_config
        elif serving_config and steps:
            if isinstance(steps, PipelineStep):
                steps = [steps]
            self.config = InferenceConfiguration(
                steps=steps, serving_config=serving_config
            )
        else:
            self.config = InferenceConfiguration()
        self.config_path = config_path
        self.jar_path = jar_path
        self.pid_file_path = pid_file_path
        self.process = None
        if extra_start_args is None:
            extra_start_args = []

        if extra_jar_args is None:
            extra_jar_args = []

        # Handle singular element case
        if type(extra_start_args) is not list:
            self.extra_start_args = [extra_start_args]
        else:
            self.extra_start_args = extra_start_args

        if type(extra_jar_args) is not list:
            self.extra_jar_args = [extra_jar_args]
        else:
            self.extra_jar_args = extra_jar_args

    def get_client(self,
                   input_data_format=None,
                   prediction_type=None,
                   output_data_format=None):
        """Get a Konduit Client instance from this Server instance.
        :param output_data_format: optional, same as in Client signature
        :return: konduit.Client
        """
        serving_config = self.config._get_serving_config()
        steps = self.config._get_steps()
        input_names = []
        output_names = []
        for step in steps:
            input_names += step._get_input_names()
            output_names += step._get_output_names()

        port = self.port
        host = serving_config._get_listen_host()
        if not host.startswith("http://"):
            host = "http://" + host

        if not input_data_format:
            input_data_format = "NUMPY"

        if not prediction_type:
            prediction_type = "RAW"

        if not output_data_format:
            output_data_format = serving_config._get_output_data_format()

        return Client(
            host=host,
            port=port,
            input_data_format=input_data_format,
            output_data_format=output_data_format,
            prediction_type=prediction_type,
            input_names=input_names,
            output_names=output_names,
        )

    def start(self, kill_existing_server=True):
        """Start the Konduit server

        :param kill_existing_server: whether to kill any previously started server if it wasn't stop.
        """
        if kill_existing_server:
            if os.path.exists(self.pid_file_path):
                with open(self.pid_file_path, "rb") as pid_file:
                    pid = int(pid_file.readline().strip())
                    try:
                        stop_server_by_pid(pid)
                    except OSError:
                        logging.debug(
                            "Attempt to kill existing process by pid: '{}' failed. The process might not "
                            "exist. ".format(pid)
                        )

                os.remove(self.pid_file_path)

        json_config = config_to_dict_with_type(self.config)
        with open(self.config_path, "w") as f:
            abs_path = os.path.abspath(self.config_path)
            logging.info("Wrote config.json to path " + abs_path)
            json.dump(json_config, f)

        args = self._process_extra_args(abs_path)
        process = subprocess.Popen(args=args, stdout=subprocess.PIPE)
        self.process = process

        # Check if the server is up or not.
        request_timeout = 5

        started = False

        print("Starting server")

        port = -1
        while True:
            output = process.stdout.readline()
            if output == b'' and process.poll() is not None:
                break
            else:
                print(output.decode("utf-8"), end="")
                if b'Server started on port' in output:
                    m = re.search('port (.+?) with', str(output))
                    if m:
                        port = int(m.group(1))
                        self.port = port
                        break

        if port != -1:
            try:
                r = requests.get(
                    "http://localhost:{}/healthcheck".format(port),
                    timeout=request_timeout,
                )
                if r.status_code == 204:
                    started = True
            except Exception as ex:
                logging.debug("Server health checks failed...\n{}".format(str(ex)))

        process = process.poll()

        if started:
            print("\n\nServer has started successfully on port {}".format(port))
        else:
            print("\n\nThe server wasn't able to start.")
            self.stop()

        return process, port, started

    def stop(self):
        """Stop the server"""
        if self.process is None:
            if os.path.exists(self.config_path):
                os.remove(self.config_path)
            raise Exception("Server is not started!")
        else:
            if os.path.exists(self.config_path):
                os.remove(self.config_path)
            self.process.kill()

    def _process_extra_args(self, absolute_path):
        """Process submitted extra arguments list.

        :param absolute_path: absolute path of the configuration file
        :return: concatenated string arguments
        """
        classpath = [self.jar_path]
        classpath.extend(self.extra_jar_args)

        args = ["java"]
        # Pass extra jvm arguments such as memory.
        if self.extra_start_args:
            args.extend(self.extra_start_args)
        args.append("-cp")
        args.append(os.pathsep.join(classpath))
        args.append("ai.konduit.serving.configprovider.KonduitServingMain")
        args.append("--pidFile")
        args.append(os.path.abspath(self.pid_file_path))
        args.append("--configPath")
        args.append(absolute_path)
        args.append("--verticleClassName")
        args.append("ai.konduit.serving.verticles.inference.InferenceVerticle")
        logging.info("Running with args\n" + " ".join(args))
        return args
