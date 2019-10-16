#!/bin/sh

if ! getent passwd pipelines > /dev/null
then
    echo "adding user pipelines:daemon"
    useradd -r -M -d /srv/konduit-serving -s /sbin/nologin \
    -c "Konduit-serving Daemon" konduit-serving
fi

exit 0