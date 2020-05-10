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

package ai.konduit.serving.models.tensorflow.step;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.bytedeco.tensorflow.TF_Graph;
import org.bytedeco.tensorflow.TF_Status;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.tensorflow.conversion.TensorflowConversion;
import org.nd4j.tensorflow.conversion.graphrunner.GraphRunner;
import org.nd4j.tensorflow.conversion.graphrunner.SavedModelConfig;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.tensorflow.global.tensorflow.*;

public class TensorFlowStepRunnerBackup implements PipelineStepRunner {

    private final TensorFlowPipelineStep step;
    private GraphRunner graphRunner;

    public TensorFlowStepRunnerBackup(@NonNull TensorFlowPipelineStep step){
        this.step = step;
        init();
    }

    @Override
    public void close() {
        graphRunner.close();
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {

        Map<String, INDArray> in = new HashMap<>();
        for(String s : step.getInputNames()){
            INDArray arr = data.getNDArray(s).getAs(INDArray.class);
            in.put(s, arr);
        }

        Map<String,INDArray> out = graphRunner.run(in);

        Data d = Data.empty();
        for(Map.Entry<String,INDArray> e : out.entrySet()){
            d.put(e.getKey(), NDArray.create(e.getValue()));
        }
        return d;
    }



    protected void init() {
        try{
            initHelper();
        } catch (Throwable t){
            throw new RuntimeException("Error loading TensorFlow model", t);
        }
    }

    protected void initHelper() throws Exception {
        byte[] graphDefContent = FileUtils.readFileToByteArray(new File(new URI(step.getModelUri())));
        List<String> inputNames = step.getInputNames();
        List<String> outputNames = step.getOutputNames();

        SavedModelConfig c = SavedModelConfig.builder()
                .savedModelPath(new File(URI.create(step.getModelUri())).getAbsolutePath())
                .modelTag("serving")
                .build();

        TF_Status status = TF_NewStatus();;
        TF_Graph g = TensorflowConversion.getInstance().loadGraph(graphDefContent, status);
        if (TF_GetCode(status) != TF_OK) {
            throw new IllegalStateException("ERROR: Unable to open session " + TF_Message(status).getString());
        }



        graphRunner = GraphRunner.builder()
//                .graphBytes(graphDefContent)
                .graph(g)
                .inputNames(inputNames)
                .outputNames(outputNames)
                .savedModelConfig(c)
                .inputDataTypes(null)
                .outputDataTypes(null)
                .build();
    }

//    public TF_Graph loadGraph(byte[] content, TF_Status status) {
//        byte[] toLoad = content;
//        TF_Buffer graph_def = TF_NewBufferFromString(new BytePointer(toLoad), content.length);
//        TF_Graph graphC = TF_NewGraph();
//        TF_ImportGraphDefOptions opts = TF_NewImportGraphDefOptions();
//        TF_GraphImportGraphDef(graphC, graph_def, opts, status);
//        if (TF_GetCode(status) != TF_OK) {
//            throw new IllegalStateException("ERROR: Unable to import graph " + TF_Message(status).getString());
//        }
//
//
//        TF_DeleteImportGraphDefOptions(opts);
//
//        return graphC;
//    }
}
