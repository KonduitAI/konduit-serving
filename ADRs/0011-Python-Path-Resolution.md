# Python Path Resolution

## Status
PROPOSED

Proposed by: Shams Ul Azeem (15/09/2020)

Discussed with: Adam Gibson

## Context
Konduit Serving has the ability to run a python script using a PythonStep. There are a bunch of options that needs to be setup for the PythonStep which includes, names and data types for inputs/outputs, script locations, python library paths. Most of these options remain static from system to system apart from the python library paths variable. Since, every system has a different set of python installations using either conda, virtual environments or regular python installs, it becomes a hassle to manage a widely usable konduit-serving configuration between different systems. The point of creating this ADR is to specify a simple enough workflow that can take care of the python library paths resolution for us through both using the konduit-serving cli profiles and simple configs.  

## Proposal



## Consequences 

### Advantages

  
### Disadvantages


## Discussion

