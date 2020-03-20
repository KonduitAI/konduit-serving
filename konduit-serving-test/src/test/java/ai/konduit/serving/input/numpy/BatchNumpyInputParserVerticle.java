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

package ai.konduit.serving.input.numpy;

import ai.konduit.serving.input.conversion.BatchInputParser;
import ai.konduit.serving.input.conversion.ConverterArgs;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.numpy.VertxBufferNumpyInputAdapter;
import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.Getter;
import org.datavec.api.records.Record;
import org.nd4j.linalg.api.buffer.DataType;

import java.io.IOException;
import java.util.Collections;

@Getter
public class BatchNumpyInputParserVerticle extends BaseRoutableVerticle {

    public final static String INPUT_NAME_KEY = "inputNameKey";
    private BatchInputParser inputParser;
    private String inputName = "input1";
    private Record[] batch;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        router = Router.router(vertx);
        if (config().containsKey(INPUT_NAME_KEY)) {
            inputName = config().getString(INPUT_NAME_KEY);
        }

        BatchInputParser batchInputParser = BatchInputParser.builder()
                .inputParts(Collections.singletonList(inputName))
                .converters(Collections.singletonMap(inputName, new VertxBufferNumpyInputAdapter()))
                .converterArgs(Collections.singletonMap(inputName, ConverterArgs.builder()
                        .strings(Collections.singletonList(DataType.LONG.name())).build())).build();
        BatchNumpyInputParserVerticle.this.inputParser = batchInputParser;


        router().post().handler(BodyHandler.create().setMergeFormAttributes(true));
        router.post("/").handler(itemHandler -> {
            try {
                BatchNumpyInputParserVerticle.this.batch = batchInputParser.createBatch(itemHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }

            itemHandler.response().end();
        });
    }
}
