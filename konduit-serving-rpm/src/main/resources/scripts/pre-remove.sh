#!/bin/sh

if [ -z "${KONDUIT_SERVING_LOG_DIR}" ]; then
  rm -rf "${KONDUIT_SERVING_LOG_DIR}"
fi

if [ -z "${KONDUIT_SERVING_HOME}" ]; then
  rm -rf "${KONDUIT_SERVING_HOME}"
fi

rm /etc/profile.d/konduit-serving-env.sh

exit 0