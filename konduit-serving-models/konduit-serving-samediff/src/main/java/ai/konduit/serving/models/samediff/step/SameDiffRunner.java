/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.models.samediff.step;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.exception.ModelLoadingException;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import org.nd4j.autodiff.listeners.At;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.autodiff.samediff.internal.InferenceSession;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CanRun(SameDiffStep.class)
public class SameDiffRunner implements PipelineStepRunner {

    public static final String DEFAULT_OUT_NAME_SINGLE = "default";


    private SameDiffStep step;
    private final SameDiff sd;

    public SameDiffRunner(SameDiffStep step) {
        this.step = step;

        String uri = step.modelUri();
        Preconditions.checkState(uri != null && !uri.isEmpty(), "No model URI was provided (model URI was null or empty)");

        try {
            File f = URIResolver.getFile(uri);
            Preconditions.checkState(f.exists(), "No model file exists at URI: %s", uri);
            sd = SameDiff.load(f, true);
        } catch (Throwable e) {
            throw new ModelLoadingException("Failed to load SameDiff model from URI " + step.modelUri(), e);
        }
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

        //First: Get array

        Map<String,INDArray> m = new HashMap<>();
        List<String> inputs = sd.inputs();
        for(String s : inputs){
            if(!data.has(s))
                throw new IllegalStateException("Expected to find NDArray with name \"" + s + "\" in data - not found. Data keys: " + data.keys());
            if(data.type(s) != ValueType.NDARRAY)
                throw new IllegalStateException("Input Data field \"" + s + "\" is not an NDArray - is type : " + data.type(s));
            m.put(s, data.getNDArray(s).getAs(INDArray.class));
        }

        List<String> outNames = step.outputNames();
        Preconditions.checkState(outNames != null && !outNames.isEmpty(), "No output names were provided in the SameDiffStep configuration");
        ;

        Map<String,INDArray> out = sd.output(m,outNames.toArray(new String[outNames.size()]));

        Data d = Data.empty();
        for(Map.Entry<String,INDArray> e : out.entrySet()){
            d.put(e.getKey(), NDArray.create(e.getValue()));
        }

        return d;
    }
}
