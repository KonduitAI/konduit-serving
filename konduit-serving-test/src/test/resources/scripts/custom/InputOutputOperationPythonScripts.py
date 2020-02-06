# -*- coding: utf-8 -*-
"""
Created on Mon Jan 27 12:48:58 2020
"""
if type(inputVar) == int:
    output = inputVar * 4
    print("The variable is int ",output)
elif type(inputVar)==float:
    output = inputVar + 2
    print("The variable is float",output)
elif type(inputVar) == str:
    output = inputVar + ' String'
    print("The variable is string",output)
else:
    output= inputVar
    print("Could not identify the variable",output)
