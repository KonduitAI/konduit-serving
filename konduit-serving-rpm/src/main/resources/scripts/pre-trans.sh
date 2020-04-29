#!/bin/sh

if [ -z "${KONDUIT_ENDPOINT_LOGS_DIR}" ]; then
  rm -rf "${KONDUIT_ENDPOINT_LOGS_DIR}"
fi

if [ -z "${KONDUIT_SERVING_HOME}" ]; then
  rm  "${KONDUIT_SERVING_HOME}"/konduit-serving-*.jar
  rm  "${KONDUIT_SERVING_HOME}"/install-python.sh
fi

rm /etc/profile.d/konduit-serving-env.sh

exit 0