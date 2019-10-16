# RPM Builder module

This builds konduit on an rpm based system.
An example build could be as follows:

```bash
mvn -Djavacpp.platform=linux-x86_64 -Prpm -Ppython -Dchip=cpu clean install -Dmaven.test.skip=true
```

Note that this builds a jar for intel based linux systems only.
An example docker file for building an rpm on a centos system is included.

