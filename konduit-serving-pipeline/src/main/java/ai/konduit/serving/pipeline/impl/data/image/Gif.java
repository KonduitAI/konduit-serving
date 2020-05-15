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

package ai.konduit.serving.pipeline.impl.data.image;

import ai.konduit.serving.pipeline.impl.data.image.base.BaseImageFile;

import java.io.File;

public class Gif extends BaseImageFile {

    public Gif(File file) {
        this(file, null, null);
    }

    public Gif(File file, Integer height, Integer width){
        super(file, height, width);
    }

    public Gif(byte[] bytes){
        this(bytes, null, null);
    }

    public Gif(byte[] bytes, Integer height, Integer width){
        super(bytes, height, width);
    }

    @Override
    public String formatName() {
        return "GIF";
    }
}
