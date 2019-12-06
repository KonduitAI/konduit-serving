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

import ai.konduit.serving.output.types.BatchOutput;
import io.vertx.ext.web.RoutingContext;
import org.datavec.api.transform.schema.Schema;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link MultiOutputAdapter} for elnino.
 * Internally it uses a cached {@link RegressionOutputAdapter}
 * to generate the json needed for obtaining interpretable
 * results for elnino.
 *
 * @author Adam Gibson
 */
public class RegressionMultiOutputAdapter implements MultiOutputAdapter<INDArray[]> {

    private RegressionOutputAdapter regressionOutputAdapter;

    @Override
    public Map<String, BatchOutput> adapt(INDArray[] array, List<String> outputNames, RoutingContext routingContext) {
        Map<String, BatchOutput> ret = new LinkedHashMap<>();
        if (regressionOutputAdapter == null) {
            Schema.Builder schemaBuilder = new Schema.Builder();
            for (int i = 0; i < outputNames.size(); i++) {
                schemaBuilder.addColumnDouble(outputNames.get(i));
            }

            regressionOutputAdapter = new RegressionOutputAdapter(schemaBuilder.build());
        }
        ret.put(outputNames.get(0), regressionOutputAdapter.adapt(array[0], routingContext));

        return ret;
    }

    @Override
    public List<Class<? extends OutputAdapter<?>>> outputAdapterTypes() {
        return Arrays.asList(RegressionOutputAdapter.class);
    }


}
