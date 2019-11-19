# Tests 
The following is a quick discussion of the test files for Konduit's Python SDK.

## Pipeline steps 

### [`test_start.py`](test_start.py)

Checks if a server configured with a TensorFlow model can be started, then stops it. 

Starts a server configured with an [InferenceConfiguration](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L2020-L2063) with a single [ModelStep](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L1426-L1575) built with a model trained in TensorFlow, configured using the [TensorFlowConfig](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L663-L736) class. 

This server is configured to accept input data type 'NUMPY' and output data type 'NUMPY'. 

Possible data types for the server configuration are listed in [`konduit.inference`](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L886-L891) and should be specified in [ServingConfig](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L884-L1044):
- Accepted input data types are JSON, ARROW, IMAGE, ND4J and NUMPY. 
- Accepted output data types are NUMPY, JSON, ND4J ([not yet implemented](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/client.py#L70-L71)) and ARROW.

### [`test_transform_process.py`](test_transform_process.py)

Checks if a [TransformProcessPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L1304-L1423) can be performed. 

This tests builds [InferenceConfiguration](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L2020-L2063) with [TransformProcessPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L1304-L1423). 

The transform process is defined by [TransformProcessBuilder](https://github.com/eclipse/deeplearning4j/blob/master/datavec/datavec-api/src/main/java/org/datavec/api/transform/TransformProcess.java#L611) (the Builder subclass of [TransformProcess](https://deeplearning4j.org/docs/latest/datavec-transforms) in DataVec), written to a JSON file and loaded again for use in a [TransformProcessPipelineStep](../konduit/inference.py#L1400-L1573). [TransformProcess](https://deeplearning4j.org/docs/latest/datavec-transforms) in turn requires defining a [Schema](https://deeplearning4j.org/docs/latest/datavec-schema). 

Given a JSON file as input, the Client sends a query to the Server and prints the predicted output. 

This server is configured for JSON input and output. 

### [`test_transform_process_arrow.py`](test_transform_process_arrow.py)

Similar to `test_transform_process.py`, checks if a DataVec transformation can be configured, but with the server configured to return [Arrow](https://arrow.apache.org/) output instead of JSON. 

### [`test_python_serving.py`](test_python_serving.py)

Tests whether Python pipelines can be served. Takes as input a NumPy array saved in `../data/input-0.npy` and performs a simple operation on the model inputs in a [PythonPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L1183-L1301).

The input and output data types of this server are configured to be NUMPY. 

### [`test_bert_serving.py`](test_bert_serving.py)

Similar to `test_start.py`, creates a configuration with a TensorFlow BERT model loaded with [TensorFlowConfig](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L663-L736), but also prints the predicted output, given NumPy arrays. 

The input and output data types of this server are configured to be NUMPY. 

## Saving configurations 

### [`test_json.py`](test_json.py)

After configuring a server, saves server configuration as JSON, loads it again, and checks if the existing configuration steps have the same length as the configuration loaded from the JSON file. 

Two server configurations are defined: 
- Similar to `test_start.py`, `test_json_compare()` configures a server with a TensorFlow model loaded with [TensorFlowConfig](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L663-L736), 
- `test_python_serde()` configures a server with a [PythonPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L1183-L1301) similar to `test_python_serving.py`.


## Client conversion and encoding 

### [`test_client_serde.py`](test_client_serde.py)

This file tests serialization and deserialization ('serde') by the Client.

#### `test_multipart_encode()`
Tests the following methods for the [Client](../konduit/client.py) class: 
- [`_convert_numpy_to_binary()`](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/client.py#L80-L85): converts a NumPy array into binary data.
- [`_convert_multi_part_inputs()`](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/client.py#L101-L110): encodes a dictionary of key-value pairs from `convert_numpy_to_binary()` in a multipart request body. Values will be reformatted into binary format if they are not already in binary format.
- [`_encode_multi_part_input()`](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/client.py#L93-L99): encodes the output from `convert_multi_part_inputs()`.
- [`_convert_multi_part_output()`](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/client.py#L112-L132): decodes output returned by the server into the output type requested by the client.

#### `test_python_serde()`

Tests serializing [InferenceConfiguration](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L2020-L2063) to JSON.

Similar to `test_python_serving.py`, this function configures a server with [PythonPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/6d12ebd5e37a2092c66aee04ee588b5c0d028445/python/konduit/inference.py#L1183-L1301), converts the configuration into a Python dictionary, then converts it into a JSON file containing the configuration.
