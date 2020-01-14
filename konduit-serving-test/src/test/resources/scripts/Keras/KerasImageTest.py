import numpy as np
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import cv2

# let's keep our keras backend tensorflow quiet
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='3'
# for testing on CPU
#os.environ['CUDA_VISIBLE_DEVICES'] = ''

# keras imports for the dataset and building our neural network
from keras.datasets import mnist
from keras.models import Sequential, load_model
from keras.layers.core import Dense, Dropout, Activation
from keras.utils import np_utils
work_dir = os.path.abspath(".")

class KerasTestImage:

    def __init__(self):
         self.mnist_model = load_model(os.path.join(work_dir, "src\\test\\resources\\inference\\keras\\keras_Image_NDArray.h5"))

    def test(self,inputImg):
        predicted_classes = self.mnist_model.predict_classes(inputImg)
        return predicted_classes

TestImg = KerasTestImage ()
XTestImg1 = imgPath.reshape(1, 784)
imageArray = TestImg.test(XTestImg1)
print("imageArray",imageArray)