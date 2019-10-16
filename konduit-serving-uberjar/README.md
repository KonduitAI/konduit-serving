## Uber jar module

This module contains a configuration for creating an uber jar based on
the model server.

In order to build the uber jar, the uberjar profile must be enabled as follows:

```bash
mvn -Puberjar clean install -DskipTests
```

Otherwise, when running the configuration from the top this module will just be skipped.
The reason for this is due to the length of time an uber jar takes to create.

Generally each module (war files, tar files,..) takes a while to build
and not all are needed.
