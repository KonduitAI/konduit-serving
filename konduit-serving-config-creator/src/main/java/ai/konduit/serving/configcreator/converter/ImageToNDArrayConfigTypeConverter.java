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

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts an {@link ImageToNDArrayConfig} with the following format:
 * Split fields of {@link ImageToNDArrayConfig} by comma
 * with each key/value being
 * fieldName=value
 */
public class ImageToNDArrayConfigTypeConverter implements CommandLine.ITypeConverter<ImageToNDArrayConfig> {

    @Override
    public ImageToNDArrayConfig convert(String value) throws Exception {
        String[] split = value.split(",");
        Map<String,String> input = new HashMap<>();
        for(String keyVal : split) {
            String[] keyValSplit = keyVal.split("=");
            input.put(keyValSplit[0],keyValSplit[1]);
        }

        ImageToNDArrayConfig imageToNDArrayConfig = new ImageToNDArrayConfig();
        for(Map.Entry<String,String> entry : input.entrySet()) {
            switch(entry.getKey()) {
                case "height":
                    imageToNDArrayConfig.height(Integer.parseInt(entry.getValue()));
                    break;
                case "width":
                    imageToNDArrayConfig.width(Integer.parseInt(entry.getValue()));
                    break;
                case "format":
                    imageToNDArrayConfig.format(NDFormat.valueOf(entry.getValue().toUpperCase()));
                    break;
                case "channelLayout":
                    imageToNDArrayConfig.channelLayout(NDChannelLayout.valueOf(entry.getValue().toUpperCase()));
                    break;
                case "aspectRatioHandling":
                    imageToNDArrayConfig.aspectRatioHandling(AspectRatioHandling.valueOf(entry.getValue()));
                    break;
                case "dataType":
                    imageToNDArrayConfig.dataType(NDArrayType.valueOf(entry.getValue().toUpperCase()));
                    break;
                case "normalization":
                    ImageNormalizationTypeConverter imageNormalizationTypeConverter = new ImageNormalizationTypeConverter();
                    ImageNormalization convert = imageNormalizationTypeConverter.convert(entry.getValue());
                    imageToNDArrayConfig.normalization(convert);
                    break;

            }
        }

        return imageToNDArrayConfig;
    }
}
