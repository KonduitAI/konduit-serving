#!/bin/sh

if command -v systemctl > /dev/null
then
    if systemctl is-enabled konduit-serving.service
    then
        echo "stopping konduit-serving service"
        systemctl stop konduit-serving
    fi
fi


rm -rf  /run/konduit-serving
rm -rf /var/konduit-serving
rm -rf /var/log/konduit-serving
rm -rf -R konduit-serving /var/log/konduit-serving



exit 0