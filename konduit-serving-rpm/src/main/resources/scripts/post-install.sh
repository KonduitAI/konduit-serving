#!/bin/sh

. /etc/profile.d/konduit-serving-env.sh

# Installing miniconda and other package
sh "${KONDUIT_SERVING_HOME}"/install-python.sh

exit 0