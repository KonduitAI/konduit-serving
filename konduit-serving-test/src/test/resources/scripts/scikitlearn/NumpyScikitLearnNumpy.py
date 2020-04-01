import os
import sys
from sklearn.datasets import load_iris
from sklearn.externals import joblib
from sklearn import metrics
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
import numpy as np
work_dir = os.path.abspath(".")
sys.path.append(work_dir)

iris = load_iris()

# store the feature matrix (X) and response vector (y)
X = iris.data
y = iris.target

# splitting X and y into training and testing sets

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.4, random_state=1)

# training the model on training set

knn = KNeighborsClassifier(n_neighbors=5)
knn.fit(X_train, y_train)

# making predictions on the testing set
y_pred = knn.predict(X_test)

# comparing actual response values (y_test) with predicted response values (y_pred)
print("kNN model accuracy:", metrics.accuracy_score(y_test, y_pred))

# making prediction for out of sample data
test_input_np=inputValue
preds = knn.predict(test_input_np)
pred_species = [iris.target_names[p] for p in preds]
outputValue = np.array(preds).astype('float')
# saving the model
joblib.dump(knn, 'iris_knn.pkl')


