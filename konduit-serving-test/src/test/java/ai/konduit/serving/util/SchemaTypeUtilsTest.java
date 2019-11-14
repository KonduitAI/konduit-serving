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

package ai.konduit.serving.util;

import org.datavec.api.records.Record;
import org.datavec.api.writable.NDArrayWritable;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

public class SchemaTypeUtilsTest {

    @Test
    public void testToArrays() {
        Record[] inputs = new Record[2];
        for(int i = 0; i < inputs.length; i++)
            inputs[i] = new org.datavec.api.records.impl.Record(Arrays.asList(
                    new NDArrayWritable(Nd4j.scalar(1.0)),
                    new NDArrayWritable(Nd4j.scalar(1.0))
            ),null);

        INDArray[] indArrays = SchemaTypeUtils.toArrays(inputs);
    }

}
