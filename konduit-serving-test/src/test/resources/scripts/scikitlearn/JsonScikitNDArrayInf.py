import os
import sys
import numpy as np
import json
work_dir = os.path.abspath("./src/test/resources/scripts/scikitlearn")
sys.path.append(work_dir)

# read file
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


import pickle
filename =work_dir+'\\'+'pickle_object.pkl'

with open(filename, 'rb') as file:
    pickle_model = pickle.load(file)

 # Calculate the accuracy score and predict target values
Ypredict = pickle_model.predict(Xr).astype('float')