import numpy as np
import tensorflow.compat.v1 as tf
tf.disable_v2_behavior()
import os, sys
work_dir = os.path.abspath("./src/test/resources/scripts/TensorFlow")
sys.path.append(work_dir)

def linear_regression():
	x = tf.placeholder(tf.float32, shape=(None, ), name='x')
	y = tf.placeholder(tf.float32, shape=(None, ), name='y')

	with tf.variable_scope('lreg') as scope:
		w = tf.Variable(np.random.normal(), name='W')
		b = tf.Variable(np.random.normal(), name='b')
		
		y_pred = tf.add(tf.multiply(w, x), b)

		loss = tf.reduce_mean(tf.square(y_pred - y))

	return x, y, y_pred, loss

x_batch = input_numpy
y_batch = input_numpy
x, y, y_pred, loss = linear_regression()

optimizer = tf.train.GradientDescentOptimizer(0.1)
train_op = optimizer.minimize(loss)

with tf.Session() as session:
	session.run(tf.global_variables_initializer())

	feed_dict = {x: x_batch, y: y_batch}
		
	for i in range(30):
		_ = session.run(train_op, feed_dict)
	y_pred_batch = session.run(y_pred, {x : x_batch})

output_var = y_pred_batch.reshape(1,5)