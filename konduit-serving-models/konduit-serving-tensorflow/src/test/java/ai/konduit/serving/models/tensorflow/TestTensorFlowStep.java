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

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.models.tensorflow.step.TensorFlowPipelineStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.tensorflow.*;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.NodeDef;
import org.tensorflow.framework.SavedModel;
import org.tensorflow.types.UInt8;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TestTensorFlowStep {

    @Test
    public void testBasic(){

        File f = new File("C:\\Users\\Alex\\Downloads\\frozen_inference_graph_face.pb");

        //image_tensor", img), "detection_boxes", "detection_scores", "detection_classes", "num_detections");

        Pipeline p = SequencePipeline.builder()
                .add(TensorFlowPipelineStep.builder()
                        .modelUri(f.toURI().toString())
                        .inputNames(Arrays.asList("image_tensor:0"))
                        .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                        .build())
                .build();

        PipelineExecutor exec = p.executor();

        Image i = Image.create(new File("C:/Users/Alex/Downloads/testimage.png"));
        Data d = Data.singleton("image_tensor:0", i);

        Data out = exec.exec(null, d);
        for(String s : out.keys()){
            INDArray arr = out.getNDArray(s).getAs(INDArray.class);
            System.out.println(s);
            System.out.println(arr);
        }


    }


    @Test
    public void testBasic2(){

        File f = new File("C:\\Users\\Alex\\Downloads\\mobilenetv1_saved_model.pb");

        //image_tensor", img), "detection_boxes", "detection_scores", "detection_classes", "num_detections");

        Pipeline p = SequencePipeline.builder()
                .add(TensorFlowPipelineStep.builder()
                        .modelUri(f.toURI().toString())
                        .inputNames(Arrays.asList("image_tensor:0"))
                        .outputNames(Arrays.asList("detection_boxes", "detection_scores", "detection_classes", "num_detections"))
                        .build())
                .build();

        PipelineExecutor exec = p.executor();

        Image i = Image.create(new File("C:/Users/Alex/Downloads/testimage.png"));
        Data d = Data.singleton("image_tensor:0", i);

        Data out = exec.exec(null, d);
        for(String s : out.keys()){
            INDArray arr = out.getNDArray(s).getAs(INDArray.class);
            System.out.println(s);
            System.out.println(arr);
        }


    }


    @Test
    public void test3() throws Exception {

//        File f = new File("C:\\Temp\\TF_SavedModel\\testModel\\saved_model.pb");
        File f = new File("C:\\Users\\Alex\\Downloads\\frozen_inference_graph_face.pb");

        try(InputStream is = new BufferedInputStream(new FileInputStream(f))){
            GraphDef gd = GraphDef.parseFrom(is);
            int n = gd.getNodeCount();
            for( int i=0; i<n; i++ ){
                NodeDef nd = gd.getNode(i);
                System.out.println(i + " - " + nd.getName() + " - " + nd.getOp());
            }
        }
    }

    @Test
    public void testSavedModelGraphDef6() throws Exception {
//        File f = new File("C:\\Temp\\TF_SavedModel\\testModel\\saved_model.pb");
        File f = new File("C:\\Users\\Alex\\Downloads\\mobilenetv1_saved_model.pb");
//        File f = new File("C:\\Users\\Alex\\Downloads\\frozen_inference_graph_face.pb");
        try(InputStream is = new BufferedInputStream(new FileInputStream(f))){
            SavedModel sm = SavedModel.parseFrom(is);

            int mgc = sm.getMetaGraphsCount();
            System.out.println("Meta graph count: " + mgc);

            MetaGraphDef mgd = sm.getMetaGraphs(0);
            GraphDef gd = mgd.getGraphDef();
            int n = gd.getNodeCount();
            System.out.println("NODE COUNT: " + n);
            for( int i=0; i<n; i++ ){
                NodeDef nd = gd.getNode(i);
                System.out.println(i + " - " + nd.getName() + " - " + nd.getOp());
            }
        }
    }

    @Test
    public void testAgain() throws Exception{

        //        File f = new File("C:\\Temp\\TF_SavedModel\\testModel\\saved_model.pb");
//        File f = new File("C:\\Users\\Alex\\Downloads\\mobilenetv1_saved_model.pb");
//        File f = new File("C:\\Users\\Alex\\Downloads\\frozen_inference_graph_face.pb");
        File f = new File("C:\\Users\\Alex\\Downloads\\small_saved_model.pb");

        byte[] graph = FileUtils.readFileToByteArray(f);

        try (Graph g = new Graph()) {
            g.importGraphDef(graph);
            //open session using imported graph
            try (Session sess = new Session(g)) {
                float[][] inputData = {{4, 3, 2, 1}};
                // We have to create tensor to feed it to session,
                // unlike in Python where you just pass Numpy array
                Tensor inputTensor = Tensor.create(inputData, Float.class);
                float[][] output = predict(sess, inputTensor);
                for (int i = 0; i < output[0].length; i++) {
                    System.out.println(output[0][i]);//should be 41. 51.5 62.
                }
            }
        }
    }

    private static float[][] predict(Session sess, Tensor inputTensor) {
        Tensor result = sess.runner()
                .feed("input", inputTensor)
                .fetch("not_activated_output").run().get(0);
        float[][] outputBuffer = new float[1][3];
        result.copyTo(outputBuffer);
        return outputBuffer;
    }

    @Test
    public void testAgain2() throws Exception{

        //https://medium.com/@alexkn15/tensorflow-save-model-for-use-in-java-or-c-ab351a708ee4
//        File f = new File("C:\\Users\\Alex\\Downloads\\mobilenetv1_saved_model.pb");    //Unable to load SAVEDMODEL FORMAT
        File f = new File("C:\\Users\\Alex\\Downloads\\frozen_inference_graph_face.pb");        //https://github.com/yeephycho/tensorflow-face-detection
//        File f = new File("C:\\Users\\Alex\\Downloads\\small_saved_model.pb");

        byte[] graph = FileUtils.readFileToByteArray(f);

        Image img = Image.create(new File("C:/Users/Alex/Downloads/testimage.png"));

        ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
//                .dataType(NDArrayType.FLOAT)
                .dataType(NDArrayType.UINT8)
                .normalization(null)
                .build();

        NDArray arr = ImageToNDArray.convert(img, c);
        SerializedNDArray s = arr.getAs(SerializedNDArray.class);

//        INDArray a2f = arr.getAs(INDArray.class);
//        INDArray a2 = a2f.castTo(DataType.UINT8);
//
//        SerializedNDArray s2 = NDArray.create(a2).getAs(SerializedNDArray.class);


        try (Graph g = new Graph()) {
            g.importGraphDef(graph);

            Iterator<Operation> ops = g.operations();
            while(ops.hasNext()){
                Operation o = ops.next();
                System.out.println(o.name() + " - " + o.type());
            }

            //open session using imported graph
            try (Session sess = new Session(g)) {
//                Tensor<Float> t = Tensor.create(s.getShape(), s.getBuffer().asFloatBuffer());
//                Tensor<UInt8> t = Tensor.create(UInt8.class, s2.getShape(), s2.getBuffer());
                Tensor<UInt8> t = Tensor.create(UInt8.class, s.getShape(), s.getBuffer());

                /*
                detection_boxes - Identity
detection_scores - Identity
detection_classes - Identity
num_detections - Identity
                 */

                List<Tensor<?>> l = sess.runner()
                        .feed("image_tensor", t)
                        .fetch("detection_boxes", 0)
                        .fetch("detection_scores", 0)
                        .fetch("detection_classes", 0)
                        .fetch("num_detections", 0)
                        //.fetch("detection_boxes", "detection_scores", "detection_classes", "num_detections")
                        .run();

                for(Tensor<?> tensor : l){
                    System.out.println(tensor);
                    Tensor<Float> tFloat = tensor.expect(Float.class);
//                    tensor.dataType()
                    INDArray arr2 = NDArray.create(tFloat).getAs(INDArray.class);
                    System.out.println(arr2);
                }

//                float[][] inputData = {{4, 3, 2, 1}};
                // We have to create tensor to feed it to session,
                // unlike in Python where you just pass Numpy array
//                Tensor inputTensor = Tensor.create(inputData, Float.class);
//                float[][] output = predict(sess, inputTensor);
//                for (int i = 0; i < output[0].length; i++) {
//                    System.out.println(output[0][i]);//should be 41. 51.5 62.
//                }
            }
        }
    }



    @Test
    public void testFaceDetectorCleanFrozen() throws Exception{

        //https://medium.com/@alexkn15/tensorflow-save-model-for-use-in-java-or-c-ab351a708ee4
        File f = new File("C:\\Users\\Alex\\Downloads\\frozen_inference_graph_face.pb");        //https://github.com/yeephycho/tensorflow-face-detection

        byte[] graph = FileUtils.readFileToByteArray(f);

        Image img = Image.create(new File("C:/Users/Alex/Downloads/testimage.png"));

        ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null)
                .build();

        NDArray arr = ImageToNDArray.convert(img, c);
//        SerializedNDArray s = arr.getAs(SerializedNDArray.class);


        try (Graph g = new Graph()) {
            g.importGraphDef(graph);

            Iterator<Operation> ops = g.operations();
            while(ops.hasNext()){
                Operation o = ops.next();
                System.out.println(o.name() + " - " + o.type());
            }

            //open session using imported graph
            try (Session sess = new Session(g)) {
                Tensor<?> t = arr.getAs(Tensor.class);
                List<Tensor<?>> l = sess.runner()
                        .feed("image_tensor", t)
                        .fetch("detection_boxes", 0)
                        .fetch("detection_scores", 0)
                        .fetch("detection_classes", 0)
                        .fetch("num_detections", 0)
                        //.fetch("detection_boxes", "detection_scores", "detection_classes", "num_detections")
                        .run();

                for(Tensor<?> tensor : l){
                    System.out.println(tensor);
                    Tensor<Float> tFloat = tensor.expect(Float.class);
                    INDArray arr2 = NDArray.create(tFloat).getAs(INDArray.class);
                    System.out.println(arr2);
                }
            }
        }
    }

