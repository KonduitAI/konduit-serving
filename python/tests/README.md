# Tests 
This article discusses the tests for the Python SDK of Konduit.

## Pipeline steps 

### [`test_start.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_start.py)

Starts a server based on an [InferenceConfiguration](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/inference.py#L2386-L2427) with a single [ModelPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/inference.py#L1579-L1778) built with a model trained in TensorFlow, configured using the [TensorFlowConfig](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/inference.py#L656-L720) class. Checks if the server is started, then stops it. The server takes as input data type 'NUMPY' and outputs a 'NUMPY' object. 

Possible data types are listed in [`konduit.inference`](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/inference.py#L863-L865) and should be specified in ServingConfig:
- Accepted input data types are JSON, ARROW, IMAGE, ND4J (not yet implemented), NUMPY. 
- Accepted output data types are NUMPY, JSON, ND4J (not yet implemented), ARROW
- Prediction types supported are CLASSIFICATION, YOLO, SSD, RCNN, RAW, REGRESSION. 

### [`test_transform_process.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_transform_process.py)

Build [InferenceConfiguration](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/inference.py#L2386-L2427) with [TransformProcessPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/inference.py#L1400-L1573), dump JSON. Use [InferenceConfiguration](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/inference.py#L2386-L2427) object in Server, Client queries Server and prints output from pipeline. 

### [`test_transform_process_arrow.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_transform_process_arrow.py)

Similar to `test_transform_process.py`, but with Arrow output from client. 

### [`test_python_serving.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_python_serving.py)

Tests whether Python pipelines can be served. Takes as input a NumPy array saved in `../data/input-0.npy` and adds 2 to the model inputs in a [PythonPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/inference.py#L1221-L1394).

### [`test_bert_serving.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_bert_serving.py)

Similar to `test_start.py`, but also prints the predicted output given NumPy arrays. 

## Saving configurations 

### [`test_json.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_json.py)

Saves file to JSON, loads it again, and checks if the loaded and existing configuration steps have equal length. 

`test_json_compare()` creates a configuration similar to `test_start.py`, while `test_python_serde()` creates a Python pipeline similar to `test_python_serving.py`.


## Client conversion and encoding 

### [`test_client_serde.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_client_serde.py)

#### `test_multipart_encode()`
Tests the following methods for the [Client](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/client.py#L10-L139) class: 
- [`_convert_numpy_to_binary()`](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/client.py#L72-L76) - converts inputs into a dictionary with keys and binary data as values
- [`_convert_multi_part_inputs()`](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/client.py#L100-L108) - encodes the dictionary from `convert_numpy_to_binary()` in a multipart request body
- [`_encode_multi_part_input()`](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/client.py#L85-L97) - decodes the multipart response into binary data or, depending on the output type, converts into the corresponding output class
- [`_convert_multi_part_output()`](https://github.com/KonduitAI/konduit-serving/blob/1f50481aa24e7d84a44b41ead592f7935a0f4b20/python/konduit/client.py#L110-L129) - converts output returned by the server into the output type requested by the client

#### `test_python_serde()`
Similar to `test_python_serving.py`. Dumps JSON.
