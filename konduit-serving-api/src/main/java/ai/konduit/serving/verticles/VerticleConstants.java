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

package ai.konduit.serving.verticles;

/**
 * Constants for the config.json
 * for initializing verticles.
 *
 * @author Adam Gibson
 */
public class VerticleConstants {

    // General

    public final static String KONDUIT_SERVING_PORT = "KONDUIT_SERVING_PORT";

    public final static String CONVERTED_INFERENCE_DATA = "convertedInferenceData";
    public final static String HTTP_PORT_KEY = "httpPort";
    public final static String TRANSACTION_ID = "transactionId";
    //keys for the routing context when doing object recognition
    public final static String ORIGINAL_IMAGE_HEIGHT = "originalImageHeight";
    public final static String ORIGINAL_IMAGE_WIDTH = "originalImageWidth";


    // Mem map
    public final static String MEM_MAP_VECTOR_PATH = "memMapVectorPath";


}
