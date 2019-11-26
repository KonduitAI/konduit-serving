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

package ai.konduit.serving.config;

/**
 * Enums for specifying different kinds of
 * outputs available to the server.
 *
 * @author Adam Gibson
 */
public class Output {

    /**
     * Data types for output
     */
    public enum DataType {
        NUMPY,
        JSON,
        ND4J,
        ARROW,
    }


    /**
     * Used by {@link ai.konduit.serving.output.adapter.OutputAdapter}.
     * This is for specifying modifications of outputs.
     */
    public enum PredictionType {
        CLASSIFICATION,
        YOLO,
        SSD,
        RCNN,
        RAW,
        REGRESSION
    }
}

