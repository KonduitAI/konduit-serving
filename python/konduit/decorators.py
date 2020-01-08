import cv2
import os
import functools

from .server import Server
from .inference import PythonConfig
from .utils import default_python_path

def serving_config(http_port, input_data_format, output_data_format, log_timings = False):
    def serving_config_outer(cls):
        cls._konduit_config_class = True
        cls._konduit_http_port = http_port
        cls._konduit_input_data_format = input_data_format
        cls._konduit_output_data_format = output_data_format
        cls._konduit_log_timings = log_timings

        @functools.wraps(cls)
        def serving_config_wrapper(*args, **kwargs):
            return cls(*args, **kwargs)

    return serving_config_outer


def serving_function(**kwargs):
    def serving_function_outer(fn):
        fn._konduit_prediction_fn = True
        fn._konduit_inputs = fn.__annotations__
        fn._konduit_outputs = {}
        for key, value in kwargs:
            fn._konduit_outputs[key] = value

        @functools.wraps(fn)
        def serving_function_wrapper(*args, **kwargs):
            return fn(*args, **kwargs)

    return serving_function_outer

class KonduitConfigurationError(RuntimeException):
    pass

class KonduitServer:
    def __init__(self):
        pass

    @staticmethod
    def serve(config_class):
        # Set the working directory to this folder and register
        # the "detect_image.py" script as code to be executed by konduit.

        if not hasattr(config_class, '_konduit_config_class'):
            raise KonduitConfigurationError(
                'Configuration missing.  A serving_config decorator is required')
        methods = [getattr(config_class, fn)
                   for fn in dir(config_class)
                   if callable(getattr(config_class, fn))]
        run_methods = [fn
                       for fn in methods
                       if hasattr(fn, "_konduit_prediction_fn")]
        if len(run_methods) == 0:
            raise KonduitConfigurationError(
                'a method with serving_function decorator is required')

        # XXX: Supporting multiple run methods could be a thing we could support.
        run_method = run_methods[0]

        http_port = config_class._konduit_http_port
        input_data_format = config_class._konduit_input_data_format
        output_data_format = config_class._konduit_output_data_format
        log_timings = config_class._konduit_log_timings

        work_dir = os.path.abspath(".")
        python_config = PythonConfig(
            python_path=default_python_path(work_dir),
            python_code_path=os.path.realpath(__file__),
            python_inputs=run_method._konduit_inputs,
            python_outputs=run_method._konduit_outputs,
        )

        python_pipeline_step = PythonStep().step(
            python_config,
            input_name="default"
        )
        serving_config = ServingConfig(
            http_port=http_port,
            input_data_format=input_data_format,
            output_data_format=output_data_format,
            log_timings=log_timings
        )

        # Start a konduit server and wait for it to start
        server = Server(
            serving_config=serving_config,
            steps=[python_pipeline_step]
        )
        server.start()