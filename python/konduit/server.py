from konduit.inference import InferenceConfiguration
from konduit.json_utils import json_with_type

import json
import subprocess
import os


class Server(object):

    def __init__(self,
                 config=InferenceConfiguration(),
                 extra_start_args=[],
                 config_path='config.json',
                 jar_path='konduit.jar'):
        self.config = config
        self.config_path = config_path
        self.jar_path = jar_path
        self.process = None
        # Handle singular element case
        if extra_start_args is not None and type(extra_start_args) is not list:
            self.extra_start_args = [extra_start_args]
        else:
            self.extra_start_args = extra_start_args

    def start(self):

        json_config = json_with_type(self.config)
        with open(self.config_path, 'w') as f:
            abs_path = os.path.abspath(self.config_path)
            print('Wrote config.json to path ' + abs_path)
            json.dump(json_config, f)

        args = ['java']
        # Pass extra jvm arguments such as memory.
        if self.extra_start_args is not None:
            args.extend(self.extra_start_args)
        args.append('-cp')
        args.append(self.jar_path)
        args.append('ai.konduit.serving.configprovider.KonduitServingMain')
        args.append('--configPath')
        args.append(abs_path)
        args.append('--verticleClassName')
        args.append('ai.konduit.serving.verticles.inference.InferenceVerticle')
        print('Running with args\n' + ' '.join(args))
        process = subprocess.Popen(args=args)
        self.process = process
        return process

    def stop(self):
        if self.process is None:
            if os.path.exists(self.config_path):
                os.remove(self.config_path)
            raise Exception('Server is not started!')
        else:
            if os.path.exists(self.config_path):
                os.remove(self.config_path)
            self.process.kill()
