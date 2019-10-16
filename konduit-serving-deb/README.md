# Debian Builder Module

This builds Konduit Serving on an Debian based system.
An example build could be as follows:

```bash

mvn -Djavacpp.platform=linux-x86_64 -deb -Ppython -Dchip=cpu clean install -Dmaven.test.skip=true

```

Note that this builds a jar for intel-based linux systems only. An example 
Docker file for building an rpm on an ubuntu system is included here.

