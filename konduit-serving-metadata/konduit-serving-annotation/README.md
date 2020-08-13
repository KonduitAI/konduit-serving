Developmnent notes
----------------------------

In order to debug the annotation processor in intellij,
follow [this guide](https://medium.com/@joachim.beckers/debugging-an-annotation-processor-using-intellij-idea-in-2018-cde72758b78a)

Summary:
-----------------

1. Setup a remote debug configuration.

2. Copy and paste the command line arguments. Optional: 
Specify the port for debug connection, then copy and paste. 

3. Configure Shared Process build VM Process options
adding the text from above to the shared build process vm options.

4. Run a build where the annotation processor is used. 
Concurrently, run the debug configuration and if everything works
you should be connected. (Ensure you actually place breakpoints in your annotation processor!)
