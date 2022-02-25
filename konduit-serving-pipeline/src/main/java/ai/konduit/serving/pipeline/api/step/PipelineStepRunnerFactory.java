/* ******************************************************************************
 * Copyright (c) 2022 Konduit K.K.
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

/**
 * PipelineRunnerFactory is used to instantiate a {@link PipelineStepRunner} from a {@link PipelineStep}
 * i.e., get the pipeline step execution class from the pipeline step definitionh/configuration class.
 * <p>
 * This design is used for a number of reasons:
 * (a) It allows for multiple PipelineStepRunner implementations that are able to run a given pipeline step
 * In principle we could have multiple on the classpath - or swap in different pipeline step runners for the same
 * pipeline step, in different situations / use cases.
 * (b) It is more appropriate a design for OSGi-based implementations where things like Class.forName can't be used
 * <p>
 * <p>
 * TODO: We'll need a way to prioritize PipelineRunnerFactory instances - what happens if there are 2 or more ways to
 * run a given pipeline? Which should we choose?
 */
public interface PipelineStepRunnerFactory {

    /**
     * Returns true if the PipelineRunnerFactory is able to create a PipelineStepRunner for this particular
     * type of pipeline step
     *
     * @param step The pipeline step to check if this PipelineRunnerFactory can execute
     * @return True if the pipeline step can be executed by the type of runners that this factory creates
     */
    boolean canRun(PipelineStep step);

    /**
     * Returns a {@link PipelineStepRunner} that can be used to execute the given PipelineStep
     *
     * @param step The pipeline step to execute
     * @return The instantiated PipelineStepRunner
     */
    PipelineStepRunner create(PipelineStep step);

}
