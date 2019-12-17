#!/bin/sh

# Check if the user konduit exists in the system database if not then create it
# and add /opt/konduit as it's home directory
if ! getent passwd konduit >/dev/null; then
  echo "adding user konduit:daemon"
  # Adding /opt/konduit as the home directory for the user "konduit"
  useradd -r -M -d /opt/konduit -s /sbin/nologin -c "konduit Daemon" konduit
fi

exit 0
