def _check_input_for_as_dict(input):
    if not hasattr(input.__class__, 'as_dict') and callable(getattr(input.__class__, 'as_dict')):
        raise AttributeError('Passed in object does not have an as_dict method.')

def _invoke_setter_on(input_val,property_name,value):
    remove = '_' + type(input_val).__name__ + '__'
    real_property_name = property_name.replace(remove, '')
    setter_method = '_set_' + real_property_name
    if hasattr(input_val, setter_method):
        method = getattr(input_val, setter_method)
        method(value)


def _ensure_serializable(input_config):
    if not hasattr(input_config, '__dict__' or type(input_config) is DictWrapper or type(input_config) is ListWrapper):
        return

    for property_name, value in vars(input_config).items():
        if type(value) is dict:
            _invoke_setter_on(input_config,property_name,DictWrapper(value))
        elif type(value) is list:
            for item in value:
                _ensure_serializable(item)
            _invoke_setter_on(input_config,property_name,ListWrapper(value))

        elif hasattr(value, 'as_dict'):
            _ensure_serializable(value)


def json_with_type(input_config):
    _check_input_for_as_dict(input_config)
    _ensure_serializable(input_config)
    input_dict = input_config.as_dict()
    input_dict['@type'] = input_config.__class__.__name__
    return input_dict

def dict_with_type(input2):
    d = dict()
    d['@type'] = input2.__class__.__name__
    return d

class DictWrapper:
    def __init__(self, input_dict):
        assert type(input_dict) is dict, 'Input type must be a dictionary!'
        self.input_dict = input_dict

    def as_dict(self):
        ret = {}
        for key,value in self.input_dict.items():
            if hasattr(value,'as_dict'):
                ret[key] = value.as_dict()
            else:
                ret[key] = value

        return ret

class ListWrapper:
    def __init__(self, input_list):
        assert type(input_list) is list, 'Input type must be a dictionary!'
        self.input_list = input_list

    def as_dict(self):
        return self

    def __iter__(self):
        return self.input_list.__iter__()

