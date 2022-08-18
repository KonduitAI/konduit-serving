/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2022 Konduit K.K.
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
import ai.konduit.serving.verticles.VerticleConstants;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import org.deeplearning4j.zoo.util.BaseLabels;
import org.deeplearning4j.zoo.util.Labels;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * An input adapter for ssd in tensorflow.
 *
 * @author Adam Gibson
 */
public class SSDOutputAdapter implements MultiOutputAdapter<INDArray[]> {

    public final static String DEFAULT_LABELS_RESOURCE_NAME = "/mscoco_label_map.pbtxt";
    private double threshold;
    private int[] inputShape;
    private Labels labels;
    private int numLabels;
    @Getter
    private String[] inputs = new String[]{"image_tensor"};
    @Getter
    private String[] outputs = new String[]{"detection_boxes", "detection_scores", "detection_classes", "num_detections"};


    public SSDOutputAdapter(double threshold, Labels labels, int numLabels) {
        this.threshold = threshold;
        inputShape = new int[]{3, 0, 0};
        this.labels = labels;
        this.numLabels = numLabels;

    }

    public SSDOutputAdapter(double threshold, int numLabels) {
        this(threshold, getLabels(), numLabels);

    }


    public SSDOutputAdapter(double threshold, InputStream labels, int numLabels) {
        this(threshold, getLabels(labels, numLabels), numLabels);

    }

    public static Labels getLabels(InputStream is, int numLabels) {
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

    public static Labels getLabels() {
        return getLabels(SSDOutputAdapter.class.getResourceAsStream(DEFAULT_LABELS_RESOURCE_NAME), 100);
    }

    @Override
    public Map<String, BatchOutput> adapt(INDArray[] input, List<String> outputNames, RoutingContext routingContext) {
        int originalHeight = (int) routingContext.data().get(VerticleConstants.ORIGINAL_IMAGE_HEIGHT);
        int originalWidth = (int) routingContext.data().get(VerticleConstants.ORIGINAL_IMAGE_WIDTH);

        DetectedObjectsBatch[] detectedObjects = getPredictedObjects(input, threshold, outputNames.toArray(new String[outputNames.size()]), originalHeight, originalWidth);

        Map<String, BatchOutput> ret = new HashMap<>();
        for (int i = 0; i < outputNames.size(); i++) {
            ret.put(outputNames.get(i), detectedObjects[i]);
        }

        return ret;
    }

    @Override
    public List<Class<? extends OutputAdapter<?>>> outputAdapterTypes() {
        return null;
    }

    private DetectedObjectsBatch[] getPredictedObjects(INDArray[] outputs, double threshold, String[] outputNames, int originalHeight, int originalWidth) {
        INDArray boxes = null, classes = null, scores = null;
        for (int i = 0; i < outputs.length; i++) {
            if (outputNames[i].contains("box")) {
                boxes = outputs[i];
            } else if (outputNames[i].contains("class")) {
                classes = outputs[i];
            } else if (outputNames[i].contains("score")) {
                scores = outputs[i];
            }
        }

        List<DetectedObjectsBatch> detectedObjects = new ArrayList<>();
        for (int i = 0; i < scores.columns(); i++) {
            double score = scores.getDouble(0, i);
            if (score < threshold) {
                continue;
            }

            int n = classes.rank() >= 2 ? classes.getInt(0, i) : classes.getInt(i);
            String label = labels.getLabel(n);
            double y1 = boxes.getDouble(0, i, 0) * originalHeight;
            double x1 = boxes.getDouble(0, i, 1) * originalWidth;
            double y2 = boxes.getDouble(0, i, 2) * originalHeight;
            double x2 = boxes.getDouble(0, i, 3) * originalWidth;

            DetectedObjectsBatch d = new DetectedObjectsBatch();
            d.setCenterX((float) (x1 + x2) / 2);
            d.setCenterY((float) (y1 + y2) / 2);
            d.setWidth((float) (x2 - x1));
            d.setHeight((float) (y2 - y1));
            d.setPredictedClassNumbers(new int[]{n});
            d.setPredictedClasses(new String[]{label});
            d.setConfidences(new float[]{(float) score});
            detectedObjects.add(d);
        }

        return detectedObjects.toArray(new DetectedObjectsBatch[detectedObjects.size()]);
    }

}
