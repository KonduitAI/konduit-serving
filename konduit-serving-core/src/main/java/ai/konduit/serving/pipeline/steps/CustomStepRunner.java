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

import ai.konduit.serving.pipeline.step.CustomPipelineStep;
import ai.konduit.serving.pipeline.CustomPipelineStepUDF;
import ai.konduit.serving.pipeline.PipelineStep;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Writable;

import java.util.List;

import static java.lang.Class.forName;

public class CustomStepRunner extends BaseStepRunner {

    private CustomPipelineStepUDF customPipelineStepUDF;

    public CustomStepRunner(PipelineStep pipelineStep) {
        super(pipelineStep);
        CustomPipelineStep customPipelineStep = (CustomPipelineStep) pipelineStep;
        try {
            this.customPipelineStepUDF = (CustomPipelineStepUDF) forName(customPipelineStep.getCustomUdfClazz()).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Record[] transform(Record[] input) {
        return customPipelineStepUDF.udf(input);
    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs) {
        throw new UnsupportedOperationException();
    }
}
