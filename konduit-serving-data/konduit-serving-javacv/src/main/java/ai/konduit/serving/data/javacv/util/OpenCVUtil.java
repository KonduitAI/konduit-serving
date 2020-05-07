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

package ai.konduit.serving.data.javacv.util;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

import java.util.concurrent.atomic.AtomicBoolean;

public class OpenCVUtil {

    private OpenCVUtil(){ }


    private static final AtomicBoolean opencvLoaded = new AtomicBoolean();

    /**
     *
     */
    public static synchronized void ensureOpenCVLoaded(){
        if(opencvLoaded.get())
            return;

        /*
        Call Loader.load(opencv_java.class) before using the API in the org.opencv namespace.
         */
        Loader.load(opencv_java.class);

    }

}
