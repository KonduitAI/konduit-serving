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

package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.ImageLoading;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.util.ImagePermuter;
import org.datavec.api.writable.BytesWritable;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.datavec.image.data.ImageWritable;
import org.datavec.image.loader.NativeImageLoader;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageTransformProcessPipelineStepRunner extends BasePipelineStepRunner {

    private Map<String, NativeImageLoader> imageLoaders;
    private ImageLoading imageLoadingConfig;

    public ImageTransformProcessPipelineStepRunner(PipelineStep pipelineStep) {
        super(pipelineStep);

        imageLoaders = new HashMap<>();
        this.imageLoadingConfig = (ImageLoading) pipelineStep;

        for(int i = 0; i < pipelineStep.getInputNames().size(); i++) {
            String s = pipelineStep.getInputNames().get(i);
            if(imageLoadingConfig.getDimensionsConfigs().containsKey(s)) {
                Long[] values = imageLoadingConfig.getDimensionsConfigs().get(s);
                NativeImageLoader nativeImageLoader = new NativeImageLoader(values[0], values[1], values[2]);
                imageLoaders.put(s, nativeImageLoader);
            } else {
                NativeImageLoader nativeImageLoader = new NativeImageLoader();
                imageLoaders.put(s,nativeImageLoader);
            }
        }

        Preconditions.checkState(!imageLoaders.isEmpty(),"No image loaders specified.");
    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs) {
        NativeImageLoader nativeImageLoader = getImageLoaderAtIndex(inputIndex);

        if (writable instanceof ImageWritable) {
            ImageWritable imageWritable = (ImageWritable) writable;

            try {
                INDArray arr = nativeImageLoader.asMatrix(imageWritable.getFrame());

                if(!imageLoadingConfig.initialImageLayoutMatchesFinal()) {
                    arr = ImagePermuter.permuteOrder(arr,
                            imageLoadingConfig.getImageProcessingInitialLayout(),
                            imageLoadingConfig.getImageProcessingRequiredLayout());
                }

                record.add(new NDArrayWritable(arr));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (writable instanceof BytesWritable) {
            BytesWritable bytesWritable = (BytesWritable) writable;

            try {
                INDArray arr = nativeImageLoader.asMatrix(bytesWritable.getContent());

                if(!imageLoadingConfig.initialImageLayoutMatchesFinal()) {
                    arr = ImagePermuter.permuteOrder(arr,
                            imageLoadingConfig.getImageProcessingInitialLayout(),
                            imageLoadingConfig.getImageProcessingRequiredLayout());
                }

                record.add(new NDArrayWritable(arr));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (writable instanceof Text) {
            try {
                INDArray arr = nativeImageLoader.asMatrix(writable.toString());

                if(!imageLoadingConfig.initialImageLayoutMatchesFinal()) {
                    arr = ImagePermuter.permuteOrder(arr,
                            imageLoadingConfig.getImageProcessingInitialLayout(),
                            imageLoadingConfig.getImageProcessingRequiredLayout());
                }

                record.add(new NDArrayWritable(arr));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if(writable instanceof NDArrayWritable) {
            NDArrayWritable ndArrayWritable = (NDArrayWritable) writable;
            INDArray arr = ndArrayWritable.get();

            if(!imageLoadingConfig.initialImageLayoutMatchesFinal()) {
                arr = ImagePermuter.permuteOrder(arr,
                        imageLoadingConfig.getImageProcessingInitialLayout(),
                        imageLoadingConfig.getImageProcessingRequiredLayout());
            }

            record.add(new NDArrayWritable(arr));
        } else {
            throw new IllegalArgumentException("Illegal type to load from " + writable.getClass());
        }
    }

    public NativeImageLoader getImageLoaderAtIndex(int i) {
        return imageLoaders.get(imageLoadingConfig.getInputNames().get(i));
    }

}
