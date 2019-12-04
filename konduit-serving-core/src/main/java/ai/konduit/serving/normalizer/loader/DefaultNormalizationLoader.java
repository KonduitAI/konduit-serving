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

package ai.konduit.serving.normalizer.loader;

import lombok.AllArgsConstructor;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;

/**
 * The default normalization loader
 * uses dl4j's {@link NormalizerSerializer}
 * to load the model from a given path.
 * <p>
 * Typically this will be a persisted zip file.
 * See {@link NormalizerSerializer}
 * for more information on the expected file format.
 *
 * @author Adam Gibson
 */
@AllArgsConstructor
public class DefaultNormalizationLoader implements NormalizationLoader {

    protected String path;


    @Override
    public DataNormalization load() throws Exception {
        return NormalizerSerializer.getDefault().restore(path);
    }


}
