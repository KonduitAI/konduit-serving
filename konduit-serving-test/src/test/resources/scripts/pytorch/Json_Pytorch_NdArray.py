import os
import numpy as np
import torch
from torch import nn
from torch.autograd import Variable
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score
from keras.utils import to_categorical
import torch.nn.functional as F
work_dir = os.path.abspath(".")

import json

# read file
with open(JsonInput, 'r') as myfile:
    data=myfile.read()

# parse file
obj = json.loads(data)
Xj = obj['Iris']
Yj = obj['IrisY']

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

class Model(nn.Module):
    def __init__(self, input_dim):
        super(Model, self).__init__()
        self.layer1 = nn.Linear(input_dim,50)
        self.layer2 = nn.Linear(50, 20)
        self.layer3 = nn.Linear(20, 3)
        
    def forward(self, x):
        x = F.relu(self.layer1(x))
        x = F.relu(self.layer2(x))
        x = F.softmax(self.layer3(x)) # To check with the loss function
        return x
    
    
features, labels = load_iris(return_X_y=True)

features_train,features_test, labels_train, labels_test = train_test_split(features, labels, random_state=42, shuffle=True)

# Training
model = Model(features_train.shape[1])
optimizer = torch.optim.Adam(model.parameters(), lr=0.01)
loss_fn = nn.CrossEntropyLoss()
epochs = 100

def print_(loss):
    print ("The loss calculated: ", loss)
    
    
# Not using dataloader
x_train, y_train = Variable(torch.from_numpy(features_train)).float(), Variable(torch.from_numpy(labels_train)).long()
for epoch in range(1, epochs+1):
    y_pred = model(x_train)
    loss = loss_fn(y_pred, y_train)
    
    # Zero gradients
    optimizer.zero_grad()
    loss.backward() # Gradients
    optimizer.step() # Update
    
    
# Prediction
x_test = Variable(torch.from_numpy(features_test)).float()
my_test = Variable(torch.from_numpy(Xr)).float()
output_pred1 = model(my_test)
output_pred1 = output_pred1.detach().numpy()
output_value=np.array([np.argmax(output_pred1)])