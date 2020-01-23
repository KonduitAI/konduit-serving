import os

from keras.models import Sequential
from keras.layers import Dense
from keras.models import model_from_json
import numpy as np

work_dir = os.path.abspath(".")
print("Work Directory", work_dir)
import json

# read file
#JsonInput=""
#with open('..\\Json\\IrisY.json', 'r') as myfile:
with open(JsonInput, 'r') as myfile:
    data=myfile.read()

# parse file
obj = json.loads(data)
print(obj)
print("Iris: " + str(obj['Iris']))
Xj = obj['Iris']
Yj = obj['IrisY']
print(Xj)

Xt = []
X = []
for x in range(0,4):
    Xt.append(Xj[x])
X.append(Xt)
    
Yt = []
Y = []
for y in range(0,3):
    Yt.append(Yj[y])
    
Y.append(Yt)

Xr = np.array([[Xj[0], Xj[1], Xj[2], Xj[3]],[Xj[0], Xj[1], Xj[2], Xj[3]]] , dtype = float)
Yr = np.array([[0,0,1],[0,0,1]], dtype = float)

#dataset = numpy.loadtxt("pima-indians-diabetes.csv", delimiter=",")
# split into input (X) and output (Y) variables
#X = dataset[:,0:4]
#Y = dataset[:,1]
# load json and create model
json_file = open('src\\test\\resources\\Json\\KerasJsonmodel.json', 'r')
loaded_model_json = json_file.read()
json_file.close()
loaded_model = model_from_json(loaded_model_json)
# load weights into new model
loaded_model.load_weights("src\\test\\resources\\inference\\keras\\KerasJsonModel.h5")

# evaluate loaded model on test data
loaded_model.compile(loss='binary_crossentropy', optimizer='rmsprop', metrics=['accuracy'])
score = loaded_model.evaluate(Xr, Yr, verbose=0)
print("%s: %.2f%%" % (loaded_model.metrics_names[1], score[1]*100))
