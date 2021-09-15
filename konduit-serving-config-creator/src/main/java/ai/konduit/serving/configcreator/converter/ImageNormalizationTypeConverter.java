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

import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import picocli.CommandLine;


/**
 * Split the image normalization configuration by space
 * due to us using CSV for parsing other values.
 */
public class ImageNormalizationTypeConverter implements CommandLine.ITypeConverter<ImageNormalization> {
    @Override
    public ImageNormalization convert(String value) throws Exception {
        String[] split = value.split(" ");
        ImageNormalization.Type type = ImageNormalization.Type.valueOf(split[0].toUpperCase());
        //first 3 values are mean, second 3 values are standard deviation
        double[] mean = null,std = null;
        Double maxValue = null;
        if(split.length >= 4) {
            mean = new double[3];
            mean[0] = Double.parseDouble(split[1]);
            mean[1] = Double.parseDouble(split[2]);
            mean[2] = Double.parseDouble(split[3]);
        }

        if(split.length >= 7) {
            std = new double[3];
            std[0] = Double.parseDouble(split[4]);
            std[1] = Double.parseDouble(split[5]);
            std[2] = Double.parseDouble(split[6]);
        }

        if(split.length >= 8) {
            maxValue = Double.parseDouble(split[7]);
        }

        return new ImageNormalization(type,maxValue,mean,std);
    }
}
