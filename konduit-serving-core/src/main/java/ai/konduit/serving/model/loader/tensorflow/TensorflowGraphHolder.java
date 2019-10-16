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

package ai.konduit.serving.model.loader.tensorflow;

import ai.konduit.serving.model.TensorDataType;
import ai.konduit.serving.threadpool.tensorflow.conversion.TensorflowConversion;
import ai.konduit.serving.threadpool.tensorflow.conversion.graphrunner.GraphRunner;
import ai.konduit.serving.model.SavedModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.tensorflow.TF_Graph;
import org.bytedeco.tensorflow.TF_Status;
import org.nd4j.base.Preconditions;
import org.tensorflow.framework.ConfigProto;
import org.tensorflow.framework.GraphDef;

import java.util.List;
import java.util.Map;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TensorflowGraphHolder {

    private GraphDef graphDef;
    private TF_Graph tfGraph;
    private ConfigProto configProto;
    private List<String> inputNames,outputNames;
    private TF_Status tfStatus;
    private SavedModelConfig savedModelConfig;
    private byte[] graphDefContent;
    private Map<String, TensorDataType> castingInputTypes,castingOutputTypes;


    @Builder
    public TensorflowGraphHolder(byte[] graphContent,
                                 byte[] configProto,
                                 List<String> inputNames,
                                 List<String> outputNames,
                                 Map<String, TensorDataType> castingInputTypes,
                                 Map<String, TensorDataType> castingOutputTypes,
                                 SavedModelConfig savedModelConfig) throws Exception {
        if(savedModelConfig == null)
            Preconditions.checkNotNull(graphContent,"No graph content found!");
        if(inputNames != null && outputNames != null) {
            if(inputNames.equals(outputNames)) {
                throw new IllegalArgumentException("Inputs and outputSchema must not be the same!");
            }
        }

        if(castingInputTypes != null && !castingInputTypes.isEmpty()) {
            for(Map.Entry<String,TensorDataType> entry : castingInputTypes.entrySet()) {
                Preconditions.checkNotNull(entry.getKey());
                Preconditions.checkNotNull(entry.getValue());
            }
        }

        if(castingOutputTypes != null && !castingOutputTypes.isEmpty()) {
            for(Map.Entry<String,TensorDataType> entry : castingOutputTypes.entrySet()) {
                Preconditions.checkNotNull(entry.getKey());
                Preconditions.checkNotNull(entry.getValue());
            }
        }


        this.savedModelConfig = savedModelConfig;
        this.castingInputTypes = castingInputTypes;
        this.castingOutputTypes = castingOutputTypes;
        this.graphDefContent = graphContent;
        if(graphDefContent != null && savedModelConfig == null)
            graphDef = GraphDef.parseFrom(graphContent);
        else if(savedModelConfig == null){
            throw new IllegalStateException("No saved model configuration or graph proto file loaded!");
        }
        this.inputNames = inputNames;
        this.outputNames = outputNames;
        tfStatus = TF_Status.newStatus();
        log.info("Loading graph");
        if(graphDefContent != null)
            tfGraph = TensorflowConversion.getInstance().loadGraph(graphDefContent,tfStatus);
        if(configProto != null) {
            this.configProto = ConfigProto.parseFrom(configProto);
        }
    }


    /**
     * Creates a graph runner
     * based on the configuration
     * of the graph holder.
     * @return he created {@link GraphRunner}
     */
    public GraphRunner createRunner() {

        GraphRunner graphRunner = GraphRunner.builder()
                .graphBytes(graphDefContent)
                .inputNames(getInputNames())
                .outputNames(getOutputNames())
                .savedModelConfig(getSavedModelConfig())
                .inputDataTypes(castingInputTypes)
                .outputDataTypes(castingOutputTypes)
                .build();

        return graphRunner;

    }

}
