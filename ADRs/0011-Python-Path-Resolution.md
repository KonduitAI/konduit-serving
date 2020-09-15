# Python Path Resolution

## Status
PROPOSED

Proposed by: Shams Ul Azeem (15/09/2020)

Discussed with: Adam Gibson

## Context
Konduit Serving has the ability to run a python script using a PythonStep. There are a bunch of options that needs to be setup for the PythonStep which includes, names and data types for inputs/outputs, script locations, python library paths. Most of these options remain static from system to system apart from the python library paths variable. Since, every system has a different set of python installations using either conda, virtual environments or regular python installs, it becomes a hassle to manage a widely usable konduit-serving configuration between different systems. The point of creating this ADR is to specify a simple enough workflow that can take care of the python library paths resolution for us through both using the konduit-serving cli profiles and simple configs.  

## Proposal
The base of the proposal is to be able to assign identification tokens to different python install locations. Instead of knowing and specifying absolute python path locations, the user can just specify the identification token of system found python installs. There is still going to be options for a custom python installs that are not present in system environment path variables. 

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

Based on the above info (gained from command: `konduit pythonpaths`), it's much easier to specify python paths in configuration files (using `type`, `location`, `environment` properties):

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
- `location` will have the id of the python install for `type==python` or the id of the conda install for `type==conda`. Otherwise, it will contain the absolute location of the python executable file when `type==custom`
- `environment` will be looked into if `type==conda` to locate the python installation for conda environment.
                               
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

When the above command is run, all the python paths are automatically updated to the values specified in the profile settings.
 
### Installed Packages Details


## Consequences 
### Advantages
1. No need to run `python -c "import sys, os; print(os.pathsep.join([path for path in sys.path if path]))"` to find out the python libraries locations, manually. 
2. Profiles will make sure the intended python path is used between system transitions.
  
### Disadvantages
1. Not entirely straightforward in the beginning. Needs documentation...

## Discussion
To be updated after PR comments...

