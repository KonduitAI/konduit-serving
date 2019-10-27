package ai.konduit.serving.basic;

import ai.konduit.serving.pipeline.ImageLoading;
import ai.konduit.serving.pipeline.steps.ImageTransformProcessPipelineStepRunner;
import ai.konduit.serving.util.image.NativeImageLoader;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Text;
import org.datavec.image.data.ImageWritable;
import org.datavec.image.transform.ImageTransformProcess;

import java.lang.annotation.Native;
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
                .resizeImageTransform(28,28)
                .build();

        ImageLoading imageLoading = ImageLoading.builder()
                .inputName("default")
                .dimensionsConfig("default", new Long[]{640L, 480L, 3L})
                .imageTransformProcess("default", imageTransformProcess)
                .build();

        ImageTransformProcessPipelineStepRunner imageTransformProcessPipelineStepRunner =
                new ImageTransformProcessPipelineStepRunner(imageLoading);

        Record[] output = imageTransformProcessPipelineStepRunner.transform(new Record[] {
                new org.datavec.api.records.impl.Record(
                        Collections.singletonList(new Text("C:\\Users\\shams\\Desktop\\Tests\\Tensorflow20\\train2014\\COCO_train2014_000000000009.jpg")),
                        null)
        });

        System.out.println(output[0].getRecord().get(0));
    }
}
