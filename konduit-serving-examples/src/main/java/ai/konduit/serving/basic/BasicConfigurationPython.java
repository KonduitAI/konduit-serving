package ai.konduit.serving.basic;

import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.PythonPipelineStep;
import ai.konduit.serving.util.python.PythonVariables;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;

import java.util.Arrays;

public class BasicConfigurationPython {
    public static void main(String[] args) throws Exception {
        String pythonPath = "C:/miniconda/envs/py37/python37.zip;" +
                "C:/miniconda/envs/py37/DLLs;" +
                "C:/miniconda/envs/py37/lib;C:/miniconda/envs/py37;" +
                "C:/miniconda/envs/py37/lib/site-packages;" +
                "C:/miniconda/envs/py37/lib/site-packages/win32;" +
                "C:/miniconda/envs/py37/lib/site-packages/win32/lib;" +
                "C:/miniconda/envs/py37/lib/site-packages/Pythonwin";

        String pythonCodePath = new ClassPathResource("scripts/loadimage.py").getFile().getAbsolutePath();
        String imagePath =  new ClassPathResource("images/COCO_train2014_000000000009.jpg").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonPath(pythonPath)
                .pythonCodePath(pythonCodePath)
                //.pythonCode("y = x + 2")
                .pythonInput("x", PythonVariables.Type.STR.name())
                .pythonOutput("y", PythonVariables.Type.NDARRAY.name())
                .build();

        PythonPipelineStep pythonPipelineStep = new PythonPipelineStep().step(pythonConfig);

        Writable[][] output = pythonPipelineStep.getRunner().transform(imagePath);

        INDArray image = ((NDArrayWritable) output[0][0]).get();
        System.out.println(Arrays.toString(image.shape()));
        System.out.println(image);
    }
}
