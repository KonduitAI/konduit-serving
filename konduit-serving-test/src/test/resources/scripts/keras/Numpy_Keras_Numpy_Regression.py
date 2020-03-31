import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error
import keras
from keras.models import Sequential
from keras.layers import Dense


input_data = np.array([[0.0380759,	0.0506801,	0.0616962,	0.0218724,	0.0442235,	0.0348208,	0.0434008,	0.00259226,	0.0199084,	0.0176461,	0.0380759,	0.0506801]])

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
pred= model.predict(input_data)
numPred = np.array(pred)