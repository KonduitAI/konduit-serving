from konduit.base_inference import *
from jnius import autoclass

SchemaTypeUtils = autoclass("ai.konduit.serving.util.SchemaTypeUtils")


def set_input_columns_func(self, input_name, column_names):
    input_columns = self._get_input_column_names()
    if input_columns is None:
        input_columns = {}
    input_columns[input_name] = column_names
    self._set_input_column_names(input_columns)


def set_output_columns_func(self, output_name, column_names):
    output_columns = self._get_output_column_names()
    if output_columns is None:
        output_columns = {}
    output_columns[output_name] = column_names
    self._set_output_column_names(output_columns)


def set_input_types_func(self, input_name, types):
    schemas = self._get_input_schemas()
    if schemas is None:
        schemas = {}
    schemas[input_name] = types
    self._set_input_schemas(schemas)


def set_output_types_func(self, output_name, types):
    schemas = self._get_output_schemas()
    if schemas is None:
        schemas = {}
    schemas[output_name] = types
    self._set_output_schemas(schemas)


def set_input_func(self, input_name, schema=None, column_names=None, types=None):
    names = self._get_input_names()
    if names is None:
        names = []
    if input_name not in names:
        if schema is not None:
            column_names = SchemaTypeUtils.columnNames(schema)
            types = SchemaTypeUtils.typesForSchema(schema)
        if column_names is None or types is None:
            raise Exception(
                "Please provide either an input schema, or column names and types")
        names.append(input_name)
        self._set_input_names(names)

        self.set_input_columns(input_name, column_names)
        self.set_input_types(input_name, types)

        return self
    else:
        raise Exception("Input name" + input_name + " is already configured for this PipelineStep, " +
                        "please choose another name for your next step.")


def set_output_func(self, output_name, schema=None, column_names=None, types=None):
    names = self._get_output_names()
    if names is None:
        names = []
    if output_name not in names:
        if schema is not None:
            column_names = SchemaTypeUtils.columnNames(schema)
            types = SchemaTypeUtils.typesForSchema(schema)
        if column_names is None or types is None:
            raise Exception(
                "Please provide either an output schema, or column names and types")
        names.append(output_name)
        self._set_output_names(names)

        self.set_output_columns(output_name, column_names)
        self.set_output_types(output_name, types)

        return self
    else:
        raise Exception("Input name" + output_name + " is already configured for this PipelineStep, " +
                        "please choose another name for your next step.")


PipelineStep.set_input_columns = set_input_columns_func
PipelineStep.set_output_columns = set_output_columns_func
PipelineStep.set_input_types = set_input_types_func
PipelineStep.set_output_types = set_output_types_func
PipelineStep.set_input = set_input_func
PipelineStep.set_output = set_output_func


def step_func(self, input_schema, output_schema, transform_process, step_name=None):
    if step_name is None:
        step_name = "default"
    self.set_input(step_name, input_schema)
    self.set_output(step_name, output_schema)
    self.transform_process(step_name, transform_process)

    return self


def transform_process_func(self, input_name, transform_process):
    tps = self._get_transform_processes()
    if tps is None:
        tps = {}
    tps[input_name] = transform_process
    self._set_transform_processes(tps)

    return self


TransformProcessPipelineStep.step = step_func
TransformProcessPipelineStep.transform_process = transform_process_func


def python_step_func(self, python_config, step_name=None, input_schema=None, input_column_names=None, input_types=None,
                     output_schema=None, output_column_names=None, output_types=None):
    if step_name is None:
        step_name = "default"
    self.set_input(step_name, input_schema, input_column_names, input_types)
    self.set_output(step_name, output_schema,
                    output_column_names, output_types)
    self.python_config(step_name, python_config)

    return self


def python_config_func(self, step_name, python_config):
    python_configs = self._get_python_configs()
    if python_configs is None:
        python_configs = {}
    python_configs[step_name] = python_config
    self._set_python_configs(python_configs)

    return self


PythonPipelineStep.step = python_step_func
PythonPipelineStep.python_config = python_config_func
