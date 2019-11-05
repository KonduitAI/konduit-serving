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

package ai.konduit.serving.inference;

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.executioner.Pipeline;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.PythonPipelineStep;
import ai.konduit.serving.pipeline.TransformProcessPipelineStep;
import ai.konduit.serving.pipeline.steps.PythonPipelineStepRunner;
import ai.konduit.serving.pipeline.steps.TransformProcessPipelineStepRunner;
import ai.konduit.serving.util.python.PythonVariables;
import org.datavec.api.records.Record;
import org.datavec.api.transform.MathOp;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@NotThreadSafe
@org.junit.Ignore
public class PythonPipelineTests {

    
    @Test
    public void testPipeline() throws Exception {

        Schema schema = new Schema.Builder()
                .addColumnNDArray("first", new long[]{1, 1})
                .build();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCode("first += 2")
                .pythonInput("first", PythonVariables.Type.NDARRAY.name())
                .pythonOutput("first", PythonVariables.Type.NDARRAY.name())
                .returnAllInputs(false)
                .build();
        
        PythonPipelineStep config = new PythonPipelineStep(
                pythonConfig, new String[] {"first"}, new SchemaType[]{SchemaType.NDArray});
        PythonPipelineStepRunner pythonPipelineStep = new PythonPipelineStepRunner(config);

        TransformProcess transformProcess = new TransformProcess.Builder(schema)
                .ndArrayScalarOpTransform("first", MathOp.Add,1.0).build();

        TransformProcessPipelineStep tpStep = new TransformProcessPipelineStep()
                .input(new String[]{"first"}, new SchemaType[]{SchemaType.NDArray})
                .output(new String[]{"first"}, new SchemaType[]{SchemaType.NDArray})
                .transformProcess(transformProcess);
        
        TransformProcessPipelineStepRunner transformProcessPipelineStep = new TransformProcessPipelineStepRunner(tpStep);

        List<Writable> record = new ArrayList<>();
        
        record.add(new NDArrayWritable(Nd4j.scalar(1.0)));
        org.datavec.api.records.impl.Record record1 = new org.datavec.api.records.impl.Record(record,null);
        Pipeline pipeline = Pipeline.builder()
                .steps(Arrays.asList(pythonPipelineStep, transformProcessPipelineStep))
                .build();
        
        INDArray[] indArrays = pipeline.doPipelineArrays(new Record[]{record1});
        assertEquals(1,indArrays.length);
        assertEquals(Nd4j.scalar(4.0),indArrays[0]);
        
        Pipeline pipeline1 = Pipeline.getPipeline(Arrays.asList(config, tpStep));
        INDArray[] indArrays2 = pipeline1.doPipelineArrays(new Record[]{record1});
        assertEquals(1,indArrays2.length);
        assertEquals(Nd4j.scalar(4.0),indArrays2[0]);
    }
    
}
