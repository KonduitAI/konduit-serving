# Python Pipeline Steps

Konduit comes with an embedded cpython based on python
3.7x. We rely on the [Javacpp presets](https://github.com/bytedeco/javacpp-presets/)
for an embedded python distribution.

This allows us to launch python processes from java and interop
with the interpreters from the server in memory.

A python pipeline step is configured with a python configuration which contains the following:

1. A python path for custom libraries
2. Either python code or a python script path to execute
3. Inputs/Output variable names and their expected types
4. A boolean option to return all outputs (eg: do not have to specify output names and types)
all outputs are collected and types inferred.


