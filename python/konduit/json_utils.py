def has_as_dict_attribute(python_object):
    """Checks if an object has a callable "as_dict" attribute

    :param python_object: any Python object
    """
    if not hasattr(python_object.__class__, "as_dict") and callable(
        getattr(python_object.__class__, "as_dict")
    ):
        raise AttributeError("Passed in Python object does not have an as_dict method.")


def _invoke_setter_on(input_object, method_name, value):
    """ If a given input object has the given setter method
    name, use this method to set "value" on the object.

    :param input_object: Python object
    :param method_name: name of the setter method
    :param value: value to set
    """
    remove = "_" + type(input_object).__name__ + "__"
    real_method_name = method_name.replace(remove, "")
    setter_method = "_set_" + real_method_name
    if hasattr(input_object, setter_method):
        method = getattr(input_object, setter_method)
        method(value)


def _ensure_serializable(input_config):
    """Modifies a given input configuration, if necessary,
    to make it serializable.
    """
    if not (
        hasattr(input_config, "__dict__")
        or type(input_config) is DictWrapper
        or type(input_config) is ListWrapper
    ):
        return

    # iterate over all config properties
    for setter_method, value in vars(input_config).items():
        if type(value) is dict:
            _invoke_setter_on(input_config, setter_method, DictWrapper(value))
        elif type(value) is list:
            for item in value:
                _ensure_serializable(item)
            _invoke_setter_on(input_config, setter_method, ListWrapper(value))

        elif hasattr(value, "as_dict"):
            _ensure_serializable(value)


def config_to_dict_with_type(inference_config):
    """
    Converts an inference configuration to a Python dictionary
    with '@type' key.

    :param inference_config: InferenceConfig object
    :return: Python dict
    """
    has_as_dict_attribute(inference_config)
    _ensure_serializable(inference_config)
    input_dict = inference_config.as_dict()
    input_dict["@type"] = inference_config.__class__.__name__
    return input_dict


def empty_type_dict(input_object):
    """Generates an otherwise empty Python dict with the correct
    "@type" key from Java.

    :param input_class: arbitrary instance of a Python class
    :return:
    """
    d = dict()
    d["@type"] = input_object.__class__.__name__
    return d


class DictWrapper:
    """Wraps a Python dictionary. The `as_dict` method unrolls
    to a regular Python dict.
    """

    def __init__(self, input_dict):
        assert type(input_dict) is dict, "Input type must be a dictionary!"
        self.input_dict = input_dict

    def as_dict(self):
        ret = {}
        for key, value in self.input_dict.items():
            if hasattr(value, "as_dict"):
                ret[key] = value.as_dict()
            else:
                ret[key] = value

        return ret


class ListWrapper:
    """Wraps a Python list. The `as_dict` method should not
    be used. It's there for duck-type-style checks of Java
    classes.
    """

    def __init__(self, input_list):
        assert type(input_list) is list, "Input type must be a list!"
        self.input_list = input_list

    def as_dict(self):
        return self

    def as_list(self):
        return self.input_list

    def __iter__(self):
        return self.input_list.__iter__()
