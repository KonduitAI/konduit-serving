import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error

import keras
from keras.models import Sequential
from keras.layers import Dense
import json

with open('Housing.json', 'r') as myfile:
    data=myfile.read()

# parse file
obj = json.loads(data)
print(obj)
print("Data: " + str(obj['Data']))
Xj = obj['Data']
Yj = obj['Data1']
print(Xj)

Xr = np.array([[Xj[0],Xj[1],Xj[2],Xj[3],Xj[4],Xj[5],Xj[6],Xj[7],Xj[8],Xj[9],Xj[0],Xj[1]]])


df = pd.read_csv('housing.csv')
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

pred= model.predict(Xr)