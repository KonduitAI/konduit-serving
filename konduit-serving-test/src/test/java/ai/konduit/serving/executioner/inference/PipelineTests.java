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

package ai.konduit.serving.executioner.inference;

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.pipeline.PmmlInferenceExecutionerStepRunner;
import ai.konduit.serving.pipeline.step.*;
import ai.konduit.serving.pipeline.steps.*;
import ai.konduit.serving.util.SchemaTypeUtils;
import org.bytedeco.tensorflow.Mod;
import org.datavec.api.records.Record;
import org.datavec.api.transform.MathOp;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Text;
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
public class PipelineTests {

    @Test
    public void transformProcessPipelineTest() {
        Schema schema = new Schema.Builder()
                .addColumnString("first")
                .build();

        TransformProcess transformProcess = new TransformProcess.Builder(schema)
                .appendStringColumnTransform("first", "two")
                .build();


        TransformProcessStep config = TransformProcessStep.builder()
                .inputName("default")
                .inputSchema("default", new SchemaType[]{SchemaType.String})
                .outputSchema("default", new SchemaType[]{SchemaType.String})

                .inputColumnName("default", Arrays.asList(new String[]{"first"}))
                .transformProcess("default", transformProcess)
                .build();

        TransformProcessStepRunner step = new TransformProcessStepRunner(config);

        List<Writable> ret = new ArrayList<>();
        ret.add(new Text("appended"));

        Record[] transform = step.transform(new Record[]{
                new org.datavec.api.records.impl.Record(ret, null)
        });

        assertEquals(1, transform.length);

        Writable writable = transform[0].getRecord().get(0);
        assertEquals("appendedtwo", writable.toString());

        assertEquals(1, step.inputTypes().size());
        assertEquals(1, step.outputTypes().size());


    }

    @Test
    public void testNdArrayTransform() {
        Schema schema = new Schema.Builder()
                .addColumnNDArray("first", new long[]{1, 1})
                .build();

        TransformProcess transformProcess = new TransformProcess.Builder(schema)
                .ndArrayScalarOpTransform("first", MathOp.Add, 1.0)
                .build();

        TransformProcessStep config = TransformProcessStep.builder()
                .inputName("default")
                .inputColumnName("default", Arrays.asList(new String[]{"first"}))
                .transformProcess("default", transformProcess)
                .build();

        TransformProcessStepRunner step = new TransformProcessStepRunner(config);

        List<Writable> ret = new ArrayList<>();
        ret.add(new NDArrayWritable(Nd4j.scalar(1.0)));

        Record[] transform = step.transform(new Record[]{
                new org.datavec.api.records.impl.Record(ret, null)
        });

        assertEquals(1, transform.length);


        INDArray[] transformed = SchemaTypeUtils.toArrays(transform);
        assertEquals(Nd4j.scalar(2.0).reshape(1,1), transformed[0].reshape(1,1));

    }


    @Test
    public void testStepToRunnerMapping() {
        ArrayConcatenationStep arrayConcatStep = ArrayConcatenationStep.builder().build();
        assertEquals(ArrayConcatenationStepRunner.class.getName(), arrayConcatStep.pipelineStepClazz());

        CustomPipelineStep customStep = CustomPipelineStep.builder().build();
        assertEquals(CustomStepRunner.class.getName(), customStep.pipelineStepClazz());

        JsonExpanderTransformStep jsonStep = JsonExpanderTransformStep.builder().build();
        assertEquals(JsonExpanderTransformStepRunner.class.getName(), jsonStep.pipelineStepClazz());

        TransformProcessStep config = TransformProcessStep.builder().build();
        assertEquals(TransformProcessStepRunner.class.getName(), config.pipelineStepClazz());

        ModelStep modelStep = ModelStep.builder().build();
        assertEquals(InferenceExecutionerStepRunner.class.getName(), modelStep.pipelineStepClazz());

        PmmlStep pmmlStep = PmmlStep.builder().build();
        assertEquals(PmmlInferenceExecutionerStepRunner.class.getName(), pmmlStep.pipelineStepClazz());

        TransformProcessStep tpStep = TransformProcessStep.builder().build();
        assertEquals(TransformProcessStepRunner.class.getName(), tpStep.pipelineStepClazz());
    }

}
