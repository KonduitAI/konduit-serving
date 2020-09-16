# Python Path Resolution

## Status
PROPOSED

Proposed by: Shams Ul Azeem (15/09/2020)

Discussed with: Adam Gibson

## Context
Konduit Serving has the ability to run a python script using a PythonStep. There are a bunch of options that need to be setup for the PythonStep which includes, names and data types for inputs & outputs, script locations, python library paths. All of these options remain static from system to system apart from the python library paths variable. Since, every system has a different set of python installations using either conda, virtual environments or regular python installs, it becomes a hassle to manage a PythonStep configuration that's widely usable across different systems. The point of creating this ADR is to specify a simple enough workflow that can take care of the python library paths resolution for us through both using the konduit-serving cli **profiles** and simple configs. This basically will try to find the python and conda installations through executables found inside the **PATH** environment variable.

## Proposal
The base of the proposal is to be able to assign identification tokens to different python install locations. Instead of knowing and specifying absolute python path locations, the user can just specify the identification token of system found python installs. There is still going to be options available for specifying a custom python install that are not present in system environment **PATH** variable. 

### The `pythonpaths` Command
An example of such a workflow is as follows: 

```shell script
konduit pythonpaths
```  

which will output the python install locations as follows:

```text
-------------------------------------------PYTHON INSTALLS-------------------------------------------
-   id: 1
    location: C:\Program Files (x86)\Microsoft Visual Studio\Shared\Python37_64\python.exe
    version: 3.7.8

-   id: 2
    location: C:\Users\shams\miniconda3\python.exe
    version: 3.7.7

-   id: 3   
    location: C:\Users\shams\AppData\Local\Microsoft\WindowsApps\python.exe                        
    version: 3.6.1
-----------------------------------------------------------------------------------------------------

--------------------------------------------CONDA INSTALLS-------------------------------------------
-   id: 1
    location: C:\Users\shams\miniconda3\Scripts\conda.exe
    version: 4.8.4
    ----------------------------------------ENVIRONMENTS---------------------------------------------
    -   name: base
        location: C:\Users\shams\miniconda3
        version: 3.6.1
    
    -   name: py37
        location: C:\Users\shams\miniconda3\envs\py37
        version: 3.7.7
    
    -   name: py38
        location: C:\Users\shams\miniconda3\envs\py38
        version: 3.8.1
    -------------------------------------------------------------------------------------------------

-   id: 2
    location: C:\Users\shams\miniconda2\Scripts\conda.exe
    version: 4.7.1
    ----------------------------------------ENVIRONMENTS---------------------------------------------
    -   name: base
        location: C:\Users\shams\miniconda2
        version: 2.6.1
    
    -   name: py27
        location: C:\Users\shams\miniconda2\envs\py27
        version: 2.7.7
    
    -   name: py25
        location: C:\Users\shams\miniconda2\envs\py25
        version: 2.5.1
    -------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------
```

Based on the above info (gained from the command: `konduit pythonpaths`), it's much easier to specify python paths in configuration files (using `type`, `location`, `environment` properties):

```json
{
  "@type" : "PYTHON",
  "pythonConfig" : {
    
    "type": "conda",
    "location": "1",
    "environment": "py37", 

    "python" : "run.py",
    "importCodePath" : "imports.py",
    "pythonInputs" : { },
    "pythonOutputs" : { },
    "extraInputs" : { },
    "returnAllInputs" : false,
    "setupAndRun" : false,
    "ioInputs" : {
      "images" : {
        "name" : "images",
        "pythonType" : "list",
        "secondaryType" : "NDARRAY",
        "type" : "LIST"
      }
    },
    "ioOutputs" : {
      "texts" : {
        "name" : "texts",
        "pythonType" : "list",
        "secondaryType" : "STRING",
        "type" : "LIST"
      },
      "boxes" : {
        "name" : "boxes",
        "pythonType" : "list",
        "secondaryType" : "NDARRAY",
        "type" : "LIST"
      }
    },
    "jobSuffix" : "konduit_job"
  }
}
```

#### Possible Values
- The `type` property will take one of the following values: [`python`, `conda`, `custom`]
- `location` will have the *id* of the python install for `type==python` or the id of the conda install for `type==conda`. Otherwise, it will contain the absolute location of the python executable file when `type==custom`
- `environment` will be looked into if `type==conda` to locate the python installation for a particular conda environment.
                               
### Defaults and Priorities
The defaults for python paths are to be expected in the following way:

![Python Paths Defaults](../markdown_images/python-installation-paths-defaults.png)

### Usage with Profiles
The profiles should be extended to incorporate the python paths settings for running with the `serve` command. The same items names that are used for the PythonStep config are going to be used here. For example: 

```shell script
konduit profile create python_37_env --python-type=conda --python-location=1 --conda-env=py37 <...other_options...>
                                        # OR
konduit profile create python_37_env -pt=conda -pl=1 -ce=py37 <...other_options...>
```

When the above profile is used with the `serve` command, the usage will look like the following:

```shell script
konduit serve -c config.json -id python-server -p python_37_env
```

When the above command is run, all the python paths are automatically updated in the configurations to the values specified in the profile settings. 

### Registering a Python Path
If you needed python or conda installation isn't available inside the **PATH** environment variable, you can register it through the `pythonpaths add` command. For example:

```shell script
konduit pythonpaths add --type=python --path=E:\python37\python.exe
                        # OR
konduit pythonpaths add -t=python -p=E:\python37\python.exe
```

Where `--type` or `-t` can be either `python` or `conda`. By running the above command, you will see the following output

