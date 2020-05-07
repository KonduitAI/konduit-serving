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

package ai.konduit.serving.data.image.format;

import ai.konduit.serving.data.image.data.FrameImage;
import ai.konduit.serving.data.image.data.OpenCVMatImage;
import ai.konduit.serving.data.image.data.MatImage;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.format.ImageFactory;
import org.bytedeco.javacv.Frame;
import org.nd4j.common.base.Preconditions;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JavaCVImageFactory implements ImageFactory {

    private static Set<Class<?>> SUPPORTED_TYPES = new HashSet<>();
    static {
        SUPPORTED_TYPES.add(Frame.class);
        SUPPORTED_TYPES.add(Mat.class);
        SUPPORTED_TYPES.add(org.bytedeco.opencv.opencv_core.Mat.class);
    }

    @Override
    public Set<Class<?>> supportedTypes() {
        return Collections.unmodifiableSet(SUPPORTED_TYPES);
    }

    @Override
    public boolean canCreateFrom(Object o) {
        return SUPPORTED_TYPES.contains(o.getClass());
    }

    @Override
    public Image create(Object o) {
        Preconditions.checkState(canCreateFrom(o), "Unable to create Image from object of type %s", o.getClass());

        if(o instanceof Frame){
            return new FrameImage((Frame) o);
        } else if(o instanceof Mat){
            return new OpenCVMatImage((Mat)o);
        } else if(o instanceof org.bytedeco.opencv.opencv_core.Mat){
            return new MatImage((org.bytedeco.opencv.opencv_core.Mat) o);
        } else {
            throw new IllegalStateException("Unable to create image from format " + o.getClass());
        }
    }
}
