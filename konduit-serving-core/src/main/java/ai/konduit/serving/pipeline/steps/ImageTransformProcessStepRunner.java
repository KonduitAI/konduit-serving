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

import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.util.ImagePermuter;
import org.datavec.api.writable.BytesWritable;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.datavec.image.data.ImageWritable;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.transform.ImageTransformProcess;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.primitives.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ImageTransformProcessStepRunner extends BaseStepRunner {

    private Map<String, NativeImageLoader> imageLoaders;
    private ImageLoadingStep imageLoadingStepConfig;

    public ImageTransformProcessStepRunner(BasePipelineStep pipelineStep) {
        super(pipelineStep);

        this.imageLoadingStepConfig = (ImageLoadingStep) pipelineStep;

        imageLoaders = imageLoadingStepConfig.getInputNames().stream()
                .map(inputName -> {
                    Long[] values = imageLoadingStepConfig.getDimensionsConfigs().getOrDefault(inputName, null);
                    if (values != null) {
                        return new Pair<>(inputName, new NativeImageLoader(values[0], values[1], values[2]));
                    } else {
                        return new Pair<>(inputName, new NativeImageLoader());
                    }
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        Preconditions.checkState(!imageLoaders.isEmpty(), "No image loaders specified.");
    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs) {
        String inputName = imageLoadingStepConfig.getInputNames().get(inputIndex);

        NativeImageLoader nativeImageLoader = imageLoaders.get(inputName);
        ImageTransformProcess imageTransformProcess = imageLoadingStepConfig.getImageTransformProcesses().get(inputName);

        INDArray input;

        try {
            if (writable instanceof ImageWritable) {
                input = nativeImageLoader.asMatrix(((ImageWritable) writable).getFrame());
            } else if (writable instanceof BytesWritable) {
                input = nativeImageLoader.asMatrix(((BytesWritable) writable).getContent());
            } else if (writable instanceof Text) {
                input = nativeImageLoader.asMatrix(writable.toString());
            } else if (writable instanceof NDArrayWritable) {
                input = ((NDArrayWritable) writable).get();
            } else {
                throw new IllegalArgumentException("Illegal type to load from " + writable.getClass());
            }

            INDArray output;

            if (imageLoadingStepConfig.isUpdateOrderingBeforeTransform()) {
                output = applyTransform(imageTransformProcess, nativeImageLoader, permuteImageOrder(input));
            } else {
                output = permuteImageOrder(applyTransform(imageTransformProcess, nativeImageLoader, input));
            }

            record.add(new NDArrayWritable(output));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private INDArray permuteImageOrder(INDArray input) {
        if (!imageLoadingStepConfig.initialImageLayoutMatchesFinal()) {
            return ImagePermuter.permuteOrder(input,
                    imageLoadingStepConfig.getImageProcessingInitialLayout(),
                    imageLoadingStepConfig.getImageProcessingRequiredLayout());
        } else {
            return input;
        }
    }

    private INDArray applyTransform(ImageTransformProcess imageTransformProcess, NativeImageLoader nativeImageLoader, INDArray input) throws IOException {
        if (imageTransformProcess != null) {
            return imageTransformProcess.executeArray(new ImageWritable(nativeImageLoader.asFrame(input)));
        } else {
            return input;
        }
    }
}