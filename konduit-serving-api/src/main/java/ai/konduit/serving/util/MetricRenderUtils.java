/*
 *
 *  * ******************************************************************************
 *  *
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
package ai.konduit.serving.util;

import ai.konduit.serving.config.metrics.ColumnDistribution;

/**
 * Util class for anything related to {@link ai.konduit.serving.config.metrics.MetricsRenderer}
 *
 * @author Adam Gibson
 */
public class MetricRenderUtils {




    /**
     * De normalize the given input based on the {@link ColumnDistribution}
     * @param input the input to de normalize
     * @param columnDistribution the column distribution to de normalized based on
     * @return the de normalized value
     */
    public static double normalizeValue(double input, ColumnDistribution columnDistribution) {
        switch(columnDistribution.getNormalizerType()) {
            case MIN_MAX:
                return  (input - columnDistribution.getMin()) /  columnDistribution.getMax();
            case STANDARDIZE:
                return (input - columnDistribution.getMean()) / columnDistribution.getStandardDeviation();
            default:
                throw new IllegalArgumentException("Illegal normalization type for reverting." + columnDistribution.getNormalizerType());
        }
    }


    /**
     * De normalize the given input based on the {@link ColumnDistribution}
     * @param input the input to de normalize
     * @param columnDistribution the column distribution to de normalized based on
     * @return the de normalized value
     */
    public static double deNormalizeValue(double input, ColumnDistribution columnDistribution) {
        switch(columnDistribution.getNormalizerType()) {
            case MIN_MAX:
                return  (input * columnDistribution.getMax()) + columnDistribution.getMin();
            case STANDARDIZE:
                return (input * columnDistribution.getStandardDeviation()) + columnDistribution.getMean();
            default:
                throw new IllegalArgumentException("Illegal normalization type for reverting." + columnDistribution.getNormalizerType());
        }
    }

}