```text
Id: 4
Python path at location "E:\python37\python.exe" has been registered.
```

#### Specifying Different Python Paths for Different PythonSteps
If you want to specify different python paths for different PythonSteps and don't want profiles to update your configurations dynamically, then you can specify `python-paths-resolution: static` in the particular step configuration and it won't be affected by the profiles settings.
 
### Installed Packages Details
You will also be able to see the installed packages in each python installation with the `with-installed-packages` or `wip` flag. For example:

```shell script
konduit pythonpaths --with-installed-packages
                # OR
konduit pythonpaths -wip
```

The output will look something like: 

```text
-------------------------------------------PYTHON INSTALLS-------------------------------------------
-   id: 1
    location: C:\Program Files (x86)\Microsoft Visual Studio\Shared\Python37_64\python.exe
    version: 3.7.8
    packages: 
        - absl-py | 0.9.0
        - aenum | 2.2.4
        - apipkg | 1.5
        - appdirs | 1.4.3
        - argcomplete | 1.11.1
        - astor | 0.8.1
        - astroid | 2.3.2
        - attrs | 19.3.0
        - autopep8 | 1.5
        - black | 19.10b0
        - bleach | 3.1.4
        - cachetools | 4.0.0
        - certifi | 2020.4.5.2
        - chardet | 3.0.4
        - click | 7.1.2
        - colorama | 0.4.1
        - coverage | 5.0.3
        - cycler | 0.10.0
        - Cython | 0.29.20
        - dbf | 0.99.0
        - dbfpy | 2.3.1
        - decorator | 4.4.2
        - docutils | 0.16
        - execnet | 1.7.1
        - gast | 0.2.2
        - google-auth | 1.12.0
        - google-auth-oauthlib | 0.4.1
        - google-pasta | 0.2.0
        - grpcio | 1.27.2
        - h5py | 2.10.0
        - hurry.filesize | 0.9
        - idna | 2.9
        - imageio-ffmpeg | 0.4.2
        - importlib-metadata | 1.6.1
        - isort | 4.3.21
        - Jinja2 | 2.11.1
        - joblib | 0.14.1
        - jsonschema2popo | 0.12
         -e git+https://github.com/eclipse/deeplearning4j.git@6b736eae3333a94c1ef22ae5e53b1139434bb4b5#egg=jumpy&subdirectory=jumpy
        - Keras | 2.3.1
        - Keras-Applications | 1.0.8
        - Keras-Preprocessing | 1.1.0
        - keyring | 21.2.0
        - kiwisolver | 1.1.0
        - konduit | 0.1.10
        - lazy-object-proxy | 1.4.2
        - Markdown | 3.2.1
        - MarkupSafe | 1.1.1
        - matplotlib | 3.2.1
        - mccabe | 0.6.1
        - mock | 4.0.1
        - networkx | 1.9
        - numpy @ file:///C:/Users/shams/Downloads/numpy-1.19.1%2Bmkl-cp37-cp37m-win_amd64.whl
        - oauthlib | 3.1.0
        - opencv-python | 4.2.0.32
        - opt-einsum | 3.2.0
        - packaging | 20.4
        - pandas | 0.24.2
        - pathspec | 0.7.0
        - pep8 | 1.7.1
        - Pillow | 7.0.0
        - pkginfo | 1.5.0.1
        - protobuf | 3.11.3
        - py4j | 0.10.7
        - pyarrow | 0.13.0
        - pyasn1 | 0.4.8
        - pyasn1-modules | 0.2.8
        - pycodestyle | 2.5.0
        - pydatavec | 0.1.2
        - pydl4j | 0.1.5
        - Pygments | 2.6.1
        - pyjnius | 1.3.0
        - pylint | 2.4.3
        - pymongo | 3.11.0
        - pyparsing | 2.4.7
        - pyspark | 2.4.5
        - pytest-cache | 1.0
        - pytest-cov | 2.8.1
        - pytest-pep8 | 1.0.6
        - python-dateutil | 2.8.1
        - pytz | 2020.1
        - pywin32-ctypes | 0.2.0
        - PyYAML | 5.3.1
        - readme-renderer | 25.0
        - regex | 2020.2.20
        - requests | 2.24.0
        - requests-oauthlib | 1.3.0
        - requests-toolbelt | 0.9.1
        - rsa | 4.0
        - scikit-learn | 0.22.2.post1
        - scipy | 1.4.1
        - setupext-janitor | 1.1.2
        - six | 1.15.0
        - tensorboard | 2.1.1
        - tensorflow | 2.1.0
        - tensorflow-estimator | 2.1.0
        - termcolor | 1.1.0
        - toml | 0.10.0
        - torch | 1.4.0+cpu
        - torchvision | 0.5.0+cpu
        - tqdm | 4.45.0
        - twine | 3.1.1
        - typed-ast | 1.4.0
        - urllib3 | 1.25.9
        - webencodings | 0.5.1
        - Werkzeug | 1.0.0
        - wrapt | 1.11.2
        - zipp | 3.1.0
.
.
.

-   id: 3   
    location: C:\Users\shams\AppData\Local\Microsoft\WindowsApps\python.exe                        
    version: 3.6.1
    .
    .
    .

-----------------------------------------------------------------------------------------------------

.
.
.

```

## Consequences 
### Advantages
1. No need to run `python -c "import sys, os; print(os.pathsep.join([path for path in sys.path if path]))"` to find out the python libraries locations, manually. 
2. Profiles will make sure the intended python path is used between system transitions.
  
### Disadvantages
1. Not entirely straightforward in the beginning. Needs documentation...

## Discussion
To be updated after PR comments...

