#!/usr/bin/env bash
# This file defines the environment variables needed to run the konduit serving admin command.

export KONDUIT_SERVING_HOME=/srv
export KONDUIT_SERVING_CLASS_PATH=${KONDUIT_SERVING_HOME}/pipelines.jar
export KONDUIT_SERVING_LOG_DIR=/tmp/pipelines.log
export KONDUIT_SERVING_PID_FILE=/run/konduit-serving/konduit.serving.pid

if [ -z "$JAVA_HOME" ]
then
  export JAVA_HOME=/usr/lib/jvm/java
fi

if [ -z "$JDK_HOME" ]
then
  export JDK_HOME=/usr/lib/jvm/java
fi