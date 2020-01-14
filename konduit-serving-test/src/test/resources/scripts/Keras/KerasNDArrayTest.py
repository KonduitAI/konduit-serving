import os

import numpy as np
from keras.models import Sequential
from keras.layers import Dense
from keras.models import load_model
work_dir = os.path.abspath(".")

class KerasTest:
    def __init__(self):
        self.model = load_model(os.path.join(work_dir, "src\\test\\resources\\inference\\keras\\model_ndarray_in_ndarray_out.h5"))

    def test(self,inputarray):
        arr = self.model.predict(inputarray)
        return arr

objKeras = KerasTest ()
print("my_test---", default)
arr = objKeras.test(default)
print(arr)
