/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.pipeline.impl.pipeline.graph;

public class GraphConstants {

    private GraphConstants(){ }

    public static final String INPUT_KEY = "@input";
    public static final String TYPE_KEY = "@type";

    public static final String GRAPH_MERGE_JSON_KEY = "MERGE";
    public static final String GRAPH_ANY_JSON_KEY = "ANY";
    public static final String GRAPH_SWITCH_JSON_KEY = "SWITCH";
    public static final String GRAPH_SWITCH_OUTPUT_JSON_KEY = "SWITCH_OUTPUT";

}
