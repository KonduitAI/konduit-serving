#!/usr/bin/env bash

# copy konduit jar from base folder (assuming it has been generated there
cp ../../konduit.jar .

# run the "prepare java files" part from the unit test
python test_docs.py

# compile the java source files
javac -cp ./konduit.jar -d . ai/konduit/serving/*.java

# only keep the compiled files
rm ai/konduit/serving/*.java

# update the jar with all new test classes
jar uf konduit.jar ai/konduit/serving/*
