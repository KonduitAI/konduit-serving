/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.models.tensorflow;

import ai.konduit.serving.camera.step.capture.FrameCapturePipelineStep;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.data.image.step.draw.DrawBoundingBoxStep;
import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.models.tensorflow.step.TensorFlowPipelineStep;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphBuilder;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.SSDToBoundingBoxStep;
import ai.konduit.serving.pipeline.util.ArchiveUtils;
import ai.konduit.serving.pipeline.util.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Slf4j
public class TestTensorFlowStep {

    @Test @Ignore   //To be run manually due to need for webcam and frame output
    public void testFrozenModelGraph() throws Exception {
        //Pretrained model source: https://github.com/yeephycho/tensorflow-face-detection
        String fileUrl = "https://drive.google.com/uc?export=download&id=0B5ttP5kO_loUdWZWZVVrN2VmWFk";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/facedetection");
        File f = new File(saveDir, "frozen_inference_graph_face.pb");

        if (!f.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, f.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(fileUrl), f);
            log.info("Download complete");
        }

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        //Capture frame from webcam
        GraphStep camera = input.then("camera", FrameCapturePipelineStep.builder()
                .camera(0)
                .outputKey("image")
                .build());

        //Convert image to NDArray (can configure size, BGR/RGB, normalization, etc here)
        ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null)
                .build();

        GraphStep i2n = camera.then("image2NDArray", ImageToNDArrayStep.builder()
                .config(c)
                .keys(Arrays.asList("image"))
                .outputNames(Arrays.asList("image_tensor")) //TODO varargs builder method
                .build());

        //Run image in TF model
        GraphStep tf = i2n.then("tf", TensorFlowPipelineStep.builder()
                .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                .modelUri(f.toURI().toString())      //Face detection model
                .build());

        //Post process SSD outputs to BoundingBox objects
        GraphStep ssdProc = tf.then("bbox", SSDToBoundingBoxStep.builder()
                .outputName("img_bbox")
                .build());

        //Merge camera image with bounding boxes
        GraphStep merged = camera.mergeWith("img_bbox", ssdProc);

        //Draw bounding boxes on the image
        GraphStep drawer = merged.then("drawer", DrawBoundingBoxStep.builder()
                .imageName("image")
                .bboxName("img_bbox")
                .lineThickness(2)
                .build());

        //Show image in Java frame
        GraphStep show = drawer.then("show", ShowImagePipelineStep.builder()
                .displayName("Face detection")
                .imageName("image")
                .build());


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for( int i=0; i<1000; i++ ){
            exec.exec(in);
        }
    }


    @Test
    public void testFrozenModel() throws Exception {

        //Pretrained model source: https://github.com/yeephycho/tensorflow-face-detection
        String fileUrl = "https://drive.google.com/uc?export=download&id=0B5ttP5kO_loUdWZWZVVrN2VmWFk";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/facedetection");
        File f = new File(saveDir, "frozen_inference_graph_face.pb");

        if (!f.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, f.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(fileUrl), f);
            log.info("Download complete");
        }

        ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null)
                .build();

        Pipeline p = SequencePipeline.builder()
                .add(ImageToNDArrayStep.builder()
                        .config(c)
                        .outputNames(Arrays.asList("image_tensor")) //TODO varargs builder method
                    .build())
                .add(TensorFlowPipelineStep.builder()
                        .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                        .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                        .modelUri(f.toURI().toString())      //Face detection model
                .build())
                .add(SSDToBoundingBoxStep.builder()
                        .keepOtherValues(false)
                        .outputName("bbox")
                    .build())
                .build();

        Image img = Image.create(Resources.asFile("data/mona_lisa.png"));
        PipelineExecutor exec = p.executor();

        Data in = Data.singleton("image", img);
        Data out = exec.exec(in);

        List<BoundingBox> l = out.getListBoundingBox("bbox");

        assertEquals(1, l.size());
    }



    @Test
    public void testSavedModel() throws Exception {

        String url = "http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v2_coco_2018_03_29.tar.gz";

        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/ssd_mobilenet_v2_coco_2018_03_29/");
        File archive = new File(saveDir, "ssd_mobilenet_v2_coco_2018_03_29.tar.gz");
        File f = new File(saveDir, "frozen_model.pb");

        if (!archive.exists()) {
            log.info("Downloading model: {} -> {}", url, archive.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(url), archive);
            log.info("Download complete");
        }
        if(!f.exists()){
            ArchiveUtils.tarGzExtractSingleFile(archive, f, "ssd_mobilenet_v2_coco_2018_03_29/saved_model/saved_model.pb");
        }

        String testImageUrl = "https://github.com/tensorflow/models/blob/master/research/deeplab/g3doc/img/image2.jpg?raw=true";
        File testImg = new File(saveDir, "image2.jpg");
        if(!testImg.exists()){
            log.info("Downloading test image...");
            FileUtils.copyURLToFile(new URL(testImageUrl), testImg);
        }



        ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                .height(128)
                .width(128)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null)
                .build();

        Pipeline p = SequencePipeline.builder()
                .add(ImageToNDArrayStep.builder()
                        .config(c)
                        .build())
                .add(TensorFlowPipelineStep.builder()
                        .modelUri(f.toURI().toString())
                        .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                        .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                        .build())
                .add(new SSDToBoundingBoxStep())
                .build();

        Image img = Image.create(testImg);

        PipelineExecutor exec = p.executor();

        log.info("About to perform inference...");
        try {
            Data in = Data.singleton("image_tensor", img);

            Data out = exec.exec(in);
            System.out.println(out);
            System.out.println(out.getListBoundingBox("bounding_boxes"));
        } catch (Throwable t){
            t.printStackTrace();
            throw t;
        } finally {
            log.info("Closing executor...");
            exec.close();
        }
    }
}
