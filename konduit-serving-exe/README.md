# EXE module

This submodule builds an exe and requires
the exe profile to be active in order to be used.

An example build is as follows:
```bash
mvn -Djavacpp.platform=windows-x86_64 -Pexe -Ppython -Dchip=cpu clean install -Dmaven.test.skip=true

```

This build will only succeed on windows.
