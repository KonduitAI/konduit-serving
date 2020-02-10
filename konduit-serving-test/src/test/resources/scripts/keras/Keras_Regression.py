# Import required libraries
import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error

import keras
from keras.models import Sequential
from keras.layers import Dense
import keras.backend.tensorflow_backend as tb
tb._SYMBOLIC_SCOPE.value = True

work_dir = os.path.abspath(".")
#df = pd.read_csv('housing.csv')
df = pd.read_csv(os.path.join(work_dir,'src\\test\\resources\\scripts\\keras\\housing.csv'))
print(df.shape)
df.describe()

target_column = ['TAX']
predictors = list(set(list(df.columns))-set(target_column))
df[predictors] = df[predictors]/df[predictors].max()
df.describe()

X = df[predictors].values
y = df[target_column].values

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.30, random_state=40)
print(X_train.shape); print(X_test.shape)

# Define model
model = Sequential()
model.add(Dense(100, input_dim=12, activation= "relu"))
model.add(Dense(100, input_dim=12, activation= "relu"))
model.add(Dense(100, input_dim=12, activation= "relu"))
model.add(Dense(1))
model.summary()

model.compile(loss= "mean_squared_error" , optimizer="adam", metrics=["mean_squared_error"])
model.fit(X_train, y_train, epochs=20)

pred_train= model.predict(X_train)
print(np.sqrt(mean_squared_error(y_train,pred_train)))
#My_test = np.array(0.00632,18,2.31,0,0.538,6.575,65.2,4.09,1,296,15.3,4.98,24)
My_test = inputData
print("My_test----------",My_test)
pred= model.predict(X_test)
#print(np.sqrt(mean_squared_error(y_test,pred)))

