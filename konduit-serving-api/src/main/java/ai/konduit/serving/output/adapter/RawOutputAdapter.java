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

import ai.konduit.serving.output.types.NDArrayOutput;
import io.vertx.ext.web.RoutingContext;
import org.dmg.pmml.FieldName;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;
import java.util.Map;

public class RawOutputAdapter implements OutputAdapter<NDArrayOutput> {


    @Override
    public NDArrayOutput adapt(INDArray array, RoutingContext routingContext) {
        return NDArrayOutput.builder().ndArray(array).build();
    }

    @Override
    public NDArrayOutput adapt(List<? extends Map<FieldName, ?>> pmmlExamples, RoutingContext routingContext) {
        throw new UnsupportedOperationException("Unable to convert pmml to ndarrays.");
    }

    @Override
    public NDArrayOutput adapt(Object input, RoutingContext routingContext) {
        if (input instanceof INDArray) {
            INDArray input2 = (INDArray) input;
            return adapt(input2, routingContext);
        }

        return null;
    }

    @Override
    public Class<NDArrayOutput> outputAdapterType() {
        return NDArrayOutput.class;
    }
}
