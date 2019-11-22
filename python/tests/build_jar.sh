#!/usr/bin/env bash

javac -d target org/test/*.java
cd target
jar cvf test.jar ./*
cd -