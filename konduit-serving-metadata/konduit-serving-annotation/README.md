Development notes
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


Turning off IDE builds and delegating to maven:
This [link](https://www.jetbrains.com/help/idea/delegate-build-and-run-actions-to-maven.html#delegate_to_maven)
shows hot to disable intellij building the projects.

For the amount of annotation processing and other special tricks
in this project, it's recomended to delegate to maven.
It's a bit slower but will generally fix issues with running
special projects.