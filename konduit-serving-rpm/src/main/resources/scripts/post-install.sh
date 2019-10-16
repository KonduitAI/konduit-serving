#!/bin/sh

if command -v systemctl > /dev/null
then
    echo "updating systemd services"
    systemctl daemon-reload
fi

exit 0