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

package ai.konduit.serving.pipeline.handlers.array.transform;

import ai.konduit.serving.config.SchemaType;
import org.datavec.api.records.Record;
import org.datavec.api.writable.NDArrayWritable;
import org.nd4j.autodiff.execution.NativeGraphExecutioner;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.util.*;

/**
 * Base class for running samediff op graphs
 *
 * @author Adam Gibson
 */
public abstract class BaseSameDiffTransform implements ArrayTransform {

    protected SameDiff sameDiff;
    protected  List<String> inputs,outputs;
    protected NativeGraphExecutioner nativeGraphExecutioner;

    /**
     * Load samediff from the given content.
     * See {@link #getSameDiffFromBytes(byte[])}
     * for information on how to setup the bytebuffer
     * @param sameDiffFlatBuffers the content of the flatbuffers
     * @param inputs the input names
     * @param  outputs the output names
     */
    public BaseSameDiffTransform(byte[] sameDiffFlatBuffers,List<String> inputs,List<String> outputs) {
        sameDiff = getSameDiffFromBytes(sameDiffFlatBuffers);
        nativeGraphExecutioner = new NativeGraphExecutioner();
        nativeGraphExecutioner.registerGraph(sameDiff);
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Load samediff from the given file.
     * @param file the file to load
     * @param inputs the input names
     */
    public BaseSameDiffTransform(File file,List<String> inputs) {
        sameDiff = getSameDiffFromFile(file);
        nativeGraphExecutioner = new NativeGraphExecutioner();
        nativeGraphExecutioner.registerGraph(sameDiff);
        this.inputs = inputs;
    }


    /**
     * Read samediff from a file
     * @param file the file to read from
     * @return the loaded samediff instance
     */
    public abstract SameDiff getSameDiffFromFile(File file);


    /**
     * Read samediff from an in memory byte array.
     * Note that if the byte buffer is samediff
     * and you are trying to samediff.asFlatBuffers()
     * use:
     *  ByteBuffer byteBuffer = permuteGraph.asFlatBuffers();
     byte[] content = new byte[byteBuffer.capacity() - byteBuffer.position()];
     byteBuffer.get(content);

     *  The reason for this is due to how flatbuffers positions buffers.
     * @param content the content to use
     * @return the loaded samediff instance
     */
    public abstract SameDiff getSameDiffFromBytes(byte[] content);

    @Override
    public Record[] transform(Record[] input) {
        Map<String,INDArray> mapIndexes = new HashMap<>();
        SDVariable[] inputs = new SDVariable[input.length];
        for(int i = 0; i < input.length; i++) {
            inputs[i] = sameDiff.getVariable(this.inputs.get(i));
            NDArrayWritable ndArrayWritable = (NDArrayWritable) input[i].getRecord().get(0);
            mapIndexes.put(this.inputs.get(i),ndArrayWritable.get());
        }

        Map<String, INDArray> stringINDArrayMap = sameDiff.outputAll(mapIndexes);
        Record[] ret = new Record[stringINDArrayMap.size()];

        int count = 0;
        for(String key : stringINDArrayMap.keySet()) {
            ret[count++] = new org.datavec.api.records.impl.Record(Collections.singletonList(
                    new NDArrayWritable(stringINDArrayMap.get(key))),
                    null);
        }

        return ret;
    }


    @Override
    public void setInputNames(String... inputNames) {
        this.inputs = new ArrayList<>(Arrays.asList(inputNames));
    }

    @Override
    public void setOutputNames(String... outputNames) {
        this.outputs = new ArrayList<>(Arrays.asList(outputNames));
    }

    @Override
    public String[] inputNames() {
        return inputs.toArray(new String[inputs.size()]);
    }

    @Override
    public String[] outputNames() {
        return outputs.toArray(new String[outputs.size()]);
    }

    @Override
    public Map<String, SchemaType[]> inputTypes() {
        Map<String,SchemaType[]> ret = new LinkedHashMap<>();
        for(int i = 0; i < inputNames().length; i++) {
            ret.put(inputNames()[i],new SchemaType[]{SchemaType.NDArray});
        }
        return ret;
    }

    @Override
    public Map<String, SchemaType[]> outputTypes() {
        Map<String,SchemaType[]> ret = new LinkedHashMap<>();
        for(int i = 0; i < outputNames().length; i++) {
            ret.put(outputNames()[i],new SchemaType[]{SchemaType.NDArray});
        }

        return ret;
    }

}
