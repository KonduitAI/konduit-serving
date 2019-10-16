import sys
sys.version = '3.7.3 | packaged by conda-forge | (default, Jul  1 2019, 21:52:21)\n[GCC 7.3.0]'
from keras.applications.resnet50 import ResNet50
from keras.preprocessing import image
from keras.applications.resnet50 import preprocess_input, decode_predictions
import numpy as np
import tensorflow as tf
import keras

sess = tf.Session()
keras.backend.set_session(sess)

model = ResNet50(weights='imagenet')

#img_path = 'African_Bush_Elephant_1-783x1024.jpg'
img = image.load_img(img_path, target_size=(224, 224))
x = image.img_to_array(img)
x = np.expand_dims(x, axis=0)
x = preprocess_input(x)

preds = model.predict(x)
decoded = decode_predictions(preds, top=3)[0]
id,label,proba = decoded[0]
proba = str(proba)