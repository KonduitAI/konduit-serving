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

package ai.konduit.serving.threadpool.tensorflow.conversion;

import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.graphmapper.tf.TFGraphMapper;

import java.io.File;
import java.io.IOException;

/**
 * Conversion from models saved using the Google's Protocol Buffer
 * (https://github.com/protocolbuffers/protobuf) to flatbuffer format
 * (https://github.com/google/flatbuffers)
 * <p>
 * This is especially useful for executing models using only the C++ libnd4j
 * library, as the protobuf loader is only available through the Java API
 * <p>
 * It simply loads a file as a SameDiff and saves it as a flat file.
 * <p>
 * There is a special case for BERT models where a pre-processing is necessary:
 * See nd4j/nd4j-backends/nd4j-tests/src/test/java/org/nd4j/imports/TFGraphs/BERTGraphTest.java
 * for details
 *
 * @author Yves Quemener
 */
public class ProtoBufToFlatBufConversion {

    /**
     * Converts a file containing a model from the Protocol Buffer format to the Flat
     * Buffer format.
     *
     * @param inFile  input file (.pb format)
     * @param outFile output file (.fb format)
     * @throws IOException                                         I/O Exception
     * @throws org.nd4j.linalg.exception.ND4JIllegalStateException if an error occurs during conversion
     */
    public static void convert(String inFile, String outFile)
            throws IOException, org.nd4j.linalg.exception.ND4JIllegalStateException {
        SameDiff tg = TFGraphMapper.importGraph(new File(inFile));
        tg.asFlatFile(new File(outFile));
    }


    /**
     * Main function.
     * The conversion tool can be called from the command line with the following syntax:
     * mvn exec:java -Dexec.mainClass="org.nd4j.tensorflow.conversion.ProtoBufToFlatBufConversion" -Dexec.args="input_file.pb output_file.fb"
     *
     * @param args the first argument is the input filename (protocol buffer format),
     *             the second one is the output filename (flat buffer format)
     * @throws IOException I/O exception
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage:\n"
                    + "mvn exec:java -Dexec.mainClass=\"org.nd4j.tensorflow.conversion.ProtoBufToFlatBufConversion\" -Dexec.args=\"<input_file.pb> <output_file.fb>\"\n");
        } else {
            convert(args[0], args[1]);
        }
    }

}
