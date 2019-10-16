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

package ai.konduit.serving.input.conversion;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.image.transform.ImageTransformProcess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The converter arguments
 * needed for input binary data.
 example usage.
 *
 * @author Adam Gibson
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConverterArgs implements Serializable {

    private Schema schema;
    private TransformProcess transformProcess;
    private ImageTransformProcess imageTransformProcess;

    @Builder.Default
    private List<Integer> integers = new ArrayList<>();
    @Builder.Default
    private List<Long> longs = new ArrayList<>();
    @Builder.Default
    private List<Float> floats = new ArrayList<>();
    @Builder.Default
    private List<Double> doubles = new ArrayList<>();
    @Builder.Default
    private List<String> strings = new ArrayList<>();

    private String imageProcessingRequiredLayout,imageProcessingInitialLayout = "NCHW";

}
