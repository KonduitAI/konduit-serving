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
import org.dmg.pmml.FieldName;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;
import java.util.Map;

/**
 * Convert an input {@link INDArray}
 * or {@link FieldName} list map
 * from PMML to human readable json.
 *
 * @param <T> the type to convert to
 * @author Adam Gibson
 * @deprecated To be removed - see https://github.com/KonduitAI/konduit-serving/issues/298
 */
@Deprecated
public interface OutputAdapter<T extends BatchOutput> {

    /**
     * Given an input array,
     * output the desired type
     *
     * @param array          the input array
     * @param routingContext Vert.x routing context
     * @return the desired output
     */
    T adapt(INDArray array, RoutingContext routingContext);

    /**
     * Convert the pmml to
     * the desired type
     *
     * @param pmmlExamples   the list of examples to convert
     * @param routingContext Vert.x routing context
     * @return the desired output type
     */
    T adapt(List<? extends Map<FieldName, ?>> pmmlExamples, RoutingContext routingContext);

    /**
     * Adapt an arbitrary object.
     * This is for types that may be outside of pmml
     * or {@link INDArray}
     *
     * @param input          the input to convert
     * @param routingContext routing context
     * @return the output type
     */
    T adapt(Object input, RoutingContext routingContext);

    /**
     * Returns the output type of this
     * output adapter.
     * This metadata is used for documentation
     * generation
     *
     * @return adapter type
     */
    Class<T> outputAdapterType();


}
