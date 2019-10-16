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


import ai.konduit.serving.input.adapter.InputAdapter;
import ai.konduit.serving.input.conversion.ConverterArgs;
import ai.konduit.serving.util.image.NativeImageLoader;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.datavec.image.transform.ImageTransform;
import org.datavec.image.transform.MultiImageTransform;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.IOException;
import java.util.Map;

/**
 * A base class for {@link InputAdapter}
 * for converting an input type
 * using  a configured {@link NativeImageLoader}
 * from {@link #getImageLoader(Object, ConverterArgs)}
 *
 * @param <T> the type to adapt
 */
public abstract class BaseImageInputAdapter<T> implements InputAdapter<T, Writable> {

    @Override
    public Writable convert(T input, ConverterArgs parameters, Map<String, Object> contextData) throws IOException {
        NativeImageLoader imageLoader = getImageLoader(input, parameters);
        return new NDArrayWritable(getArrayUsing(imageLoader,input, parameters));
    }

    /**
     * Uses the passed in {@link NativeImageLoader}
     * to convert the specified input to a
     * {@link INDArray} (usually a bitmap format)
     * @param nativeImageLoader the {@link NativeImageLoader}
     *                          used for conversion
     * @param input the input to convert
     * @param converterArgs converter arguments to use
     * @return the converted input: an {@link INDArray}
     * representing the image
     * @throws IOException if an error occurs during the array creation
     */
    public abstract INDArray getArrayUsing(NativeImageLoader nativeImageLoader, T input, ConverterArgs converterArgs) throws IOException;

    /**
     * Get the image loader
     * configuring it using the
     * {@link ConverterArgs}
     * @param input the input to convert
     * @param converterArgs the converter args to use
     * @return the configured {@link NativeImageLoader}
     */
    public NativeImageLoader getImageLoader(T input, ConverterArgs converterArgs) {
        if(converterArgs == null || converterArgs.getLongs().isEmpty())
            return new NativeImageLoader();
        else if(converterArgs.getLongs().size() == 3) {
            if(converterArgs.getImageTransformProcess() != null) {
                return new NativeImageLoader(converterArgs.getLongs().get(0),converterArgs.getLongs().get(1),converterArgs.getLongs().get(2),new MultiImageTransform(converterArgs.getImageTransformProcess().getTransformList().toArray(new ImageTransform[1])));

            }
            else if(converterArgs.getImageTransformProcess() != null)
                return new NativeImageLoader(converterArgs.getLongs().get(0),converterArgs.getLongs().get(1),converterArgs.getLongs().get(2),new MultiImageTransform(converterArgs.getImageTransformProcess().getTransformList().toArray(new ImageTransform[1])));
            else {
                return new NativeImageLoader(converterArgs.getLongs().get(0),converterArgs.getLongs().get(1),converterArgs.getLongs().get(2));

            }
        }
        else if(converterArgs.getLongs().size() == 3) {
            if(converterArgs.getImageTransformProcess() != null) {
                return new NativeImageLoader(converterArgs.getLongs().get(0).intValue(),converterArgs.getLongs().get(1).intValue()
                        ,converterArgs.getLongs().get(2).intValue(),new MultiImageTransform(converterArgs.getImageTransformProcess().getTransformList().toArray(new ImageTransform[1])));
            }
            else
                return new NativeImageLoader(converterArgs.getLongs().get(0).intValue(),converterArgs.getLongs().get(1).intValue()
                        ,converterArgs.getLongs().get(2).intValue());
        }


        return new NativeImageLoader();
    }


}
