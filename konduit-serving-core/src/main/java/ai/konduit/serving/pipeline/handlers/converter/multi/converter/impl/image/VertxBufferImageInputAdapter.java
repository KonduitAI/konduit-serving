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

package ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.image;

import ai.konduit.serving.input.conversion.ConverterArgs;
import ai.konduit.serving.util.image.NativeImageLoader;
import ai.konduit.serving.verticles.VerticleConstants;
import io.vertx.core.buffer.Buffer;
import org.datavec.api.writable.Writable;
import org.datavec.image.data.ImageWritable;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Convert an image from a buffer to a matrix.
 *
 * @author Adam Gibson
 */
public class VertxBufferImageInputAdapter extends BaseImageInputAdapter<Buffer> {

    @Override
    public INDArray getArrayUsing(NativeImageLoader nativeImageLoader, Buffer input, ConverterArgs converterArgs) throws IOException {
        return nativeImageLoader.asMatrix(new ByteArrayInputStream(input.getBytes()));
    }

    @Override
    public Writable convert(Buffer input, ConverterArgs parameters, Map<String, Object> contextData) throws IOException {
        NativeImageLoader imageLoader = getImageLoader(input, parameters);
        ImageWritable image = imageLoader.asWritable(new ByteArrayInputStream(input.getBytes()));
        if (contextData != null) {
            contextData.put(VerticleConstants.ORIGINAL_IMAGE_HEIGHT, image.getHeight());
            contextData.put(VerticleConstants.ORIGINAL_IMAGE_WIDTH, image.getWidth());
        }

        return image;
    }


}
