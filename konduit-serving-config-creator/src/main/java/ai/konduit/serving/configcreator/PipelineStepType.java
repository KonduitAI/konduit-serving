/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.pipeline.impl.step.ml.classifier.ClassifierOutputStep;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.SSDToBoundingBoxStep;

public enum PipelineStepType {
    CROP_GRID,
    CROP_FIXED_GRID,
    DL4J,
    KERAS,
    DRAW_BOUNDING_BOX,
    DRAW_FIXED_GRID,
    DRAW_GRID,
    DRAW_SEGMENTATION,
    EXTRACT_BOUNDING_BOX,
    CAMERA_FRAME_CAPTURE,
    VIDEO_FRAME_CAPTURE,
    IMAGE_TO_NDARRAY,
    LOGGING,
    SSD_TO_BOUNDING_BOX,
    SAMEDIFF,
    SHOW_IMAGE,
    TENSORFLOW,
    ND4JTENSORFLOW,
    PYTHON,
    ONNX,
    CLASSIFIER_OUTPUT,
    IMAGE_RESIZE,
    RELATIVE_TO_ABSOLUTE,
    DRAW_POINTS,
    DRAW_HEATMAP,
    PERSPECTIVE_TRANSFORM,
    IMAGE_CROP,
    GRAY_SCALE,
    TVM,
    TENSORRT,
    SAMEDIFF_TRAINING;

