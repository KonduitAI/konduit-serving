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
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;


@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class ClassifierOutputStep implements PipelineStep {


    String inputName;// - optional. If set: this represents the NDArray. If not set: use DataUtils.inferField to find an NDArray field
    boolean returnLabel = true; //  default is true; if false, don't return label
    boolean returnIndex = true; // default is true
    boolean returnProb = true; // default is true
    String labelName;
    String indexName;
    String probName;
    //output names for the labels, index, and probabilities
    List<String> Labels; //Optional. If not specified, the predicted class index as a string is used - i.e., "0", "1", etc
    Integer topN = null; // null by default. If non-null and > 1, we return List<String>, List<Long>, List<Double> for the predicted class/index/probability instead of String/Long/Double.
    boolean allProbabilities = true; //If true, also returns a List<List<Double>> of all probabilities (basically, converd NDArray to list. False by default.


    public ClassifierOutputStep() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        boolean returnLabel = true;
        boolean returnIndex = true;
        boolean returnProb = true;
        Integer topN = null;
        boolean allProbabilities = true;

    }

}
