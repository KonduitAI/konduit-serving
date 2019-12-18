#!/bin/sh

if [ -z "${KONDUIT_SERVING_LOG_DIR}" ]; then
  rm -rf "${KONDUIT_SERVING_LOG_DIR}"
fi

if [ -z "${KONDUIT_JAR_PATH}" ]; then
  rm "${KONDUIT_JAR_PATH}"
fi

exit 0
