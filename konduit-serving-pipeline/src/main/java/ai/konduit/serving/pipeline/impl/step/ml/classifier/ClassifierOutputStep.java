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

package ai.konduit.serving.pipeline.impl.step.ml.classifier;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.List;


/**
 * ClassifierOutputStep takes as input a numerical 2d NDArray (i.e., float/double etc type) with shape [minibatch, numClasses]"
 * which represents the probability distribution (softmax) predictions for a standard classifier and returns based on this array:<br>
 * - The predicted class label - as a String<br>
 * - The predicted class index - as an integer (long)<br>
 * - The predicted class probability - as a Double
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("CLASSIFIER_OUTPUT")
@Schema(description = "ClassifierOutputStep takes as input a numerical 2d NDArray (i.e., float/double etc type) with shape [minibatch, numClasses]" +
        " which represents the softmax predictions for a standard classifier and returns based on this array:<br>" +
        " - The predicted class label - as a String<br>" +
        "- The predicted class index - as an integer (long)<br>" +
        "- The predicted class probability - as a Double")
public class ClassifierOutputStep implements PipelineStep {
    public static final String DEFAULT_PROB_NAME = "prob";
    public static final String DEFAULT_INDEX_NAME = "index";
    public static final String DEFAULT_LABEL_NAME = "label";


    @Schema(description = "inputName - optional. If set: this represents the NDArray. If not set: use DataUtils.inferField to find an NDArray field", defaultValue = "null")
    private String inputName = null;

    @Schema(description = "returnLabel, default is true; if false, don't return label", defaultValue = "true")
    private boolean returnLabel = true;

    @Schema(description = "returnIndex, default is true", defaultValue = "true")
    private boolean returnIndex = true;


    @Schema(description = " returnProb, default is true", defaultValue = "true")
    private boolean returnProb = true;


    @Schema(description = "output names for the labels", defaultValue = DEFAULT_LABEL_NAME)
    private String labelName = DEFAULT_LABEL_NAME;


    @Schema(description = "output names for the index", defaultValue = DEFAULT_INDEX_NAME)
    private String indexName = DEFAULT_INDEX_NAME;


    @Schema(description = "output names for the labels propabilities", defaultValue = DEFAULT_PROB_NAME)
    private String probName = DEFAULT_PROB_NAME;

    @Schema(description = "as a List<String>. Optional. If not specified, the predicted class index as a string is used - i.e., \"0\", \"1\", etc", defaultValue = "null")
    private List<String> labels = null;

    @Schema(description = "Integer, null by default. If non-null and > 1, we return List<String>, List<Long>, List<Double> for the predicted class/index/probability instead of String/Long/Double.", defaultValue = "null")
    private Integer topN = null;

    @Schema(description = "If true, also returns a List<List<Double>> of all probabilities (basically, converd NDArray to list. False by default.", defaultValue = "false")
    private boolean allProbabilities = false;


}
