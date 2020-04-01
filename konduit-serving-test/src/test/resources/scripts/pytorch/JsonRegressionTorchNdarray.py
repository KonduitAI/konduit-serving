import torch
from torch.autograd import Variable 
import numpy as np

import json 

# read file
with open('Regression.json', 'r') as myfile:
    data=myfile.read()

# parse file
obj = json.loads(data)
Xj = obj['Data']
Yj = obj['Data1']

Xr = np.array([[Xj[0]], [Xj[1]],[Xj[2]],[Xj[3]],[Xj[4]],[Xj[5]],[Xj[6]],[Xj[7]],[Xj[8]],[Xj[9]],[Xj[0]], [Xj[1]],[Xj[2]],[Xj[3]],[Xj[4]],[Xj[5]],[Xj[6]],[Xj[7]],[Xj[8]],[Xj[9]]])


x_data = Variable(torch.Tensor([[Xj[0]], [Xj[1]], [Xj[2]]]))
y_data = Variable(torch.Tensor([[Xj[0]], [Xj[1]], [Xj[2]]]))

class LinearRegressionModel(torch.nn.Module): 
  
    def __init__(self): 
        super(LinearRegressionModel, self).__init__() 
        self.linear = torch.nn.Linear(1, 1)  # One in and one out 
  
    def forward(self, x): 
        y_pred = self.linear(x) 
        return y_pred 
    
our_model = LinearRegressionModel() 
criterion = torch.nn.MSELoss(size_average = False) 
optimizer = torch.optim.SGD(our_model.parameters(), lr = 0.01)

for epoch in range(500): 
  
    # Forward pass: Compute predicted y by passing  
    # x to the model 
    pred_y = our_model(x_data) 
  
    # Compute and print loss 
    loss = criterion(pred_y, y_data) 
  
    # Zero gradients, perform a backward pass,  
    # and update the weights. 
    optimizer.zero_grad() 
    loss.backward() 
    optimizer.step()

new_var = Variable(torch.Tensor([[4.0]])) 
pred_y = our_model(new_var)
output = pred_y