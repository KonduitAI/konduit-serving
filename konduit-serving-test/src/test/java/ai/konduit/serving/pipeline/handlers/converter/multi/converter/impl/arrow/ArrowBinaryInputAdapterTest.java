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

import ai.konduit.serving.input.conversion.ConverterArgs;
import ai.konduit.serving.train.TrainUtils;
import io.vertx.core.buffer.Buffer;
import org.apache.commons.io.FileUtils;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.partition.NumberOfRecordsPartitioner;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.datavec.arrow.recordreader.ArrowRecordWriter;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ArrowBinaryInputAdapterTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test(timeout = 60000)

    public void testArrowBinary() throws Exception {
        Schema irisInputSchema = TrainUtils.getIrisInputSchema();
        ArrowRecordWriter arrowRecordWriter = new ArrowRecordWriter(irisInputSchema);
        CSVRecordReader reader = new CSVRecordReader();
        reader.initialize(new FileSplit(new ClassPathResource("iris.txt").getFile()));
        List<List<Writable>>  writables = reader.next(150);

        File tmpFile = new File(temporary.getRoot(),"tmp.arrow");
        FileSplit fileSplit = new FileSplit(tmpFile);
        arrowRecordWriter.initialize(fileSplit,new NumberOfRecordsPartitioner());
        arrowRecordWriter.writeBatch(writables);
        byte[] arrowBytes = FileUtils.readFileToByteArray(tmpFile);

        Buffer buffer = Buffer.buffer(arrowBytes);
        ArrowBinaryInputAdapter arrowBinaryInputAdapter = new ArrowBinaryInputAdapter();
        ArrowWritableRecordBatch convert = arrowBinaryInputAdapter.convert(buffer, ConverterArgs.builder().schema(irisInputSchema).build(), null);
        assertEquals(writables.size(),convert.size());
    }

}
