package ai.konduit.serving.deeplearning4j.step;

import ai.konduit.serving.deeplearning4j.DL4JConfiguration;
import ai.konduit.serving.pipeline.api.BaseModelPipelineStep;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class DL4JModelPipelineStep extends BaseModelPipelineStep<DL4JConfiguration> {

    private List<String> inputNames;        //Mainly for ComputationGraph with multiple inputs - map Data keys to ComputationGraph outputs
    private List<String> outputNames;       //Mainly for ComputationGraph with multiple outputs - map INDArray[] to Data keys

    public DL4JModelPipelineStep(String modelUri, DL4JConfiguration config) {
        super(modelUri, config);
    }

    public DL4JModelPipelineStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("config") DL4JConfiguration config,
                                 @JsonProperty("inputNames") List<String> inputNames, @JsonProperty("outputNames") List<String> outputNames){
        super(modelUri, config);
        this.inputNames = inputNames;
        this.outputNames = outputNames;
    }


}
