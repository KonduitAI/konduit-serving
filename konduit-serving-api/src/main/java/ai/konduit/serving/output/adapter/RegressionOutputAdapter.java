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

import ai.konduit.serving.output.types.RegressionOutput;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.datavec.api.transform.schema.Schema;
import org.dmg.pmml.FieldName;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Convert the input based on the input
 * {@link Schema} to {@link RegressionOutput}
 * representing real valued output.
 *
 * @author Adam Gibson
 */
@AllArgsConstructor
@NoArgsConstructor
public class RegressionOutputAdapter implements OutputAdapter<RegressionOutput> {

    private Schema schema;
    private List<FieldName> fieldNames;

    /**
     * Create the output adapter
     * with the output inputSchema
     *
     * @param schema the inputSchema of the output
     */
    public RegressionOutputAdapter(Schema schema) {
        this.schema = schema;
        fieldNames = new ArrayList<>(schema.numColumns());
        for (int i = 0; i < schema.numColumns(); i++) {
            fieldNames.add(FieldName.create(schema.getName(i)));
        }

    }

    @Override
    public RegressionOutput adapt(INDArray array, RoutingContext routingContext) {
        return RegressionOutput
                .builder()
                .values(array.toDoubleMatrix())
                .build();
    }

    @Override
    public RegressionOutput adapt(List<? extends Map<FieldName, ?>> pmmlExamples, RoutingContext routingContext) {
        if (schema == null) {
            throw new IllegalStateException("No inputSchema found. A inputSchema is required in order to create results.");
        }

        double[][] values = new double[pmmlExamples.size()][pmmlExamples.get(0).size()];
        for (int i = 0; i < pmmlExamples.size(); i++) {
            Map<FieldName, ?> example = pmmlExamples.get(i);
            for (int j = 0; j < schema.numColumns(); j++) {
                Double result = (Double) example.get(fieldNames.get(j));
                values[i][j] = result;
            }
        }

        return RegressionOutput.builder().values(values).build();
    }

    @Override
    public RegressionOutput adapt(Object input, RoutingContext routingContext) {
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
    public Class<RegressionOutput> outputAdapterType() {
        return RegressionOutput.class;
    }
}
