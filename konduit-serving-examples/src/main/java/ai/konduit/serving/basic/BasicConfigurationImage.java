package ai.konduit.serving.basic;

import ai.konduit.serving.pipeline.ImageLoading;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.datavec.image.transform.ImageTransformProcess;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;

import java.util.Arrays;

public class BasicConfigurationImage {
    public static void main(String[] args) throws Exception {
        /*
         * All the configurations are contained inside the InferenceConfiguration class:
         *
         * It has two items inside it.
         * ---------------------------
         * 1. ServingConfig: That contains configuration related to where the server will run and which
         *    path it will listen to.
         * 2. List<PipelineStep>: This is responsible for containing a list of steps in a Machine Learning Pipeline.
         * ---------------------------
         * PipelineStep is the base of how we will define all of our configuration inside Konduit Serving.
         *
         * Let's get started...
         */

        ImageTransformProcess imageTransformProcess = new ImageTransformProcess.Builder()
                .scaleImageTransform(20.0f)
                //.resizeImageTransform(28,28)
                .build();

        ImageLoading imageLoading = ImageLoading.builder()
                .imageProcessingInitialLayout("NCHW")
                .imageProcessingRequiredLayout("NHWC")
                .inputName("default")
                .dimensionsConfig("default", new Long[]{ 240L, 320L, 3L }) // Height, width, channels
                .imageTransformProcess("default", imageTransformProcess)
                .build();

        String imagePath =  new ClassPathResource("images/COCO_train2014_000000000009.jpg").getFile().getAbsolutePath();

        Writable[][] output = imageLoading.getRunner().transform(imagePath);

        INDArray image = ((NDArrayWritable) output[0][0]).get();

        System.out.println(Arrays.toString(image.shape()));
        System.out.println(image);
    }
}
