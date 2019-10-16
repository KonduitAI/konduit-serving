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

package ai.konduit.serving.input.upload;

import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

/**
 * Meant for processing a whole stream of
 * one or more parts of a multi part file upload.
 */
public interface MultipartFileUploadHandler {

    /**
     * The listeners for results.
     * This lets each {@link ResultListener}
     * know that an upload has happened
     * @param context the routing context to use
     *                for the uploads
     * @param listeners the listeners for implementing a particular
     *                  action upon upload
     * @throws IOException I/O exception
     */
    void onMultiPart(RoutingContext context, ResultListener[] listeners) throws IOException;


    /**
     * A listener for taking action
     * upon an upload being processed.
     */
    interface  ResultListener {
        /**
         * The result method for processing
         * the upload output
         * @param ctx the routing context to use
         * @param result the results to use with the listener
         */
        void onResult(RoutingContext ctx, Object... result);
    }

}
