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
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.tensorflow.Graph;
import org.tensorflow.SavedModelBundle;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TensorFlowStepRunner implements PipelineStepRunner {

    private final TensorFlowPipelineStep step;
    private Graph graph;

    public TensorFlowStepRunner(@NonNull TensorFlowPipelineStep step){
        this.step = step;
        init();
    }

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {

        Map<String, INDArray> in = new HashMap<>();
//        for(String s : step.getInputNames()){
//            INDArray arr = data.getNDArray(s).getAs(INDArray.class);
//            in.put(s, arr);
//        }
//
//
//        Data d = Data.empty();
//        for(Map.Entry<String,INDArray> e : out.entrySet()){
//            d.put(e.getKey(), NDArray.create(e.getValue()));
//        }
//        return d;
        return Data.empty();        //TODO
    }



    protected void init() {
        try{
            initHelper();
        } catch (Throwable t){
            throw new RuntimeException("Error loading TensorFlow model", t);
        }
    }

    protected void initHelper() throws Exception {
        File origFile = new File(new URI(step.getModelUri()));
        Preconditions.checkState(origFile.exists(), "Model file does not exist: " + step.getModelUri());


        //Try to load frozen model:
        Throwable frozenErr = null;
        try{
            byte[] bytes = FileUtils.readFileToByteArray(origFile);
            graph = new Graph();
            graph.importGraphDef(bytes);
            log.info("Loaded TensorFlow frozen model");
        } catch (Throwable t){
            frozenErr = t;
            graph = null;
        }


        //Try to load saved model:
        //TF has bad API here: The DIRECTORY path is provided, and the file must be exactly "saved_model.pb" - this is hardcoded in TF
        if(graph == null){
            try{
                File dir = ai.konduit.serving.pipeline.util.FileUtils.getTempFileDir("tf_model_" + System.nanoTime());
                File f = new File(dir, "saved_model.pb");
                FileUtils.copyFile(origFile, f);

                SavedModelBundle b = SavedModelBundle.load(dir.getAbsolutePath(), "serve");

                graph = b.graph();
                log.info("Loaded TensorFlow SavedModel");
            } catch (Throwable t){
                log.error("Error loading graph: Attempted to load as both a frozen model .pb and a SavedModel .pb - both failed");
                log.error("Frozen model loading exception:", frozenErr);
                log.error("SavedModel loading exception:", t);
                throw new IllegalStateException("Unable to load TensorFlow model as either a frozen model .pb or Savedmodel .pb", t);
            }
        }
    }
}
