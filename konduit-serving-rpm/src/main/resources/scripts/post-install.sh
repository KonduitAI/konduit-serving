#!/bin/sh

. /etc/profile.d/konduit-serving-env.sh

chmod u+x "${KONDUIT_SERVING_BIN_DIRECTORY}"/konduit-serving
# Installing miniconda and other package
sh "${KONDUIT_SERVING_HOME}"/install-python.sh >/dev/tty 2>&1 # Sending stdout to the parent process and sending stderr to stdout...

chown -R konduit:konduit /opt/konduit

exit 0