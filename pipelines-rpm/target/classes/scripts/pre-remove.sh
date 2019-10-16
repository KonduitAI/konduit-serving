#!/bin/sh

if command -v systemctl > /dev/null
then
    if systemctl is-enabled pipelines.service
    then
        echo "stopping Pipelines service"
        systemctl stop pipelines
    fi
fi


rm -rf  /run/pipelines
rm -rf /var/pipelines
rm -rf /var/log/pipelines
rm -rf -R pipelines /var/log/pipelines



exit 0