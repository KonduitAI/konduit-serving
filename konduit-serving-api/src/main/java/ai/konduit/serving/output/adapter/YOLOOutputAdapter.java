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

package ai.konduit.serving.output.adapter;

import ai.konduit.serving.output.types.BatchOutput;
import ai.konduit.serving.output.types.DetectedObjectsBatch;
import ai.konduit.serving.output.types.ManyDetectedObjects;
import ai.konduit.serving.verticles.VerticleConstants;
import io.vertx.ext.web.RoutingContext;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.layers.objdetect.DetectedObject;
import org.deeplearning4j.nn.layers.objdetect.YoloUtils;
import org.deeplearning4j.zoo.model.YOLO2;
import org.deeplearning4j.zoo.model.helper.DarknetHelper;
import org.deeplearning4j.zoo.util.BaseLabels;
import org.deeplearning4j.zoo.util.ClassPrediction;
import org.deeplearning4j.zoo.util.Labels;
import org.deeplearning4j.zoo.util.darknet.COCOLabels;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;


@Slf4j
public class YOLOOutputAdapter implements MultiOutputAdapter<INDArray[]> {

    private double threshold;
    @Builder.Default
    private int[] inputShape = {3, 608, 608};
    private Labels labels;
    @Builder.Default
    private INDArray boundingBoxPriors = Nd4j.create(YOLO2.DEFAULT_PRIOR_BOXES);
    private int gridWidth;
    private int gridHeight;
    private int numLabels;


    @Builder
    public YOLOOutputAdapter(double threshold, int[] inputShape, Labels labels, int numLabels, double[][] boundingBoxPriors) {
        this.labels = labels == null ? getLabels() : labels;
        if (threshold == 0.0)
            this.threshold = 0.5;
        else
            this.threshold = threshold;
        if (inputShape != null)
            this.inputShape = inputShape;
        else
            this.inputShape = new int[]{3, 608, 608};
        this.labels = labels;
        this.numLabels = numLabels;
        if (boundingBoxPriors == null)
            this.boundingBoxPriors = Nd4j.create(YOLO2.DEFAULT_PRIOR_BOXES).castTo(DataType.FLOAT);
        else {
            this.boundingBoxPriors = Nd4j.create(boundingBoxPriors).castTo(DataType.FLOAT);
        }

        gridWidth = DarknetHelper.getGridWidth(inputShape);
        gridHeight = DarknetHelper.getGridHeight(inputShape);

    }

    public YOLOOutputAdapter(double threshold, Labels labels, int numLabels) {
        this.threshold = threshold;
        inputShape = new int[]{3, 608, 608};
        this.labels = labels;
        this.numLabels = numLabels;
        boundingBoxPriors = Nd4j.create(YOLO2.DEFAULT_PRIOR_BOXES).castTo(DataType.FLOAT);
        gridWidth = DarknetHelper.getGridWidth(inputShape);
        gridHeight = DarknetHelper.getGridHeight(inputShape);

    }

    public YOLOOutputAdapter(double threshold, int numLabels) {
        this(threshold, getLabels(), numLabels);

    }

    public YOLOOutputAdapter(double threshold, InputStream labels, int numLabels) {
        this(threshold, labels == null ? getLabels() : getLabels(labels, numLabels), numLabels);

    }

    private static Labels getLabels(InputStream is, int numLabels) {
        try {
            return new BaseLabels() {
                protected ArrayList<String> getLabels() {
                    Scanner scanner = new Scanner(is);
                    int id1 = -1;
                    int count = 0;
                    List<String> ret = new ArrayList<>();
                    String name = null;
                    while (scanner.hasNext()) {
                        String token = scanner.next();
                        if (token.equals("id:")) {
                            id1 = scanner.nextInt();
                        }
                        if (token.equals("display_name:")) {
                            name = scanner.nextLine();
                            name = name.substring(2, name.length() - 1);
                        }
                        if (id1 > 0 && name != null) {
                            ret.add(name);
                            id1 = -1;
                            name = null;
                        }
                    }

                    return (ArrayList<String>) ret;
                }

                @Override
                public List<List<ClassPrediction>> decodePredictions(INDArray predictions, int n) {
                    return super.decodePredictions(predictions, n);
                }

                @Override
                protected URL getURL() {
                    return null;
                }

                @Override
                protected String resourceName() {
                    return null;
                }

                @Override
                protected String resourceMD5() {
                    return null;
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Labels getLabels() {
        try {
            return new COCOLabels();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Map<String, BatchOutput> adapt(INDArray[] input, List<String> outputNames, RoutingContext routingContext) {
        int originalHeight = (int) routingContext.data().get(VerticleConstants.ORIGINAL_IMAGE_HEIGHT);
        int originalWidth = (int) routingContext.data().get(VerticleConstants.ORIGINAL_IMAGE_WIDTH);

        DetectedObjectsBatch[] detectedObjects = getPredictedObjects(input, threshold, outputNames.toArray(new String[outputNames.size()]), originalHeight, originalWidth);
        Map<String, BatchOutput> ret = new HashMap<>();
        ret.put(outputNames.get(0), ManyDetectedObjects.builder().detectedObjectsBatches(detectedObjects).build());
        return ret;
    }

    @Override
    public List<Class<? extends OutputAdapter<?>>> outputAdapterTypes() {
        return null;
    }

    private DetectedObjectsBatch[] getPredictedObjects(INDArray[] outputs, double threshold, String[] outputNames, int originalHeight, int originalWidth) {
        // assuming "standard" output from TensorFlow using a "normal" YOLOv2 model
        //INDArray permuted = outputs[0].permute(0, 3, 1, 2);
        INDArray permuted = outputs[0];
        INDArray activated = YoloUtils.activate(boundingBoxPriors, permuted);

        List<DetectedObject> predictedObjects1 = YoloUtils.getPredictedObjects(boundingBoxPriors, activated, threshold, 0.4);


        DetectedObjectsBatch[] detectedObjects = new DetectedObjectsBatch[predictedObjects1.size()];

        int n = numLabels; // an arbitrary number of classes returned per object
        for (int i = 0; i < detectedObjects.length; i++) {
            DetectedObject detectedObject = predictedObjects1.get(i);
            long x = Math.round(originalWidth * predictedObjects1.get(i).getCenterX() / gridWidth);
            long y = Math.round(originalHeight * predictedObjects1.get(i).getCenterY() / gridHeight);
            long w = Math.round(originalWidth * predictedObjects1.get(i).getWidth() / gridWidth);
            long h = Math.round(originalHeight * predictedObjects1.get(i).getHeight() / gridHeight);

            detectedObjects[i] = new DetectedObjectsBatch();
            detectedObjects[i].setCenterX(x);
            detectedObjects[i].setCenterY(y);
            detectedObjects[i].setWidth(w);
            detectedObjects[i].setHeight(h);
            detectedObjects[i].setPredictedClasses(new String[]{labels.getLabel(detectedObject.getPredictedClass())});
            detectedObjects[i].setPredictedClassNumbers(new int[]{detectedObject.getPredictedClass()});
            detectedObjects[i].setConfidences(new float[]{detectedObject.getClassPredictions().getFloat(detectedObject.getPredictedClass())});

        }

        return detectedObjects;


    }

}
