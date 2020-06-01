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
package ai.konduit.serving.models.deeplearning4j.step;

import ai.konduit.serving.models.deeplearning4j.step.keras.KerasModelStep;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.exception.ModelLoadingException;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.JData;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModel;
import org.deeplearning4j.nn.modelimport.keras.KerasSequentialModel;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.utils.DL4JKerasModelValidator;
import org.deeplearning4j.nn.modelimport.keras.utils.KerasModelBuilder;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.DL4JModelValidator;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@Slf4j
public class DL4JPipelineStepRunner implements PipelineStepRunner {

    public static final String DEFAULT_OUT_NAME_SINGLE = "default";


    private DL4JModelPipelineStep step;
    private KerasModelStep kStep;
    private MultiLayerNetwork net;
    private ComputationGraph graph;

    public DL4JPipelineStepRunner(KerasModelStep step) {
        this.kStep = step;
        File f = getFile(step.modelUri());
        KerasModelBuilder b;
        try{
            b = new KerasModel().modelBuilder().modelHdf5Filename(f.getAbsolutePath())
                    .enforceTrainingConfig(false);
        } catch (IOException e){
            throw new ModelLoadingException("Failed to load Keras model", e);
        } catch (InvalidKerasConfigurationException | UnsupportedKerasConfigurationException e) {
            throw new ModelLoadingException("Failed to load Keras model: model file is invalid or can't be loaded" +
                    " by DL4JPipelineStepRunner", e);
        }

        try {
            graph = b.buildModel().getComputationGraph();
            net = null;
        } catch (Throwable t) {
            if (t.getMessage() != null && t.getMessage().toLowerCase().contains("sequential")) {
                try {
                    net = b.buildSequential().getMultiLayerNetwork();
                    graph = null;
                } catch (Throwable t2) {
                    throw new ModelLoadingException("Failed to load Keras Sequential model: model file is invalid or can't be loaded" +
                            " by DL4JPipelineStepRunner", t2);
                }
            } else {
                throw new ModelLoadingException("Failed to load Keras model: model file is invalid or can't be loaded" +
                        " by DL4JPipelineStepRunner", t);
            }
        }
        System.out.println();
    }

    public DL4JPipelineStepRunner(DL4JModelPipelineStep step) {
        this.step = step;
        File f = getFile(step.modelUri());  //TODO DON'T ASSUME LOCAL FILE URI!

        boolean isMLN = DL4JModelValidator.validateMultiLayerNetwork(f).isValid();
        boolean isCG = !isMLN && DL4JModelValidator.validateComputationGraph(f).isValid();

        Preconditions.checkState(isMLN || isCG, "Model at URI %s is not a valid MultiLayerNetwork or ComputationGraph model", step.modelUri());

        if (isMLN) {
            try {
                net = MultiLayerNetwork.load(f, false);
                graph = null;
            } catch (IOException e) {
                throw new ModelLoadingException("Failed to load Deeplearning4J MultiLayerNetwork from URI " + step.modelUri(), e);
            }
        } else {
            try {
                graph = ComputationGraph.load(f, false);
                net = null;
            } catch (IOException e) {
                throw new ModelLoadingException("Failed to load Deeplearning4J ComputationGraph from URI " + step.modelUri(), e);
            }
        }
    }

    protected static File getFile(String uri) {
        Preconditions.checkState(uri != null && !uri.isEmpty(), "No model URI was provided (model URI was null or empty)");
        URI u = null;
        try {
            u = URI.create(uri);
        } catch (IllegalArgumentException e) {
            try {
                u = new File(uri).toURI();
            } catch (Throwable t) {
                t.printStackTrace();
                ;
            }
        }

        File f = new File(u);
        Preconditions.checkState(f.exists(), "No model file exists at URI: %s", u);
        return f;
    }


    @Override
    public void close() {
        try {
            if (net != null) {
                net.close();
            } else {
                graph.close();
            }
        } catch (Throwable t) {
            log.warn("Error when closing model", t);
        }
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step != null ? step : kStep;
    }

    @Override
    public Data exec(Context ctx, Data data) {

        //First: Get array
        //TODO HANDLE DIFFERENT NAMES (Not hardcoded)
        int numInputs = net != null ? 1 : graph.getNumInputArrays();
        Preconditions.checkArgument(numInputs == data.size(), "Expected %s inputs to DL4JModelStep but got Data instance with %s inputs (keys: %s)",
                numInputs, data.size(), data.keys());

        if (net != null) {
            INDArray arr = getOnlyArray(data);
            INDArray out;
            synchronized (net) {
                out = net.output(arr);
            }

            String outName = outputName();

            return Data.singleton(outName, NDArray.create(out));
        } else {
            INDArray[] input;
            if (numInputs == 1) {
                input = new INDArray[]{getOnlyArray(data)};
            } else {
                //TODO make configurable input names/order
                if (step.inputNames() != null) {
                    input = new INDArray[numInputs];
                    int i = 0;
                    for (String s : step.inputNames()) {
                        input[i++] = (INDArray) data.getNDArray(s).get();      //TODO FIX NDARRAY
                    }
                } else {
                    //Configuration does not have names specified
                    //See if model input names matches data
                    List<String> networkInputs = graph.getConfiguration().getNetworkInputs();
                    if (data.hasAll(networkInputs)) {
                        input = new INDArray[numInputs];
                        int i = 0;
                        for (String s : networkInputs) {
                            input[i++] = (INDArray) data.getNDArray(s).get();      //TODO FIX NDARRAY
                        }
                    } else {
                        throw new IllegalStateException("Network has " + numInputs + " inputs, but no Data input names were specified." +
                                " Attempting to infer input names also failed: Model has input names " + networkInputs + " but Data object has keys " + data.keys());
                    }
                }
            }
            INDArray[] out;
            synchronized (graph) {
                out = graph.output(input);
            }

            //Work out output names
            List<String> outNames;
            if (outputNames() != null) {
                outNames = outputNames();
            } else {
                if (out.length == 1) {
                    outNames = Collections.singletonList(DEFAULT_OUT_NAME_SINGLE);
                } else {
                    outNames = graph.getConfiguration().getNetworkOutputs();
                }
            }

            Preconditions.checkState(outNames.size() == out.length);

            JData.DataBuilder b = JData.builder();
            for (int i = 0; i < out.length; i++) {
                b.add(outNames.get(i), NDArray.create(out[i]));
            }
            return b.build();
        }
    }

    private List<String> outputNames() {
        return step != null ? step.outputNames() : kStep.outputNames();
    }

    private String outputName() {
        if (step != null) {
            return step.outputNames() == null || step.outputNames().isEmpty() ? DEFAULT_OUT_NAME_SINGLE : step.outputNames().get(0);
        } else {
            return kStep.outputNames() == null || kStep.outputNames().isEmpty() ? DEFAULT_OUT_NAME_SINGLE : kStep.outputNames().get(0);
        }
    }

    private INDArray getOnlyArray(Data data) {
        //TODO Fix NDArray

        String key = data.keys().get(0);
        NDArray array = data.getNDArray(key);

        INDArray out = (INDArray) array.get();          //TOOD NO CAST
        return out;
    }
}
