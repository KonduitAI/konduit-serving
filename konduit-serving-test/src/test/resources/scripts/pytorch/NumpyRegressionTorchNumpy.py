import torch
from torch.autograd import Variable 
import numpy as np


Xj = np.array([[1],[2],[3]])

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
output_numpy = np.array(pred_y.detach().numpy())