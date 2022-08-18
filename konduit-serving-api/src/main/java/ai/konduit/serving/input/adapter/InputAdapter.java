/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.input.adapter;


import ai.konduit.serving.input.conversion.ConverterArgs;

import java.io.IOException;
import java.util.Map;

/**
 * An input adapter covers converting input data of 1
 * type to a suitable output type for use with
 * ETL pipeline components such as {@link org.datavec.api.transform.TransformProcess}
 * . Usually the input type is a raw {@link io.vertx.core.json.JsonArray}
 * or {@link io.vertx.core.buffer.Buffer} that is then mapped to some input
 * such as an ndarray or ArrowWritableRecordBatch
 *
 * @param <INPUT_TYPE>  the input type (usually json objects or buffers coming in off the wire)
 * @param <OUTPUT_TYPE> the output type for use with internal ETL tooling and inference
 *                      by a verticle
 * @author Adam Gibson
 */
public interface InputAdapter<INPUT_TYPE, OUTPUT_TYPE> {

    /**
     * Convert the input type
     * to the desired output type
     * given the {@link ConverterArgs}
     *
     * @param input       the input to convert
     * @param parameters  the parameters relevant
     *                    for conversion of the output
     * @param contextData the routing context when converting
     * @return the desired output
     * @throws IOException I/O exception
     */
    OUTPUT_TYPE convert(INPUT_TYPE input, ConverterArgs parameters, Map<String, Object> contextData) throws IOException;

}
