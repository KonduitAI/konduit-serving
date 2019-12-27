## Tar module

This module contains a configuration for creating a tar distribution of konduit-serving

In order to build the tar file, the `tar` profile must be enabled as follows:

```bash
mvn -Ptar clean package -DskipTests
```

Otherwise, when running the configuration from the top this module will just be skipped.
The reason for this is due to the length of time an archive takes to create.

Generally each module (war files, tar files,..) takes a while to build
and not all are needed.
