package ai.konduit.serving.codegen.pythoncodegen;

public class PythonDocStrings {

    public static String generateDocs(String code) {

        code = code.replace(
                "class TensorDataTypesConfig(object):\n",
                "class TensorDataTypesConfig(object):\n"+
                        "    \"\"\"TensorDataTypesConfig\n" +
                        "    \n" +
                        "    Configures TensorDataTypes for inputs and outputs of a step.\n" +
                        "    \n" +
                        "    :param input_data_types: List of input konduit.TensorDataType\n" +
                        "    :param output_data_types: List of output konduit.TensorDataType\n" +
                        "    \"\"\""
        );


        code = code.replace(
                "class SavedModelConfig(object):\n",
                "class SavedModelConfig(object):\n" +
                        "    \"\"\"SavedModelConfig\n" +
                        "\n" +
                        "    SavedModel Configuration for TensorFlow models\n" +
                        "\n" +
                        "    :param saved_model_path: path to the saved model\n" +
                        "    :param model_tag: a tag to give the model, e.g. \"serve\"\n" +
                        "    :param signature_key: TensorFlow SignatureDef key, e.g. \"incr_counter_by\"\n" +
                        "    :param saved_model_input_order: list of input variables in order\n" +
                        "    :param save_model_output_order: list of output variables in order\n" +
                        "    \"\"\""
        );

        // TODO: write this properly
        code = code.replace(
                "class ParallelInferenceConfig(object):\n",
                "class ParallelInferenceConfig(object):\n" +
                        "    \"\"\"ParallelInferenceConfig\n" +
                        "\n" +
                        "    Configuration for parallel inference.\n" +
                        "\n" +
                        "    :param queue_limit:\n" +
                        "    :param queue_limit:\n" +
                        "    :param batch_limit:\n" +
                        "    :param workers:\n" +
                        "    :param max_train_epochs:\n" +
                        "    :param inference_mode:\n" +
                        "    :param vertx_config_json:\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ModelConfigType(object):\n",
                "class ModelConfigType(object):\n" +
                        "    \"\"\"ModelConfigType\n" +
                        "\n" +
                        "    This model configuration has meta data for a model loader, which\n" +
                        "    includes the model loading path and the model type. It's used in\n" +
                        "    konduit.ModelConfig.\n" +
                        "\n" +
                        "    :param model_type: Can be any of 'COMPUTATION_GRAPH', 'MULTI_LAYER_NETWORK',\n" +
                        "        'PMML', 'TENSORFLOW', 'KERAS', and 'SAMEDIFF'.\n" +
                        "    :param model_loading_path: path to the model file\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ModelConfig(object):\n",
                "class ModelConfig(object):\n" +
                        "    \"\"\"ModelConfig\n" +
                        "\n" +
                        "    Model configurations hold the TensorDataTypeConfig and the\n" +
                        "    .ModelConfigType of the model you want to serve.\n" +
                        "\n" +
                        "    :param tensor_data_types_config: konduit.TensorDataTypeConfig\n" +
                        "    :param model_config_type: konduit.ModelConfigType\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class TensorDataType(object):\n",
                "class TensorDataType(object):\n" +
                        "    \"\"\"TensorDataType\n" +
                        "\n" +
                        "    Possible data types for tensors. Comes with conversions from TensorFlow\n" +
                        "    and Python and between ND4J types. Choose from\n" +
                        "\n" +
                        "    INVALID, FLOAT, DOUBLE, INT32, UINT8, INT16, INT8, STRING, COMPLEX64,\n" +
                        "    INT64, BOOL, QINT8, QUINT8, QINT32, BFLOAT16, QINT16, QUINT16, UINT16,\n" +
                        "    COMPLEX128, HALF, RESOURCE, VARIANT, UINT32, UINT64\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class PmmlConfig(object):\n",
                "class PmmlConfig(object):\n" +
                        "    \"\"\"PmmlConfig\n" +
                        "\n" +
                        "    Configuration for models in PMML format\n" +
                        "\n" +
                        "    :param tensor_data_types_config: konduit.TensorDataTypesConfig\n" +
                        "    :param model_config_type: konduit.ModelConfigType\n" +
                        "    :param evaluator_factory_name: defaults to \"org.jpmml.evaluator.ModelEvaluatorFactory\". Custom extensions\n" +
                        "           have to be written in Java.\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ObjectDetectionConfig(object):\n",
                "class ObjectDetectionConfig(object):\n" +
                        "    \"\"\"ObjectDetectionConfig\n" +
                        "\n" +
                        "     Configuration for object detection output of models.\n" +
                        "\n" +
                        "    :param threshold: cut-off threshold for detected objects, defaults to 0.5\n" +
                        "    :param num_labels: the number of labels to predict with your model.\n" +
                        "    :param labels_path: Path to file containing the labels\n" +
                        "    :param priors: list of bounding box priors (list of list of floating point numbers)\n" +
                        "    :param input_shape: input shape of the data\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class SchemaType(object):\n",
                "class SchemaType(object):\n" +
                        "    \"\"\"SchemaType\n" +
                        "\n" +
                        "    Type of an input or output to a pipeline step. Can be any of:\n" +
                        "\n" +
                        "    'String', 'Integer', 'Long', 'Double', 'Float', 'Categorical', 'Time', 'Bytes',\n" +
                        "    'Boolean', 'NDArray', 'Image'.\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class Input(object):\n",
                "class Input(object):\n" +
                        "    \"\"\"Input\n" +
                        "\n" +
                        "    Used for specifying various kinds of configuration about inputs\n" +
                        "    for the server. Input.DataFormat defines in which data format\n" +
                        "    an input variable is expected to be specified. Can be any of\n" +
                        "\n" +
                        "    'NUMPY', 'JSON', 'ND4J', 'IMAGE', or 'ARROW'\n" +
                        "    \"\"\""
        );


        code = code.replace(
                "class Output(object):\n",
                "class Output(object):\n" +
                        "    \"\"\"Output\n" +
                        "\n" +
                        "    Used for specifying various kinds of configuration about outputs\n" +
                        "    for the server. Outnput.DataFormat defines in which data format\n" +
                        "    an input variable is expected to be specified. Can be any of\n" +
                        "\n" +
                        "    'NUMPY', 'JSON', 'ND4J', or 'ARROW'.\n" +
                        "\n" +
                        "    Additionally, Output.PredictionType defines the type of prediction\n" +
                        "    you want to specify for your pipeline. The prediction type determines\n" +
                        "    which \"output adapter\" is used to transform the output. Currently you\n" +
                        "    can choose from the following values:\n" +
                        "\n" +
                        "    'CLASSIFICATION', 'YOLO', 'SSD', 'RCNN', 'RAW', 'REGRESSION'\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class SameDiffConfig(object):\n",
                "class SameDiffConfig(object):\n" +
                        "    \"\"\"SameDiffConfig\n" +
                        "\n" +
                        "    Extension of ModelConfig to DL4J SameDiff models\n" +
                        "\n" +
                        "    :param tensor_data_types_config: konduit.TensorDataTypesConfig\n" +
                        "    :param model_config_type: konduit.ModelConfigType\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class TensorFlowConfig(object):\n",
                "class TensorFlowConfig(object):\n" +
                        "    \"\"\"TensorFlowConfig\n" +
                        "\n" +
                        "    TensorFlow extension of konduit.ModelConfig used to define model steps\n" +
                        "    with TensorFlow models.\n" +
                        "\n" +
                        "    :param tensor_data_types_config: konduit.TensorDataTypesConfig\n" +
                        "    :param model_config_type: konduit.ModelConfigType\n" +
                        "    :param config_proto_path: path to the TensorFlow ProtoBuf model file.\n" +
                        "    :param saved_model_config: konduit.SavedModelConfig\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class PythonConfig(object):\n",
                "class PythonConfig(object):\n" +
                        "    \"\"\"PythonConfig\n" +
                        "\n" +
                        "    Extension of konduit.ModelConfig for custom Python code. Provide your Python\n" +
                        "    code either as string to `python_code` or as path to a Python script to `python_code_path`.\n" +
                        "    Additionally, you can modify or extend your Python path by setting `python_path` accordingly.\n" +
                        "\n" +
                        "    :param python_code: Python code as str\n" +
                        "    :param python_code_path: full qualifying path to the Python script you want to run, as str\n" +
                        "    :param python_inputs: list of Python input variable names\n" +
                        "    :param python_outputs: list of Python output variable names\n" +
                        "    :param extra_inputs: potential extra input variables\n" +
                        "    :param python_path: your desired Python PATH as str\n" +
                        "    :param return_all_inputs: whether or not to return all inputs additionally to outputs\n" +
                        "    :param setup_and_run: whether or not to use the setup-and-run schematics, defaults to False.\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ServingConfig(object):\n",
                "class ServingConfig(object):\n" +
                        "    \"\"\"ServingConfig\n" +
                        "\n" +
                        "    A serving configuration collects all properties needed to serve your\n" +
                        "    model pipeline within a konduit.InferenceConfig.\n" +
                        "\n" +
                        "    :param http_port: HTTP port of the konduit.Server\n" +
                        "    :param listen_host: host of the konduit.Server, defaults to 'localhost'\n" +
                        "    :param output_data_format: Output data format, see konduit.Output for more information\n" +
                        "    :param uploads_directory: to which directory to store file uploads to, defaults to 'file-uploads/'\n" +
                        "    :param log_timings: whether to log timings for this config, defaults to False\n" +
                        "    :param metric_types: the types of metrics logged for your ServingConfig can currently only be configured and\n" +
                        "           extended from Java. don't modify this property.\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class PipelineStep(object):\n",
                "class PipelineStep(object):\n" +
                        "    \"\"\"PipelineStep\n" +
                        "\n" +
                        "    PipelineStep collects all ETL and model related properties (input schema,\n" +
                        "    normalization and transform steps, output schema, potential pre-\n" +
                        "    or post-processing etc.). This config is passed to the respective\n" +
                        "    verticle along with konduit.ServingConfig.\n" +
                        "\n" +
                        "    :param input_schemas: dictionary of konduit.SchemaType for input names\n" +
                        "    :param output_schemas: dictionary of konduit.SchemaType for output names\n" +
                        "    :param input_names: list on step input names\n" +
                        "    :param output_names: list of step output names\n" +
                        "    :param input_column_names: dictionary mapping input names to lists of names of your columnar data (e.g.\n" +
                        "           { \"input_1\": [\"col1\", \"col2\"]}\n" +
                        "    :param output_column_names: dictionary mapping output names to lists of names of your columnar data (e.g.\n" +
                        "           { \"output_1\": [\"col1\", \"col2\"]}\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class NormalizationConfig(object):\n",
                "class NormalizationConfig(object):\n" +
                        "    \"\"\"NormalizationConfig\n" +
                        "\n" +
                        "    Configuration for data normalization in the ETL part of your pipeline.\n" +
                        "\n" +
                        "    :param config: dictionary of str values defining you normalization step.\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class PythonStep(PipelineStep):\n",
                "class PythonStep(PipelineStep):\n" +
                        "    \"\"\"PythonStep\n" +
                        "\n" +
                        "    PythonStep defines a custom Python konduit.PipelineStep from a konduit.PythonConfig.\n" +
                        "\n" +
                        "    :param input_schemas: Input konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param output_schemas: Output konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param input_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param output_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param input_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param output_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param python_configs: konduit.PythonConfig\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class TransformProcessStep(PipelineStep):\n",
                "class TransformProcessStep(PipelineStep):\n" +
                        "    \"\"\"TransformProcessStep\n" +
                        "\n" +
                        "    TransformProcessStep defines a konduit.PipelineStep from a DataVec TransformProcess\n" +
                        "\n" +
                        "    :param input_schemas: Input konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param output_schemas: Output konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param input_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param output_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param input_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param output_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param transform_processes: DataVec TransformProcess\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ModelStep(PipelineStep):\n",
                "class ModelStep(PipelineStep):\n" +
                        "    \"\"\"ModelStep\n" +
                        "\n" +
                        "    ModelStep extends konduit.PipelineStep and is the base class for all pipeline steps\n" +
                        "    involving machine learning models.\n" +
                        "\n" +
                        "    :param input_schemas: Input konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param output_schemas: Output konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param input_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param output_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param input_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param output_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param model_config: konduit.ModelConfig\n" +
                        "    :param parallel_inference_config: konduit.ParallelInferenceConfig\n" +
                        "    :param normalization_config: konduit.NormalizationConfig\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ArrayConcatenationStep(PipelineStep):\n",
                "class ArrayConcatenationStep(PipelineStep):\n" +
                        "    \"\"\"ArrayConcatenationStep\n" +
                        "\n" +
                        "    konduit.PipelineStep that concatenates two or more arrays along the specified dimensions.\n" +
                        "\n" +
                        "    :param input_schemas: Input konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param output_schemas: Output konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param input_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param output_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param input_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param output_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param concat_dimensions: dictionary of array indices to concatenation dimension\n" +
                        "    \"\"\"\n"
        );

        code = code.replace(
                "class JsonExpanderTransformStep(PipelineStep):\n",
                "class JsonExpanderTransformStep(PipelineStep):\n" +
                        "    \"\"\"JsonExpanderTransformStep\n" +
                        "\n" +
                        "    Executes expansion of JSON objects in to \"real\" objects.\n" +
                        "    This is needed when integrating with PipelineStepRunner\n" +
                        "    that may output {@link Text} with json arrays or json objects.\n" +
                        "    This kind of output is generally expected from Python or PMML based pipelines\n" +
                        "    which have a lot more complicated output and schema based values\n" +
                        "    rather than straight NDArrays like\n" +
                        "    most deep learning pipelines will be.\n" +
                        "\n" +
                        "    :param input_schemas: Input konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param output_schemas: Output konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param input_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param output_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param input_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param output_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "        \"\"\""
        );

        code = code.replace(
                "class ImageLoadingStep(PipelineStep):\n",
                "class ImageLoadingStep(PipelineStep):\n" +
                        "    \"\"\"ImageLoadingStep\n" +
                        "\n" +
                        "    Loads an input image into an NDArray.\n" +
                        "\n" +
                        "    :param input_schemas: Input konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param output_schemas: Output konduit.SchemaTypes, see konduit.PipelineStep.\n" +
                        "    :param input_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param output_names: list of step input names, see konduit.PipelineStep.\n" +
                        "    :param input_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param output_column_names: Input name to column name mapping, see konduit.PipelineStep.\n" +
                        "    :param original_image_height: input image height in pixels\n" +
                        "    :param original_image_width: input image width in pixels\n" +
                        "    :param update_ordering_before_transform: boolean, defaults to False\n" +
                        "    :param dimensions_configs: dictionary defining input shapes per input name, e.g. {\"input\", [28,28,3]}\n" +
                        "    :param image_processing_required_layout: desired channel ordering after this pipeline step has been applied,\n" +
                        "           either \"NCHW\" or \"NHWC\", defaults to the prior\n" +
                        "    :param image_processing_initial_layout: channel ordering before processing, either\n" +
                        "           \"NCHW\" or \"NHWC\", defaults to the prior\n" +
                        "    :param image_transform_processes: a DataVec ImageTransformProcess\n" +
                        "    :param object_detection_config: konduit.ObjectDetectionConfig\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class MemMapConfig(object):\n",
                "class MemMapConfig(object):\n" +
                        "    \"\"\"MemMapConfig\n" +
                        "\n" +
                        "    Configuration for managing serving of memory-mapped files. The goal is to mem-map\n" +
                        "    and serve a large array stored in \"array_path\" and get slices of this array on demand\n" +
                        "    by index. If an index is specified that does not match an index of the mem-mapped array,\n" +
                        "    an default or \"unknown\" vector is inserted into the slice instead, which is stored in\n" +
                        "    \"unk_vector_path\".\n" +
                        "\n" +
                        "    For instance, let's say we want to mem-map [[1, 2, 3], [4, 5, 6]], a small array with two\n" +
                        "    valid slices. Our unknown vector is simply [0, 0, 0] in this example. Now, if we query for\n" +
                        "    the indices {-2, 1} we'd get [[0, 0, 0], [4, 5, 6]].\n" +
                        "\n" +
                        "    :param array_path: path: path to the file containing the large array you want to memory-map\n" +
                        "    :param unk_vector_path: path to the file containing the \"unknown\" vector / slice\n" +
                        "    :param initial_memmap_size: size of the mem-map, defaults to 1000000000\n" +
                        "    :param work_space_name: DL4J 'WorkSpace' name, defaults to 'memMapWorkspace'\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class InferenceConfiguration(object):\n",
                "class InferenceConfiguration(object):\n" +
                        "    \"\"\"InferenceConfiguration\n" +
                        "\n" +
                        "    This configuration object brings together all properties to serve a set of\n" +
                        "    pipeline steps for inference.\n" +
                        "\n" +
                        "    :param steps: list of konduit.PipelineStep\n" +
                        "    :param serving_config: a konduit.ServingConfig\n" +
                        "    :param mem_map_config: a konduit.MemMapConfig\n" +
                        "    \"\"\""
        );


        return code;
    }
}
