import os
import numpy as np
from keras.models import load_model
import keras.backend.tensorflow_backend as tb
tb._SYMBOLIC_SCOPE.value = True
work_dir = os.path.abspath(".")

class KerasTest:
    def __init__(self):
        self.model = load_model(os.path.join(work_dir, "src/test/resources/inference/keras/model_ndarray_in_ndarray_out.h5"))

    def test(self, inputarray):
        arr = self.model.predict(inputarray)
        return arr

my_test = inputValue
objkeras = KerasTest()
arr = objkeras.test(my_test)
output_np = np.array(arr)