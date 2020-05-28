/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.data.image.serde;

import ai.konduit.serving.data.image.step.bb.draw.DrawBoundingBoxStep;
import ai.konduit.serving.data.image.step.bb.extract.ExtractBoundingBoxStep;
import ai.konduit.serving.data.image.step.grid.crop.CropFixedGridStep;
import ai.konduit.serving.data.image.step.grid.crop.CropGridStep;
import ai.konduit.serving.data.image.step.grid.draw.DrawFixedGridStep;
import ai.konduit.serving.data.image.step.grid.draw.DrawGridStep;
import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.data.image.step.segmentation.index.DrawSegmentationStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import ai.konduit.serving.pipeline.api.step.PipelineStep;

import java.util.ArrayList;
import java.util.List;

public class ImageJsonSubTypesMapping implements JsonSubTypesMapping {
    @Override
    public List<JsonSubType> getSubTypesMapping() {
        List<JsonSubType> l = new ArrayList<>();
        l.add(new JsonSubType("SHOW_IMAGE", ShowImagePipelineStep.class, PipelineStep.class));
        l.add(new JsonSubType("IMAGE_TO_NDARRAY", ImageToNDArrayStep.class, PipelineStep.class));
        l.add(new JsonSubType("DRAW_BOUNDING_BOX", DrawBoundingBoxStep.class, PipelineStep.class));
        l.add(new JsonSubType("EXTRACT_BOUNDING_BOX", ExtractBoundingBoxStep.class, PipelineStep.class));
        l.add(new JsonSubType("DRAW_GRID", DrawGridStep.class, PipelineStep.class));
        l.add(new JsonSubType("DRAW_FIXED_GRID", DrawFixedGridStep.class, PipelineStep.class));
        l.add(new JsonSubType("CROP_GRID", CropGridStep.class, PipelineStep.class));
        l.add(new JsonSubType("CROP_FIXED_GRID", CropFixedGridStep.class, PipelineStep.class));
        l.add(new JsonSubType("DRAW_SEGMENTATION", DrawSegmentationStep.class, PipelineStep.class));
        return l;
    }
}
