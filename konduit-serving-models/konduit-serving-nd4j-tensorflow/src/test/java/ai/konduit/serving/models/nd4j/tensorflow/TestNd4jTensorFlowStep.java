/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.models.nd4j.tensorflow;


import ai.konduit.serving.models.nd4j.tensorflow.step.Nd4jTensorFlowStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;

import static org.junit.Assert.assertEquals;


@Slf4j
public class TestNd4jTensorFlowStep {



    @Test
    public void testStep() throws Exception {
        ClassPathResource classPathResource = new ClassPathResource("add.pb");
        File f = classPathResource.getFile();
        Nd4jTensorFlowStep nd4jTensorFlowStep = new Nd4jTensorFlowStep()
                .modelUri(f.getAbsolutePath())
                .inputNames("a","b")
                .outputNames("output");

        SequencePipeline sequencePipeline = SequencePipeline.builder()
                .add(nd4jTensorFlowStep)
                .build();

        PipelineExecutor executor = sequencePipeline.executor();
        Data data = Data.empty();
        data.put("a", NDArray.create(Nd4j.scalar(1.0f)));
        data.put("b", NDArray.create(Nd4j.scalar(1.0f)));
        Data exec = executor.exec(data);
        INDArray arr = exec.getNDArray("output").getAs(INDArray.class);
        assertEquals(2.0f,arr.sumNumber().floatValue(),1e-2f);

    }


}



