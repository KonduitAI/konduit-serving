import matplotlib.pyplot as plt
import numpy as np
from sklearn import datasets, linear_model
import json 

# read file
with open('Diabetics.json', 'r') as myfile:
    data=myfile.read()

# parse file
obj = json.loads(data)
Xj = obj['Data']
Yj = obj['Data1']

Xr = np.array([[Xj[0]], [Xj[1]],[Xj[2]],[Xj[3]],[Xj[4]],[Xj[5]],[Xj[6]],[Xj[7]],[Xj[8]],[Xj[9]],[Xj[0]], [Xj[1]],[Xj[2]],[Xj[3]],[Xj[4]],[Xj[5]],[Xj[6]],[Xj[7]],[Xj[8]],[Xj[9]]])

# Load the diabetes dataset
diabetes = datasets.load_diabetes()

# Use only one feature
diabetes_X = diabetes.data[:, np.newaxis]
diabetes_X_temp = diabetes_X[:, :, 2]

# Split the data into training/testing sets
diabetes_X_train = diabetes_X_temp[:-20]
diabetes_X_test = diabetes_X_temp[-20:]
input_data = diabetes_X_test #20x1 array
print(input_data)

# Split the targets into training/testing sets
diabetes_y_train = diabetes.target[:-20]
diabetes_y_test = diabetes.target[-20:]

# Create linear regression object
regr = linear_model.LinearRegression()

# Train the model using the training sets
regr.fit(diabetes_X_train, diabetes_y_train)

# The coefficients
print('Coefficients: \n', regr.coef_)
# The mean square error
print("Residual sum of squares: %.2f"
      % np.mean((regr.predict(diabetes_X_test) - diabetes_y_test) ** 2))
# Explained variance score: 1 is perfect prediction
print('Variance score: %.2f' % regr.score(diabetes_X_test, diabetes_y_test))

# Plot outputs
plt.scatter(diabetes_X_test, diabetes_y_test,  color='black')
plt.plot(diabetes_X_test, regr.predict(diabetes_X_test), color='blue',
         linewidth=3)
output_data= regr.predict(Xr)