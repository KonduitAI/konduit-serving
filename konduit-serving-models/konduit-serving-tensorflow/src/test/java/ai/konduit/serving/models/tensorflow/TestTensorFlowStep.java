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

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.data.image.step.bb.draw.DrawBoundingBoxStep;
import ai.konduit.serving.data.image.step.bb.extract.ExtractBoundingBoxStep;
import ai.konduit.serving.data.image.step.capture.CameraFrameCaptureStep;
import ai.konduit.serving.data.image.step.capture.VideoFrameCaptureStep;
import ai.konduit.serving.data.image.step.face.DrawFaceKeyPointsStep;
import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.data.image.step.point.draw.DrawPointsStep;
import ai.konduit.serving.data.image.step.point.heatmap.DrawHeatmapStep;
import ai.konduit.serving.data.image.step.segmentation.index.DrawSegmentationStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
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
import ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchFn;
import ai.konduit.serving.pipeline.impl.step.bbox.filter.BoundingBoxFilterStep;
import ai.konduit.serving.pipeline.impl.step.bbox.point.BoundingBoxToPointStep;
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

import static ai.konduit.serving.models.tensorflow.step.TensorFlowPipelineStep.builder;
import static org.junit.Assert.assertEquals;

@Slf4j
public class TestTensorFlowStep {

    @Test
    @Ignore   //To be run manually due to need for webcam and frame output
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
        GraphStep camera = input.then("camera", new CameraFrameCaptureStep()
                .camera(0)
                .outputKey("image")
        );

        //Convert image to NDArray (can configure size, BGR/RGB, normalization, etc here)
        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        GraphStep i2n = camera.then("image2NDArray", new ImageToNDArrayStep()
                .config(c)
                .keys(Arrays.asList("image"))
                .outputNames(Arrays.asList("image_tensor")) //TODO varargs builder method
        );

        //Run image in TF model
        GraphStep tf = i2n.then("tf", builder()
                .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                .modelUri(f.toURI().toString())      //Face detection model
                .build());

        //Post process SSD outputs to BoundingBox objects
        GraphStep ssdProc = tf.then("bbox", new SSDToBoundingBoxStep()
                .outputName("img_bbox"));

        //Merge camera image with bounding boxes
        GraphStep merged = camera.mergeWith("img_bbox", ssdProc);

        //Draw bounding boxes on the image
        GraphStep drawer = merged.then("drawer", new DrawBoundingBoxStep()
                .imageName("image")
                .bboxName("img_bbox")
                .lineThickness(2)
                .imageToNDArrayConfig(c)        //Provide the config to account for the fact that the input image is cropped
                .drawCropRegion(true)           //Draw the region of the camera that is cropped when using ImageToNDArray
        );

        /*
        //Crop out the detected face region instead, for visualization
        //This works, but is a little buggy ATM as there's obviously no image to draw when there's no face, and it
        // can't yet draw multiple images simultaneously
        GraphStep drawer = merged.then("drawer", ExtractBoundingBoxStep()
                .imageName("image")
                .bboxName("img_bbox")
                .imageToNDArrayConfig(c)        //Provide the config to account for the fact that the input image is cropped
                .build());
         */

