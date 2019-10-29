# Konduit Python Client: Enterprise Runtime for Machine Learning Models

[![PyPI version](https://badge.fury.io/py/konduit.svg)](https://badge.fury.io/py/konduit)

<p align="center">
  <img src="https://s3.amazonaws.com/TWFiles/486936/companyLogo/tf_44cf7e67-0538-4a83-b686-e5145eca1c41.Konduit_teamwork.jpg">
</p>

---

## Installation

To install this Python package from source, run `python setup.py install` in this directory. 

You can also install it from PyPI with `pip install konduit`.

We recommend using **Python 3.7+**.

## Build Konduit Serving JAR
 
To run any examples with `konduit`, you need to build a Konduit Serving JAR first. 
We provide a Python script `build_jar.py` at the base of this project to 
build a Konduit Serving JAR `konduit.jar`: 

```bash
cd ..
python build_jar.py --os <your-platform>
```

where `<your-platform>` is picked from `windows-x86_64`,`linux-x86_64`,`linux-x86_64-gpu`,
`macosx-x86_64`, `linux-armhf` and `windows-x86_64-gpu`, depending on your operating system
and architecture.

`build_jar.py` uses **Python 3.4+**, but `konduit` itself is Python 2.7 compatible.

## Using Konduit

To use konduit, make sure to export the environment variable `KONDUIT_JAR_PATH`. If you don't, this
will path will default to `konduit.jar`. So if you have `konduit.jar` at the base of the Python script you
want to execute, it will work without setting the above environment variable.

## Running tests

Install test dependencies using `pip install 'konduit[tests]'` if you want to run tests. 

On Windows, compiling the test dependencies requires Visual Studio Build Tools 14.0, which can be installed from [here](https://visualstudio.microsoft.com/downloads/). You may also need to install the Windows 8.1 / 10 SDK. See Python's [*WindowsCompilers*](https://wiki.python.org/moin/WindowsCompilers) page for details. 

The tests also require `bert_mrpc_frozen.pb` to be placed in the `python/tests` folder. Run the following code in `python/tests`: 
```
curl https://deeplearning4jblob.blob.core.windows.net/testresources/bert_mrpc_frozen_v1.zip --output bert.zip
unzip bert.zip 
```

The resulting JAR will be generated at the base of the `konduit` project.
Copy it to the `tests` folder:

```bash
cp konduit.jar python/tests
```

Run the tests with `pytest`:

```bash
cd python/tests
python -m pytest .
```

## Developing `konduit` from source

`konduit` is largely generated from Java source files. Specifically, the large `inference.py` file is generated
this way. If you change the interface in `konduit-serving-api`, you should regenerate `konduit` Python code as well,
using:

```bash
cd ..
sh build_client.sh
```

This script uses the `jsonschema2popo` tool underneath, which transforms JSON to Python objects.
That means you first need to install this tool with (note that this requires you to use python 3.4+,
which you could install in a virtual environment):

```bash
pip install jsonschema2popo
```

and then you have to put `jsonschema2popo` on your `PATH`, so that you can use it from anywhere. To test
the installation, simply type `jsonschema2popo` in your console. If the command is recognized
by your shell, you should be able to build `konduit` from source.
