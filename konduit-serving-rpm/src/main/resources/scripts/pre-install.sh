#!/bin/sh

if ! getent passwd konduit-serving > /dev/null
then
    echo "adding user konduit-serving:daemon"
    useradd -r -M -d /srv/konduit-serving -s /sbin/nologin \
    -c "Konduit-serving Daemon" konduit-serving
fi

exit 0