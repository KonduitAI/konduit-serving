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
public class BBoxXY implements BoundingBox {

    private final double x1;
    private final double x2;
    private final double y1;
    private final double y2;
    private final String label;
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
