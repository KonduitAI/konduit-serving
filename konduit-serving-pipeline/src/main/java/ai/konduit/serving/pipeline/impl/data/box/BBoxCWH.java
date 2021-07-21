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
package ai.konduit.serving.pipeline.impl.data.box;

import ai.konduit.serving.pipeline.api.data.BoundingBox;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@Schema(description = "A bounding box based on center X, center Y, height and width format.")
public class BBoxCWH implements BoundingBox {

    @Schema(description = "Center X coordinate.")
    private final double cx;

    @Schema(description = "Center Y coordinate.")
    private final double cy;

    @Schema(description = "Box height.")
    private final double h;

    @Schema(description = "Box width.")
    private final double w;

    @Schema(description = "Class label.")
    private final String label;

    @Schema(description = "Class probability.")
    private final Double probability;

    public BBoxCWH(double cx, double cy, double w, double h){
        this(cx, cy, w, h, null, null);
    }

    public BBoxCWH(double cx, double cy, double w, double h, String label, Double probability){
        this.cx = cx;
        this.cy = cy;
        this.h = h;
        this.w = w;
        this.label = label;
        this.probability = probability;
    }


    @Override
    public double x1() {
        return cx - w/2;
    }

    @Override
    public double x2() {
        return cx + w/2;
    }

    @Override
    public double y1() {
        return cy - h/2;
    }

    @Override
    public double y2() {
        return cy + h/2;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof BoundingBox))
            return false;
        return BoundingBox.equals(this, (BoundingBox)o);
    }
}
