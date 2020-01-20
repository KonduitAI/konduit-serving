import os
import sys
from sklearn.linear_model import LogisticRegression
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
work_dir = os.path.abspath("./src/test/resources/scripts/scikitlearn")
sys.path.append(work_dir)

X_test = imgPath
# Load and split data
data = load_iris()
Xtrain, Xtest, Ytrain, Ytest = train_test_split(data.data, data.target, test_size=0.3, random_state=4)

import pickle
#filename = "pickle_object.pkl"
filename =work_dir+'\\'+'pickle_object.pkl'

with open(filename, 'rb') as file:
    pickle_model = pickle.load(file)
    
 # Calculate the accuracy score and predict target values
#score = pickle_model.score(Xtest, Ytest)
#print("Test score: {0:.2f} %".format(100 * score))
Ypredict = pickle_model.predict(Xtest)

print(Ypredict)