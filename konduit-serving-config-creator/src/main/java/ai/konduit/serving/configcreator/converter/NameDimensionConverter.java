/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator.converter;

import ai.konduit.serving.tensorrt.NamedDimension;
import ai.konduit.serving.tensorrt.NamedDimensionList;
import picocli.CommandLine;

public class NameDimensionConverter implements CommandLine.ITypeConverter<NamedDimensionList> {
    public final static String ENTRY_DELIMITER = ";";

    @Override
    public NamedDimensionList convert(String value) throws Exception {
        String[] split = value.split(ENTRY_DELIMITER);
        NamedDimensionList namedDimensions = new NamedDimensionList();
        for(String entry : split) {
            NamedDimension.NamedDimensionBuilder builder = NamedDimension.builder();
            String[] entrySplit = entry.split("=");
            String key = entrySplit[0];
            String[] valSplit = entrySplit[1].split(",");
            long[] result = new long[valSplit.length];
            for(int i = 0; i < result.length; i++) {
                result[i] = Long.parseLong(valSplit[i]);
            }

            builder.name(key);
            builder.dimensions(result);
            namedDimensions.add(builder.build());

            builder.build();
        }
        return namedDimensions;
    }
}
