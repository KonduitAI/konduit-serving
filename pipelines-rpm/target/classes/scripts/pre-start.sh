#!/bin/sh

PIPELINES_PID_FILE=${PIPELINES_PID_FILE:-/run/PIPELINES/PIPELINES.pid}
PIPELINES_PID_DIR=$(dirname ${PIPELINES_PID_FILE})

echo "checking for PID dir ${PIPELINES_PID_DIR}"

if [ ! -d ${PIPELINES_PID_DIR} ]
then
    echo "creating PID directory ${PIPELINES_PID_DIR}"
    mkdir ${PIPELINES_PID_DIR}
    chown pipelines:daemon ${PIPELINES_PID_DIR}
fi