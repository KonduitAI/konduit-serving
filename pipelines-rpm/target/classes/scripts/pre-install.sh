#!/bin/sh

if ! getent passwd pipelines > /dev/null
then
    echo "adding user pipelines:daemon"
    useradd -r -M -d /srv/pipelines -s /sbin/nologin \
    -c "Pipelines Daemon" pipelines
fi

exit 0