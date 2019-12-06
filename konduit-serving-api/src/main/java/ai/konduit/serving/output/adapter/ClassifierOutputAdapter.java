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
import io.vertx.ext.web.RoutingContext;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.transform.schema.Schema;
import org.dmg.pmml.FieldName;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An output adapter for displaying
 * {@link ClassifierOutput}
 *
 * @author Adam Gibson
 */
@NoArgsConstructor
@Slf4j
public class ClassifierOutputAdapter implements OutputAdapter<ClassifierOutput> {
    private Schema schema;
    private List<FieldName> fieldNames;

    public ClassifierOutputAdapter(Schema schema) {
        this.schema = schema;
        fieldNames = new ArrayList<>(schema.numColumns());
        for (int i = 0; i < schema.numColumns(); i++) {
            fieldNames.add(FieldName.create(schema.getName(i)));
        }
    }

    @Override
    public ClassifierOutput adapt(INDArray array, RoutingContext routingContext) {
        INDArray argMax = Nd4j.argMax(array, -1);
        return ClassifierOutput.builder()
                .labels(getLabels())
                .decisions(argMax.data().asInt())
                .probabilities(array.isVector() ? new double[][]{array.toDoubleVector()} : array.toDoubleMatrix())
                .build();
    }

    @Override
    public ClassifierOutput adapt(List<? extends Map<FieldName, ?>> pmmlExamples, RoutingContext routingContext) {
        if (schema == null) {
            throw new IllegalStateException("No inputSchema found. A inputSchema is required in order to create results.");
        }

        int[] labelIndices = new int[pmmlExamples.size()];
        double[][] values = new double[pmmlExamples.size()][schema.numColumns()];
        for (int i = 0; i < pmmlExamples.size(); i++) {
            Map<FieldName, ?> example = pmmlExamples.get(i);
            int maxIdx = -1;
            double compare = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < schema.numColumns(); j++) {
                Double result = (Double) example.get(FieldName.create("probability(" + schema.getName(j) + ")"));
                if (result == null) {
                    throw new IllegalArgumentException("No label found for " + schema.getName(j));
                }
                if (result > compare) {
                    maxIdx = j;
                    compare = maxIdx;
                }
                values[i][j] = result;
            }

            labelIndices[i] = maxIdx;
        }


        return ClassifierOutput.builder()
                .probabilities(values)
                .labels(getLabels())
                .decisions(labelIndices)
                .build();
    }

    @Override
    public ClassifierOutput adapt(Object input, RoutingContext routingContext) {
        if (input instanceof INDArray) {
            INDArray arr = (INDArray) input;
            return adapt(arr, routingContext);
        } else if (input instanceof List) {
            List<? extends Map<FieldName, ?>> pmmlExamples = (List<? extends Map<FieldName, ?>>) input;
            return adapt(pmmlExamples, routingContext);
        }
        throw new UnsupportedOperationException("Unable to convert input of type " + input);
    }

    @Override
    public Class<ClassifierOutput> outputAdapterType() {
        return ClassifierOutput.class;
    }

    public String[] getLabels() {
        String[] labels = new String[schema.numColumns()];
        for (int i = 0; i < schema.numColumns(); i++) {
            labels[i] = schema.getName(i);
        }

        return labels;
    }
}
