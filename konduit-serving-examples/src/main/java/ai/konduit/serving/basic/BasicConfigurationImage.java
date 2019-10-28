package ai.konduit.serving.basic;

import ai.konduit.serving.pipeline.ImageLoading;
import ai.konduit.serving.pipeline.steps.ImageTransformProcessPipelineStepRunner;
import org.datavec.api.records.Record;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Text;
import org.datavec.image.transform.ImageTransformProcess;
import org.nd4j.linalg.io.ClassPathResource;

import java.util.Arrays;
import java.util.Collections;

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
                .scaleImageTransform(14.0f)
                //.resizeImageTransform(28,28)
                .build();


        ImageLoading imageLoading = ImageLoading.builder()
                .imageProcessingInitialLayout("NCHW")
                .imageProcessingRequiredLayout("NHWC")
                .updateOrderingBeforeTransform(false)
                .inputName("default")
                .dimensionsConfig("default", new Long[]{28L, 28L, 3L})
                .imageTransformProcess("default", imageTransformProcess)
                .build();

        ImageTransformProcessPipelineStepRunner imageTransformProcessPipelineStepRunner =
                new ImageTransformProcessPipelineStepRunner(imageLoading);

        Record[] output = imageTransformProcessPipelineStepRunner.transform(new Record[] {
                new org.datavec.api.records.impl.Record(
                        Collections.singletonList(new Text( new ClassPathResource("images/COCO_train2014_000000000009.jpg").getFile().getAbsolutePath())),
                        null)
        });

        System.out.println(Arrays.toString(((NDArrayWritable) output[0].getRecord().get(0)).get().shape()));
        System.out.println(output[0].getRecord().get(0));
    }
}
