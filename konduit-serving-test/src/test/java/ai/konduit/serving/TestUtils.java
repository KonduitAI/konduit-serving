/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */
package ai.konduit.serving;

import java.io.File;

public class TestUtils {

    private TestUtils(){ }

    private static File baseResourcesDir;

    /**
     * Get the base storage directory for any test resources (downloaded and cached on system)
     */
    public static File testResourcesStorageDir(){
        if(baseResourcesDir == null){
            File f = new File(System.getProperty("user.home"), ".konduittest/");
            if(!f.exists())
                f.mkdirs();
            baseResourcesDir = f;
        }
        return baseResourcesDir;
    }

}
