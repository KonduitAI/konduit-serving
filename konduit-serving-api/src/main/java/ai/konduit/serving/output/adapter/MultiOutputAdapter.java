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
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;
import java.util.Map;

/**
 * Convert one or more input {@link INDArray}
 * (one per output name) in to an appropriate
 * json object representing the domain to be interpreted.
 *
 * @author Adam Gibson
 * @deprecated To be removed - see https://github.com/KonduitAI/konduit-serving/issues/298
 */
@Deprecated
public interface MultiOutputAdapter<T> {

    /**
     * Adapt a pair of {@link INDArray}
     * and the output names,
     * with the array input ordered by the output name
     *
     * @param input          the arrays to adapt
     * @param outputNames    the output names to adapt
     * @param routingContext routing context
     * @return Adapted inputs
     */
    Map<String, BatchOutput> adapt(T input, List<String> outputNames, RoutingContext routingContext);

    /**
     * Returns a map of the strings by output name
     * to the {@link OutputAdapter}
     * for each output
     *
     * @return the output adapter types for this multi output adapter
     */
    List<Class<? extends OutputAdapter<?>>> outputAdapterTypes();
}
