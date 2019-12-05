/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
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

package ai.konduit.serving.configprovider;


import ai.konduit.serving.configprovider.args.InputDataType;
import ai.konduit.serving.configprovider.args.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A configurable POJO that converts
 * this POJO's fields to command line arguments.
 * For more information on what these fields do,
 * please see {@link KonduitServingMain}
 *
 * @author Adam Gibson
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KonduitServingMainArgs implements Serializable {

    private static Field[] fields;
    private String configHost;
    private int configPort;
    private String configStoreType;
    private String configPath;
    @Builder.Default
    private boolean workerNode = true;
    @Builder.Default
    private boolean ha = true;
    @Builder.Default
    private int numInstances = 1;
    @Builder.Default
    private boolean multiThreaded = true;
    @Builder.Default
    private int workerPoolSize = 1;
    private String verticleClassName;
    @Builder.Default
    private String vertxWorkingDirectory = System.getProperty("user.home");
    private InputDataType inputType;
    private ModelType modelType;

    public String[] toArgs() {
        if (fields == null) {
            fields = KonduitServingMainArgs.class.getDeclaredFields();

        }

        List<String> args = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            if (java.lang.reflect.Modifier.isStatic(fields[i].getModifiers())) {
                continue;
            }

            try {
                Object o = fields[i].get(this);
                if (o != null) {
                    if (!(o instanceof Boolean)) {
                        args.add("--" + fields[i].getName());
                        args.add(String.valueOf(o));
                    } else {
                        Boolean bool = (Boolean) o;
                        if (bool) {
                            args.add("--" + fields[i].getName());
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return args.toArray(new String[0]);
    }

}
