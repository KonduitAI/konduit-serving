from konduit import *
from konduit.json_utils import *
import pytest


def test_empty_dict_type():
    d1 = empty_type_dict(InferenceConfiguration())
    d2 = {"@type": "InferenceConfiguration"}
    assert d1 == d2


def test_dict_wrapper():
    d = {"foo": {"bar": "baz"}}
    dw = DictWrapper(d)

    assert d == dw.as_dict()


def test_list_wrapper():
    l = ['a', 'b', 'c']
    lw = ListWrapper(l)

    # This is a national tragedy, but OK
    assert lw == lw.as_dict()

    assert l == lw.as_list()

    x = ''
    for i in lw:
        x = i
    assert x == 'c'


def test_as_dict_checker():

    class FooCallable(object):
        def __init__(self):
            pass

        def as_dict(self):
            pass

    class FooNotCallable(object):
        def __init__(self):
            self.as_dict = {}

    has_as_dict_attribute(FooCallable())

    with pytest.raises(Exception):
        has_as_dict_attribute(FooNotCallable())