    /**
     * Returns the {@link PipelineStepType}
     * of a class that extends {@link PipelineStep}.
     * Note that all {@link PipelineStep} s that interact
     * with this class must be annotated with {@link JsonName}
     *  - that annotation information is mapped to
     *  the proper type
     * @param clazz the class
     * @return
     */
    public static PipelineStepType typeForClazz(Class<? extends PipelineStep> clazz) {
        if(clazz == null || clazz.getDeclaredAnnotation(JsonName.class) == null) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " is null or does not have annotation JsonName!");
        }
        JsonName annotation = clazz.getDeclaredAnnotation(JsonName.class);
        return PipelineStepType.valueOf(annotation.value().toUpperCase());
    }

    public static Class<? extends PipelineStep> clazzForType(PipelineStepType pipelineStepType) throws ClassNotFoundException {
        Class<?> clazz;
        switch (valueOf(pipelineStepType.name().toUpperCase())) {
            case IMAGE_CROP:
                clazz = Class.forName("ai.konduit.serving.data.image.step.crop.ImageCropStep");
                return (Class<? extends PipelineStep>) clazz;
            case DRAW_HEATMAP:
                clazz = Class.forName("ai.konduit.serving.data.image.step.point.heatmap.DrawHeatmapStep");
                return (Class<? extends PipelineStep>) clazz;
            case GRAY_SCALE:
                clazz = Class.forName("ai.konduit.serving.data.image.step.grayscale.GrayScaleStep");
                return (Class<? extends PipelineStep>) clazz;
            case DRAW_POINTS:
                clazz = Class.forName("ai.konduit.serving.data.image.step.point.draw.DrawPointsStep");
                return (Class<? extends PipelineStep>) clazz;
            case RELATIVE_TO_ABSOLUTE:
                clazz = Class.forName("ai.konduit.serving.data.image.step.point.convert.RelativeToAbsoluteStep");
                return (Class<? extends PipelineStep>) clazz;
            case IMAGE_RESIZE:
                clazz = Class.forName("ai.konduit.serving.data.image.step.resize.ImageResizeStep");
                return (Class<? extends PipelineStep>) clazz;
            case PERSPECTIVE_TRANSFORM:
                clazz = Class.forName("ai.konduit.serving.data.image.step.point.perspective.convert.PerspectiveTransformStep");
                return (Class<? extends PipelineStep>) clazz;
            case CROP_GRID:
                clazz = Class.forName("ai.konduit.serving.data.image.step.grid.crop.CropGridStep");
                return (Class<? extends PipelineStep>) clazz;
            case CROP_FIXED_GRID:
                clazz = Class.forName("ai.konduit.serving.data.image.step.grid.crop.CropFixedGridStep");
                return (Class<? extends PipelineStep>) clazz;
            case DL4J:
                clazz = Class.forName("ai.konduit.serving.models.deeplearning4j.step.DL4JStep");
                return (Class<? extends PipelineStep>) clazz;

            case KERAS:
                clazz = Class.forName("ai.konduit.serving.models.deeplearning4j.step.keras.KerasStep");
                return (Class<? extends PipelineStep>) clazz;
            case DRAW_BOUNDING_BOX:
                clazz = Class.forName("ai.konduit.serving.data.image.step.bb.draw.DrawBoundingBoxStep");
                return (Class<? extends PipelineStep>) clazz;
            case DRAW_FIXED_GRID:
                clazz = Class.forName("ai.konduit.serving.data.image.step.grid.draw.DrawFixedGridStep");
                return (Class<? extends PipelineStep>) clazz;
            case DRAW_GRID:
                clazz = Class.forName("ai.konduit.serving.data.image.step.grid.draw.DrawGridStep");
                return (Class<? extends PipelineStep>) clazz;
            case DRAW_SEGMENTATION:
                clazz = Class.forName("ai.konduit.serving.data.image.step.segmentation.index.DrawSegmentationStep");
                return (Class<? extends PipelineStep>) clazz;
            case EXTRACT_BOUNDING_BOX:
                clazz = Class.forName("ai.konduit.serving.data.image.step.bb.extract.ExtractBoundingBoxStep");
                return (Class<? extends PipelineStep>) clazz;
            case CAMERA_FRAME_CAPTURE:
                clazz = Class.forName("ai.konduit.serving.data.image.step.capture.CameraFrameCaptureStep");
                return (Class<? extends PipelineStep>) clazz;
            case VIDEO_FRAME_CAPTURE:
                clazz = Class.forName("ai.konduit.serving.data.image.step.capture.VideoFrameCaptureStep");
                return (Class<? extends PipelineStep>) clazz;
            case IMAGE_TO_NDARRAY:
                clazz = Class.forName("ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep");
                return (Class<? extends PipelineStep>) clazz;
            case LOGGING:
                return LoggingStep.class;
            case SSD_TO_BOUNDING_BOX:
                return SSDToBoundingBoxStep.class;
            case SAMEDIFF:
                clazz = Class.forName("ai.konduit.serving.models.samediff.step.SameDiffStep");
                return (Class<? extends PipelineStep>) clazz;
            case SAMEDIFF_TRAINING:
                clazz = Class.forName("ai.konduit.serving.models.samediff.step.trainer.SameDiffTrainerStep");
                return (Class<? extends PipelineStep>) clazz;
            case SHOW_IMAGE:
                clazz = Class.forName("ai.konduit.serving.data.image.step.show.ShowImageStep");
                return (Class<? extends PipelineStep>) clazz;
            case TENSORFLOW:
                clazz = Class.forName("ai.konduit.serving.models.tensorflow.step.TensorFlowStep");
                return (Class<? extends PipelineStep>) clazz;
            case ND4JTENSORFLOW:
                clazz = Class.forName("ai.konduit.serving.models.nd4j.tensorflow.step.Nd4jTensorFlowStep");
                return (Class<? extends PipelineStep>) clazz;
            case ONNX:
                clazz = Class.forName("ai.konduit.serving.models.onnx.step.ONNXStep");
                return (Class<? extends PipelineStep>) clazz;
            case PYTHON:
                return (Class<? extends PipelineStep>) Class.forName("ai.konduit.serving.python.PythonStep");
            case CLASSIFIER_OUTPUT:
                return ClassifierOutputStep.class;
            case TVM:
                return (Class<? extends PipelineStep>)  Class.forName("ai.konduit.serving.models.tvm.step.TVMStep");
            case TENSORRT:
                return (Class<? extends PipelineStep>)  Class.forName("ai.konduit.serving.tensorrt.TensorRTStep");

        }

        return null;
    }
}
