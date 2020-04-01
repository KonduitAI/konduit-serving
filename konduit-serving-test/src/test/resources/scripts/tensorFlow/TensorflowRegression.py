import os
import sys
import numpy as np
import pandas as pd 
from sklearn.preprocessing import StandardScaler 
import tensorflow.compat.v1 as tf
tf.disable_v2_behavior()
from tensorflow.python.platform import gfile
work_dir = os.path.abspath("./src/test/resources")
sys.path.append(work_dir)

# Loading and Preprocessing test data
df_test = pd.read_csv(os.path.join(work_dir,'data\\tensorflow\\tensorflowinput.csv'))
cols = ['OverallQual', 'GrLivArea', 'GarageCars', 'FullBath', 'YearBuilt']
id_col = df_test['Id'].values.tolist()
df_test['GrLivArea'] = np.log1p(df_test['GrLivArea'])
df_test = pd.get_dummies(df_test)
df_test = df_test.fillna(df_test.mean())

X_test=inputData
X_test = df_test[cols].values
scale = StandardScaler()
X_test = scale.fit_transform(X_test)
path=os.path.join(work_dir,"inference\\tensorflow\\tensorflowregressionmodel.pb")
f = gfile.FastGFile(path,'rb')
graph_def = tf.GraphDef()
graph_def.ParseFromString(f.read())
f.close()

# Getting Predictions
sess = tf.Session()
sess.graph.as_default()
tf.import_graph_def(graph_def)
softmax_tensor = sess.graph.get_tensor_by_name('import/dense_4/BiasAdd:0')
predictions = np.array(sess.run(softmax_tensor, {'import/dense_1_input:0': X_test[:1]}))