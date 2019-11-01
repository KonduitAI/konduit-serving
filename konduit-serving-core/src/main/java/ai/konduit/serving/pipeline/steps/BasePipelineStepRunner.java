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
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.PipelineStepRunner;
import org.datavec.api.records.Record;
import org.datavec.api.writable.*;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.activation.UnsupportedDataTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BasePipelineStepRunner implements PipelineStepRunner {

    protected PipelineStep pipelineStep;

    public BasePipelineStepRunner(PipelineStep pipelineStep) {
        this.pipelineStep = pipelineStep;
    }

    /**
     * no-op
     */
    public void destroy() {}

    @Override
    public Writable[][] transform(Object... input) {
        return transform(new Object[][]{input});
    }

    @Override
    public Writable[][] transform(Object[]... input){
        Record[] outputRecords = transform(Arrays.stream(input)
                .map(writables -> new org.datavec.api.records.impl.Record(
                        Arrays.stream(writables).map(this::getWritableFromObject).collect(Collectors.toList()), null))
                .toArray(Record[]::new));

        return Arrays.stream(outputRecords)
                .map(record -> record.getRecord().toArray(new Writable[0]))
                .toArray(Writable[][]::new);
    }

    @Override
    public INDArray[][] transform(Writable[]... input) {
        Record[] outputRecords = transform(Arrays.stream(input)
                .map(writables -> new org.datavec.api.records.impl.Record(Arrays.asList(writables), null))
                .toArray(Record[]::new));

        return Arrays.stream(outputRecords)
                .map(record -> record.getRecord().stream()
                        .map(writable -> ((NDArrayWritable) writable).get())
                        .toArray(INDArray[]::new))
                .toArray(INDArray[][]::new);
    }

    @Override
    public Record[] transform(Record[] input) {
        int batchSize = input.length;
        Record[] ret = new Record[input.length];

        for(int example = 0; example < batchSize; example++) {
            for(int name = 0; name < pipelineStep.getInputNames().size(); name++) {
                String inputName = pipelineStep.inputNameAt(name);

                if(pipelineStep.inputNameIsValidForStep(pipelineStep.inputNameAtIndex(name))) {
                    List<Writable> currRecord;
                    if(ret[example] == null) {
                        currRecord = new ArrayList<>();
                        ret[example] = new org.datavec.api.records.impl.Record(currRecord,null);
                    } else {
                        currRecord = ret[example].getRecord();
                    }

                    Writable currWritable = input[example].getRecord().get(name);
                    //Add filtering for column size equal to 1, to reduce boilerplate
                    if(pipelineStep.processColumn(inputName,name)) {
                        processValidWritable(currWritable,currRecord,name);
                    } else {
                        currRecord.add(input[example].getRecord().get(name));
                    }
                } else {
                    ret[example] = input[example];
                }
            }
        }

        return ret;
    }

    @Override
    public Map<String, SchemaType[]> inputTypes() {
        return pipelineStep.getInputSchemas();
    }

    @Override
    public Map<String, SchemaType[]> outputTypes() {
        return pipelineStep.getOutputSchemas();
    }

    public abstract void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs);

    private Writable getWritableFromObject(Object object){
        Writable output = null;

        try {
            if (object instanceof INDArray) {
                output = new NDArrayWritable((INDArray) object);
            } else if (object instanceof String) {
                output = new Text((String) object);
            } else if (object instanceof Integer) {
                output = new IntWritable((Integer) object);
            } else if (object instanceof Float) {
                output = new FloatWritable((Float) object);
            } else if (object instanceof Double) {
                output = new DoubleWritable((Double) object);
            } else if (object instanceof Long) {
                output = new LongWritable((Long) object);
            } else {
                throw new UnsupportedDataTypeException(String.format("Cannot convert %s to a writable", object.getClass().getName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output;
    }
}
