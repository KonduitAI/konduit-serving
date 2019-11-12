from konduit.client import Client

import numpy as np
import time
import random

import sys
import base64
import cv2
import json

from jnius import autoclass

input_names = ['default']
output_names = ['default']
#port = random.randint(1000, 65535)
port = 9233

client = Client(input_names=input_names,
                output_names=output_names,
                input_type='JSON',
                return_output_type='JSON',
                endpoint_output_type='RAW',
                url='http://localhost:' + str(port))

def encodeToBase64(filename):
    with open(filename, "rb") as f:
        img_base64 = base64.b64encode(f.read())
    return str(img_base64)[2:-1]  # remove b'...'

"""
workdir = '/home/ubuntu/work/Ultra-Light-Fast-Generic-Face-Detector-1MB'
image = encodeToBase64(workdir + '/imgs/1.jpg')
#orig_image = cv2.imread(workdir + '/imgs/1.jpg')
#image = cv2.cvtColor(orig_image, cv2.COLOR_BGR2RGB)
#image = image.astype(np.float32)
#print(image.shape)
#image /= 255.
print('type(image)', type(image))
data_input = {
    #'default': np.load('./data/input-0.npy'),
    'default': image
}
#print(type(orig_image))
"""
image = 'test'
#ArrayList = autoclass("java.util.ArrayList")
try:
    #predicted = client.predict(data_input)
    predicted = client.predict({'default': 'test'})
    print(predicted)
    #server.stop()
except Exception as e:
    print(e)
    #server.stop()