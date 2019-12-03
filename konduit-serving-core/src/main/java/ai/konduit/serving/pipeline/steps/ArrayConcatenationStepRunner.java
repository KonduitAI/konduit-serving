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

import ai.konduit.serving.pipeline.step.ArrayConcatenationStep;
import ai.konduit.serving.pipeline.PipelineStep;
import org.datavec.api.records.Record;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merge input records composed of ndarrays
 * along specified dimensions
 * for each input.
 *
 * This is meant to be used mainly right before a
 * {@link InferenceExecutionerStepRunner}
 * that takes in 1 array per named input.
 *
 * @author Adam Gibson
 */
public class ArrayConcatenationStepRunner extends BaseStepRunner {

    private Map<Integer,Integer> concatDimensionsForIndex;

    public ArrayConcatenationStepRunner(PipelineStep pipelineStep) {
        super(pipelineStep);
        ArrayConcatenationStep arrayConcatenationStepConfig = (ArrayConcatenationStep) pipelineStep;
        this.concatDimensionsForIndex = arrayConcatenationStepConfig.getConcatDimensions();
    }

    @Override
    public Record[] transform(Record[] input) {
        Record[] ret = new Record[1];
        ret[0] = new org.datavec.api.records.impl.Record(new ArrayList<>(),null);
        //allow setting writables at particular indices
        for(int i = 0; i < input[0].getRecord().size(); i++) {
            ret[0].getRecord().add(null);
        }

        Map<Integer,List<INDArray>> arrays = new LinkedHashMap<>();
        for (Record record : input) {
            for (int j = 0; j < record.getRecord().size(); j++) {
                List<INDArray> nameArraysToConcat;
                if (!arrays.containsKey(j)) {
                    nameArraysToConcat = new ArrayList<>();
                    arrays.put(j, nameArraysToConcat);
                } else {
                    nameArraysToConcat = arrays.get(j);
                }

                NDArrayWritable ndArrayWritable = (NDArrayWritable) record.getRecord().get(j);
                nameArraysToConcat.add(ndArrayWritable.get());
            }
        }

        //concatneate all the arrays together
        for(Map.Entry<Integer,List<INDArray>> entry : arrays.entrySet()) {
            INDArray[] toConcat = entry.getValue().toArray(new INDArray[0]);
            int concatDim;
            concatDim = concatDimensionsForIndex.getOrDefault(entry.getKey(), 0);

            ret[0].getRecord().set(entry.getKey(), new NDArrayWritable(Nd4j.concat(concatDim,toConcat)));
        }

        return ret;
    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs) {
        throw new UnsupportedOperationException();
    }
}
