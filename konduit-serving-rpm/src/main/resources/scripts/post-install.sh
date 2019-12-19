#!/bin/sh

. /etc/profile.d/konduit-serving-env.sh

chmod u+x "${KONDUIT_SERVING_BIN_DIRECTORY}"/konduit-serving
# Installing miniconda and other package
sh "${KONDUIT_SERVING_HOME}"/install-python.sh >/dev/tty

exit 0