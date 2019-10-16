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

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import lombok.Builder;
import lombok.Data;

/**
 * A small utility class for interacting with multi part
 * endpoints with vertx.
 *
 * @author Adam Gibson
 */
@Builder
@Data
public class MultipartClient {

    private HttpClient client;
    private Buffer[] fileData;
    private String[] fileNames;
    private String[] names;
    private int port;
    @Builder.Default
    private String host = "localhost";
    private String uri;
    @Builder.Default
    private String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
    private Handler<Buffer> bodyHandler;
    private Handler<Throwable> exceptionHandler;


    /**
     * Send a proper multi part request.
     */
    public void send() {
        Buffer buffer = UploadUtils.uploadBufferFor(fileData, fileNames, names, boundary);

        HttpClientRequest request = client.request(HttpMethod.POST, port, "localhost", uri, req -> {
            req.bodyHandler(bodyHandler);
            req.exceptionHandler(exceptionHandler);
        }).setChunked(true);


        UploadUtils.prepareRequestFor(request, buffer, boundary);
    }

}

