class Data(object):
    def __init__(self):
        self.map = {}
        self.meta = None

    def __setitem__(self, key, value):
        pass

    def __getitem__(self, key):
        return self.map[key]

    def __contains__(self, key):
        return key in self.map


