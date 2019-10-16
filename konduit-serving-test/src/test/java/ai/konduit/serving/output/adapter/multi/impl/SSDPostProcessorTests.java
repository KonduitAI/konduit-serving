/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.output.adapter.multi.impl;

import ai.konduit.serving.executioner.inference.TensorflowInferenceExecutioner;
import ai.konduit.serving.input.conversion.ConverterArgs;
import ai.konduit.serving.model.loader.tensorflow.TensorflowModelLoader;
import ai.konduit.serving.output.types.BatchOutput;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.image.VertxBufferImageInputAdapter;
import ai.konduit.serving.output.adapter.SSDOutputAdapter;
import ai.konduit.serving.output.adapter.YOLOOutputAdapter;
import ai.konduit.serving.config.ParallelInferenceConfig;
import io.vertx.core.buffer.Buffer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.image.data.Image;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.transform.ImageTransformProcess;
import org.deeplearning4j.parallelism.inference.InferenceMode;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.compression.BasicNDArrayCompressor;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2RGB;

@Ignore
public class SSDPostProcessorTests {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();


    @Test(timeout = 60000)

    public void testPreProcessing() throws Exception {

//        TODO tf_graphs does not exist
        ClassPathResource classPathResource = new ClassPathResource("/tf_graphs/examples/ssd_inception_v2_coco_2018_01_28/frozen_inference_graph.pb");
        File tmpFile = folder.newFile();


        try(InputStream is = classPathResource.getInputStream();
            FileOutputStream bufferedOutputStream = new FileOutputStream(tmpFile)) {
            IOUtils.copy(is,bufferedOutputStream);
            bufferedOutputStream.flush();
        }

        File image = new ClassPathResource("sample_yolo_input.jpeg").getFile();
        byte[] content = FileUtils.readFileToByteArray(image);
        NativeImageLoader nativeImageLoader = new NativeImageLoader();
        Image image1 = nativeImageLoader.asImageMatrix(image);
        String[] outputs = new String[] { "detection_boxes", "detection_scores", "detection_classes", "num_detections"};

        SSDOutputAdapter yoloOutputAdapter = new SSDOutputAdapter(0.5,80);
        YOLOOutputAdapter yoloOutputAdapter1 = new YOLOOutputAdapter(0.5,80);
        TensorflowInferenceExecutioner tensorflowInferenceExecutioner = new TensorflowInferenceExecutioner();

        TensorflowModelLoader tensorflowModelLoader = TensorflowModelLoader.builder()
                .inputNames(Arrays.asList(yoloOutputAdapter.getInputs()))
                .outputNames(Arrays.asList(outputs))
                .build();

        tensorflowInferenceExecutioner.initialize(tensorflowModelLoader, ParallelInferenceConfig.builder()
                .batchLimit(1)
                .workers(1)
                .queueLimit(1)
                .inferenceMode(InferenceMode.SEQUENTIAL)
                .build());

        ConverterArgs converterArgs2 = ConverterArgs.builder()
                .imageTransformProcess(new ImageTransformProcess.Builder()
                        .colorConversionTransform(COLOR_BGR2RGB).build()).build();
        VertxBufferImageInputAdapter vertxBufferImageInputAdapter = new VertxBufferImageInputAdapter();
        Buffer inputBuffer = Buffer.buffer(content);
        NDArrayWritable convertWritable = (NDArrayWritable) vertxBufferImageInputAdapter.convert(inputBuffer, converterArgs2, null);
        INDArray convert = convertWritable.get();
        convert = BasicNDArrayCompressor.getInstance().compress(convert.permute(0, 2, 3, 1).dup('c'), "UINT8");
        INDArray[] execute = tensorflowInferenceExecutioner.execute(new INDArray[]{convert});
        Map<String, BatchOutput> adapt = yoloOutputAdapter.adapt(execute, Arrays.asList(outputs), null);
        System.out.println(adapt);
    }

}
