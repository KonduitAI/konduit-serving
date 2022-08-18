/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
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

package ai.konduit.serving.verticles;

/**
 * This package contains {@link io.vertx.core.Verticle} implementations meant to be standalone servers.
 * These classes can be invoked from the command line via {@link ai.konduit.serving.launcher.KonduitServingLauncher}
 * <p>
 * All verticles in this package implement the same idea of some form of scoring of arbitrary
 * input over a network. The verticles have the same building blocks for the following workflow:
 * <p>
 * 1. network input (multi part or json) as a {@link io.vertx.core.buffer.Buffer}
 * 2. 1 or more {@link ai.konduit.serving.pipeline.handlers.converter.multi.InputAdapter} for conversion
 * 3. Output from the {@link ai.konduit.serving.pipeline.handlers.converter.multi.InputAdapter} input in to an {@link ai.konduit.serving.executioner.inference.InferenceExecutioner}
 * 4. Output from the {@link ai.konduit.serving.executioner.inference.InferenceExecutioner} goes in to 1 or more {@link ai.konduit.serving.output.adapter.OutputAdapter}
 * for return by the verticle to the user as easily consumable json or binary.
 * <p>
 * <p>
 * Other notes:
 * {@link ai.konduit.serving.executioner.inference.InferenceExecutioner} uses {@link ai.konduit.serving.model.loader.ModelLoader}
 * implementations to load the model configurations from disk or a varied data source.
 * <p>
 * Implementations of {@link io.vertx.core.Verticle} doing inference should be using these as building blocks
 * for I/O, scoring data.
 * <p>
 * Typically, extensions to the core may include new {@link ai.konduit.serving.pipeline.handlers.converter.multi.InputAdapter}
 * and {@link ai.konduit.serving.output.adapter.OutputAdapter} for various use cases.
 **/