        //Show image in Java frame
        GraphStep show = drawer.then("show", new ShowImagePipelineStep()
                .displayName("Face detection")
                .imageName("image")
        );


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for (int i = 0; i < 1000; i++) {
            exec.exec(in);
        }
    }


    @Ignore("24/06/2020 Failed on CI https://github.com/KonduitAI/konduit-serving/issues/403")
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

        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        Pipeline p = SequencePipeline.builder()
                .add(new ImageToNDArrayStep()
                        .config(c)
                        .outputNames(Arrays.asList("image_tensor")) //TODO varargs builder method
                )
                .add(builder()
                        .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                        .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                        .modelUri(f.toURI().toString())      //Face detection model
                        .build())
                .add(new SSDToBoundingBoxStep()
                        .keepOtherValues(false)
                        .outputName("bbox")
                )
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
        if (!f.exists()) {
            ArchiveUtils.tarGzExtractSingleFile(archive, f, "ssd_mobilenet_v2_coco_2018_03_29/saved_model/saved_model.pb");
        }

        String testImageUrl = "https://github.com/tensorflow/models/blob/master/research/deeplab/g3doc/img/image2.jpg?raw=true";
        File testImg = new File(saveDir, "image2.jpg");
        if (!testImg.exists()) {
            log.info("Downloading test image...");
            FileUtils.copyURLToFile(new URL(testImageUrl), testImg);
        }


        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(128)
                .width(128)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        Pipeline p = SequencePipeline.builder()
                .add(new ImageToNDArrayStep()
                        .config(c)
                )
                .add(builder()
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
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            log.info("Closing executor...");
            exec.close();
        }
    }


    @Test
    @Ignore   //To be run manually due to need for webcam and frame output
    public void testPersonDetection() throws Exception {
        //Pretrained model source: https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md#coco-trained-models

        String fileUrl = "http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_coco_2018_01_28.tar.gz";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/persondetection");
        File f = new File(saveDir, "frozen_inference_graph.pb");

        if (!f.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, saveDir.getAbsolutePath());
            File archive = new File(saveDir, "ssd_mobilenet_v1_coco_2018_01_28.tar.gz");
            FileUtils.copyURLToFile(new URL(fileUrl), archive);
            ArchiveUtils.tarGzExtractSingleFile(archive, f, "ssd_mobilenet_v1_coco_2018_01_28/frozen_inference_graph.pb");
            log.info("Download complete");
        }

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        //Capture frame from webcam
        GraphStep camera = input.then("camera", new CameraFrameCaptureStep()
                .camera(0)
                .outputKey("image")
        );

        //Convert image to NDArray (can configure size, BGR/RGB, normalization, etc here)
        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(300)  // https://github.com/tensorflow/models/blob/master/research/object_detection/samples/configs/ssd_mobilenet_v1_coco.config#L43L46
                .width(300)   // size origin
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        GraphStep i2n = camera.then("image2NDArray", new ImageToNDArrayStep()
                .config(c)
                .keys(Arrays.asList("image"))
                .outputNames(Arrays.asList("image_tensor")) //TODO varargs builder method
        );

        //Run image in TF model
        GraphStep tf = i2n.then("tf", builder()
                .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                .modelUri(f.toURI().toString())      //Face detection model
                .build());

        //Post process SSD outputs to BoundingBox objects
        GraphStep ssdProc = tf.then("bbox", new SSDToBoundingBoxStep()
                .outputName("img_bbox")
        );

        //Merge camera image with bounding boxes
        GraphStep merged = camera.mergeWith("img_bbox", ssdProc);

        //Draw bounding boxes on the image
        GraphStep drawer = merged.then("drawer", new DrawBoundingBoxStep()
                .imageName("image")
                .bboxName("img_bbox")
                .lineThickness(2)
                .imageToNDArrayConfig(c)        //Provide the config to account for the fact that the input image is cropped
                .drawCropRegion(true)           //Draw the region of the camera that is cropped when using ImageToNDArray
        );


        //Show image in Java frame
        GraphStep show = drawer.then("show", new ShowImagePipelineStep()
                .displayName("person detection")
                .imageName("image")
        );


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for (int i = 0; i < 1000; i++) {
            exec.exec(in);
        }
    }

    @Test
    @Ignore   //To be run manually due to need for webcam and frame output
    public void testPersonDetectionFromVideo() throws Exception {
        //Pretrained model source: https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md#coco-trained-models

        String fileUrl = "http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_coco_2018_01_28.tar.gz";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/persondetection");
        File f = new File(saveDir, "frozen_inference_graph.pb");

        if (!f.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, saveDir.getAbsolutePath());
            File archive = new File(saveDir, "ssd_mobilenet_v1_coco_2018_01_28.tar.gz");
            FileUtils.copyURLToFile(new URL(fileUrl), archive);
            ArchiveUtils.tarGzExtractSingleFile(archive, f, "ssd_mobilenet_v1_coco_2018_01_28/frozen_inference_graph.pb");
            log.info("Download complete");
        }

        String videoUrl = "http://www.robots.ox.ac.uk/ActiveVision/Research/Projects/2009bbenfold_headpose/Datasets/TownCentreXVID.avi";
        File v = new File(saveDir, "TownCentreXVID.avi");
        if (!v.exists()) {
            log.info("Downloading demo video: {} -> {}", videoUrl, saveDir.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(videoUrl), v);
            log.info("Download complete");
        }

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        //Capture frame from video
        GraphStep camera = input.then("video", new VideoFrameCaptureStep()
                .filePath(v.getAbsolutePath())
                .outputKey("image")
        );

        //Convert image to NDArray (can configure size, BGR/RGB, normalization, etc here)
        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(300)  // https://github.com/tensorflow/models/blob/master/research/object_detection/samples/configs/ssd_mobilenet_v1_coco.config#L43L46
                .width(300)   // size origin
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        GraphStep i2n = camera.then("image2NDArray", new ImageToNDArrayStep()
                .config(c)
                .keys(Arrays.asList("image"))
                .outputNames(Arrays.asList("image_tensor")) //TODO varargs builder method
        );

        //Run image in TF model
        GraphStep tf = i2n.then("tf", builder()
                .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                .modelUri(f.toURI().toString())      //Face detection model
                .build());

        //Post process SSD outputs to BoundingBox objects
        GraphStep ssdProc = tf.then("bbox", new SSDToBoundingBoxStep()
                .outputName("img_bbox")
        );

        //Merge camera image with bounding boxes
        GraphStep merged = camera.mergeWith("img_bbox", ssdProc);

        //Draw bounding boxes on the image
        GraphStep drawer = merged.then("drawer", new DrawBoundingBoxStep()
                .imageName("image")
                .bboxName("img_bbox")
                .lineThickness(2)
                .imageToNDArrayConfig(c)        //Provide the config to account for the fact that the input image is cropped
                .drawCropRegion(true)           //Draw the region of the camera that is cropped when using ImageToNDArray
        );


        //Show image in Java frame
        GraphStep show = drawer.then("show", new ShowImagePipelineStep()
                .displayName("person detection")
                .imageName("image")
        );


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for (int i = 0; i < 1000; i++) {
            exec.exec(in);
        }
    }


    @Test
    @Ignore   //To be run manually due to need for webcam and frame output
    public void testImageSegmentation() throws Exception {
        //Pretrained model source:https://github.com/tensorflow/models/blob/master/research/deeplab/g3doc/model_zoo.md#model-details

        String fileUrl = "http://download.tensorflow.org/models/deeplabv3_mnv2_dm05_pascal_trainval_2018_10_01.tar.gz";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/image-segmentation");
        File f = new File(saveDir, "frozen_inference_graph.pb");

        if (!f.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, saveDir.getAbsolutePath());
            File archive = new File(saveDir, "image_segmentation.tar.gz");
            FileUtils.copyURLToFile(new URL(fileUrl), archive);
            ArchiveUtils.tarGzExtractSingleFile(archive, f, "deeplabv3_mnv2_dm05_pascal_trainval/frozen_inference_graph.pb");
            log.info("Download complete");
        }

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        //Capture frame from webcam
        GraphStep camera = input.then("camera", new CameraFrameCaptureStep()
                .camera(0)
                .outputKey("image")
        );

        //Convert image to NDArray (can configure size, BGR/RGB, normalization, etc here)
        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(300)
                .width(300)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        GraphStep i2n = camera.then("image2NDArray", new ImageToNDArrayStep()
                .config(c)
                .keys(Arrays.asList("image"))
                .outputNames(Arrays.asList("ImageTensor")) //TODO varargs builder method
                );

        //Run image in TF model
        GraphStep tf = i2n.then("tf", builder()
                .inputNames(Collections.singletonList("ImageTensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("SemanticPredictions"))
                .modelUri(f.toURI().toString())
                .build());


        //Merge camera image with bounding boxes
        GraphStep merged = camera.mergeWith("img_segmentation", tf);

        //Draw bounding boxes on the image
        GraphStep drawer = merged.then("drawer", new DrawSegmentationStep()
                .image("image")
                .segmentArray("SemanticPredictions")
                .opacity(0.5)
                .outputName("out")
                .imageToNDArrayConfig(c)
                .backgroundClass(0)
        );


        //Show image in Java frame
        GraphStep show = drawer.then("show", new ShowImagePipelineStep()
                .displayName("image segmentation")
                .imageName("out")
        );


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for (int i = 0; i < 1000; i++) {
            exec.exec(in);
        }
    }


    @Test
    @Ignore   //To be run manually due to need for webcam and frame output
    public void testBBoxFilter() throws Exception {
        //Pretrained model source:https://github.com/tensorflow/models/blob/master/research/deeplab/g3doc/model_zoo.md#model-details

        String fileUrl = "https://github.com/kcg2015/Vehicle-Detection-and-Tracking/raw/master/ssd_mobilenet_v1_coco_11_06_2017/frozen_inference_graph.pb";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/bbox-filter");
        File f = new File(saveDir, "frozen_inference_graph.pb");

        if (!f.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, f.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(fileUrl), f);
            log.info("Download complete");
        }

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();
        //Capture frame from webcam
        GraphStep camera = input.then("camera", new CameraFrameCaptureStep()
                .camera(0)
                .outputKey("image")
        );

        //Convert image to NDArray (can configure size, BGR/RGB, normalization, etc here)
        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(300)  // https://github.com/tensorflow/models/blob/master/research/object_detection/samples/configs/ssd_mobilenet_v1_coco.config#L43L46
                .width(300)   // size origin
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        GraphStep i2n = camera.then("image2NDArray", new ImageToNDArrayStep()
                .config(c)
                .keys(Arrays.asList("image"))
                .outputNames(Arrays.asList("image_tensor")) //TODO varargs builder method
                );

        //Run image in TF model
        GraphStep tf = i2n.then("tf", builder()
                .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                .modelUri(f.toURI().toString())
                .build());

        //Post process SSD outputs to BoundingBox objects
        String[] COCO_LABELS = new String[]{"person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light", "fire hydrant", "street sign", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "hat", "backpack", "umbrella", "shoe", "eye glasses", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle", "plate", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed", "mirror", "dining table", "window", "desk", "toilet", "door", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "blender", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush", "hair brush"};

        GraphStep ssdProc = tf.then("bbox", new SSDToBoundingBoxStep()
                .outputName("img_bbox")
                .classLabels(SSDToBoundingBoxStep.COCO_LABELS)
        );

        String[] classesToKeep = new String[]{"person", "car"};

        //Post process SSD outputs to BoundingBox objects
        GraphStep bboxFilter = ssdProc.then("filtered_bbox", new BoundingBoxFilterStep()
                .classesToKeep(classesToKeep)
                .inputName("img_bbox")
                .outputName("img_filtered_bbox")
        );


        //Merge camera image with bounding boxes
        GraphStep merged = camera.mergeWith("img_filtered_bbox", bboxFilter);

        //Draw bounding boxes on the image
        GraphStep drawer = merged.then("drawer", new DrawBoundingBoxStep()
                .imageName("image")
                .bboxName("img_filtered_bbox")
                .lineThickness(2)
                .imageToNDArrayConfig(c)        //Provide the config to account for the fact that the input image is cropped
                .drawCropRegion(true)           //Draw the region of the camera that is cropped when using ImageToNDArray
        );


        //Show image in Java frame
        GraphStep show = drawer.then("show", new ShowImagePipelineStep()
                .displayName("bbox filter")
                .imageName("image")
        );


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for (int i = 0; i < 1000; i++) {
            exec.exec(in);
        }

    }

    @Test
    @Ignore   //To be run manually due to need for webcam and frame output
    public void testFacialKeyPointsDetection() throws Exception {
        //Pretrained model source:https://github.com/songhengyang/face_landmark_factory

        // keypoint model downloading
        String fileUrl = "https://github.com/songhengyang/face_landmark_factory/raw/master/model/facial_landmark_SqueezeNet.pb";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/face-keypoints-detection");
        File keypoints_graph = new File(saveDir, "keypoints_detection_frozen_inference_graph.pb");

        if (!keypoints_graph.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, saveDir.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(fileUrl), keypoints_graph);
            log.info("Download complete");
        }

        // face bbox model downloading
        //Pretrained model source: https://github.com/yeephycho/tensorflow-face-detection
        fileUrl = "https://drive.google.com/u/0/uc?id=0B5ttP5kO_loUdWZWZVVrN2VmWFk&export=download";
        testDir = TestUtils.testResourcesStorageDir();
        saveDir = new File(testDir, "konduit-serving-tensorflow/face-keypoints-detection");
        File face_detector_graph = new File(saveDir, "face_detection_frozen_inference_graph.pb");

        if (!face_detector_graph.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, saveDir.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(fileUrl), face_detector_graph);
            log.info("Download complete");
        }

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        //Capture frame from webcam
        GraphStep camera = input.then("camera", new CameraFrameCaptureStep()
//                    .height(720)
//                    .width(1280)
                        .height(360)
                        .width(640)
                        .camera(0)
                        .outputKey("image")
        );

        //Detect faces using mobilenet model: image -> NDArray -> TF -> SSD post processor
        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(128)  // https://github.com/tensorflow/models/blob/master/research/object_detection/samples/configs/ssd_mobilenet_v1_coco.config#L43L46
                .width(128)   // size origin
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);


        GraphStep i2n = camera.then("image2NDArrayFaceDetectorInference", new ImageToNDArrayStep()
                .config(c)
                .keys(Collections.singletonList("image"))
                .outputNames(Collections.singletonList("image_tensor")) //TODO varargs builder method
        );

        GraphStep tf = i2n.then("tf", builder()
                .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                .modelUri(face_detector_graph.toURI().toString()).build()
        );

        GraphStep ssdProc = tf.then("bbox", new SSDToBoundingBoxStep()
                .outputName("img_bbox")
                .keepOtherValues(true)
                .threshold(0.5)
                .scale(1.15)
                .aspectRatio(1.0)
        );


        //Extract the face bounding boxes as images
        GraphStep merged1 = ssdProc.mergeWith("merge1", camera);

        GraphStep[] switchOut = b.switchOp("switch", new SwitchFn() {
            @Override
            public int numOutputs() {
                return 2;
            }

            @Override
            public int selectOutput(Data data) {
                return data.getListBoundingBox("img_bbox").isEmpty() ? 0 : 1;
            }
        }, merged1);
        GraphStep noBoxes = switchOut[0];
        GraphStep withBoxes = switchOut[1];


        GraphStep extractBBox = withBoxes.then("extracted_bbox", new ExtractBoundingBoxStep()
                .imageName("image")
                .bboxName("img_bbox")
                .imageToNDArrayConfig(c)
                .outputName("face_image_bbox")
                .keepOtherFields(false)
        );


        //Convert the face bounding boxes to NDArrays
        ImageToNDArrayConfig faceImageConfig = new ImageToNDArrayConfig()
                .height(128)
                .width(128)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                //Model expects "subtract mean" normalization
                .normalization(new ImageNormalization(ImageNormalization.Type.SUBTRACT_MEAN).meanRgb(new double[]{104.0, 177.0, 123.0}))
                .listHandling(ImageToNDArrayConfig.ListHandling.FIRST);  //These models only support minibatch 1

        GraphStep face2n = extractBBox.then("FaceBBoxtoNDArray", new ImageToNDArrayStep()
                .config(faceImageConfig)
                .keys(Arrays.asList("face_image_bbox"))
                .outputNames(Arrays.asList("input_image_tensor"))   //Name to match face keypoint
        );


        //Detect keypoints on the face boxes
        GraphStep tf_keydetector = face2n.then("keydetector", builder()
                .inputNames(Collections.singletonList("input_image_tensor"))
                .outputNames(Arrays.asList("logits/BiasAdd"))
                .modelUri(keypoints_graph.toURI().toString())
                .build());

        //  Merge camera image with face keypoints
        GraphStep merged = camera.mergeWith("facial-keypoints", ssdProc, tf_keydetector);

        // Draw face keypoints on the image
        GraphStep drawer = merged.then("keypoints-drawer", new DrawFaceKeyPointsStep()
                .image("image")
                .imageToNDArrayConfig(c)
                .landmarkArray("logits/BiasAdd")
        );


        //Show image in Java frame

        GraphStep any = b.any("any", noBoxes, drawer);
        GraphStep show = /*drawer.*/any.then("show", new ShowImagePipelineStep()
                .displayName("Face keypoints")
                .imageName("image")
        );


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for (int i = 0; i < 1000; i++) {
            exec.exec(in);
        }
    }

    @Test
    @Ignore   //To be run manually due to need for webcam and frame output
    public void testPersonDetectionHeatmap() throws Exception {
        //Pretrained model source: https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md#coco-trained-models

        String fileUrl = "http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_coco_2018_01_28.tar.gz";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/persondetection");
        File f = new File(saveDir, "frozen_inference_graph.pb");

        if (!f.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, saveDir.getAbsolutePath());
            File archive = new File(saveDir, "ssd_mobilenet_v1_coco_2018_01_28.tar.gz");
            FileUtils.copyURLToFile(new URL(fileUrl), archive);
            ArchiveUtils.tarGzExtractSingleFile(archive, f, "ssd_mobilenet_v1_coco_2018_01_28/frozen_inference_graph.pb");
            log.info("Download complete");
        }

        String videoUrl = "http://www.robots.ox.ac.uk/ActiveVision/Research/Projects/2009bbenfold_headpose/Datasets/TownCentreXVID.avi";
        File v = new File(saveDir, "TownCentreXVID.avi");
        if (!v.exists()) {
            log.info("Downloading demo video: {} -> {}", videoUrl, saveDir.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(videoUrl), v);
            log.info("Download complete");
        }

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        //Capture frame from video
        GraphStep camera = input.then("video", new VideoFrameCaptureStep()
                .filePath(v.getAbsolutePath())
                .outputKey("image"));

        //Convert image to NDArray (can configure size, BGR/RGB, normalization, etc here)
        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(300)  // https://github.com/tensorflow/models/blob/master/research/object_detection/samples/configs/ssd_mobilenet_v1_coco.config#L43L46
                .width(300)   // size origin
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        GraphStep i2n = camera.then("image2NDArray", new ImageToNDArrayStep()
                .config(c)
                .keys(Arrays.asList("image"))
                .outputNames(Arrays.asList("image_tensor")));

        //Run image in TF model
        GraphStep tf = i2n.then("tf", builder()
                .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                .modelUri(f.toURI().toString())      //Face detection model
                .build());

        //Post process SSD outputs to BoundingBox objects
        GraphStep ssdProc = tf.then("bbox", new SSDToBoundingBoxStep()
                .outputName("img_bbox"));

        //Merge camera image with bounding boxes
        GraphStep merged = camera.mergeWith("img_bbox", ssdProc);

        GraphStep drawer = merged.then("toPoints", new BoundingBoxToPointStep()
                .bboxName("img_bbox")
                .method(BoundingBoxToPointStep.ConversionMethod.CENTER)
                .keepOtherFields(true)
                .outputName("img_points")
        ).then("drawer", new DrawHeatmapStep()
                .image("image")
                .imageToNDArrayConfig(c)        //Provide the config to account for the fact that the input image is cropped
                .points(Arrays.asList("img_points"))
                .fadingFactor(1.0)
                .radius(30));


        //Show image in Java frame
        GraphStep show = drawer.then("show", new ShowImagePipelineStep()
                .displayName("person detection")
                .imageName("image")
                .width(800)
                .height(450));


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for (int i = 0; i < 1000; i++) {
            exec.exec(in);
        }
    }

    @Test
    @Ignore   //To be run manually due to need for webcam and frame output
    public void testPersonDetectionPoints() throws Exception {
        //Pretrained model source: https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md#coco-trained-models

        String fileUrl = "http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_coco_2018_01_28.tar.gz";
        File testDir = TestUtils.testResourcesStorageDir();
        File saveDir = new File(testDir, "konduit-serving-tensorflow/persondetection");
        File f = new File(saveDir, "frozen_inference_graph.pb");

        if (!f.exists()) {
            log.info("Downloading model: {} -> {}", fileUrl, saveDir.getAbsolutePath());
            File archive = new File(saveDir, "ssd_mobilenet_v1_coco_2018_01_28.tar.gz");
            FileUtils.copyURLToFile(new URL(fileUrl), archive);
            ArchiveUtils.tarGzExtractSingleFile(archive, f, "ssd_mobilenet_v1_coco_2018_01_28/frozen_inference_graph.pb");
            log.info("Download complete");
        }

        String videoUrl = "http://www.robots.ox.ac.uk/ActiveVision/Research/Projects/2009bbenfold_headpose/Datasets/TownCentreXVID.avi";
        File v = new File(saveDir, "TownCentreXVID.avi");
        if (!v.exists()) {
            log.info("Downloading demo video: {} -> {}", videoUrl, saveDir.getAbsolutePath());
            FileUtils.copyURLToFile(new URL(videoUrl), v);
            log.info("Download complete");
        }

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        //Capture frame from video
        GraphStep camera = input.then("video", new VideoFrameCaptureStep()
                .filePath(v.getAbsolutePath())
                .outputKey("image"));

        //Convert image to NDArray (can configure size, BGR/RGB, normalization, etc here)
        ImageToNDArrayConfig c = new ImageToNDArrayConfig()
                .height(300)  // https://github.com/tensorflow/models/blob/master/research/object_detection/samples/configs/ssd_mobilenet_v1_coco.config#L43L46
                .width(300)   // size origin
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null);

        GraphStep i2n = camera.then("image2NDArray", new ImageToNDArrayStep()
                .config(c)
                .keys(Arrays.asList("image"))
                .outputNames(Arrays.asList("image_tensor")));

        //Run image in TF model
        GraphStep tf = i2n.then("tf", builder()
                .inputNames(Collections.singletonList("image_tensor"))      //TODO varargs builder method
                .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                .modelUri(f.toURI().toString())      //Face detection model
                .build());

        //Post process SSD outputs to BoundingBox objects
        GraphStep ssdProc = tf.then("bbox", new SSDToBoundingBoxStep().outputName("img_bbox"));

        //Merge camera image with bounding boxes
        GraphStep merged = camera.mergeWith("img_bbox", ssdProc);

        GraphStep drawer = merged.then("toPoints", new BoundingBoxToPointStep()
                .bboxName("img_bbox")
                .method(BoundingBoxToPointStep.ConversionMethod.CENTER)
                .keepOtherFields(true)
                .outputName("img_points")
        ).then("drawer", new DrawPointsStep()
                .image("image")
                .imageToNDArrayConfig(c)        //Provide the config to account for the fact that the input image is cropped
                .points(Arrays.asList("img_points"))
                .classColors(Collections.singletonMap(null, "red"))
                .radius(5));


        //Show image in Java frame
        GraphStep show = drawer.then("show", new ShowImagePipelineStep()
                .displayName("person detection")
                .imageName("image")
                .width(800)
                .height(450));


        GraphPipeline p = b.build(show);


        PipelineExecutor exec = p.executor();

        Data in = Data.empty();
        for (int i = 0; i < 1000; i++) {
            exec.exec(in);
        }
    }
}



