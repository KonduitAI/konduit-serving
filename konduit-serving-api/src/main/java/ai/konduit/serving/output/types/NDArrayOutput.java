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

package ai.konduit.serving.output.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.serde.jackson.shaded.NDArrayTextDeSerializer;
import org.nd4j.serde.jackson.shaded.NDArrayTextSerializer;
import org.nd4j.shade.jackson.databind.annotation.JsonDeserialize;
import org.nd4j.shade.jackson.databind.annotation.JsonSerialize;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NDArrayOutput implements BatchOutput {

    private String batchId;
    @JsonSerialize(using = NDArrayTextSerializer.class)
    @JsonDeserialize(using = NDArrayTextDeSerializer.class)
    private INDArray ndArray;

    @Override
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public String batchId() {
        return batchId;
    }
}
