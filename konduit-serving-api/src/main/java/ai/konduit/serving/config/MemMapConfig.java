/*
 *
 *  * ******************************************************************************
 *  *  *
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

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemMapConfig implements Serializable {

    public final static String ARRAY_URL = "arrayPath";
    public final static String INITIAL_MEM_MAP_SIZE = "initialMemmapSize";
    public final static long DEFAULT_INITIAL_SIZE = 1000000000;
    public final static String WORKSPACE_NAME = "memMapWorkspace";

    private String arrayPath,unkVectorPath;
    @Builder.Default
    private long initialMemmapSize = DEFAULT_INITIAL_SIZE;
    @Builder.Default
    private String workSpaceName = WORKSPACE_NAME;



}
