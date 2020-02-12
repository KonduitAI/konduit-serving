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

package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.executioner.inference.InferenceExecutioner;
import ai.konduit.serving.executioner.inference.InitializedInferenceExecutionerConfig;
import ai.konduit.serving.executioner.inference.factory.InferenceExecutionerFactory;
import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.util.SchemaTypeUtils;
import lombok.Getter;
import org.datavec.api.records.Record;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.datavec.api.writable.WritableType;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link InferenceExecutioner}
 * is run as part of a pipeline step.
 * <p>
 * For the kinds of {@link InferenceExecutioner}
 * that can be run as part of a pipeline, please reference
 * the {@link ai.konduit.serving.model.ModelConfigType}
 * which contains all of the available types.
 * <p>
 * This pipeline step is used for executing standalone models
 * such as tensorflow,keras or dl4j.
 *
 * @author Adam Gibson
 */
public class InferenceExecutionerStepRunner extends BaseStepRunner {
    @Getter
    private InferenceExecutioner inferenceExecutioner;

    public InferenceExecutionerStepRunner(PipelineStep pipelineStep) {
        super(pipelineStep);
        ModelStep modelPipelineStepConfig = (ModelStep) pipelineStep;
        InferenceExecutionerFactory inferenceExecutionerFactory;
        try {
            Preconditions.checkNotNull(modelPipelineStepConfig.getModelConfig().getModelConfigType(), "No model state was specified!");
            Preconditions.checkNotNull(modelPipelineStepConfig.getModelConfig().getModelConfigType().getModelType(), "No model type was specified!");

            switch (modelPipelineStepConfig.getModelConfig().getModelConfigType().getModelType()) {
                case SAMEDIFF:
                    inferenceExecutionerFactory = (InferenceExecutionerFactory) Class.forName("ai.konduit.serving.executioner.inference.factory.SameDiffInferenceExecutionerFactory").newInstance();
                    break;
                case TENSORFLOW:
                    inferenceExecutionerFactory = (InferenceExecutionerFactory) Class.forName("ai.konduit.serving.executioner.inference.factory.TensorflowInferenceExecutionerFactory").newInstance();
                    break;
                case DL4J:
                    inferenceExecutionerFactory = (InferenceExecutionerFactory) Class.forName("ai.konduit.serving.executioner.inference.factory.Dl4jInferenceExecutionerFactory").newInstance();
                    break;
                case PMML:
                    inferenceExecutionerFactory = (InferenceExecutionerFactory) Class.forName("ai.konduit.serving.executioner.inference.factory.PmmlInferenceExecutionerFactory").newInstance();
                    break;
                case KERAS:
                    inferenceExecutionerFactory = (InferenceExecutionerFactory) Class.forName("ai.konduit.serving.executioner.inference.factory.KerasInferenceExecutionerFactory").newInstance();
                    break;
                default:
                    throw new IllegalStateException("No model type specified!");

            }

            Preconditions.checkNotNull(modelPipelineStepConfig, "NO pipeline configuration found!");
            Preconditions.checkNotNull(modelPipelineStepConfig.getParallelInferenceConfig(), "No parallel inference configuration found!");
            InitializedInferenceExecutionerConfig init = inferenceExecutionerFactory.create(modelPipelineStepConfig);
            Preconditions.checkNotNull(init, "Initialized inference executioner configuration should not be null!");
            inferenceExecutioner = init.getInferenceExecutioner();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }


    }

    @Override
    public void destroy() {
        inferenceExecutioner.stop();
    }

    @Override
    public Map<String, SchemaType[]> inputTypes() {
        Map<String, SchemaType[]> ret = new LinkedHashMap<>();
        for (int i = 0; i < pipelineStep.getInputNames().size(); i++) {
            ret.put(pipelineStep.getInputNames().get(i), new SchemaType[]{SchemaType.NDArray});
        }
        return ret;
    }

    @Override
    public Map<String, SchemaType[]> outputTypes() {
        Map<String, SchemaType[]> ret = new LinkedHashMap<>();
        for (int i = 0; i < pipelineStep.getOutputNames().size(); i++) {
            ret.put(pipelineStep.getOutputNames().get(i), new SchemaType[]{SchemaType.NDArray});
        }

        return ret;
    }

    @Override
    public Record[] transform(Record[] input) {
        //not a singular ndarray record type
        //try to convert to matrix if all numeric,
        //otherwise throw an exception
        if (input[0].getRecord().size() > 1 || recordIsAllNumeric(input[0]))
            input = toNDArray(input);
        INDArray[] arrayInputs = SchemaTypeUtils.toArrays(input);
        INDArray[] execution = (INDArray[]) inferenceExecutioner.execute(arrayInputs);
        return SchemaTypeUtils.toRecords(execution);
    }


    private Record[] toNDArray(Record[] records) {
        if (records[0].getRecord().size() > 1 && !recordIsAllNumeric(records[0])) {
            throw new IllegalArgumentException("Invalid record type passed in. This pipeline only accepts records with singular ndarray records representing 1 input array per name for input graphs or purely numeric arrays that can be converted to a matrix");
        } else if (allNdArray(records)) {
            return records;
        } else {
            INDArray arr = Nd4j.create(records.length, records[0].getRecord().size());
            for (int i = 0; i < arr.rows(); i++) {
                for (int j = 0; j < arr.columns(); j++) {
                    arr.putScalar(i, j, records[i].getRecord().get(j).toDouble());
                }
            }

            return new Record[]{
                    new org.datavec.api.records.impl.Record(
                            Arrays.asList(new NDArrayWritable(arr))
                            , null
                    )};
        }
    }

    private boolean allNdArray(Record[] records) {
        boolean isAllNdArrays = true;
        for (int i = 0; i < records.length; i++) {
            if (records[i].getRecord().size() != 1 && records[i].getRecord().get(0).getType() != WritableType.NDArray) {
                isAllNdArrays = false;
                break;
            }
        }

        return isAllNdArrays;
    }

    private boolean recordIsAllNumeric(Record record) {
        boolean recordIsAllNumbers = true;
        for (int i = 0; i < record.getRecord().size(); i++) {
            Writable currWritable = record.getRecord().get(i);
            switch (currWritable.getType()) {
                case Double:
                case Float:
                case Int:
                case Long:
                case NDArray:
                    break;
                default:
                    recordIsAllNumbers = false;
                    break;
            }
        }

        return recordIsAllNumbers;
    }


    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs) {
        throw new UnsupportedOperationException();
    }
}