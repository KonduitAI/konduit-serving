from jnius import autoclass
from konduit.base_inference import *

try:
    SchemaTypeUtils = autoclass("ai.konduit.serving.util.SchemaTypeUtils")
except Exception as e:
    raise Exception(
        "We couldn't initialize the Java classes used by Konduit. "
        "Make sure to have a konduit.jar available on your PATH, "
        "for instance by setting the environment variable KONDUIT_JAR_PATH "
        "to point to such a JAR.\n",
        e,
    )


def set_input_columns_func(self, column_names, input_name="default"):
    input_columns = self._get_input_column_names()
    if input_columns is None:
        input_columns = {}
    input_columns[input_name] = column_names
    self._set_input_column_names(input_columns)


def set_output_columns_func(self, column_names, output_name="default"):
    output_columns = self._get_output_column_names()
    if output_columns is None:
        output_columns = {}
    output_columns[output_name] = column_names
    self._set_output_column_names(output_columns)


def set_input_types_func(self, types, input_name="default"):
    schemas = self._get_input_schemas()
    if schemas is None:
        schemas = {}
    schemas[input_name] = types
    self._set_input_schemas(schemas)


def set_output_types_func(self, types, output_name="default"):
    schemas = self._get_output_schemas()
    if schemas is None:
        schemas = {}
    schemas[output_name] = types
    self._set_output_schemas(schemas)


def set_input_func(
    self, schema=None, column_names=None, types=None, input_name="default"
):
    names = self._get_input_names()
    if names is None:
        names = []
    if input_name not in names:
        if schema is not None:
            column_names = SchemaTypeUtils.columnNames(schema)
            types = SchemaTypeUtils.typesForSchema(schema)
        if column_names is None or types is None:
            raise Exception(
                "Please provide either an input schema, or column names and types"
            )
        names.append(input_name)
        self._set_input_names(names)

        self.set_input_columns(input_name=input_name, column_names=column_names)
        self.set_input_types(input_name=input_name, types=types)

        return self
    else:
        raise Exception(
            "Input name"
            + input_name
            + " is already configured for this PipelineStep, "
            + "please choose another name for your next step."
        )


def set_output_func(
    self, schema=None, column_names=None, types=None, output_name="default"
):
    names = self._get_output_names()
    if names is None:
        names = []
    if output_name not in names:
        if schema is not None:
            column_names = SchemaTypeUtils.columnNames(schema)
            types = SchemaTypeUtils.typesForSchema(schema)
        if column_names is None or types is None:
            raise Exception(
                "Please provide either an output schema, or column names and types"
            )
        names.append(output_name)
        self._set_output_names(names)

        self.set_output_columns(output_name=output_name, column_names=column_names)
        self.set_output_types(output_name=output_name, types=types)

        return self
    else:
        raise Exception(
            "Input name"
            + output_name
            + " is already configured for this PipelineStep, "
            + "please choose another name for your next step."
        )


PipelineStep.set_input_columns = set_input_columns_func
PipelineStep.set_output_columns = set_output_columns_func
PipelineStep.set_input_types = set_input_types_func
PipelineStep.set_output_types = set_output_types_func
PipelineStep.set_input = set_input_func
PipelineStep.set_output = set_output_func


def step_func(
    self, input_schema, output_schema, transform_process, input_name="default"
):
    self.set_input(input_name=input_name, schema=input_schema)
    self.set_output(output_name=input_name, schema=output_schema)
    self.transform_process(input_name=input_name, transform_process=transform_process)

    return self


def transform_process_func(self, transform_process, input_name="default"):
    tps = self._get_transform_processes()
    if tps is None:
        tps = {}
    tps[input_name] = transform_process
    self._set_transform_processes(tps)

    return self


TransformProcessStep.step = step_func
TransformProcessStep.transform_process = transform_process_func


def python_step_func(
    self,
    python_config,
    input_name="default",
    input_schema=None,
    input_column_names=None,
    input_types=None,
    output_schema=None,
    output_column_names=None,
    output_types=None,
):
    if not input_name: 
        raise TypeError("input_name must not be None or empty string")

    # if nothing else is defined, we can derive all properties just from the Python configuration
    if (
        input_schema is None
        and input_column_names is None
        and input_types is None
        and output_schema is None
        and output_column_names is None
        and output_types is None
    ):
        inputs = python_config._get_python_inputs()
        outputs = python_config._get_python_outputs()
        if isinstance(inputs, list):
            input_column_names = inputs
            input_types = ["NDArray" for i in inputs]
        else:
            input_column_names = list(inputs.keys())
            input_types = [konduit_type_mapping(v) for v in inputs.values()]
        if isinstance(outputs, list):
            output_column_names = outputs
            output_types = ["NDArray" for i in outputs]
        else:
            output_column_names = list(outputs.keys())
            output_types = [konduit_type_mapping(v) for v in outputs.values()]

    self.set_input(
        schema=input_schema,
        column_names=input_column_names,
        types=input_types,
        input_name=input_name,
    )
    self.set_output(
        schema=output_schema,
        column_names=output_column_names,
        types=output_types,
        output_name=input_name,
    )
    self.python_config(python_config=python_config, input_name=input_name)

    return self


def python_config_func(self, python_config, input_name="default"):
    python_configs = self._get_python_configs()
    if python_configs is None:
        python_configs = {}
    python_configs[input_name] = python_config
    self._set_python_configs(python_configs)

    return self


def konduit_type_mapping(name):
    type_map = {
        "BOOL": "Boolean",
        "STR": "String",
        "INT": "Integer",
        "FLOAT": "Float",
        "NDARRAY": "NDArray",
    }
    if name not in type_map.keys():
        raise Exception("Can't convert input type " + str(name))
    else:
        return type_map.get(name)


PythonStep.step = python_step_func
PythonStep.python_config = python_config_func
