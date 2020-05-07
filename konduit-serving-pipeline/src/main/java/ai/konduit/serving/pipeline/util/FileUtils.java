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

package ai.konduit.serving.pipeline.util;

import java.io.File;

public class FileUtils {

    private FileUtils(){ }

    public static File getTempFileBaseDir(){
        File f = new File(System.getProperty("java.io.tmpdir"), "konduit-serving");
        if(!f.exists())
            f.mkdirs();
        return f;
    }

    public static File getTempFileDir(String subdirectory){
        File f = new File(getTempFileBaseDir(), subdirectory);
        if(!f.exists())
            f.mkdirs();
        return f;
    }

}
