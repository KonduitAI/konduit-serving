package ai.konduit.serving.deeplearning4j.step;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.exception.ModelLoadingException;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.NDArray;
import lombok.AllArgsConstructor;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;
import java.net.URI;


public class DL4JPipelineStepRunner implements PipelineStepRunner {

    private DL4JModelPipelineStep step;
    private MultiLayerNetwork net;
    private ComputationGraph graph;

    public DL4JPipelineStepRunner(DL4JModelPipelineStep step){
        this.step = step;

        //TODO DON'T ASSUME MultiLayerNetwork OR LOCAL FILE URI!

        String uri = step.getModelUri();
        Preconditions.checkState(uri != null && !uri.isEmpty(), "No model URI was provided (model URI was null or empty)");
        URI u = URI.create(uri);
        File f = new File(u);
        try {
            net = MultiLayerNetwork.load(f, false);
        } catch (IOException e){
            throw new ModelLoadingException("Failed to load Deeplearning4J MultiLayerNetwork from URI " + step.getModelUri(), e);
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

        if(net != null){
            String key = data.keys().get(0);
            NDArray array = data.getNDArray(key);

            //TODO Fix NDArray
            INDArray arr = (INDArray) array.getArrayValue();
            INDArray out = net.output(arr);

            return Data.singleton("default", new NDArray(out));      //TODO - NO HARDCODED MAGIC CONSTANT
        } else {
            throw new IllegalStateException("Not yet implemented");
        }
    }
}
