Native Module
------------------

This module contains all the dependencies needed for cpu
execution on x86/intel including:

1. mkldnn/mkl
2. Tensorflow on cpu
3. Dl4j/nd4j bound to mkl


This modules main role is just to supply cpu support
for model-server-core (which contains the apis)

