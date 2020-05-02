package ai.konduit.serving.deeplearning4j.step;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.exception.ModelLoadingException;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.data.NDArray;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.DL4JModelValidator;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;


public class DL4JPipelineStepRunner implements PipelineStepRunner {

    public static final String DEFAULT_OUT_NAME_SINGLE = "default";


    private DL4JModelPipelineStep step;
    private final MultiLayerNetwork net;
    private final ComputationGraph graph;

    public DL4JPipelineStepRunner(DL4JModelPipelineStep step) {
        this.step = step;

        //TODO DON'T ASSUME MultiLayerNetwork OR LOCAL FILE URI!

        String uri = step.getModelUri();
        Preconditions.checkState(uri != null && !uri.isEmpty(), "No model URI was provided (model URI was null or empty)");
        URI u = URI.create(uri);
        File f = new File(u);

        Preconditions.checkState(f.exists(), "No model file exists at URI: %s", u);

        boolean isMLN = DL4JModelValidator.validateMultiLayerNetwork(f).isValid();
        boolean isCG = !isMLN && DL4JModelValidator.validateComputationGraph(f).isValid();

        Preconditions.checkState(isMLN || isCG, "Model at URI %s is not a valid MultiLayerNetwork or ComputationGraph model", uri);

        if (isMLN) {
            try {
                net = MultiLayerNetwork.load(f, false);
                graph = null;
            } catch (IOException e) {
                throw new ModelLoadingException("Failed to load Deeplearning4J MultiLayerNetwork from URI " + step.getModelUri(), e);
            }
        } else {
            try {
                graph = ComputationGraph.load(f, false);
                net = null;
            } catch (IOException e) {
                throw new ModelLoadingException("Failed to load Deeplearning4J ComputationGraph from URI " + step.getModelUri(), e);
            }
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
    public Data exec(Data data) {

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

            String outName = step.getOutputNames() == null || step.getOutputNames().isEmpty() ? DEFAULT_OUT_NAME_SINGLE : step.getOutputNames().get(0);

            return Data.singleton(outName, new NDArray(out));
        } else {
            INDArray[] input;
            if (numInputs == 1) {
                input = new INDArray[]{getOnlyArray(data)};
            } else {
                //TODO make configurable input names/order
                if (step.getInputNames() != null) {
                    input = new INDArray[numInputs];
                    int i = 0;
                    for (String s : step.getInputNames()) {
                        input[i++] = (INDArray) data.getNDArray(s).getArrayValue();      //TODO FIX NDARRAY
                    }
                } else {
                    //Configuration does not have names specified
                    //See if model input names matches data
                    List<String> networkInputs = graph.getConfiguration().getNetworkInputs();
                    if (data.hasAll(networkInputs)) {
                        input = new INDArray[numInputs];
                        int i = 0;
                        for (String s : networkInputs) {
                            input[i++] = (INDArray) data.getNDArray(s).getArrayValue();      //TODO FIX NDARRAY
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
            if (step.getOutputNames() != null) {
                outNames = step.getOutputNames();
                ;
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
                b.add(outNames.get(i), new NDArray(out[i]));
            }
            return b.build();
        }
    }

    private INDArray getOnlyArray(Data data) {
        //TODO Fix NDArray

        String key = data.keys().get(0);
        NDArray array = data.getNDArray(key);

        INDArray out = (INDArray) array.getArrayValue();
        return out;
    }
}
