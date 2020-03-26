# -*- coding: utf-8 -*-
"""
Created on Mon Mar 23 20:28:22 2020

@author: Shiva
"""

import numpy as np
#import tensorflow as tf
import tensorflow.compat.v1 as tf
tf.disable_v2_behavior()
import matplotlib.pyplot as plt
import json
import os, sys
work_dir = os.path.abspath("./src/test/resources/scripts/TensorFlow")
sys.path.append(work_dir)
print("Working Directory", work_dir)

# read file
#with open('TensorFlowRegressionInput.json', 'r') as myfile:
with open(JsonInput, 'r') as myfile:
    data=myfile.read()

# parse file
obj = json.loads(data)
print(obj)
print("Data: " + str(obj['Data']))
Xj = obj['Data']
Yj = obj['Data1']
print(Xj)

Xt = []
X = []
for x in range(0,4):
    Xt.append(Xj[x])
X.append(Xt)
    
Yt = []
Y = []
for y in range(0,4):
    Yt.append(Yj[y])
    
Y.append(Yt)

Xr = np.array([Xj[0], Xj[1], Xj[2], Xj[3], Xj[4]], dtype = float)
Yr = np.array([Yj[0], Yj[1], Yj[2], Yj[3], Yj[4]], dtype = float)

def getDataFromJson():
    x_batch = Xr
    y_batch = Yr
    return x_batch, y_batch
 
def generate_dataset():
	x_batch = np.linspace(0, 2, 100)
	y_batch = 1.5 * x_batch + np.random.randn(*x_batch.shape) * 0.2 + 0.5
	return x_batch, y_batch

def linear_regression():
	x = tf.placeholder(tf.float32, shape=(None, ), name='x')
	y = tf.placeholder(tf.float32, shape=(None, ), name='y')

	with tf.variable_scope('lreg') as scope:
		w = tf.Variable(np.random.normal(), name='W')
		b = tf.Variable(np.random.normal(), name='b')
		
		y_pred = tf.add(tf.multiply(w, x), b)

		loss = tf.reduce_mean(tf.square(y_pred - y))

	return x, y, y_pred, loss


#x_batch, y_batch = generate_dataset()
x_batch, y_batch = getDataFromJson()

x, y, y_pred, loss = linear_regression()

optimizer = tf.train.GradientDescentOptimizer(0.1)
train_op = optimizer.minimize(loss)

with tf.Session() as session:
	session.run(tf.global_variables_initializer())

	feed_dict = {x: x_batch, y: y_batch}
		
	for i in range(30):
		_ = session.run(train_op, feed_dict)
		#print(i, "loss:", loss.eval(feed_dict))


	print('Predicting')
	y_pred_batch = session.run(y_pred, {x : x_batch})

output_var = y_pred_batch.reshape(1,5)
print(output_var)
#plt.scatter(x_batch, y_batch)
#plt.plot(x_batch, y_pred_batch, color='red')
#plt.xlim(0, 2)
#plt.ylim(0, 2)
#plt.savefig('plot.png')

