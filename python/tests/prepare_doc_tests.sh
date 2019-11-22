#!/usr/bin/env bash

python test_docs.py
javac -cp ./konduit.jar -d . ai/konduit/serving/*.java
jar uf konduit.jar ai/konduit/serving/BasicsTest*.class
