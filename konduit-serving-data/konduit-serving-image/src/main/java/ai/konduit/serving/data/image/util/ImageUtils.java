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

package ai.konduit.serving.data.image.util;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.Point;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.nd4j.common.base.Preconditions;
import org.nd4j.nativeblas.NativeOpsHolder;

public class ImageUtils {

    private ImageUtils(){ }

    public static BoundingBox accountForCrop(Image image, BoundingBox bbox, ImageToNDArrayConfig config) {
        return accountForCrop(bbox, image.width(), image.height(), config);
    }

    public static BoundingBox accountForCrop(BoundingBox bbox, int width, int height, ImageToNDArrayConfig config) {
        if (config == null)
            return bbox;

        BoundingBox cropRegion = ImageToNDArray.getCropRegion(width, height, config);

        double cropWidth = cropRegion.width();
        double cropHeight = cropRegion.height();

        double x1 = cropRegion.x1() + cropWidth * bbox.x1();
        double x2 = cropRegion.x1() + cropWidth * bbox.x2();
        double y1 = cropRegion.y1() + cropHeight * bbox.y1();
        double y2 = cropRegion.y1() + cropHeight * bbox.y2();

        return BoundingBox.createXY(x1, x2, y1, y2, bbox.label(), bbox.probability());
    }


    public static Point accountForCrop(Point relPoint, int width, int height, ImageToNDArrayConfig imageToNDArrayConfig) {
        if(imageToNDArrayConfig == null){
            return relPoint.toAbsolute(width, height);
        }

        BoundingBox cropRegion = ImageToNDArray.getCropRegion(width, height, imageToNDArrayConfig);
        double cropWidth = cropRegion.width();
        double cropHeight = cropRegion.height();

        return Point.create(
                cropRegion.x1() + cropWidth * relPoint.x(),
                cropRegion.y1() + cropHeight * relPoint.y(),
                relPoint.label(),
                relPoint.probability()
        ).toAbsolute(width, height);
    }



}
