import enum
from konduit.json_utils import dict_with_type,DictWrapper,ListWrapper





class TensorDataTypesConfig(object):

    _types_map = {
        'inputDataTypes': {'type': dict, 'subtype': None},
        'outputDataTypes': {'type': dict, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , input_data_types=None
            , output_data_types=None
            ):
        self.__input_data_types = input_data_types
        self.__output_data_types = output_data_types
    
    def _get_input_data_types(self):
        return self.__input_data_types
    def _set_input_data_types(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputDataTypes must be type")
        self.__input_data_types = value
    input_data_types = property(_get_input_data_types, _set_input_data_types)
    
    def _get_output_data_types(self):
        return self.__output_data_types
    def _set_output_data_types(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputDataTypes must be type")
        self.__output_data_types = value
    output_data_types = property(_get_output_data_types, _set_output_data_types)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__input_data_types is not None:
            d['inputDataTypes']  =  self.__input_data_types.as_dict() if hasattr(self.__input_data_types, 'as_dict') else self.__input_data_types
        if self.__output_data_types is not None:
            d['outputDataTypes']  =  self.__output_data_types.as_dict() if hasattr(self.__output_data_types, 'as_dict') else self.__output_data_types
        return d





class PubsubConfig(object):

    _types_map = {
        'httpMethod': {'type': str, 'subtype': None},
        'pubsubUrl': {'type': str, 'subtype': None},
        'contentType': {'type': str, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , http_method=None
            , pubsub_url=None
            , content_type=None
            ):
        self.__http_method = http_method
        self.__pubsub_url = pubsub_url
        self.__content_type = content_type
    
    def _get_http_method(self):
        return self.__http_method
    def _set_http_method(self, value):
        if not isinstance(value, str):
            raise TypeError("httpMethod must be str")
        self.__http_method = value
    http_method = property(_get_http_method, _set_http_method)
    
    def _get_pubsub_url(self):
        return self.__pubsub_url
    def _set_pubsub_url(self, value):
        if not isinstance(value, str):
            raise TypeError("pubsubUrl must be str")
        self.__pubsub_url = value
    pubsub_url = property(_get_pubsub_url, _set_pubsub_url)
    
    def _get_content_type(self):
        return self.__content_type
    def _set_content_type(self, value):
        if not isinstance(value, str):
            raise TypeError("contentType must be str")
        self.__content_type = value
    content_type = property(_get_content_type, _set_content_type)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__http_method is not None:
            d['httpMethod']  =  self.__http_method.as_dict() if hasattr(self.__http_method, 'as_dict') else self.__http_method
        if self.__pubsub_url is not None:
            d['pubsubUrl']  =  self.__pubsub_url.as_dict() if hasattr(self.__pubsub_url, 'as_dict') else self.__pubsub_url
        if self.__content_type is not None:
            d['contentType']  =  self.__content_type.as_dict() if hasattr(self.__content_type, 'as_dict') else self.__content_type
        return d





class SavedModelConfig(object):

    _types_map = {
        'savedModelPath': {'type': str, 'subtype': None},
        'modelTag': {'type': str, 'subtype': None},
        'signatureKey': {'type': str, 'subtype': None},
        'savedModelInputOrder': {'type': list, 'subtype': str},
        'saveModelOutputOrder': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'savedModelInputOrder': 'table',
        'saveModelOutputOrder': 'table',
    }

    def __init__(self
            , saved_model_path=None
            , model_tag=None
            , signature_key=None
            , saved_model_input_order=None
            , save_model_output_order=None
            ):
        self.__saved_model_path = saved_model_path
        self.__model_tag = model_tag
        self.__signature_key = signature_key
        self.__saved_model_input_order = saved_model_input_order
        self.__save_model_output_order = save_model_output_order
    
    def _get_saved_model_path(self):
        return self.__saved_model_path
    def _set_saved_model_path(self, value):
        if not isinstance(value, str):
            raise TypeError("savedModelPath must be str")
        self.__saved_model_path = value
    saved_model_path = property(_get_saved_model_path, _set_saved_model_path)
    
    def _get_model_tag(self):
        return self.__model_tag
    def _set_model_tag(self, value):
        if not isinstance(value, str):
            raise TypeError("modelTag must be str")
        self.__model_tag = value
    model_tag = property(_get_model_tag, _set_model_tag)
    
    def _get_signature_key(self):
        return self.__signature_key
    def _set_signature_key(self, value):
        if not isinstance(value, str):
            raise TypeError("signatureKey must be str")
        self.__signature_key = value
    signature_key = property(_get_signature_key, _set_signature_key)
    
    def _get_saved_model_input_order(self):
        return self.__saved_model_input_order
    def _set_saved_model_input_order(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("savedModelInputOrder must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("savedModelInputOrder list valeus must be str")
        self.__saved_model_input_order = value
    saved_model_input_order = property(_get_saved_model_input_order, _set_saved_model_input_order)
    
    def _get_save_model_output_order(self):
        return self.__save_model_output_order
    def _set_save_model_output_order(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("saveModelOutputOrder must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("saveModelOutputOrder list valeus must be str")
        self.__save_model_output_order = value
    save_model_output_order = property(_get_save_model_output_order, _set_save_model_output_order)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__saved_model_path is not None:
            d['savedModelPath']  =  self.__saved_model_path.as_dict() if hasattr(self.__saved_model_path, 'as_dict') else self.__saved_model_path
        if self.__model_tag is not None:
            d['modelTag']  =  self.__model_tag.as_dict() if hasattr(self.__model_tag, 'as_dict') else self.__model_tag
        if self.__signature_key is not None:
            d['signatureKey']  =  self.__signature_key.as_dict() if hasattr(self.__signature_key, 'as_dict') else self.__signature_key
        if self.__saved_model_input_order is not None:
            d['savedModelInputOrder']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__saved_model_input_order]
        if self.__save_model_output_order is not None:
            d['saveModelOutputOrder']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__save_model_output_order]
        return d





class ParallelInferenceConfig(object):

    _inferenceMode_enum = enum.Enum('_inferenceMode_enum', 'SEQUENTIAL BATCHED INPLACE', module=__name__)
    _types_map = {
        'queueLimit': {'type': int, 'subtype': None},
        'batchLimit': {'type': int, 'subtype': None},
        'workers': {'type': int, 'subtype': None},
        'maxTrainEpochs': {'type': int, 'subtype': None},
        'inferenceMode': {'type': str, 'subtype': None},
        'vertxConfigJson': {'type': str, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , queue_limit=None
            , batch_limit=None
            , workers=None
            , max_train_epochs=None
            , inference_mode=None
            , vertx_config_json=None
            ):
        self.__queue_limit = queue_limit
        self.__batch_limit = batch_limit
        self.__workers = workers
        self.__max_train_epochs = max_train_epochs
        self.__inference_mode = inference_mode
        self.__vertx_config_json = vertx_config_json
    
    def _get_queue_limit(self):
        return self.__queue_limit
    def _set_queue_limit(self, value):
        if not isinstance(value, int):
            raise TypeError("queueLimit must be int")
        self.__queue_limit = value
    queue_limit = property(_get_queue_limit, _set_queue_limit)
    
    def _get_batch_limit(self):
        return self.__batch_limit
    def _set_batch_limit(self, value):
        if not isinstance(value, int):
            raise TypeError("batchLimit must be int")
        self.__batch_limit = value
    batch_limit = property(_get_batch_limit, _set_batch_limit)
    
    def _get_workers(self):
        return self.__workers
    def _set_workers(self, value):
        if not isinstance(value, int):
            raise TypeError("workers must be int")
        self.__workers = value
    workers = property(_get_workers, _set_workers)
    
    def _get_max_train_epochs(self):
        return self.__max_train_epochs
    def _set_max_train_epochs(self, value):
        if not isinstance(value, int):
            raise TypeError("maxTrainEpochs must be int")
        self.__max_train_epochs = value
    max_train_epochs = property(_get_max_train_epochs, _set_max_train_epochs)
    
    def _get_inference_mode(self):
        return self.__inference_mode
    def _set_inference_mode(self, value):
        if not isinstance(value, str):
            raise TypeError("inferenceMode must be str")
        if value in self._inferenceMode_enum.__members__:
            self.__type = value
        else:
            raise ValueError("Value {} not in _inferenceMode_enum list".format(value))
    inference_mode = property(_get_inference_mode, _set_inference_mode)
    
    def _get_vertx_config_json(self):
        return self.__vertx_config_json
    def _set_vertx_config_json(self, value):
        if not isinstance(value, str):
            raise TypeError("vertxConfigJson must be str")
        self.__vertx_config_json = value
    vertx_config_json = property(_get_vertx_config_json, _set_vertx_config_json)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__queue_limit is not None:
            d['queueLimit']  =  self.__queue_limit.as_dict() if hasattr(self.__queue_limit, 'as_dict') else self.__queue_limit
        if self.__batch_limit is not None:
            d['batchLimit']  =  self.__batch_limit.as_dict() if hasattr(self.__batch_limit, 'as_dict') else self.__batch_limit
        if self.__workers is not None:
            d['workers']  =  self.__workers.as_dict() if hasattr(self.__workers, 'as_dict') else self.__workers
        if self.__max_train_epochs is not None:
            d['maxTrainEpochs']  =  self.__max_train_epochs.as_dict() if hasattr(self.__max_train_epochs, 'as_dict') else self.__max_train_epochs
        if self.__inference_mode is not None:
            d['inferenceMode']  =  self.__inference_mode.as_dict() if hasattr(self.__inference_mode, 'as_dict') else self.__inference_mode
        if self.__vertx_config_json is not None:
            d['vertxConfigJson']  =  self.__vertx_config_json.as_dict() if hasattr(self.__vertx_config_json, 'as_dict') else self.__vertx_config_json
        return d





class ModelConfigType(object):

    _modelType_enum = enum.Enum('_modelType_enum', 'COMPUTATION_GRAPH MULTI_LAYER_NETWORK PMML TENSORFLOW KERAS SAMEDIFF', module=__name__)
    _types_map = {
        'modelType': {'type': str, 'subtype': None},
        'modelLoadingPath': {'type': str, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , model_type=None
            , model_loading_path=None
            ):
        self.__model_type = model_type
        self.__model_loading_path = model_loading_path
    
    def _get_model_type(self):
        return self.__model_type
    def _set_model_type(self, value):
        if not isinstance(value, str):
            raise TypeError("modelType must be str")
        if value in self._modelType_enum.__members__:
            self.__type = value
        else:
            raise ValueError("Value {} not in _modelType_enum list".format(value))
    model_type = property(_get_model_type, _set_model_type)
    
    def _get_model_loading_path(self):
        return self.__model_loading_path
    def _set_model_loading_path(self, value):
        if not isinstance(value, str):
            raise TypeError("modelLoadingPath must be str")
        self.__model_loading_path = value
    model_loading_path = property(_get_model_loading_path, _set_model_loading_path)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__model_type is not None:
            d['modelType']  =  self.__model_type.as_dict() if hasattr(self.__model_type, 'as_dict') else self.__model_type
        if self.__model_loading_path is not None:
            d['modelLoadingPath']  =  self.__model_loading_path.as_dict() if hasattr(self.__model_loading_path, 'as_dict') else self.__model_loading_path
        return d





class ModelConfig(object):

    _types_map = {
        'tensorDataTypesConfig': {'type': TensorDataTypesConfig, 'subtype': None},
        'modelConfigType': {'type': ModelConfigType, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , tensor_data_types_config=None
            , model_config_type=None
            ):
        self.__tensor_data_types_config = tensor_data_types_config
        self.__model_config_type = model_config_type
    
    def _get_tensor_data_types_config(self):
        return self.__tensor_data_types_config
    def _set_tensor_data_types_config(self, value):
        if not isinstance(value, TensorDataTypesConfig):
            raise TypeError("tensorDataTypesConfig must be TensorDataTypesConfig")
        self.__tensor_data_types_config = value
    tensor_data_types_config = property(_get_tensor_data_types_config, _set_tensor_data_types_config)
    
    def _get_model_config_type(self):
        return self.__model_config_type
    def _set_model_config_type(self, value):
        if not isinstance(value, ModelConfigType):
            raise TypeError("modelConfigType must be ModelConfigType")
        self.__model_config_type = value
    model_config_type = property(_get_model_config_type, _set_model_config_type)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__tensor_data_types_config is not None:
            d['tensorDataTypesConfig']  =  self.__tensor_data_types_config.as_dict() if hasattr(self.__tensor_data_types_config, 'as_dict') else self.__tensor_data_types_config
        if self.__model_config_type is not None:
            d['modelConfigType']  =  self.__model_config_type.as_dict() if hasattr(self.__model_config_type, 'as_dict') else self.__model_config_type
        return d





class TensorDataType(object):

    _types_map = {
    }
    _formats_map = {
    }

    def __init__(self
            ):
        pass
    
    def as_dict(self):
        d = dict_with_type(self)
        return d





class PmmlConfig(object):

    _types_map = {
        'tensorDataTypesConfig': {'type': TensorDataTypesConfig, 'subtype': None},
        'modelConfigType': {'type': ModelConfigType, 'subtype': None},
        'evaluatorFactoryName': {'type': str, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , tensor_data_types_config=None
            , model_config_type=None
            , evaluator_factory_name=None
            ):
        self.__tensor_data_types_config = tensor_data_types_config
        self.__model_config_type = model_config_type
        self.__evaluator_factory_name = evaluator_factory_name
    
    def _get_tensor_data_types_config(self):
        return self.__tensor_data_types_config
    def _set_tensor_data_types_config(self, value):
        if not isinstance(value, TensorDataTypesConfig):
            raise TypeError("tensorDataTypesConfig must be TensorDataTypesConfig")
        self.__tensor_data_types_config = value
    tensor_data_types_config = property(_get_tensor_data_types_config, _set_tensor_data_types_config)
    
    def _get_model_config_type(self):
        return self.__model_config_type
    def _set_model_config_type(self, value):
        if not isinstance(value, ModelConfigType):
            raise TypeError("modelConfigType must be ModelConfigType")
        self.__model_config_type = value
    model_config_type = property(_get_model_config_type, _set_model_config_type)
    
    def _get_evaluator_factory_name(self):
        return self.__evaluator_factory_name
    def _set_evaluator_factory_name(self, value):
        if not isinstance(value, str):
            raise TypeError("evaluatorFactoryName must be str")
        self.__evaluator_factory_name = value
    evaluator_factory_name = property(_get_evaluator_factory_name, _set_evaluator_factory_name)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__tensor_data_types_config is not None:
            d['tensorDataTypesConfig']  =  self.__tensor_data_types_config.as_dict() if hasattr(self.__tensor_data_types_config, 'as_dict') else self.__tensor_data_types_config
        if self.__model_config_type is not None:
            d['modelConfigType']  =  self.__model_config_type.as_dict() if hasattr(self.__model_config_type, 'as_dict') else self.__model_config_type
        if self.__evaluator_factory_name is not None:
            d['evaluatorFactoryName']  =  self.__evaluator_factory_name.as_dict() if hasattr(self.__evaluator_factory_name, 'as_dict') else self.__evaluator_factory_name
        return d





class ObjectDetectionConfig(object):

    _types_map = {
        'threshold': {'type': float, 'subtype': None},
        'numLabels': {'type': int, 'subtype': None},
        'labelsPath': {'type': str, 'subtype': None},
        'priors': {'type': list, 'subtype': list},
        'inputShape': {'type': list, 'subtype': int},
    }
    _formats_map = {
        'priors': 'table',
        'inputShape': 'table',
    }

    def __init__(self
            , threshold=None
            , num_labels=None
            , labels_path=None
            , priors=None
            , input_shape=None
            ):
        self.__threshold = threshold
        self.__num_labels = num_labels
        self.__labels_path = labels_path
        self.__priors = priors
        self.__input_shape = input_shape
    
    def _get_threshold(self):
        return self.__threshold
    def _set_threshold(self, value):
        if not isinstance(value, float):
            raise TypeError("threshold must be float")
        self.__threshold = value
    threshold = property(_get_threshold, _set_threshold)
    
    def _get_num_labels(self):
        return self.__num_labels
    def _set_num_labels(self, value):
        if not isinstance(value, int):
            raise TypeError("numLabels must be int")
        self.__num_labels = value
    num_labels = property(_get_num_labels, _set_num_labels)
    
    def _get_labels_path(self):
        return self.__labels_path
    def _set_labels_path(self, value):
        if not isinstance(value, str):
            raise TypeError("labelsPath must be str")
        self.__labels_path = value
    labels_path = property(_get_labels_path, _set_labels_path)
    
    def _get_priors(self):
        return self.__priors
    def _set_priors(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("priors must be list")
        if not all(isinstance(i, list) for i in value):
            raise TypeError("priors list valeus must be list")
        self.__priors = value
    priors = property(_get_priors, _set_priors)
    
    def _get_input_shape(self):
        return self.__input_shape
    def _set_input_shape(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("inputShape must be list")
        if not all(isinstance(i, int) for i in value):
            raise TypeError("inputShape list valeus must be int")
        self.__input_shape = value
    input_shape = property(_get_input_shape, _set_input_shape)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__threshold is not None:
            d['threshold']  =  self.__threshold.as_dict() if hasattr(self.__threshold, 'as_dict') else self.__threshold
        if self.__num_labels is not None:
            d['numLabels']  =  self.__num_labels.as_dict() if hasattr(self.__num_labels, 'as_dict') else self.__num_labels
        if self.__labels_path is not None:
            d['labelsPath']  =  self.__labels_path.as_dict() if hasattr(self.__labels_path, 'as_dict') else self.__labels_path
        if self.__priors is not None:
            d['priors']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__priors]
        if self.__input_shape is not None:
            d['inputShape']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__input_shape]
        return d





class SchemaType(object):

    _types_map = {
    }
    _formats_map = {
    }

    def __init__(self
            ):
        pass
    
    def as_dict(self):
        d = dict_with_type(self)
        return d





class Input(object):

    _types_map = {
    }
    _formats_map = {
    }

    def __init__(self
            ):
        pass
    
    def as_dict(self):
        d = dict_with_type(self)
        return d





class Output(object):

    _types_map = {
    }
    _formats_map = {
    }

    def __init__(self
            ):
        pass
    
    def as_dict(self):
        d = dict_with_type(self)
        return d





class SameDiffConfig(object):

    _types_map = {
        'tensorDataTypesConfig': {'type': TensorDataTypesConfig, 'subtype': None},
        'modelConfigType': {'type': ModelConfigType, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , tensor_data_types_config=None
            , model_config_type=None
            ):
        self.__tensor_data_types_config = tensor_data_types_config
        self.__model_config_type = model_config_type
    
    def _get_tensor_data_types_config(self):
        return self.__tensor_data_types_config
    def _set_tensor_data_types_config(self, value):
        if not isinstance(value, TensorDataTypesConfig):
            raise TypeError("tensorDataTypesConfig must be TensorDataTypesConfig")
        self.__tensor_data_types_config = value
    tensor_data_types_config = property(_get_tensor_data_types_config, _set_tensor_data_types_config)
    
    def _get_model_config_type(self):
        return self.__model_config_type
    def _set_model_config_type(self, value):
        if not isinstance(value, ModelConfigType):
            raise TypeError("modelConfigType must be ModelConfigType")
        self.__model_config_type = value
    model_config_type = property(_get_model_config_type, _set_model_config_type)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__tensor_data_types_config is not None:
            d['tensorDataTypesConfig']  =  self.__tensor_data_types_config.as_dict() if hasattr(self.__tensor_data_types_config, 'as_dict') else self.__tensor_data_types_config
        if self.__model_config_type is not None:
            d['modelConfigType']  =  self.__model_config_type.as_dict() if hasattr(self.__model_config_type, 'as_dict') else self.__model_config_type
        return d





class TensorFlowConfig(object):

    _types_map = {
        'tensorDataTypesConfig': {'type': TensorDataTypesConfig, 'subtype': None},
        'modelConfigType': {'type': ModelConfigType, 'subtype': None},
        'configProtoPath': {'type': str, 'subtype': None},
        'savedModelConfig': {'type': SavedModelConfig, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , tensor_data_types_config=None
            , model_config_type=None
            , config_proto_path=None
            , saved_model_config=None
            ):
        self.__tensor_data_types_config = tensor_data_types_config
        self.__model_config_type = model_config_type
        self.__config_proto_path = config_proto_path
        self.__saved_model_config = saved_model_config
    
    def _get_tensor_data_types_config(self):
        return self.__tensor_data_types_config
    def _set_tensor_data_types_config(self, value):
        if not isinstance(value, TensorDataTypesConfig):
            raise TypeError("tensorDataTypesConfig must be TensorDataTypesConfig")
        self.__tensor_data_types_config = value
    tensor_data_types_config = property(_get_tensor_data_types_config, _set_tensor_data_types_config)
    
    def _get_model_config_type(self):
        return self.__model_config_type
    def _set_model_config_type(self, value):
        if not isinstance(value, ModelConfigType):
            raise TypeError("modelConfigType must be ModelConfigType")
        self.__model_config_type = value
    model_config_type = property(_get_model_config_type, _set_model_config_type)
    
    def _get_config_proto_path(self):
        return self.__config_proto_path
    def _set_config_proto_path(self, value):
        if not isinstance(value, str):
            raise TypeError("configProtoPath must be str")
        self.__config_proto_path = value
    config_proto_path = property(_get_config_proto_path, _set_config_proto_path)
    
    def _get_saved_model_config(self):
        return self.__saved_model_config
    def _set_saved_model_config(self, value):
        if not isinstance(value, SavedModelConfig):
            raise TypeError("savedModelConfig must be SavedModelConfig")
        self.__saved_model_config = value
    saved_model_config = property(_get_saved_model_config, _set_saved_model_config)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__tensor_data_types_config is not None:
            d['tensorDataTypesConfig']  =  self.__tensor_data_types_config.as_dict() if hasattr(self.__tensor_data_types_config, 'as_dict') else self.__tensor_data_types_config
        if self.__model_config_type is not None:
            d['modelConfigType']  =  self.__model_config_type.as_dict() if hasattr(self.__model_config_type, 'as_dict') else self.__model_config_type
        if self.__config_proto_path is not None:
            d['configProtoPath']  =  self.__config_proto_path.as_dict() if hasattr(self.__config_proto_path, 'as_dict') else self.__config_proto_path
        if self.__saved_model_config is not None:
            d['savedModelConfig']  =  self.__saved_model_config.as_dict() if hasattr(self.__saved_model_config, 'as_dict') else self.__saved_model_config
        return d





class PythonConfig(object):

    _types_map = {
        'tensorDataTypesConfig': {'type': TensorDataTypesConfig, 'subtype': None},
        'modelConfigType': {'type': ModelConfigType, 'subtype': None},
        'pythonCode': {'type': str, 'subtype': None},
        'pythonCodePath': {'type': str, 'subtype': None},
        'pythonInputs': {'type': dict, 'subtype': None},
        'pythonOutputs': {'type': dict, 'subtype': None},
        'extraInputs': {'type': dict, 'subtype': None},
        'pythonPath': {'type': str, 'subtype': None},
        'returnAllInputs': {'type': bool, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , tensor_data_types_config=None
            , model_config_type=None
            , python_code=None
            , python_code_path=None
            , python_inputs=None
            , python_outputs=None
            , extra_inputs=None
            , python_path=None
            , return_all_inputs=None
            ):
        self.__tensor_data_types_config = tensor_data_types_config
        self.__model_config_type = model_config_type
        self.__python_code = python_code
        self.__python_code_path = python_code_path
        self.__python_inputs = python_inputs
        self.__python_outputs = python_outputs
        self.__extra_inputs = extra_inputs
        self.__python_path = python_path
        self.__return_all_inputs = return_all_inputs
    
    def _get_tensor_data_types_config(self):
        return self.__tensor_data_types_config
    def _set_tensor_data_types_config(self, value):
        if not isinstance(value, TensorDataTypesConfig):
            raise TypeError("tensorDataTypesConfig must be TensorDataTypesConfig")
        self.__tensor_data_types_config = value
    tensor_data_types_config = property(_get_tensor_data_types_config, _set_tensor_data_types_config)
    
    def _get_model_config_type(self):
        return self.__model_config_type
    def _set_model_config_type(self, value):
        if not isinstance(value, ModelConfigType):
            raise TypeError("modelConfigType must be ModelConfigType")
        self.__model_config_type = value
    model_config_type = property(_get_model_config_type, _set_model_config_type)
    
    def _get_python_code(self):
        return self.__python_code
    def _set_python_code(self, value):
        if not isinstance(value, str):
            raise TypeError("pythonCode must be str")
        self.__python_code = value
    python_code = property(_get_python_code, _set_python_code)
    
    def _get_python_code_path(self):
        return self.__python_code_path
    def _set_python_code_path(self, value):
        if not isinstance(value, str):
            raise TypeError("pythonCodePath must be str")
        self.__python_code_path = value
    python_code_path = property(_get_python_code_path, _set_python_code_path)
    
    def _get_python_inputs(self):
        return self.__python_inputs
    def _set_python_inputs(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("pythonInputs must be type")
        self.__python_inputs = value
    python_inputs = property(_get_python_inputs, _set_python_inputs)
    
    def _get_python_outputs(self):
        return self.__python_outputs
    def _set_python_outputs(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("pythonOutputs must be type")
        self.__python_outputs = value
    python_outputs = property(_get_python_outputs, _set_python_outputs)
    
    def _get_extra_inputs(self):
        return self.__extra_inputs
    def _set_extra_inputs(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("extraInputs must be type")
        self.__extra_inputs = value
    extra_inputs = property(_get_extra_inputs, _set_extra_inputs)
    
    def _get_python_path(self):
        return self.__python_path
    def _set_python_path(self, value):
        if not isinstance(value, str):
            raise TypeError("pythonPath must be str")
        self.__python_path = value
    python_path = property(_get_python_path, _set_python_path)
    
    def _get_return_all_inputs(self):
        return self.__return_all_inputs
    def _set_return_all_inputs(self, value):
        if not isinstance(value, bool):
            raise TypeError("returnAllInputs must be bool")
        self.__return_all_inputs = value
    return_all_inputs = property(_get_return_all_inputs, _set_return_all_inputs)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__tensor_data_types_config is not None:
            d['tensorDataTypesConfig']  =  self.__tensor_data_types_config.as_dict() if hasattr(self.__tensor_data_types_config, 'as_dict') else self.__tensor_data_types_config
        if self.__model_config_type is not None:
            d['modelConfigType']  =  self.__model_config_type.as_dict() if hasattr(self.__model_config_type, 'as_dict') else self.__model_config_type
        if self.__python_code is not None:
            d['pythonCode']  =  self.__python_code.as_dict() if hasattr(self.__python_code, 'as_dict') else self.__python_code
        if self.__python_code_path is not None:
            d['pythonCodePath']  =  self.__python_code_path.as_dict() if hasattr(self.__python_code_path, 'as_dict') else self.__python_code_path
        if self.__python_inputs is not None:
            d['pythonInputs']  =  self.__python_inputs.as_dict() if hasattr(self.__python_inputs, 'as_dict') else self.__python_inputs
        if self.__python_outputs is not None:
            d['pythonOutputs']  =  self.__python_outputs.as_dict() if hasattr(self.__python_outputs, 'as_dict') else self.__python_outputs
        if self.__extra_inputs is not None:
            d['extraInputs']  =  self.__extra_inputs.as_dict() if hasattr(self.__extra_inputs, 'as_dict') else self.__extra_inputs
        if self.__python_path is not None:
            d['pythonPath']  =  self.__python_path.as_dict() if hasattr(self.__python_path, 'as_dict') else self.__python_path
        if self.__return_all_inputs is not None:
            d['returnAllInputs']  =  self.__return_all_inputs.as_dict() if hasattr(self.__return_all_inputs, 'as_dict') else self.__return_all_inputs
        return d





class ServingConfig(object):

    _inputDataType_enum = enum.Enum('_inputDataType_enum', 'NUMPY JSON ND4J IMAGE ARROW', module=__name__)
    _outputDataType_enum = enum.Enum('_outputDataType_enum', 'NUMPY JSON ND4J ARROW', module=__name__)
    _predictionType_enum = enum.Enum('_predictionType_enum', 'CLASSIFICATION YOLO SSD RCNN RAW REGRESSION', module=__name__)
    _types_map = {
        'pubSubConfig': {'type': PubsubConfig, 'subtype': None},
        'httpPort': {'type': int, 'subtype': None},
        'listenHost': {'type': str, 'subtype': None},
        'inputDataType': {'type': str, 'subtype': None},
        'outputDataType': {'type': str, 'subtype': None},
        'predictionType': {'type': str, 'subtype': None},
        'parallelInferenceConfig': {'type': ParallelInferenceConfig, 'subtype': None},
        'uploadsDirectory': {'type': str, 'subtype': None},
        'logTimings': {'type': bool, 'subtype': None},
        'metricTypes': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'metricTypes': 'table',
    }

    def __init__(self
            , pub_sub_config=None
            , http_port=None
            , listen_host=None
            , input_data_type=None
            , output_data_type=None
            , prediction_type=None
            , parallel_inference_config=None
            , uploads_directory=None
            , log_timings=None
            , metric_types=None
            ):
        self.__pub_sub_config = pub_sub_config
        self.__http_port = http_port
        self.__listen_host = listen_host
        self.__input_data_type = input_data_type
        self.__output_data_type = output_data_type
        self.__prediction_type = prediction_type
        self.__parallel_inference_config = parallel_inference_config
        self.__uploads_directory = uploads_directory
        self.__log_timings = log_timings
        self.__metric_types = metric_types
    
    def _get_pub_sub_config(self):
        return self.__pub_sub_config
    def _set_pub_sub_config(self, value):
        if not isinstance(value, PubsubConfig):
            raise TypeError("pubSubConfig must be PubsubConfig")
        self.__pub_sub_config = value
    pub_sub_config = property(_get_pub_sub_config, _set_pub_sub_config)
    
    def _get_http_port(self):
        return self.__http_port
    def _set_http_port(self, value):
        if not isinstance(value, int):
            raise TypeError("httpPort must be int")
        self.__http_port = value
    http_port = property(_get_http_port, _set_http_port)
    
    def _get_listen_host(self):
        return self.__listen_host
    def _set_listen_host(self, value):
        if not isinstance(value, str):
            raise TypeError("listenHost must be str")
        self.__listen_host = value
    listen_host = property(_get_listen_host, _set_listen_host)
    
    def _get_input_data_type(self):
        return self.__input_data_type
    def _set_input_data_type(self, value):
        if not isinstance(value, str):
            raise TypeError("inputDataType must be str")
        if value in self._inputDataType_enum.__members__:
            self.__type = value
        else:
            raise ValueError("Value {} not in _inputDataType_enum list".format(value))
    input_data_type = property(_get_input_data_type, _set_input_data_type)
    
    def _get_output_data_type(self):
        return self.__output_data_type
    def _set_output_data_type(self, value):
        if not isinstance(value, str):
            raise TypeError("outputDataType must be str")
        if value in self._outputDataType_enum.__members__:
            self.__type = value
        else:
            raise ValueError("Value {} not in _outputDataType_enum list".format(value))
    output_data_type = property(_get_output_data_type, _set_output_data_type)
    
    def _get_prediction_type(self):
        return self.__prediction_type
    def _set_prediction_type(self, value):
        if not isinstance(value, str):
            raise TypeError("predictionType must be str")
        if value in self._predictionType_enum.__members__:
            self.__type = value
        else:
            raise ValueError("Value {} not in _predictionType_enum list".format(value))
    prediction_type = property(_get_prediction_type, _set_prediction_type)
    
    def _get_parallel_inference_config(self):
        return self.__parallel_inference_config
    def _set_parallel_inference_config(self, value):
        if not isinstance(value, ParallelInferenceConfig):
            raise TypeError("parallelInferenceConfig must be ParallelInferenceConfig")
        self.__parallel_inference_config = value
    parallel_inference_config = property(_get_parallel_inference_config, _set_parallel_inference_config)
    
    def _get_uploads_directory(self):
        return self.__uploads_directory
    def _set_uploads_directory(self, value):
        if not isinstance(value, str):
            raise TypeError("uploadsDirectory must be str")
        self.__uploads_directory = value
    uploads_directory = property(_get_uploads_directory, _set_uploads_directory)
    
    def _get_log_timings(self):
        return self.__log_timings
    def _set_log_timings(self, value):
        if not isinstance(value, bool):
            raise TypeError("logTimings must be bool")
        self.__log_timings = value
    log_timings = property(_get_log_timings, _set_log_timings)
    
    def _get_metric_types(self):
        return self.__metric_types
    def _set_metric_types(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("metricTypes must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("metricTypes list valeus must be str")
        self.__metric_types = value
    metric_types = property(_get_metric_types, _set_metric_types)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__pub_sub_config is not None:
            d['pubSubConfig']  =  self.__pub_sub_config.as_dict() if hasattr(self.__pub_sub_config, 'as_dict') else self.__pub_sub_config
        if self.__http_port is not None:
            d['httpPort']  =  self.__http_port.as_dict() if hasattr(self.__http_port, 'as_dict') else self.__http_port
        if self.__listen_host is not None:
            d['listenHost']  =  self.__listen_host.as_dict() if hasattr(self.__listen_host, 'as_dict') else self.__listen_host
        if self.__input_data_type is not None:
            d['inputDataType']  =  self.__input_data_type.as_dict() if hasattr(self.__input_data_type, 'as_dict') else self.__input_data_type
        if self.__output_data_type is not None:
            d['outputDataType']  =  self.__output_data_type.as_dict() if hasattr(self.__output_data_type, 'as_dict') else self.__output_data_type
        if self.__prediction_type is not None:
            d['predictionType']  =  self.__prediction_type.as_dict() if hasattr(self.__prediction_type, 'as_dict') else self.__prediction_type
        if self.__parallel_inference_config is not None:
            d['parallelInferenceConfig']  =  self.__parallel_inference_config.as_dict() if hasattr(self.__parallel_inference_config, 'as_dict') else self.__parallel_inference_config
        if self.__uploads_directory is not None:
            d['uploadsDirectory']  =  self.__uploads_directory.as_dict() if hasattr(self.__uploads_directory, 'as_dict') else self.__uploads_directory
        if self.__log_timings is not None:
            d['logTimings']  =  self.__log_timings.as_dict() if hasattr(self.__log_timings, 'as_dict') else self.__log_timings
        if self.__metric_types is not None:
            d['metricTypes']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__metric_types]
        return d





class PipelineStep(object):

    _types_map = {
        'inputSchemas': {'type': dict, 'subtype': None},
        'outputSchemas': {'type': dict, 'subtype': None},
        'inputNames': {'type': list, 'subtype': str},
        'outputNames': {'type': list, 'subtype': str},
        'affectedInputNames': {'type': list, 'subtype': str},
        'affectedOutputNames': {'type': list, 'subtype': str},
        'inputColumnNames': {'type': dict, 'subtype': None},
        'outputColumnNames': {'type': dict, 'subtype': None},
        'targetInputStepInputNames': {'type': list, 'subtype': str},
        'targetInputStepOutputNames': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'inputNames': 'table',
        'outputNames': 'table',
        'affectedInputNames': 'table',
        'affectedOutputNames': 'table',
        'targetInputStepInputNames': 'table',
        'targetInputStepOutputNames': 'table',
    }

    def __init__(self
            , input_schemas=None
            , output_schemas=None
            , input_names=None
            , output_names=None
            , affected_input_names=None
            , affected_output_names=None
            , input_column_names=None
            , output_column_names=None
            , target_input_step_input_names=None
            , target_input_step_output_names=None
            ):
        self.__input_schemas = input_schemas
        self.__output_schemas = output_schemas
        self.__input_names = input_names
        self.__output_names = output_names
        self.__affected_input_names = affected_input_names
        self.__affected_output_names = affected_output_names
        self.__input_column_names = input_column_names
        self.__output_column_names = output_column_names
        self.__target_input_step_input_names = target_input_step_input_names
        self.__target_input_step_output_names = target_input_step_output_names
    
    def _get_input_schemas(self):
        return self.__input_schemas
    def _set_input_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputSchemas must be type")
        self.__input_schemas = value
    input_schemas = property(_get_input_schemas, _set_input_schemas)
    
    def _get_output_schemas(self):
        return self.__output_schemas
    def _set_output_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputSchemas must be type")
        self.__output_schemas = value
    output_schemas = property(_get_output_schemas, _set_output_schemas)
    
    def _get_input_names(self):
        return self.__input_names
    def _set_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("inputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("inputNames list valeus must be str")
        self.__input_names = value
    input_names = property(_get_input_names, _set_input_names)
    
    def _get_output_names(self):
        return self.__output_names
    def _set_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("outputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("outputNames list valeus must be str")
        self.__output_names = value
    output_names = property(_get_output_names, _set_output_names)
    
    def _get_affected_input_names(self):
        return self.__affected_input_names
    def _set_affected_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedInputNames list valeus must be str")
        self.__affected_input_names = value
    affected_input_names = property(_get_affected_input_names, _set_affected_input_names)
    
    def _get_affected_output_names(self):
        return self.__affected_output_names
    def _set_affected_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedOutputNames list valeus must be str")
        self.__affected_output_names = value
    affected_output_names = property(_get_affected_output_names, _set_affected_output_names)
    
    def _get_input_column_names(self):
        return self.__input_column_names
    def _set_input_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputColumnNames must be type")
        self.__input_column_names = value
    input_column_names = property(_get_input_column_names, _set_input_column_names)
    
    def _get_output_column_names(self):
        return self.__output_column_names
    def _set_output_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputColumnNames must be type")
        self.__output_column_names = value
    output_column_names = property(_get_output_column_names, _set_output_column_names)
    
    def _get_target_input_step_input_names(self):
        return self.__target_input_step_input_names
    def _set_target_input_step_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepInputNames list valeus must be str")
        self.__target_input_step_input_names = value
    target_input_step_input_names = property(_get_target_input_step_input_names, _set_target_input_step_input_names)
    
    def _get_target_input_step_output_names(self):
        return self.__target_input_step_output_names
    def _set_target_input_step_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepOutputNames list valeus must be str")
        self.__target_input_step_output_names = value
    target_input_step_output_names = property(_get_target_input_step_output_names, _set_target_input_step_output_names)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__input_schemas is not None:
            d['inputSchemas']  =  self.__input_schemas.as_dict() if hasattr(self.__input_schemas, 'as_dict') else self.__input_schemas
        if self.__output_schemas is not None:
            d['outputSchemas']  =  self.__output_schemas.as_dict() if hasattr(self.__output_schemas, 'as_dict') else self.__output_schemas
        if self.__input_names is not None:
            d['inputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__input_names]
        if self.__output_names is not None:
            d['outputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__output_names]
        if self.__affected_input_names is not None:
            d['affectedInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_input_names]
        if self.__affected_output_names is not None:
            d['affectedOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_output_names]
        if self.__input_column_names is not None:
            d['inputColumnNames']  =  self.__input_column_names.as_dict() if hasattr(self.__input_column_names, 'as_dict') else self.__input_column_names
        if self.__output_column_names is not None:
            d['outputColumnNames']  =  self.__output_column_names.as_dict() if hasattr(self.__output_column_names, 'as_dict') else self.__output_column_names
        if self.__target_input_step_input_names is not None:
            d['targetInputStepInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_input_names]
        if self.__target_input_step_output_names is not None:
            d['targetInputStepOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_output_names]
        return d





class NormalizationConfig(object):

    _types_map = {
        'config': {'type': dict, 'subtype': None},
    }
    _formats_map = {
    }

    def __init__(self
            , config=None
            ):
        self.__config = config
    
    def _get_config(self):
        return self.__config
    def _set_config(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("config must be type")
        self.__config = value
    config = property(_get_config, _set_config)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__config is not None:
            d['config']  =  self.__config.as_dict() if hasattr(self.__config, 'as_dict') else self.__config
        return d





class PythonPipelineStep(PipelineStep):

    _types_map = {
        'inputSchemas': {'type': dict, 'subtype': None},
        'outputSchemas': {'type': dict, 'subtype': None},
        'inputNames': {'type': list, 'subtype': str},
        'outputNames': {'type': list, 'subtype': str},
        'affectedInputNames': {'type': list, 'subtype': str},
        'affectedOutputNames': {'type': list, 'subtype': str},
        'inputColumnNames': {'type': dict, 'subtype': None},
        'outputColumnNames': {'type': dict, 'subtype': None},
        'pythonConfigs': {'type': dict, 'subtype': None},
        'targetInputStepInputNames': {'type': list, 'subtype': str},
        'targetInputStepOutputNames': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'inputNames': 'table',
        'outputNames': 'table',
        'affectedInputNames': 'table',
        'affectedOutputNames': 'table',
        'targetInputStepInputNames': 'table',
        'targetInputStepOutputNames': 'table',
    }

    def __init__(self
            , input_schemas=None
            , output_schemas=None
            , input_names=None
            , output_names=None
            , affected_input_names=None
            , affected_output_names=None
            , input_column_names=None
            , output_column_names=None
            , python_configs=None
            , target_input_step_input_names=None
            , target_input_step_output_names=None
            ):
        self.__input_schemas = input_schemas
        self.__output_schemas = output_schemas
        self.__input_names = input_names
        self.__output_names = output_names
        self.__affected_input_names = affected_input_names
        self.__affected_output_names = affected_output_names
        self.__input_column_names = input_column_names
        self.__output_column_names = output_column_names
        self.__python_configs = python_configs
        self.__target_input_step_input_names = target_input_step_input_names
        self.__target_input_step_output_names = target_input_step_output_names
    
    def _get_input_schemas(self):
        return self.__input_schemas
    def _set_input_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputSchemas must be type")
        self.__input_schemas = value
    input_schemas = property(_get_input_schemas, _set_input_schemas)
    
    def _get_output_schemas(self):
        return self.__output_schemas
    def _set_output_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputSchemas must be type")
        self.__output_schemas = value
    output_schemas = property(_get_output_schemas, _set_output_schemas)
    
    def _get_input_names(self):
        return self.__input_names
    def _set_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("inputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("inputNames list valeus must be str")
        self.__input_names = value
    input_names = property(_get_input_names, _set_input_names)
    
    def _get_output_names(self):
        return self.__output_names
    def _set_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("outputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("outputNames list valeus must be str")
        self.__output_names = value
    output_names = property(_get_output_names, _set_output_names)
    
    def _get_affected_input_names(self):
        return self.__affected_input_names
    def _set_affected_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedInputNames list valeus must be str")
        self.__affected_input_names = value
    affected_input_names = property(_get_affected_input_names, _set_affected_input_names)
    
    def _get_affected_output_names(self):
        return self.__affected_output_names
    def _set_affected_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedOutputNames list valeus must be str")
        self.__affected_output_names = value
    affected_output_names = property(_get_affected_output_names, _set_affected_output_names)
    
    def _get_input_column_names(self):
        return self.__input_column_names
    def _set_input_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputColumnNames must be type")
        self.__input_column_names = value
    input_column_names = property(_get_input_column_names, _set_input_column_names)
    
    def _get_output_column_names(self):
        return self.__output_column_names
    def _set_output_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputColumnNames must be type")
        self.__output_column_names = value
    output_column_names = property(_get_output_column_names, _set_output_column_names)
    
    def _get_python_configs(self):
        return self.__python_configs
    def _set_python_configs(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("pythonConfigs must be type")
        self.__python_configs = value
    python_configs = property(_get_python_configs, _set_python_configs)
    
    def _get_target_input_step_input_names(self):
        return self.__target_input_step_input_names
    def _set_target_input_step_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepInputNames list valeus must be str")
        self.__target_input_step_input_names = value
    target_input_step_input_names = property(_get_target_input_step_input_names, _set_target_input_step_input_names)
    
    def _get_target_input_step_output_names(self):
        return self.__target_input_step_output_names
    def _set_target_input_step_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepOutputNames list valeus must be str")
        self.__target_input_step_output_names = value
    target_input_step_output_names = property(_get_target_input_step_output_names, _set_target_input_step_output_names)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__input_schemas is not None:
            d['inputSchemas']  =  self.__input_schemas.as_dict() if hasattr(self.__input_schemas, 'as_dict') else self.__input_schemas
        if self.__output_schemas is not None:
            d['outputSchemas']  =  self.__output_schemas.as_dict() if hasattr(self.__output_schemas, 'as_dict') else self.__output_schemas
        if self.__input_names is not None:
            d['inputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__input_names]
        if self.__output_names is not None:
            d['outputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__output_names]
        if self.__affected_input_names is not None:
            d['affectedInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_input_names]
        if self.__affected_output_names is not None:
            d['affectedOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_output_names]
        if self.__input_column_names is not None:
            d['inputColumnNames']  =  self.__input_column_names.as_dict() if hasattr(self.__input_column_names, 'as_dict') else self.__input_column_names
        if self.__output_column_names is not None:
            d['outputColumnNames']  =  self.__output_column_names.as_dict() if hasattr(self.__output_column_names, 'as_dict') else self.__output_column_names
        if self.__python_configs is not None:
            d['pythonConfigs']  =  self.__python_configs.as_dict() if hasattr(self.__python_configs, 'as_dict') else self.__python_configs
        if self.__target_input_step_input_names is not None:
            d['targetInputStepInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_input_names]
        if self.__target_input_step_output_names is not None:
            d['targetInputStepOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_output_names]
        return d





class TransformProcessPipelineStep(PipelineStep):

    _types_map = {
        'inputSchemas': {'type': dict, 'subtype': None},
        'outputSchemas': {'type': dict, 'subtype': None},
        'inputNames': {'type': list, 'subtype': str},
        'outputNames': {'type': list, 'subtype': str},
        'affectedInputNames': {'type': list, 'subtype': str},
        'affectedOutputNames': {'type': list, 'subtype': str},
        'inputColumnNames': {'type': dict, 'subtype': None},
        'outputColumnNames': {'type': dict, 'subtype': None},
        'transformProcesses': {'type': dict, 'subtype': None},
        'targetInputStepInputNames': {'type': list, 'subtype': str},
        'targetInputStepOutputNames': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'inputNames': 'table',
        'outputNames': 'table',
        'affectedInputNames': 'table',
        'affectedOutputNames': 'table',
        'targetInputStepInputNames': 'table',
        'targetInputStepOutputNames': 'table',
    }

    def __init__(self
            , input_schemas=None
            , output_schemas=None
            , input_names=None
            , output_names=None
            , affected_input_names=None
            , affected_output_names=None
            , input_column_names=None
            , output_column_names=None
            , transform_processes=None
            , target_input_step_input_names=None
            , target_input_step_output_names=None
            ):
        self.__input_schemas = input_schemas
        self.__output_schemas = output_schemas
        self.__input_names = input_names
        self.__output_names = output_names
        self.__affected_input_names = affected_input_names
        self.__affected_output_names = affected_output_names
        self.__input_column_names = input_column_names
        self.__output_column_names = output_column_names
        self.__transform_processes = transform_processes
        self.__target_input_step_input_names = target_input_step_input_names
        self.__target_input_step_output_names = target_input_step_output_names
    
    def _get_input_schemas(self):
        return self.__input_schemas
    def _set_input_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputSchemas must be type")
        self.__input_schemas = value
    input_schemas = property(_get_input_schemas, _set_input_schemas)
    
    def _get_output_schemas(self):
        return self.__output_schemas
    def _set_output_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputSchemas must be type")
        self.__output_schemas = value
    output_schemas = property(_get_output_schemas, _set_output_schemas)
    
    def _get_input_names(self):
        return self.__input_names
    def _set_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("inputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("inputNames list valeus must be str")
        self.__input_names = value
    input_names = property(_get_input_names, _set_input_names)
    
    def _get_output_names(self):
        return self.__output_names
    def _set_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("outputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("outputNames list valeus must be str")
        self.__output_names = value
    output_names = property(_get_output_names, _set_output_names)
    
    def _get_affected_input_names(self):
        return self.__affected_input_names
    def _set_affected_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedInputNames list valeus must be str")
        self.__affected_input_names = value
    affected_input_names = property(_get_affected_input_names, _set_affected_input_names)
    
    def _get_affected_output_names(self):
        return self.__affected_output_names
    def _set_affected_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedOutputNames list valeus must be str")
        self.__affected_output_names = value
    affected_output_names = property(_get_affected_output_names, _set_affected_output_names)
    
    def _get_input_column_names(self):
        return self.__input_column_names
    def _set_input_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputColumnNames must be type")
        self.__input_column_names = value
    input_column_names = property(_get_input_column_names, _set_input_column_names)
    
    def _get_output_column_names(self):
        return self.__output_column_names
    def _set_output_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputColumnNames must be type")
        self.__output_column_names = value
    output_column_names = property(_get_output_column_names, _set_output_column_names)
    
    def _get_transform_processes(self):
        return self.__transform_processes
    def _set_transform_processes(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("transformProcesses must be type")
        self.__transform_processes = value
    transform_processes = property(_get_transform_processes, _set_transform_processes)
    
    def _get_target_input_step_input_names(self):
        return self.__target_input_step_input_names
    def _set_target_input_step_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepInputNames list valeus must be str")
        self.__target_input_step_input_names = value
    target_input_step_input_names = property(_get_target_input_step_input_names, _set_target_input_step_input_names)
    
    def _get_target_input_step_output_names(self):
        return self.__target_input_step_output_names
    def _set_target_input_step_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepOutputNames list valeus must be str")
        self.__target_input_step_output_names = value
    target_input_step_output_names = property(_get_target_input_step_output_names, _set_target_input_step_output_names)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__input_schemas is not None:
            d['inputSchemas']  =  self.__input_schemas.as_dict() if hasattr(self.__input_schemas, 'as_dict') else self.__input_schemas
        if self.__output_schemas is not None:
            d['outputSchemas']  =  self.__output_schemas.as_dict() if hasattr(self.__output_schemas, 'as_dict') else self.__output_schemas
        if self.__input_names is not None:
            d['inputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__input_names]
        if self.__output_names is not None:
            d['outputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__output_names]
        if self.__affected_input_names is not None:
            d['affectedInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_input_names]
        if self.__affected_output_names is not None:
            d['affectedOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_output_names]
        if self.__input_column_names is not None:
            d['inputColumnNames']  =  self.__input_column_names.as_dict() if hasattr(self.__input_column_names, 'as_dict') else self.__input_column_names
        if self.__output_column_names is not None:
            d['outputColumnNames']  =  self.__output_column_names.as_dict() if hasattr(self.__output_column_names, 'as_dict') else self.__output_column_names
        if self.__transform_processes is not None:
            d['transformProcesses']  =  self.__transform_processes.as_dict() if hasattr(self.__transform_processes, 'as_dict') else self.__transform_processes
        if self.__target_input_step_input_names is not None:
            d['targetInputStepInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_input_names]
        if self.__target_input_step_output_names is not None:
            d['targetInputStepOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_output_names]
        return d





class ModelPipelineStep(PipelineStep):

    _types_map = {
        'inputSchemas': {'type': dict, 'subtype': None},
        'outputSchemas': {'type': dict, 'subtype': None},
        'inputNames': {'type': list, 'subtype': str},
        'outputNames': {'type': list, 'subtype': str},
        'affectedInputNames': {'type': list, 'subtype': str},
        'affectedOutputNames': {'type': list, 'subtype': str},
        'inputColumnNames': {'type': dict, 'subtype': None},
        'outputColumnNames': {'type': dict, 'subtype': None},
        'modelConfig': {'type': ModelConfig, 'subtype': None},
        'servingConfig': {'type': ServingConfig, 'subtype': None},
        'normalizationConfig': {'type': NormalizationConfig, 'subtype': None},
        'targetInputStepInputNames': {'type': list, 'subtype': str},
        'targetInputStepOutputNames': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'inputNames': 'table',
        'outputNames': 'table',
        'affectedInputNames': 'table',
        'affectedOutputNames': 'table',
        'targetInputStepInputNames': 'table',
        'targetInputStepOutputNames': 'table',
    }

    def __init__(self
            , input_schemas=None
            , output_schemas=None
            , input_names=None
            , output_names=None
            , affected_input_names=None
            , affected_output_names=None
            , input_column_names=None
            , output_column_names=None
            , model_config=None
            , serving_config=None
            , normalization_config=None
            , target_input_step_input_names=None
            , target_input_step_output_names=None
            ):
        self.__input_schemas = input_schemas
        self.__output_schemas = output_schemas
        self.__input_names = input_names
        self.__output_names = output_names
        self.__affected_input_names = affected_input_names
        self.__affected_output_names = affected_output_names
        self.__input_column_names = input_column_names
        self.__output_column_names = output_column_names
        self.__model_config = model_config
        self.__serving_config = serving_config
        self.__normalization_config = normalization_config
        self.__target_input_step_input_names = target_input_step_input_names
        self.__target_input_step_output_names = target_input_step_output_names
    
    def _get_input_schemas(self):
        return self.__input_schemas
    def _set_input_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputSchemas must be type")
        self.__input_schemas = value
    input_schemas = property(_get_input_schemas, _set_input_schemas)
    
    def _get_output_schemas(self):
        return self.__output_schemas
    def _set_output_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputSchemas must be type")
        self.__output_schemas = value
    output_schemas = property(_get_output_schemas, _set_output_schemas)
    
    def _get_input_names(self):
        return self.__input_names
    def _set_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("inputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("inputNames list valeus must be str")
        self.__input_names = value
    input_names = property(_get_input_names, _set_input_names)
    
    def _get_output_names(self):
        return self.__output_names
    def _set_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("outputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("outputNames list valeus must be str")
        self.__output_names = value
    output_names = property(_get_output_names, _set_output_names)
    
    def _get_affected_input_names(self):
        return self.__affected_input_names
    def _set_affected_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedInputNames list valeus must be str")
        self.__affected_input_names = value
    affected_input_names = property(_get_affected_input_names, _set_affected_input_names)
    
    def _get_affected_output_names(self):
        return self.__affected_output_names
    def _set_affected_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedOutputNames list valeus must be str")
        self.__affected_output_names = value
    affected_output_names = property(_get_affected_output_names, _set_affected_output_names)
    
    def _get_input_column_names(self):
        return self.__input_column_names
    def _set_input_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputColumnNames must be type")
        self.__input_column_names = value
    input_column_names = property(_get_input_column_names, _set_input_column_names)
    
    def _get_output_column_names(self):
        return self.__output_column_names
    def _set_output_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputColumnNames must be type")
        self.__output_column_names = value
    output_column_names = property(_get_output_column_names, _set_output_column_names)
    
    def _get_model_config(self):
        return self.__model_config
    def _set_model_config(self, value):
        if not isinstance(value, ModelConfig):
            raise TypeError("modelConfig must be ModelConfig")
        self.__model_config = value
    model_config = property(_get_model_config, _set_model_config)
    
    def _get_serving_config(self):
        return self.__serving_config
    def _set_serving_config(self, value):
        if not isinstance(value, ServingConfig):
            raise TypeError("servingConfig must be ServingConfig")
        self.__serving_config = value
    serving_config = property(_get_serving_config, _set_serving_config)
    
    def _get_normalization_config(self):
        return self.__normalization_config
    def _set_normalization_config(self, value):
        if not isinstance(value, NormalizationConfig):
            raise TypeError("normalizationConfig must be NormalizationConfig")
        self.__normalization_config = value
    normalization_config = property(_get_normalization_config, _set_normalization_config)
    
    def _get_target_input_step_input_names(self):
        return self.__target_input_step_input_names
    def _set_target_input_step_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepInputNames list valeus must be str")
        self.__target_input_step_input_names = value
    target_input_step_input_names = property(_get_target_input_step_input_names, _set_target_input_step_input_names)
    
    def _get_target_input_step_output_names(self):
        return self.__target_input_step_output_names
    def _set_target_input_step_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepOutputNames list valeus must be str")
        self.__target_input_step_output_names = value
    target_input_step_output_names = property(_get_target_input_step_output_names, _set_target_input_step_output_names)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__input_schemas is not None:
            d['inputSchemas']  =  self.__input_schemas.as_dict() if hasattr(self.__input_schemas, 'as_dict') else self.__input_schemas
        if self.__output_schemas is not None:
            d['outputSchemas']  =  self.__output_schemas.as_dict() if hasattr(self.__output_schemas, 'as_dict') else self.__output_schemas
        if self.__input_names is not None:
            d['inputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__input_names]
        if self.__output_names is not None:
            d['outputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__output_names]
        if self.__affected_input_names is not None:
            d['affectedInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_input_names]
        if self.__affected_output_names is not None:
            d['affectedOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_output_names]
        if self.__input_column_names is not None:
            d['inputColumnNames']  =  self.__input_column_names.as_dict() if hasattr(self.__input_column_names, 'as_dict') else self.__input_column_names
        if self.__output_column_names is not None:
            d['outputColumnNames']  =  self.__output_column_names.as_dict() if hasattr(self.__output_column_names, 'as_dict') else self.__output_column_names
        if self.__model_config is not None:
            d['modelConfig']  =  self.__model_config.as_dict() if hasattr(self.__model_config, 'as_dict') else self.__model_config
        if self.__serving_config is not None:
            d['servingConfig']  =  self.__serving_config.as_dict() if hasattr(self.__serving_config, 'as_dict') else self.__serving_config
        if self.__normalization_config is not None:
            d['normalizationConfig']  =  self.__normalization_config.as_dict() if hasattr(self.__normalization_config, 'as_dict') else self.__normalization_config
        if self.__target_input_step_input_names is not None:
            d['targetInputStepInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_input_names]
        if self.__target_input_step_output_names is not None:
            d['targetInputStepOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_output_names]
        return d





class ArrayConcatenationStep(PipelineStep):

    _types_map = {
        'inputSchemas': {'type': dict, 'subtype': None},
        'outputSchemas': {'type': dict, 'subtype': None},
        'inputNames': {'type': list, 'subtype': str},
        'outputNames': {'type': list, 'subtype': str},
        'affectedInputNames': {'type': list, 'subtype': str},
        'affectedOutputNames': {'type': list, 'subtype': str},
        'inputColumnNames': {'type': dict, 'subtype': None},
        'outputColumnNames': {'type': dict, 'subtype': None},
        'concatDimensions': {'type': dict, 'subtype': None},
        'targetInputStepInputNames': {'type': list, 'subtype': str},
        'targetInputStepOutputNames': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'inputNames': 'table',
        'outputNames': 'table',
        'affectedInputNames': 'table',
        'affectedOutputNames': 'table',
        'targetInputStepInputNames': 'table',
        'targetInputStepOutputNames': 'table',
    }

    def __init__(self
            , input_schemas=None
            , output_schemas=None
            , input_names=None
            , output_names=None
            , affected_input_names=None
            , affected_output_names=None
            , input_column_names=None
            , output_column_names=None
            , concat_dimensions=None
            , target_input_step_input_names=None
            , target_input_step_output_names=None
            ):
        self.__input_schemas = input_schemas
        self.__output_schemas = output_schemas
        self.__input_names = input_names
        self.__output_names = output_names
        self.__affected_input_names = affected_input_names
        self.__affected_output_names = affected_output_names
        self.__input_column_names = input_column_names
        self.__output_column_names = output_column_names
        self.__concat_dimensions = concat_dimensions
        self.__target_input_step_input_names = target_input_step_input_names
        self.__target_input_step_output_names = target_input_step_output_names
    
    def _get_input_schemas(self):
        return self.__input_schemas
    def _set_input_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputSchemas must be type")
        self.__input_schemas = value
    input_schemas = property(_get_input_schemas, _set_input_schemas)
    
    def _get_output_schemas(self):
        return self.__output_schemas
    def _set_output_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputSchemas must be type")
        self.__output_schemas = value
    output_schemas = property(_get_output_schemas, _set_output_schemas)
    
    def _get_input_names(self):
        return self.__input_names
    def _set_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("inputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("inputNames list valeus must be str")
        self.__input_names = value
    input_names = property(_get_input_names, _set_input_names)
    
    def _get_output_names(self):
        return self.__output_names
    def _set_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("outputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("outputNames list valeus must be str")
        self.__output_names = value
    output_names = property(_get_output_names, _set_output_names)
    
    def _get_affected_input_names(self):
        return self.__affected_input_names
    def _set_affected_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedInputNames list valeus must be str")
        self.__affected_input_names = value
    affected_input_names = property(_get_affected_input_names, _set_affected_input_names)
    
    def _get_affected_output_names(self):
        return self.__affected_output_names
    def _set_affected_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedOutputNames list valeus must be str")
        self.__affected_output_names = value
    affected_output_names = property(_get_affected_output_names, _set_affected_output_names)
    
    def _get_input_column_names(self):
        return self.__input_column_names
    def _set_input_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputColumnNames must be type")
        self.__input_column_names = value
    input_column_names = property(_get_input_column_names, _set_input_column_names)
    
    def _get_output_column_names(self):
        return self.__output_column_names
    def _set_output_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputColumnNames must be type")
        self.__output_column_names = value
    output_column_names = property(_get_output_column_names, _set_output_column_names)
    
    def _get_concat_dimensions(self):
        return self.__concat_dimensions
    def _set_concat_dimensions(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("concatDimensions must be type")
        self.__concat_dimensions = value
    concat_dimensions = property(_get_concat_dimensions, _set_concat_dimensions)
    
    def _get_target_input_step_input_names(self):
        return self.__target_input_step_input_names
    def _set_target_input_step_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepInputNames list valeus must be str")
        self.__target_input_step_input_names = value
    target_input_step_input_names = property(_get_target_input_step_input_names, _set_target_input_step_input_names)
    
    def _get_target_input_step_output_names(self):
        return self.__target_input_step_output_names
    def _set_target_input_step_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepOutputNames list valeus must be str")
        self.__target_input_step_output_names = value
    target_input_step_output_names = property(_get_target_input_step_output_names, _set_target_input_step_output_names)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__input_schemas is not None:
            d['inputSchemas']  =  self.__input_schemas.as_dict() if hasattr(self.__input_schemas, 'as_dict') else self.__input_schemas
        if self.__output_schemas is not None:
            d['outputSchemas']  =  self.__output_schemas.as_dict() if hasattr(self.__output_schemas, 'as_dict') else self.__output_schemas
        if self.__input_names is not None:
            d['inputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__input_names]
        if self.__output_names is not None:
            d['outputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__output_names]
        if self.__affected_input_names is not None:
            d['affectedInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_input_names]
        if self.__affected_output_names is not None:
            d['affectedOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_output_names]
        if self.__input_column_names is not None:
            d['inputColumnNames']  =  self.__input_column_names.as_dict() if hasattr(self.__input_column_names, 'as_dict') else self.__input_column_names
        if self.__output_column_names is not None:
            d['outputColumnNames']  =  self.__output_column_names.as_dict() if hasattr(self.__output_column_names, 'as_dict') else self.__output_column_names
        if self.__concat_dimensions is not None:
            d['concatDimensions']  =  self.__concat_dimensions.as_dict() if hasattr(self.__concat_dimensions, 'as_dict') else self.__concat_dimensions
        if self.__target_input_step_input_names is not None:
            d['targetInputStepInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_input_names]
        if self.__target_input_step_output_names is not None:
            d['targetInputStepOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_output_names]
        return d





class JsonExpanderTransform(PipelineStep):

    _types_map = {
        'inputSchemas': {'type': dict, 'subtype': None},
        'outputSchemas': {'type': dict, 'subtype': None},
        'inputNames': {'type': list, 'subtype': str},
        'outputNames': {'type': list, 'subtype': str},
        'affectedInputNames': {'type': list, 'subtype': str},
        'affectedOutputNames': {'type': list, 'subtype': str},
        'inputColumnNames': {'type': dict, 'subtype': None},
        'outputColumnNames': {'type': dict, 'subtype': None},
        'targetInputStepInputNames': {'type': list, 'subtype': str},
        'targetInputStepOutputNames': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'inputNames': 'table',
        'outputNames': 'table',
        'affectedInputNames': 'table',
        'affectedOutputNames': 'table',
        'targetInputStepInputNames': 'table',
        'targetInputStepOutputNames': 'table',
    }

    def __init__(self
            , input_schemas=None
            , output_schemas=None
            , input_names=None
            , output_names=None
            , affected_input_names=None
            , affected_output_names=None
            , input_column_names=None
            , output_column_names=None
            , target_input_step_input_names=None
            , target_input_step_output_names=None
            ):
        self.__input_schemas = input_schemas
        self.__output_schemas = output_schemas
        self.__input_names = input_names
        self.__output_names = output_names
        self.__affected_input_names = affected_input_names
        self.__affected_output_names = affected_output_names
        self.__input_column_names = input_column_names
        self.__output_column_names = output_column_names
        self.__target_input_step_input_names = target_input_step_input_names
        self.__target_input_step_output_names = target_input_step_output_names
    
    def _get_input_schemas(self):
        return self.__input_schemas
    def _set_input_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputSchemas must be type")
        self.__input_schemas = value
    input_schemas = property(_get_input_schemas, _set_input_schemas)
    
    def _get_output_schemas(self):
        return self.__output_schemas
    def _set_output_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputSchemas must be type")
        self.__output_schemas = value
    output_schemas = property(_get_output_schemas, _set_output_schemas)
    
    def _get_input_names(self):
        return self.__input_names
    def _set_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("inputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("inputNames list valeus must be str")
        self.__input_names = value
    input_names = property(_get_input_names, _set_input_names)
    
    def _get_output_names(self):
        return self.__output_names
    def _set_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("outputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("outputNames list valeus must be str")
        self.__output_names = value
    output_names = property(_get_output_names, _set_output_names)
    
    def _get_affected_input_names(self):
        return self.__affected_input_names
    def _set_affected_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedInputNames list valeus must be str")
        self.__affected_input_names = value
    affected_input_names = property(_get_affected_input_names, _set_affected_input_names)
    
    def _get_affected_output_names(self):
        return self.__affected_output_names
    def _set_affected_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedOutputNames list valeus must be str")
        self.__affected_output_names = value
    affected_output_names = property(_get_affected_output_names, _set_affected_output_names)
    
    def _get_input_column_names(self):
        return self.__input_column_names
    def _set_input_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputColumnNames must be type")
        self.__input_column_names = value
    input_column_names = property(_get_input_column_names, _set_input_column_names)
    
    def _get_output_column_names(self):
        return self.__output_column_names
    def _set_output_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputColumnNames must be type")
        self.__output_column_names = value
    output_column_names = property(_get_output_column_names, _set_output_column_names)
    
    def _get_target_input_step_input_names(self):
        return self.__target_input_step_input_names
    def _set_target_input_step_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepInputNames list valeus must be str")
        self.__target_input_step_input_names = value
    target_input_step_input_names = property(_get_target_input_step_input_names, _set_target_input_step_input_names)
    
    def _get_target_input_step_output_names(self):
        return self.__target_input_step_output_names
    def _set_target_input_step_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepOutputNames list valeus must be str")
        self.__target_input_step_output_names = value
    target_input_step_output_names = property(_get_target_input_step_output_names, _set_target_input_step_output_names)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__input_schemas is not None:
            d['inputSchemas']  =  self.__input_schemas.as_dict() if hasattr(self.__input_schemas, 'as_dict') else self.__input_schemas
        if self.__output_schemas is not None:
            d['outputSchemas']  =  self.__output_schemas.as_dict() if hasattr(self.__output_schemas, 'as_dict') else self.__output_schemas
        if self.__input_names is not None:
            d['inputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__input_names]
        if self.__output_names is not None:
            d['outputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__output_names]
        if self.__affected_input_names is not None:
            d['affectedInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_input_names]
        if self.__affected_output_names is not None:
            d['affectedOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_output_names]
        if self.__input_column_names is not None:
            d['inputColumnNames']  =  self.__input_column_names.as_dict() if hasattr(self.__input_column_names, 'as_dict') else self.__input_column_names
        if self.__output_column_names is not None:
            d['outputColumnNames']  =  self.__output_column_names.as_dict() if hasattr(self.__output_column_names, 'as_dict') else self.__output_column_names
        if self.__target_input_step_input_names is not None:
            d['targetInputStepInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_input_names]
        if self.__target_input_step_output_names is not None:
            d['targetInputStepOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_output_names]
        return d





class ImageLoading(PipelineStep):

    _types_map = {
        'inputSchemas': {'type': dict, 'subtype': None},
        'outputSchemas': {'type': dict, 'subtype': None},
        'inputNames': {'type': list, 'subtype': str},
        'outputNames': {'type': list, 'subtype': str},
        'affectedInputNames': {'type': list, 'subtype': str},
        'affectedOutputNames': {'type': list, 'subtype': str},
        'inputColumnNames': {'type': dict, 'subtype': None},
        'outputColumnNames': {'type': dict, 'subtype': None},
        'originalImageHeight': {'type': int, 'subtype': None},
        'originalImageWidth': {'type': int, 'subtype': None},
        'dimensionsConfigs': {'type': dict, 'subtype': None},
        'imageProcessingRequiredLayout': {'type': str, 'subtype': None},
        'imageProcessingInitialLayout': {'type': str, 'subtype': None},
        'imageTransformProcesses': {'type': dict, 'subtype': None},
        'objectDetectionConfig': {'type': ObjectDetectionConfig, 'subtype': None},
        'targetInputStepInputNames': {'type': list, 'subtype': str},
        'targetInputStepOutputNames': {'type': list, 'subtype': str},
    }
    _formats_map = {
        'inputNames': 'table',
        'outputNames': 'table',
        'affectedInputNames': 'table',
        'affectedOutputNames': 'table',
        'targetInputStepInputNames': 'table',
        'targetInputStepOutputNames': 'table',
    }

    def __init__(self
            , input_schemas=None
            , output_schemas=None
            , input_names=None
            , output_names=None
            , affected_input_names=None
            , affected_output_names=None
            , input_column_names=None
            , output_column_names=None
            , original_image_height=None
            , original_image_width=None
            , dimensions_configs=None
            , image_processing_required_layout=None
            , image_processing_initial_layout=None
            , image_transform_processes=None
            , object_detection_config=None
            , target_input_step_input_names=None
            , target_input_step_output_names=None
            ):
        self.__input_schemas = input_schemas
        self.__output_schemas = output_schemas
        self.__input_names = input_names
        self.__output_names = output_names
        self.__affected_input_names = affected_input_names
        self.__affected_output_names = affected_output_names
        self.__input_column_names = input_column_names
        self.__output_column_names = output_column_names
        self.__original_image_height = original_image_height
        self.__original_image_width = original_image_width
        self.__dimensions_configs = dimensions_configs
        self.__image_processing_required_layout = image_processing_required_layout
        self.__image_processing_initial_layout = image_processing_initial_layout
        self.__image_transform_processes = image_transform_processes
        self.__object_detection_config = object_detection_config
        self.__target_input_step_input_names = target_input_step_input_names
        self.__target_input_step_output_names = target_input_step_output_names
    
    def _get_input_schemas(self):
        return self.__input_schemas
    def _set_input_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputSchemas must be type")
        self.__input_schemas = value
    input_schemas = property(_get_input_schemas, _set_input_schemas)
    
    def _get_output_schemas(self):
        return self.__output_schemas
    def _set_output_schemas(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputSchemas must be type")
        self.__output_schemas = value
    output_schemas = property(_get_output_schemas, _set_output_schemas)
    
    def _get_input_names(self):
        return self.__input_names
    def _set_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("inputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("inputNames list valeus must be str")
        self.__input_names = value
    input_names = property(_get_input_names, _set_input_names)
    
    def _get_output_names(self):
        return self.__output_names
    def _set_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("outputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("outputNames list valeus must be str")
        self.__output_names = value
    output_names = property(_get_output_names, _set_output_names)
    
    def _get_affected_input_names(self):
        return self.__affected_input_names
    def _set_affected_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedInputNames list valeus must be str")
        self.__affected_input_names = value
    affected_input_names = property(_get_affected_input_names, _set_affected_input_names)
    
    def _get_affected_output_names(self):
        return self.__affected_output_names
    def _set_affected_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("affectedOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("affectedOutputNames list valeus must be str")
        self.__affected_output_names = value
    affected_output_names = property(_get_affected_output_names, _set_affected_output_names)
    
    def _get_input_column_names(self):
        return self.__input_column_names
    def _set_input_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("inputColumnNames must be type")
        self.__input_column_names = value
    input_column_names = property(_get_input_column_names, _set_input_column_names)
    
    def _get_output_column_names(self):
        return self.__output_column_names
    def _set_output_column_names(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("outputColumnNames must be type")
        self.__output_column_names = value
    output_column_names = property(_get_output_column_names, _set_output_column_names)
    
    def _get_original_image_height(self):
        return self.__original_image_height
    def _set_original_image_height(self, value):
        if not isinstance(value, int):
            raise TypeError("originalImageHeight must be int")
        self.__original_image_height = value
    original_image_height = property(_get_original_image_height, _set_original_image_height)
    
    def _get_original_image_width(self):
        return self.__original_image_width
    def _set_original_image_width(self, value):
        if not isinstance(value, int):
            raise TypeError("originalImageWidth must be int")
        self.__original_image_width = value
    original_image_width = property(_get_original_image_width, _set_original_image_width)
    
    def _get_dimensions_configs(self):
        return self.__dimensions_configs
    def _set_dimensions_configs(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("dimensionsConfigs must be type")
        self.__dimensions_configs = value
    dimensions_configs = property(_get_dimensions_configs, _set_dimensions_configs)
    
    def _get_image_processing_required_layout(self):
        return self.__image_processing_required_layout
    def _set_image_processing_required_layout(self, value):
        if not isinstance(value, str):
            raise TypeError("imageProcessingRequiredLayout must be str")
        self.__image_processing_required_layout = value
    image_processing_required_layout = property(_get_image_processing_required_layout, _set_image_processing_required_layout)
    
    def _get_image_processing_initial_layout(self):
        return self.__image_processing_initial_layout
    def _set_image_processing_initial_layout(self, value):
        if not isinstance(value, str):
            raise TypeError("imageProcessingInitialLayout must be str")
        self.__image_processing_initial_layout = value
    image_processing_initial_layout = property(_get_image_processing_initial_layout, _set_image_processing_initial_layout)
    
    def _get_image_transform_processes(self):
        return self.__image_transform_processes
    def _set_image_transform_processes(self, value):
        if not isinstance(value, dict) and not isinstance(value,DictWrapper) and not isinstance(value,DictWrapper):
            raise TypeError("imageTransformProcesses must be type")
        self.__image_transform_processes = value
    image_transform_processes = property(_get_image_transform_processes, _set_image_transform_processes)
    
    def _get_object_detection_config(self):
        return self.__object_detection_config
    def _set_object_detection_config(self, value):
        if not isinstance(value, ObjectDetectionConfig):
            raise TypeError("objectDetectionConfig must be ObjectDetectionConfig")
        self.__object_detection_config = value
    object_detection_config = property(_get_object_detection_config, _set_object_detection_config)
    
    def _get_target_input_step_input_names(self):
        return self.__target_input_step_input_names
    def _set_target_input_step_input_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepInputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepInputNames list valeus must be str")
        self.__target_input_step_input_names = value
    target_input_step_input_names = property(_get_target_input_step_input_names, _set_target_input_step_input_names)
    
    def _get_target_input_step_output_names(self):
        return self.__target_input_step_output_names
    def _set_target_input_step_output_names(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("targetInputStepOutputNames must be list")
        if not all(isinstance(i, str) for i in value):
            raise TypeError("targetInputStepOutputNames list valeus must be str")
        self.__target_input_step_output_names = value
    target_input_step_output_names = property(_get_target_input_step_output_names, _set_target_input_step_output_names)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__input_schemas is not None:
            d['inputSchemas']  =  self.__input_schemas.as_dict() if hasattr(self.__input_schemas, 'as_dict') else self.__input_schemas
        if self.__output_schemas is not None:
            d['outputSchemas']  =  self.__output_schemas.as_dict() if hasattr(self.__output_schemas, 'as_dict') else self.__output_schemas
        if self.__input_names is not None:
            d['inputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__input_names]
        if self.__output_names is not None:
            d['outputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__output_names]
        if self.__affected_input_names is not None:
            d['affectedInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_input_names]
        if self.__affected_output_names is not None:
            d['affectedOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__affected_output_names]
        if self.__input_column_names is not None:
            d['inputColumnNames']  =  self.__input_column_names.as_dict() if hasattr(self.__input_column_names, 'as_dict') else self.__input_column_names
        if self.__output_column_names is not None:
            d['outputColumnNames']  =  self.__output_column_names.as_dict() if hasattr(self.__output_column_names, 'as_dict') else self.__output_column_names
        if self.__original_image_height is not None:
            d['originalImageHeight']  =  self.__original_image_height.as_dict() if hasattr(self.__original_image_height, 'as_dict') else self.__original_image_height
        if self.__original_image_width is not None:
            d['originalImageWidth']  =  self.__original_image_width.as_dict() if hasattr(self.__original_image_width, 'as_dict') else self.__original_image_width
        if self.__dimensions_configs is not None:
            d['dimensionsConfigs']  =  self.__dimensions_configs.as_dict() if hasattr(self.__dimensions_configs, 'as_dict') else self.__dimensions_configs
        if self.__image_processing_required_layout is not None:
            d['imageProcessingRequiredLayout']  =  self.__image_processing_required_layout.as_dict() if hasattr(self.__image_processing_required_layout, 'as_dict') else self.__image_processing_required_layout
        if self.__image_processing_initial_layout is not None:
            d['imageProcessingInitialLayout']  =  self.__image_processing_initial_layout.as_dict() if hasattr(self.__image_processing_initial_layout, 'as_dict') else self.__image_processing_initial_layout
        if self.__image_transform_processes is not None:
            d['imageTransformProcesses']  =  self.__image_transform_processes.as_dict() if hasattr(self.__image_transform_processes, 'as_dict') else self.__image_transform_processes
        if self.__object_detection_config is not None:
            d['objectDetectionConfig']  =  self.__object_detection_config.as_dict() if hasattr(self.__object_detection_config, 'as_dict') else self.__object_detection_config
        if self.__target_input_step_input_names is not None:
            d['targetInputStepInputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_input_names]
        if self.__target_input_step_output_names is not None:
            d['targetInputStepOutputNames']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__target_input_step_output_names]
        return d





class InferenceConfiguration(object):

    _types_map = {
        'pipelineSteps': {'type': list, 'subtype': PipelineStep},
        'servingConfig': {'type': ServingConfig, 'subtype': None},
    }
    _formats_map = {
        'pipelineSteps': 'table',
    }

    def __init__(self
            , pipeline_steps=None
            , serving_config=None
            ):
        self.__pipeline_steps = pipeline_steps
        self.__serving_config = serving_config
    
    def _get_pipeline_steps(self):
        return self.__pipeline_steps
    def _set_pipeline_steps(self, value):
        if not isinstance(value, list) and not isinstance(value,ListWrapper):
            raise TypeError("pipelineSteps must be list")
        if not all(isinstance(i, PipelineStep) for i in value):
            raise TypeError("pipelineSteps list valeus must be PipelineStep")
        self.__pipeline_steps = value
    pipeline_steps = property(_get_pipeline_steps, _set_pipeline_steps)
    
    def _get_serving_config(self):
        return self.__serving_config
    def _set_serving_config(self, value):
        if not isinstance(value, ServingConfig):
            raise TypeError("servingConfig must be ServingConfig")
        self.__serving_config = value
    serving_config = property(_get_serving_config, _set_serving_config)
    
    def as_dict(self):
        d = dict_with_type(self)
        if self.__pipeline_steps is not None:
            d['pipelineSteps']  =  [p.as_dict() if hasattr(p, 'as_dict') else p for p in self.__pipeline_steps]
        if self.__serving_config is not None:
            d['servingConfig']  =  self.__serving_config.as_dict() if hasattr(self.__serving_config, 'as_dict') else self.__serving_config
        return d
