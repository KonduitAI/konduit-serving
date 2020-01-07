# Konduit Python Client: Enterprise Runtime for Machine Learning Models

[![PyPI version](https://badge.fury.io/py/konduit.svg)](https://badge.fury.io/py/konduit)

<p align="center">
  <img src="https://s3.amazonaws.com/TWFiles/486936/companyLogo/tf_44cf7e67-0538-4a83-b686-e5145eca1c41.Konduit_teamwork.jpg">
</p>

---

## Installation

install `kondut` from PyPI with `pip install konduit`. You can also install this Python package
from source, run `pip install .` in this directory. We recommend using Python 3.7+ for full support
of all dev tools. To install all extensions needed for development run

```shell script
pip install -e '.[tests,codegen,dev]'
```

The `dev` dependencies use `black` as a pre-commit hook to lint your code automatically. To activate
this functionality run `pre-commit install` on the command line first.

Once the package itself is installed, you have access to a command line interface (CLI) tool
called `konduit`. This helper tool can build the Java dependencies needed for `konduit`
for you under the hood. All you need to do is run:

```shell script
konduit init --os <your-platform> --spin <spin>
```

where `<your-platform>` is picked from `windows-x86_64`,`linux-x86_64`,`linux-x86_64-gpu`,
`macosx-x86_64`, `linux-armhf` and `windows-x86_64-gpu`, depending on your operating system
and architecture, and <spin> is picked from `minimal`, `python`, `pmml` and `all`. This tool assumes that you have `git` installed on your system. 

If you don't want to use the CLI tool and have cloned this repository, you can also build
the necessary jar on your own like this:

```shell script
cd ..
python build_jar.py --os <your-platform> --spin <spin>
```

## Using Konduit

To use konduit, make sure to export the environment variable `KONDUIT_JAR_PATH`. If you used the CLI this
will be set to `~/.konduit/konduit-serving/konduit.jar`. Make sure to put this in your `zshrc`, `bashrc`, or
similar files on your respective system that you might be using, i.e. put

```shell script
export KONDUIT_JAR_PATH="~/.konduit/konduit-serving/konduit.jar"
```

in there. If you don't export this variable, it will default to `konduit.jar`. So if you have `konduit.jar` at the 
base of the Python script you want to execute, it will work without setting the above environment variable.

## Running tests

Install test dependencies using `pip install 'konduit[tests]'` if you want to run tests. 

On Windows, compiling the test dependencies requires Visual Studio Build Tools 14.0, which can be installed from
[here](https://visualstudio.microsoft.com/downloads/). You may also need to install the Windows 8.1 / 10 SDK.
See Python's [*WindowsCompilers*](https://wiki.python.org/moin/WindowsCompilers) page for details.

The tests also require `bert_mrpc_frozen.pb` to be placed in the `python/tests` folder. Run the following
code in `python/tests`: 

```shell script
curl https://deeplearning4jblob.blob.core.windows.net/testresources/bert_mrpc_frozen_v1.zip --output bert.zip
unzip bert.zip 
```

The resulting JAR will be generated at the base of the `konduit` project. To copy that JAR into the test folder
and prepare the documentation (in the `docs` folder) to be tested within the testing framework, run:

```shell script
cd tests
./prepare_doc_tests.sh
```

The tests are then run with `pytest`:

```shell script
cd python/tests
python -m pytest .
```

To quickly run unit tests (recommended before each commit), or run the full set of integration tests, you can do:

```shell script
pytest -m unit
pytest -m integration
```

to also run documentation tests with `doctest` for an individual file, simply run:

```shell script
 python -m doctest ../konduit/server.py -v
```

## Developing `konduit` from source

`konduit` is largely generated from Java source files. Specifically, the large `inference.py` file is generated
this way. If you change the interface in `konduit-serving-api`, you should regenerate `konduit` Python code as well,
using:

```shell script
cd ..
sh build_client.sh
```

This script uses the `jsonschema2popo` tool underneath, which transforms JSON to Python objects.
That means you first need to install this tool with (note that this requires you to use python 3.4+,
which you could install in a virtual environment):

```shell script
pip install jsonschema2popo autopep8
```

and then you have to put `jsonschema2popo` on your `PATH`, so that you can use it from anywhere. To test
the installation, simply type `jsonschema2popo` in your console. `autopep8` is used internally for linting.
If the command `jsonschema2popo` is recognized by your shell, you should be able to build `konduit` from source.
