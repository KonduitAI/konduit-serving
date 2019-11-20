package ai.konduit.serving.basic;

import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.datavec.image.transform.ImageTransformProcess;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;

import java.util.Arrays;

public class BasicConfigurationImage {
    public static void main(String[] args) throws Exception {
        // A Pipeline Step for loading and transforming an image file

        ImageTransformProcess imageTransformProcess = new ImageTransformProcess.Builder()
                .scaleImageTransform(20.0f)
                //.resizeImageTransform(28,28)
                .build();

        ImageLoadingStep imageLoadingStep = ImageLoadingStep.builder()
                .imageProcessingInitialLayout("NCHW")
                .imageProcessingRequiredLayout("NHWC")
                .inputName("default")
                .dimensionsConfig("default", new Long[]{ 240L, 320L, 3L }) // Height, width, channels
                .imageTransformProcess("default", imageTransformProcess)
                .build();

        String imagePath =  new ClassPathResource("images/COCO_train2014_000000000009.jpg").getFile().getAbsolutePath();

        Writable[][] output = imageLoadingStep.getRunner().transform(imagePath);

        INDArray image = ((NDArrayWritable) output[0][0]).get();

        System.out.println(Arrays.toString(image.shape()));
        System.out.println(image);
    }
}