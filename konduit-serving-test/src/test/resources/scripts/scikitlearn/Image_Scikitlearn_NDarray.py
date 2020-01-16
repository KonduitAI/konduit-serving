# -*- coding: utf-8 -*-
import os
import sys
from sklearn import datasets
from sklearn.model_selection import train_test_split
from sklearn.externals import joblib
work_dir = os.path.abspath("./src/test/resources/scripts/scikitlearn")
sys.path.append(work_dir)

#input : load images here - Array of images
digits = datasets.load_digits()
#imgPath1 =("src\\test\\resources\\data\\5.png")
X_test = imgPath
n_samples = len(digits.images)
data = digits.images.reshape((n_samples, -1))
X_train, X_test, y_train, y_test = train_test_split(
    data, digits.target, test_size=0.5, shuffle=False)

filename =work_dir+'//'+'ScikitLearn_model.sav'
loaded_model = joblib.load(filename)
result = loaded_model.predict(X_test)
results = loaded_model.score(X_test, y_test)
print("result---------->",result)
#print(results)