//    @Test
//    public void testSavedModelGraph() throws Exception {
//        File f = new File("C:\\Users\\Alex\\Downloads\\mobilenetv1_saved_model.pb");
//        try(InputStream is = new BufferedInputStream(new FileInputStream(f))){
//            SavedModel sm = SavedModel.parseFrom(is);
//            MetaGraphDef mgd = sm.getMetaGraphs(0);
//            GraphDef gd = mgd.getGraphDef();
////            int n = gd.getNodeCount();
////            for( int i=0; i<n; i++ ){
////                NodeDef nd = gd.getNode(i);
////                System.out.println(i + " - " + nd.getName() + " - " + nd.getOp());
////            }
//
////            GraphDef gd = sm.getMetaGraphs(0).getGraphDef();
////            try (Graph g = new Graph()) {
////
////
////            }
//
//
//            try (Session sess = new Session(gd)) {
//
//            }
//        }
//
//        SavedModelBundle b = null;
//        Session s =
//    }


    @Test
    public void testSavedModelGraph2() throws Exception {
//        File f = new File("C:\\Users\\Alex\\Downloads\\mobilenetv1_saved_model.pb");
        File f = new File("C:\\Temp\\tf_test\\mobilenetv1_saved_model.pb");
        try(InputStream is = new BufferedInputStream(new FileInputStream(f))){
            System.out.println("About to load");
//            SavedModelBundle b = SavedModelBundle.loader(f.getAbsolutePath()).load();     //CRASHES
            SavedModelBundle b = SavedModelBundle.loader(f.getParentFile().getAbsolutePath()).load();   //CRASHES
            System.out.println("Loaded");

            Graph g = b.graph();


            SavedModel sm = SavedModel.parseFrom(is);
            MetaGraphDef mgd = sm.getMetaGraphs(0);
            GraphDef gd = mgd.getGraphDef();

            Image img = Image.create(new File("C:/Users/Alex/Downloads/testimage.png"));

            ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                    .height(256)
                    .width(256)
                    .channelLayout(NDChannelLayout.RGB)
                    .includeMinibatchDim(true)
                    .format(NDFormat.CHANNELS_LAST)
                    .dataType(NDArrayType.UINT8)
                    .normalization(null)
                    .build();

            NDArray arr = ImageToNDArray.convert(img, c);

            Iterator<Operation> ops = g.operations();
            while(ops.hasNext()){
                Operation o = ops.next();
                System.out.println(o.name() + " - " + o.type());
                int n = o.numOutputs();
                for( int i=0; i<n; i++ )
                    System.out.println(o.output(0).dataType());

            }


            try(Session s = new Session(g)){
                Tensor<?> t = arr.getAs(Tensor.class);
                List<Tensor<?>> l = s.runner()
                        .feed("image_tensor", t)
                        .fetch("detection_boxes", 0)
                        .fetch("detection_scores", 0)
                        .fetch("detection_classes", 0)
                        .fetch("num_detections", 0)
                        //.fetch("detection_boxes", "detection_scores", "detection_classes", "num_detections")
                        .run();

                for(Tensor<?> tensor : l){
                    System.out.println(tensor);
                    Tensor<Float> tFloat = tensor.expect(Float.class);
                    INDArray arr2 = NDArray.create(tFloat).getAs(INDArray.class);
                    System.out.println(arr2);
                }
            }
        }
    }


    @Test
    public void testSavedModelGraph3() throws Exception {
//        File f = new File("C:\\Users\\Alex\\Downloads\\mobilenetv1_saved_model.pb");
        File f = new File("C:\\Temp\\tf_test\\mobilenetv1_saved_model.pb");
        System.out.println("About to load");

//        SavedModelBundle b = SavedModelBundle.loader(f.getAbsolutePath()).load();     //CRASHES
//        SavedModelBundle b = SavedModelBundle.loader(f.getParentFile().getAbsolutePath()).load();   //CRASHES

//        SavedModelBundle b = SavedModelBundle.load(f.getAbsolutePath());        //Expects DIRECTORY not .pb file
//        SavedModelBundle b = SavedModelBundle.load(f.getParentFile().getAbsolutePath()); //FAILS: org.tensorflow.TensorFlowException: Could not find SavedModel .pb or .pbtxt at supplied export directory path: C:\Temp\tf_test
        //But .pb file most definitely exists

        //SavedModelBundle b = SavedModelBundle.load(f.getParentFile().getAbsolutePath(), "mobilenetv1_saved_model");     //FAILS iwth same no .pb error
//        SavedModelBundle b = SavedModelBundle.load("C:/Temp/tf_test");    //Still not found
//        SavedModelBundle b = SavedModelBundle.load("C:/Temp/tf_test/"); //Still not found

        //Maybe it's NOT actually a SavedModel file, but a frozen model file!?


//        SavedModelBundle b = SavedModelBundle.load("C:/Temp/tf_test2");
//        SavedModelBundle b = SavedModelBundle.load("C:/Temp/tf_test2/");
//        SavedModelBundle b = SavedModelBundle.load("C:/Temp/tf_test2/small_saved_model.pb");
//        SavedModelBundle b = SavedModelBundle.load("C:\\Temp\\tf_test2\\small_saved_model.pb");
//        SavedModelBundle b = SavedModelBundle.load("C:\\Temp\\tf_test2\\");

        //OMFG - hardcoded filename "saved_model.pb" not mentioned anywhere in docs!!!
        //https://github.com/tensorflow/tensorflow/search?q=saved_model.pb&unscoped_q=saved_model.pb

        //After renaming to saved_model.pb
        SavedModelBundle b = SavedModelBundle.load(f.getParentFile().getAbsolutePath(), "serve");


        Graph g = b.graph();

        //FAILS: "Invalid GraphDef"
//        Graph g = new Graph();
//        g.importGraphDef(FileUtils.readFileToByteArray(f));           //FAILS

//        SavedModel sm = SavedModel.parseFrom(FileUtils.readFileToByteArray(f));
//        MetaGraphDef mgd = sm.getMetaGraphs(0);
//        GraphDef gd = mgd.getGraphDef();
//        Graph g = new Graph();
//        new Graph()

        System.out.println("Loaded");

        Image img = Image.create(new File("C:/Users/Alex/Downloads/testimage.png"));

        ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null)
                .build();

        NDArray arr = ImageToNDArray.convert(img, c);

        Iterator<Operation> ops = g.operations();
        while(ops.hasNext()){
            Operation o = ops.next();
            System.out.println(o.name() + " - " + o.type());
            //FFS: java.lang.IllegalArgumentException: DataType 20 is not recognized in Java (version 1.15.0)
//            int n = o.numOutputs();
//            for( int i=0; i<n; i++ )
//                System.out.println(o.output(0).dataType());

        }


        try(Session s = new Session(g)){
            Tensor<?> t = arr.getAs(Tensor.class);
            List<Tensor<?>> l = s.runner()
                    .feed("image_tensor", t)
                    .fetch("detection_boxes", 0)
                    .fetch("detection_scores", 0)
                    .fetch("detection_classes", 0)
                    .fetch("num_detections", 0)
                    //.fetch("detection_boxes", "detection_scores", "detection_classes", "num_detections")
                    .run();

            for(Tensor<?> tensor : l){
                System.out.println(tensor);
                Tensor<Float> tFloat = tensor.expect(Float.class);
                INDArray arr2 = NDArray.create(tFloat).getAs(INDArray.class);
                System.out.println(arr2);
            }
        }
    }
}
