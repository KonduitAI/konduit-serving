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

package ai.konduit.serving.model.loader;

import io.vertx.core.buffer.Buffer;

/**
 * Model loader. Given a path
 * knows how to load a model of a specified type from disk.
 *
 * @param <T> the type of the model
 * @author Adam Gibson
 */
public interface ModelLoader<T> {


    /**
     * Save a model as a buffer
     *
     * @param model the model to save
     * @return a buffer representing
     * the binary representation of the model
     */
    Buffer saveModel(T model);

    /**
     * Load the model
     *
     * @return the loaded model
     * @throws Exception if an error occurs loading the model
     */
    T loadModel() throws Exception;

}
