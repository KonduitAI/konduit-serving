try:
    import PIL.Image
    __PIL_INSTALLED__ = True
except ImportError:
    __PIL_INSTALLED__ = False
import numpy as np


class BoundingBox(object):
    def __init__(self, cx, cy, height, width, label=None, probability=None):
        self.cx = cx
        self.cy = cy
        self.height = height
        self.width = width
        self.label = label
        self.probability = probability

    @classmethod
    def fromXY(cls, x1, y1, x2, y2, label=None, probability=None):
        cx = 0.5 * (x1 + x2)
        cy = 0.5 * (y1 + y2)
        w = x2 - x1
        h = y2 - y1
        return cls(cx, cy, h, w, label, probability)

    @classmethod
    def fromCWH(cls, cx, cy, height, width, label=None, probability=None):
        return cls(cx, cy, height, width, label, probability)


class Data(object):
    @staticmethod
    def _check_type(item):
        atomic_types = (str, int, float, bool, bytes, memoryview, bytearray, Data, BoundingBox, np.ndarray)
        if __PIL_INSTALLED__:
            atomic_types += (PIL.Image.Image,)
        if isinstance(item, atomic_types):
            pass
        elif isinstance(item, (list, tuple)):
            if not item:
                raise Exception("Empty list not allowed.")
            if len(set(map(type, item))) > 1:
                raise Exception("All items in list should have same type.")
            subitem = item[0]
            if isinstance(subitem, atomic_types):
                pass
            elif isinstance(subitem, (list, tuple)):
                for x in item:
                    _check_type(item)
            else:
                types_str = ', '.join([tp.__name__ for tp in atomic_types])
                raise Exception("Unsupported type: " + type(subitem).__name__ + ". Supported types are: " + types_str + ".")
        else:
            types_str = ', '.join([tp.__name__ for tp in atomic_types])
            raise Exception("Unsupported type: " + type(item).__name__ + ". Supported types are: " + types_str + ".")

    def __init__(self):
        self.map = {}
        self.meta = None

    def __setitem__(self, key, value):
        assert isinstance(key, str)
        Data._check_type(value)
        if isinstance(value, (bytes, bytearray)):
            value = memoryview(value)
        self.map[key] = value

    def __getitem__(self, key):
        return self.map[key]

    def __contains__(self, key):
        return key in self.map

    def keys(self):
        return self.map.keys()

    def values(self):
        return self.map.values()

    def items(self):
        return self.map.items()
