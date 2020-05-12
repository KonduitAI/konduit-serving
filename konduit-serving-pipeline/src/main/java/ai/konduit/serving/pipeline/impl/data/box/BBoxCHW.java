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
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class BBoxCHW implements BoundingBox {

    private final double cx;
    private final double cy;
    private final double h;
    private final double w;
    private final String label;
    private final Double probability;

    public BBoxCHW(double cx, double cy, double h, double w){
        this(cx, cy, h, w, null, null);
    }

    public BBoxCHW(double cx, double cy, double h, double w, String label, Double probability){
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
