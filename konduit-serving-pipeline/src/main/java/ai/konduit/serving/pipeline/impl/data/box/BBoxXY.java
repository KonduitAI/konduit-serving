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
@Schema(description = "A bounding box based on x1, x2, y1 and y2 locations (i.e., lower/upper X, lower/upper Y coordinate).")
public class BBoxXY implements BoundingBox {

    @Schema(description = "Lower X coordinate.")
    private final double x1;

    @Schema(description = "Upper X coordinate.")
    private final double x2;

    @Schema(description = "Lower Y coordinate.")
    private final double y1;

    @Schema(description = "Upper Y coordinate.")
    private final double y2;

    @Schema(description = "Class label.")
    private final String label;

    @Schema(description = "Class probability.")
    private final Double probability;

    public BBoxXY(double x1, double x2, double y1, double y2){
        this(x1, x2, y1, y2, null, null);
    }

    public BBoxXY(double x1, double x2, double y1, double y2, String label, Double probability){
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.label = label;
        this.probability = probability;
    }


    @Override
    public double cx() {
        return (x1+x2)/2;
    }

    @Override
    public double cy() {
        return (y1+y2)/2;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof BoundingBox))
            return false;
        return BoundingBox.equals(this, (BoundingBox)o);
    }
}
