package ai.konduit.serving.basic;

import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.util.python.PythonVariables;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.bytedeco.numpy.presets.numpy.cachePackages;

public class BasicConfigurationPython {
    public static void main(String[] args) throws Exception {
        // A Pipeline Step for loading a numpy array file to an NDArray through a python script

        // Specify a custom python path over here. You can find this out from your python install by executing
        // the following code from your python install:
        // python -c "import os, sys; print(os.path.pathsep.join([path.strip() for path in sys.path if path.strip()]))"
        String pythonPath = Arrays.stream(cachePackages())
                .filter(Objects::nonNull)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));

        String pythonCodePath = new ClassPathResource("scripts/loadnumpy.py").getFile().getAbsolutePath();
        String npyFile =  new ClassPathResource("data/input.npy").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonPath(pythonPath) // If not null, this python path will be used.
                .pythonCodePath(pythonCodePath)
                .pythonInput("x", PythonVariables.Type.STR.name())
                .pythonOutput("y", PythonVariables.Type.NDARRAY.name())
                .build();

        PythonStep pythonPipelineStep = new PythonStep().step(pythonConfig);

        Writable[][] output = pythonPipelineStep.getRunner().transform(npyFile);

        INDArray image = ((NDArrayWritable) output[0][0]).get();

        System.out.println(Arrays.toString(image.shape()));
        System.out.println(image);
    }
}