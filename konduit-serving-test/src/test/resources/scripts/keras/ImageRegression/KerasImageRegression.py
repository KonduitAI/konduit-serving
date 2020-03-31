# -*- coding: utf-8 -*-
"""
Created on Thu Mar 19 12:51:05 2020

@author: SHIVA
"""

from keras.optimizers import Adam
from sklearn.model_selection import train_test_split
import numpy as np
import argparse
import locale
from keras.models import Sequential
from keras.layers import Dense
from keras.models import model_from_json
import numpy
import cv2
import os, sys
work_dir = os.path.abspath("./src/test/resources/scripts/keras/ImageRegression")
sys.path.append(work_dir)

from pyimagesearch import datasets
from pyimagesearch import models

json_file = open('model.json', 'r')
loaded_model_json = json_file.read()
json_file.close()
loaded_model = model_from_json(loaded_model_json)
inputPath1 = "HousesInfo.txt"
df = datasets.load_house_attributes(inputPath1)
inputPath = ".\\HousesDataset"
images = datasets.load_house_images(df,inputPath )
images = images / 255.0

#print(img_input)
preds = loaded_model.predict(images)
preds_output = preds
preds_output[abs(preds) > 0.0] = 1.0