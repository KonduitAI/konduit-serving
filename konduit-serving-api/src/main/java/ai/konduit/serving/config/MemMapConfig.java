/*
 *
 *  * ******************************************************************************
 *  *  *
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


package ai.konduit.serving.config;

import ai.konduit.serving.util.ObjectMappers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
/**
 * Configuration for managing serving of memory-mapped files. The goal is to mem-map
 * and serve a large array stored in "arrayPath" and get slices of this array on demand
 * by index. If an index is specified that does not match an index of the mem-mapped array,
 * an default or "unknown" vector is inserted into the slice instead, which is stored in
 * "unkVectorPath".
 *
 * For instance, let's say we want to mem-map [[1, 2, 3], [4, 5, 6]], a small array with two
 * valid slices. Our unknown vector is simply [0, 0, 0] in this example. Now, if we query for
 * the indices {-2, 1} we'd get [[0, 0, 0], [4, 5, 6]].
 */
public class MemMapConfig implements Serializable, TextConfig {

    public final static String ARRAY_URL = "arrayPath";
    public final static String INITIAL_MEM_MAP_SIZE = "initialMemmapSize";
    public final static long DEFAULT_INITIAL_SIZE = 1000000000;
    public final static String WORKSPACE_NAME = "memMapWorkspace";

    private String arrayPath, unkVectorPath;
    @Builder.Default
    private long initialMemmapSize = DEFAULT_INITIAL_SIZE;
    @Builder.Default
    private String workSpaceName = WORKSPACE_NAME;

    public static MemMapConfig fromJson(String json){
        return ObjectMappers.fromJson(json, MemMapConfig.class);
    }

    public static MemMapConfig fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, MemMapConfig.class);
    }
}
