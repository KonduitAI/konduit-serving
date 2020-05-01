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

package ai.konduit.serving.pipeline.api.pipeline;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;

import java.io.Serializable;

/**
 * A Pipeline object represents the configuration of a machine learning pipeline - including zero or more machine learning
 * models, and zero or more ETL steps, etc.<br>
 * Fundamentally, a Pipeline is a {@link Data} -> {@link Data} transformation, that is built out of any number of
 * {@link PipelineStep} operations internally.
 * <p>
 * A Pipeline may be a simple sequence of {@link PipelineStep}s or may be a more complex directed acyclic graph of steps,
 * perhaps including conditional operations/branching
 */
public interface Pipeline extends TextConfig, Serializable {

    /**
     * Return (or instantiate if necessary) the executor for executing this pipeline
     *
     * @return An instantiated pipeline executor for this pipeline
     */
    PipelineExecutor executor();


}
