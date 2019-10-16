#!/usr/bin/env bash
# This file defines the environment variables needed to run the SKIL admin command.

export PIPELINES_HOME=/srv
export PIPELINES_CLASS_PATH=${PIPELINES_HOME}/pipelines.jar
export PIPELINES_LOG_DIR=/tmp/pipelines.log
export PIPELINES_PID_FILE=/run/skil/skil.pid

if [ -z "$JAVA_HOME" ]
then
  export JAVA_HOME=/usr/lib/jvm/java
fi

if [ -z "$JDK_HOME" ]
then
  export JDK_HOME=/usr/lib/jvm/java
fi