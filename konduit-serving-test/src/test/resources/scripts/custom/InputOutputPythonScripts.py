# -*- coding: utf-8 -*-
"""
Created on Sat Dec 21 12:48:58 2019
"""
if type(inputVar) == int:
    output=inputVar
    print("The variable is int ",output)
elif type(inputVar)==float:
    output=inputVar
    print("The variable is float",output)
elif type(inputVar) == str:
    output=inputVar
    print("The variable is string",output)
elif type(inputVar) == bool:
    output=inputVar
    print("The variable is string",output)
elif type(inputVar)==dict:
    output=inputVar
    print("The variable is dictionary",output)
elif type(inputVar)==list:
    output=inputVar
    print("The variable is list",output)
else:
    output= inputVar
    print("Could not identify the variable",output)
