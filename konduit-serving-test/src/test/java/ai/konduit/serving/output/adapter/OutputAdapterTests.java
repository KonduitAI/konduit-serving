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

package ai.konduit.serving.output.adapter;

import ai.konduit.serving.output.types.ClassifierOutput;
import ai.konduit.serving.output.types.RegressionOutput;
import org.datavec.api.transform.schema.Schema;
import org.dmg.pmml.FieldName;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class OutputAdapterTests {

    private Schema classificationOutputSchema;
    private Schema regressionOutputSchema;

    public static INDArray arrayInput() {
        INDArray arr = Nd4j.create(new double[][]{
                {0.6, 0.4},
                {0.7, 0.3}
        });

        return arr;
    }

    public static List<Map<FieldName, Object>> pmmlRegressionInput() {
        List<Map<FieldName, Object>> list = new ArrayList<>();
        Map<FieldName, Object> row1 = new LinkedHashMap<>();
        row1.put(FieldName.create("output1"), 0.6);
        row1.put(FieldName.create("output2"), 0.4);
        list.add(row1);
        Map<FieldName, Object> row2 = new LinkedHashMap<>();
        row2.put(FieldName.create("output1"), 0.7);
        row2.put(FieldName.create("output2"), 0.3);
        list.add(row2);
        return list;
    }

    public static List<Map<FieldName, Object>> pmmlInput() {
        List<Map<FieldName, Object>> list = new ArrayList<>();
        Map<FieldName, Object> row1 = new LinkedHashMap<>();
        row1.put(FieldName.create("probability(" + "output1" + ")"), 0.6);
        row1.put(FieldName.create("probability(" + "output2" + ")"), 0.4);
        list.add(row1);
        Map<FieldName, Object> row2 = new LinkedHashMap<>();
        row2.put(FieldName.create("probability(" + "output1" + ")"), 0.7);
        row2.put(FieldName.create("probability(" + "output2" + ")"), 0.3);
        list.add(row2);
        return list;
    }

    @Before
    public void before() {
        if (classificationOutputSchema == null) {
            classificationOutputSchema = new Schema.Builder()
                    .addColumnDouble("output1")
                    .addColumnDouble("output2").build();
        }
        if (regressionOutputSchema == null) {
            regressionOutputSchema = new Schema.Builder()
                    .addColumnDouble("output1")
                    .addColumnDouble("output2").build();
        }
    }

    @Test(timeout = 60000)

    public void testRegressionAdapter() {
        INDArray arr = arrayInput();
        List<Map<FieldName, Object>> list = pmmlRegressionInput();

        RegressionOutputAdapter regressionOutputAdapter = new RegressionOutputAdapter(regressionOutputSchema);
        RegressionOutput adapt = regressionOutputAdapter.adapt(arr, null);
        assertNotNull(adapt);
        for (int i = 0; i < arr.rows(); i++)
            assertArrayEquals(arr.toDoubleMatrix()[i], adapt.getValues()[i], 1e-1);

        RegressionOutput adapt1 = regressionOutputAdapter.adapt(list, null);
        for (int i = 0; i < arr.rows(); i++)
            assertArrayEquals(arr.toDoubleMatrix()[i], adapt1.getValues()[i], 1e-1);

    }

    @Test(timeout = 60000)

    public void testClassificationAdapter() {
        INDArray arr = arrayInput();
        List<Map<FieldName, Object>> list = pmmlInput();

        ClassifierOutputAdapter classifierOutputAdapter = new ClassifierOutputAdapter(classificationOutputSchema);
        ClassifierOutput adapt = classifierOutputAdapter.adapt(arr, null);
        assertNotNull(adapt);
        assertArrayEquals(new int[]{0, 0}, adapt.getDecisions());
        for (int i = 0; i < arr.rows(); i++)
            assertArrayEquals(arr.toDoubleMatrix()[i], adapt.getProbabilities()[i], 1e-1);

        ClassifierOutput adapt1 = classifierOutputAdapter.adapt(list, null);
        for (int i = 0; i < arr.rows(); i++)
            assertArrayEquals(arr.toDoubleMatrix()[i], adapt1.getProbabilities()[i], 1e-1);
    }

}
