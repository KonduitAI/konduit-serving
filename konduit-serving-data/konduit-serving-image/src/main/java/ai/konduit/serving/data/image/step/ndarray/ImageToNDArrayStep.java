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

package ai.konduit.serving.data.image.step.ndarray;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@Builder
@Data
public class ImageToNDArrayStep implements PipelineStep {

    //TODO allow output names to be set

    private ImageToNDArrayConfig config;
    private List<String> keys;
    private List<String> outputNames;
    @Builder.Default
    private boolean keepOtherValues = true;

    public ImageToNDArrayStep(@JsonProperty("config") ImageToNDArrayConfig config, @JsonProperty("keys") List<String> keys,
                              @JsonProperty("outputNames") List<String> outputNames,
                              @JsonProperty("keepOtherValues") boolean keepOtherValues){
        this.config = config;
        this.keys = keys;
        this.outputNames = outputNames;
        this.keepOtherValues = keepOtherValues;
    }

}
