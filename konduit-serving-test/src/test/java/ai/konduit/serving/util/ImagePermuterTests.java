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

package ai.konduit.serving.util;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class ImagePermuterTests {

    @Test(timeout = 60000)

    public void testPermuteOrder() {
        String startingOrder = "nchw";
        String destinationOrder = "nhwc";
        int[] determinePermuteOrder = ImagePermuter.determinePermuteOrder(startingOrder, destinationOrder);
        assertEquals(destinationOrder,ImagePermuter.applyPermuteOrderToString(startingOrder, determinePermuteOrder));
    }

}
