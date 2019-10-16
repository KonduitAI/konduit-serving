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

package ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.arrow;


import ai.konduit.serving.input.adapter.InputAdapter;
import ai.konduit.serving.input.conversion.ConverterArgs;
import io.vertx.core.buffer.Buffer;
import org.datavec.api.split.InputStreamInputSplit;
import org.datavec.arrow.recordreader.ArrowRecordReader;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * A {@link InputAdapter}
 * for converting raw {@link Buffer}
 * to {@link ArrowWritableRecordBatch}
 *
 * @author Adam Gibson
 */
public class ArrowBinaryInputAdapter implements InputAdapter<Buffer,ArrowWritableRecordBatch> {

    @Override
    public ArrowWritableRecordBatch convert(Buffer input, ConverterArgs parameters, Map<String, Object> contextData) {
        ArrowRecordReader arrowRecordReader = new ArrowRecordReader();
        arrowRecordReader.initialize(new InputStreamInputSplit(new ByteArrayInputStream(input.getBytes())));
        arrowRecordReader.next();
        return arrowRecordReader.getCurrentBatch();
    }

}
