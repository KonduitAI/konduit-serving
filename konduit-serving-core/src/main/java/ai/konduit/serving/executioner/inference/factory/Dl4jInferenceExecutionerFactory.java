package ai.konduit.serving.executioner.inference.factory;

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.executioner.inference.InitializedInferenceExecutionerConfig;
import ai.konduit.serving.executioner.inference.MultiComputationGraphInferenceExecutioner;
import ai.konduit.serving.executioner.inference.MultiLayerNetworkInferenceExecutioner;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.loader.ModelGuesser;
import ai.konduit.serving.model.loader.dl4j.cg.ComputationGraphModelLoader;
import ai.konduit.serving.model.loader.dl4j.mln.MultiLayerNetworkModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.graph.ComputationGraph;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Slf4j
public class Dl4jInferenceExecutionerFactory implements InferenceExecutionerFactory {

    @Override
    public InitializedInferenceExecutionerConfig create(ModelStep modelPipelineStepConfig) throws Exception {
        try {
            ModelConfig inferenceConfiguration = modelPipelineStepConfig.getModelConfig();
            ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

            MultiLayerNetworkInferenceExecutioner inferenceExecutioner = new MultiLayerNetworkInferenceExecutioner();
            MultiLayerNetworkModelLoader multiLayerNetworkModelLoader = new MultiLayerNetworkModelLoader(new File(inferenceConfiguration.getModelConfigType().getModelLoadingPath()));
            inferenceExecutioner.initialize(multiLayerNetworkModelLoader, parallelInferenceConfig);
            List<String> inputNames = Collections.singletonList("default");
            List<String> outputNames = Collections.singletonList("default");
            return new InitializedInferenceExecutionerConfig(inferenceExecutioner, inputNames, outputNames);
        } catch (Exception mlnLoadingException) {
            log.error("Error loading multi layer network from file. Attempting to load computation graph instead.", mlnLoadingException);
            ModelConfig inferenceConfiguration = modelPipelineStepConfig.getModelConfig();
            ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

            ComputationGraphModelLoader computationGraphModelLoader = new ComputationGraphModelLoader(new File(inferenceConfiguration.getModelConfigType().getModelLoadingPath()));
            MultiComputationGraphInferenceExecutioner inferenceExecutioner = new MultiComputationGraphInferenceExecutioner();
            inferenceExecutioner.initialize(computationGraphModelLoader, parallelInferenceConfig);

            ComputationGraph computationGraph2 = computationGraphModelLoader.loadModel();
            List<String> inputNames = computationGraph2.getConfiguration().getNetworkInputs();
            List<String> outputNames = computationGraph2.getConfiguration().getNetworkOutputs();
            log.info("Loaded computation graph with input names " + inputNames + " and output names " + outputNames);

            return InitializedInferenceExecutionerConfig.builder()
                    .inferenceExecutioner(inferenceExecutioner)
                    .inputNames(inputNames).outputNames(outputNames)
                    .build();
        }
    }
}
