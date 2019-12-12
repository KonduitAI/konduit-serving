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

package ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.numpy;


import ai.konduit.serving.input.adapter.InputAdapter;
import ai.konduit.serving.input.conversion.ConverterArgs;
import ai.konduit.serving.util.ImagePermuter;
import io.vertx.core.buffer.Buffer;
import org.bytedeco.javacpp.BytePointer;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.shade.guava.primitives.Longs;

import java.util.Map;

/**
 * Reads in {@link Buffer} containing raw numpy arrays and converts them to {@link NDArrayWritable},
 * using {@link ConverterArgs}.
 *
 * @author Adam Gibson
 */
public class VertxBufferNumpyInputAdapter implements InputAdapter<Buffer, Writable> {

    private boolean permuteRequired(ConverterArgs parameters) {
        return parameters != null && parameters.getImageProcessingInitialLayout() != null
                && !parameters.getImageProcessingInitialLayout().equals(parameters.getImageProcessingRequiredLayout());
    }

    /**
     * Convert Buffer input to NDArray writable. Note that contextData is unused in this implementation of InputAdapter.
     */
    @Override
    public NDArrayWritable convert(Buffer input, ConverterArgs parameters, Map<String, Object> contextData) {
        Preconditions.checkState(input.length() > 0, "Buffer appears to be empty!");
        INDArray fromNpyPointer = Nd4j.getNDArrayFactory().createFromNpyPointer(
                new BytePointer(input.getByteBuf().nioBuffer())
        );
        if (permuteRequired(parameters)) {
            fromNpyPointer = ImagePermuter.permuteOrder(
                    fromNpyPointer,
                    parameters.getImageProcessingInitialLayout(),
                    parameters.getImageProcessingRequiredLayout()
            );
        }

        return new NDArrayWritable(fromNpyPointer);
    }

}
