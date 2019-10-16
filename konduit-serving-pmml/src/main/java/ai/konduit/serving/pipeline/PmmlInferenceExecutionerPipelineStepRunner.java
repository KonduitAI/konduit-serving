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

package ai.konduit.serving.pipeline;

import ai.konduit.serving.util.ObjectMapperHolder;
import ai.konduit.serving.util.WritableValueRetriever;
import ai.konduit.serving.executioner.inference.PmmlInferenceExecutioner;
import ai.konduit.serving.executioner.inference.factory.PmmlInferenceExecutionerFactory;
import ai.konduit.serving.pipeline.steps.BasePipelineStepRunner;
import org.datavec.api.records.Record;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Evaluator;
import org.nd4j.base.Preconditions;
import org.nd4j.shade.jackson.core.JsonProcessingException;

import java.util.*;


public class PmmlInferenceExecutionerPipelineStepRunner extends BasePipelineStepRunner {

    private PmmlInferenceExecutioner pmmlInferenceExecutioner;
    private Evaluator evaluator;

    public PmmlInferenceExecutionerPipelineStepRunner(PipelineStep pipelineStep) {
        super(pipelineStep);
        PmmlPipelineStep pmmlPipelineStepConfig = (PmmlPipelineStep) pipelineStep;
        PmmlInferenceExecutionerFactory inferenceExecutionerFactory = new PmmlInferenceExecutionerFactory();
        try {
            pmmlInferenceExecutioner = (PmmlInferenceExecutioner) inferenceExecutionerFactory.create(pmmlPipelineStepConfig).getInferenceExecutioner();
            evaluator = pmmlInferenceExecutioner.model();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Preconditions.checkState(pmmlPipelineStepConfig.getOutputSchemas() != null &&
                !pmmlPipelineStepConfig.getOutputSchemas().isEmpty(),"No output schemas found!");
        Preconditions.checkState(pmmlPipelineStepConfig.getInputSchemas() != null &&
                !pmmlPipelineStepConfig.getInputSchemas().isEmpty(),"No input schemas found!");
        Preconditions.checkState(pmmlPipelineStepConfig.getInputColumnNames() != null &&
                !pmmlPipelineStepConfig.getInputColumnNames().isEmpty(),"No input column names  found!");
        Preconditions.checkState(pmmlPipelineStepConfig.getOutputColumnNames() != null &&
                !pmmlPipelineStepConfig.getOutputColumnNames().isEmpty(),"No output names found!");


    }

    @Override
    public void destroy() {
        pmmlInferenceExecutioner.stop();
    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Record[] transform(Record[] input) {
        Schema schema = pipelineStep.inputSchemaForName("default");
        List<Map<FieldName, Object>> pmmlInput = new ArrayList<>(input.length);
        List<FieldName> fieldNames = new ArrayList<>();
        for(int i = 0; i < schema.numColumns(); i++) {
            fieldNames.add(FieldName.create(schema.getName(i)));
        }

        for(Record record : input) {
            Map<FieldName, Object> pmmlRecord = new LinkedHashMap<>();
            for(int i = 0; i < record.getRecord().size(); i++) {
                pmmlRecord.put(fieldNames.get(i), WritableValueRetriever.getUnderlyingValue(record.getRecord().get(i)));
            }

            pmmlInput.add(pmmlRecord);
        }

        List<Map<FieldName, Object>> execute = pmmlInferenceExecutioner.execute(pmmlInput);
        Record[] ret = new Record[1];
        String json = null;
        try {
            json = ObjectMapperHolder.getJsonMapper().writeValueAsString(execute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to write json fore records " + execute);
        }

        ret[0] = new org.datavec.api.records.impl.Record(Collections.singletonList(new Text(json)),null);


        return ret;
    }
}
