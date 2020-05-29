/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.pipeline.api.step;

import ai.konduit.serving.pipeline.api.TextConfig;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * A PipelineStep defines the configuration for one component of a Pipeline. Note that no execution-related
 * functionality is implemented in a {@link PipelineStepRunner}<br>
 * <br>
 * A given PipelineStep may be executable by more than one type of {@link PipelineStepRunner}.
 * For example, a TensorFlow model could in principle be executed by any of the TensorFlow runtime, SameDiff, or
 * the ONNX runtime.
 */
//Note: For JSON subtypes, we can't use the typical Jackson annotations, as the classes implementing the PipelineStep
//      interface won't generally be defined in konduit-serving-pipeline module - hence can't be referenced here
//     In practice this means we'll use a registration type approach via JsonSubTypesMapping
@JsonTypeInfo(use = NAME, property = "@type")
public interface PipelineStep extends TextConfig, Serializable {

    default String name() {
        return getClass().getSimpleName();
    }
}

