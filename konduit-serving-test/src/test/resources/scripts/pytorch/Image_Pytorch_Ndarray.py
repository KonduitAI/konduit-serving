import numpy as np
import torch
from torch.autograd import Variable
import torchvision
import matplotlib.pyplot as plt
from time import time
from torchvision import datasets, transforms
from torch import nn, optim
import os, sys
work_dir = os.path.abspath(".")
sys.path.append(work_dir)
print("work_dir",work_dir)

transform = transforms.Compose([transforms.ToTensor(),
                              transforms.Normalize((0.5,), (0.5,)),
                              ])


trainset = datasets.MNIST('C:\\', download=True, train=True, transform=transform)
valset = datasets.MNIST('C:\\', download=True, train=False, transform=transform)
trainloader = torch.utils.data.DataLoader(trainset, batch_size=64, shuffle=True)
valloader = torch.utils.data.DataLoader(valset, batch_size=64, shuffle=True)

dataiter = iter(trainloader)
images, labels = dataiter.next()

print(images.shape)
print(labels.shape)

#plt.imshow(images[0].numpy().squeeze(), cmap='gray_r')

#figure = plt.figure()
#num_of_images = 20
#for index in range(1, num_of_images + 1):
#    plt.subplot(6, 10, index)
 #   plt.axis('off')
  #  plt.imshow(images[index].numpy().squeeze(), cmap='gray_r')
    
    
input_size = 784
hidden_sizes = [128, 64]
output_size = 10

model = nn.Sequential(nn.Linear(input_size, hidden_sizes[0]),
                      nn.ReLU(),
                      nn.Linear(hidden_sizes[0], hidden_sizes[1]),
                      nn.ReLU(),
                      nn.Linear(hidden_sizes[1], output_size),
                      nn.LogSoftmax(dim=1))
#print(model)

criterion = nn.NLLLoss()
images, labels = next(iter(trainloader))
images = images.view(images.shape[0], -1)

logps = model(images) #log probabilities
loss = criterion(logps, labels) #calculate the NLL loss

print('Before backward pass: \n', model[0].weight.grad)
loss.backward()
print('After backward pass: \n', model[0].weight.grad)

optimizer = optim.SGD(model.parameters(), lr=0.003, momentum=0.9)
time0 = time()
epochs = 3
for e in range(epochs):
    running_loss = 0
    for images, labels in trainloader:
        # Flatten MNIST images into a 784 long vector
        images = images.view(images.shape[0], -1)
    
        # Training pass
        optimizer.zero_grad()
        
        output = model(images)
        loss = criterion(output, labels)
        
        #This is where the model learns by backpropagating
        loss.backward()
        
        #And optimizes its weights here
        optimizer.step()
        
        running_loss += loss.item()
    else:
        print("Epoch {} - T loss: {}".format(e, running_loss/len(trainloader)))
print("\n T Time (in minutes) =",(time()-time0)/60)

images, labels = next(iter(valloader))

img = np.array(image_input).reshape(1, 784)
img_inp = Variable(torch.from_numpy(img))

with torch.no_grad():
    logps = model(img_inp)

ps = torch.exp(logps)
probab = list(ps.numpy()[0])
predicted_Ouput = np.array([probab.index(max(probab))])

print("Predicted Digit =",predicted_Ouput)
#view_classify(img.view(1, 28, 28), ps)

correct_count, all_count = 0, 0
for images,labels in valloader:
  for i in range(len(labels)):
    img = images[i].view(1, 784)
    with torch.no_grad():
        logps = model(img)

    
    ps = torch.exp(logps)
    probab = list(ps.numpy()[0])
    pred_label = probab.index(max(probab))
    true_label = labels.numpy()[i]
    if(true_label == pred_label):
      correct_count += 1
    all_count += 1

print("Number Of Images Tested =", all_count)
print("\nModel Accuracy =", (correct_count/all_count))