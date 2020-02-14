import os
import sys
import joblib
import pandas as pd
import numpy as np
from sklearn.impute import SimpleImputer
work_dir = os.path.abspath("./src/test/resources")
sys.path.append(work_dir)
# import data
#train = pd.read_csv('../../data/scikitlearn/regression_train.csv')
#test = pd.read_csv('../../data/scikitlearn/regression_test.csv')
train = pd.read_csv(os.path.join(work_dir,'data\\scikitlearn\\regression_train.csv'))
test = pd.read_csv(os.path.join(work_dir,'data\\scikitlearn\\regression_test.csv'))

# divide data into predictor and target variables
train_X = train.drop('SalePrice', axis=1)
train_y = train.SalePrice
test_X = test

# one-hot encoding categorical variables for analysis
onehot_train_X = pd.get_dummies(train_X)
onehot_test_X = pd.get_dummies(test_X)
train_X, test_X = onehot_train_X.align(onehot_test_X, join='left', axis=1)

# impute missing values with the column's mean value
my_imputer = SimpleImputer()
train_X = my_imputer.fit_transform(train_X)
test_X = my_imputer.transform(test_X)
print(test_X.shape)
print(test_X[1])
test_X = inputData
#model = joblib.load('../../inference/scikitlearn/regression_model.sav')
model = joblib.load(os.path.join(work_dir,'inference\\scikitlearn\\regression_model.sav'))
predictions = model.predict(test_X)
print("Predictions ->")
print(predictions[:5])