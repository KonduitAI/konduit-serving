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

import ai.konduit.serving.model.SavedModelConfig;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.tensorflow.*;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.concurrency.AffinityManager;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.compression.CompressedDataBuffer;
import org.nd4j.linalg.compression.CompressionDescriptor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.bytedeco.tensorflow.global.tensorflow.*;

/**
 * Interop between nd4j {@link INDArray}
 * and {@link TF_Tensor}
 * <p>
 * Of note are data type conversion utilities,
 * creation of {@link TF_Tensor} objects back and forth.
 * <p>
 * Note that for data types it relies on references from the javacpp presets
 * directly generated from the tensorflow c++ sources.
 * <p>
 * For ease of use, the data type integers are presented as below:
 * DT_INVALID = 0
 * DT_FLOAT = 1
 * DT_DOUBLE = 2
 * DT_INT32 = 3
 * DT_UINT8 = 4
 * DT_INT16 = 5
 * DT_INT8 = 6
 * DT_STRING = 7
 * DT_COMPLEX64 = 8
 * DT_INT64 = 9
 * DT_BOOL = 10
 * DT_QINT8 = 11
 * DT_QUINT8 = 12
 * DT_QINT32 = 13
 * DT_BFLOAT16 = 14
 * DT_QINT16 = 15
 * DT_QUINT16 = 16
 * DT_UINT16 = 17
 * DT_COMPLEX128 = 18
 * DT_HALF = 19
 * DT_RESOURCE = 20
 * DT_VARIANT = 21
 * DT_UINT32 = 22
 * DT_UINT64 = 23
 *
 * @author Adam Gibson
 */
public class TensorflowConversion {

    //used for passing to tensorflow: this dummy de allocator
    //allows us to use nd4j buffers for memory management
    //rather than having them managed by tensorflow
    private static Deallocator_Pointer_long_Pointer calling;
    private static TensorflowConversion INSTANCE;

    private TensorflowConversion() {
        if (calling == null)
            calling = DummyDeAllocator.getInstance();

    }

    /**
     * Get a singleton instance
     *
     * @return the singleton
     */
    public static TensorflowConversion getInstance() {
        if (INSTANCE == null)
            INSTANCE = new TensorflowConversion();
        return INSTANCE;
    }

    /**
     * Convert an {@link INDArray}
     * to a {@link TF_Tensor}
     * with zero copy.
     * Uses a direct pointer to the underlying ndarray's
     * data
     *
     * @param ndArray the ndarray to use
     * @return the equivalent {@link TF_Tensor}
     */
    public TF_Tensor tensorFromNDArray(INDArray ndArray) {
        if (ndArray == null) {
            throw new IllegalArgumentException("NDArray must not be null!");
        }
        //we infer data type from the ndarray.databuffer()
        //for now we throw an exception
        if (ndArray.data() == null) {
            throw new IllegalArgumentException("Unable to infer data type from null databuffer");
        }

        if (ndArray.isView() || ndArray.ordering() != 'c') {
            ndArray = ndArray.dup('c');
        }


        long[] ndShape = ndArray.shape();
        long[] tfShape = new long[ndShape.length];
        System.arraycopy(ndShape, 0, tfShape, 0, ndShape.length);

        int type = -1;
        DataBuffer data = ndArray.data();
        data.dataType();
        DataType dataType;

        try {
            type = tfDataTypeFromNd4jDataType(data);
        } catch (Exception e) {
            try {
                Nd4j.getAffinityManager().ensureLocation(ndArray, AffinityManager.Location.HOST);
            } catch (Exception e2) {
                // ND4J won't let us access compressed data in GPU memory, so we'll leave TensorFlow do the conversion instead
                ndArray.getDouble(0); // forces decompression and data copy to host
                data = ndArray.data();
                dataType = data.dataType();
                switch (dataType) {
                    case DOUBLE:
                        type = DT_DOUBLE;
                        break;
                    case FLOAT:
                        type = DT_FLOAT;
                        break;
                    case INT:
                        type = DT_INT32;
                        break;
                    case LONG:
                        type = DT_INT64;
                        break;
                    case UTF8:
                        type = DT_STRING;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported data type: " + dataType);
                }
            }

            if (type < 0)
                throw new IllegalArgumentException("No type found!");

        }


        LongPointer longPointer = new LongPointer(tfShape);
        TF_Tensor tfTensor = createTensorWithDataBuffer(ndArray, type, tfShape, longPointer, data);

        return tfTensor;

    }

    /**
     * Convert a {@link INDArray}
     * to a {@link TF_Tensor}
     * using zero copy.
     * It will use the underlying
     * pointer with in nd4j.
     *
     * @param tensor the tensor to use
     * @return the created {@link INDArray}
     */
    public INDArray ndArrayFromTensor(TF_Tensor tensor) {
        int rank = TF_NumDims(tensor);

        int[] ndShape;
        if (rank == 0) {
            // scalar
            ndShape = new int[]{1};
        } else {
            ndShape = new int[rank];
            for (int i = 0; i < ndShape.length; i++) {
                ndShape[i] = (int) TF_Dim(tensor, i);
            }
        }

        int tfType = TF_TensorType(tensor);
        DataType nd4jType = typeFor(tfType);

        int length = ArrayUtil.prod(ndShape);
        INDArray array;
        if (nd4jType == DataType.UTF8) {
            String[] strings = new String[length];
            BytePointer data = new BytePointer(TF_TensorData(tensor)).capacity(TF_TensorByteSize(tensor));
            BytePointer str = new BytePointer((Pointer) null);
            SizeTPointer size = new SizeTPointer(1);
            TF_Status status = TF_NewStatus();
            for (int i = 0; i < length; i++) {
                long offset = data.position(8 * i).getLong();
                TF_StringDecode(data.position(8 * length + offset), data.capacity() - data.position(), str, size, status);
                if (TF_GetCode(status) != TF_OK) {
                    throw new IllegalStateException("ERROR: Unable to convert tensor " + TF_Message(status).getString());
                }
                strings[i] = str.position(0).capacity(size.get()).getString();
            }
            TF_DeleteStatus(status);
            array = Nd4j.create(strings);
        } else {
            Pointer pointer = TF_TensorData(tensor).capacity(length);
            Indexer indexer = indexerForType(nd4jType, pointer);
            DataBuffer d = Nd4j.createBuffer(indexer.pointer(), nd4jType, length, indexer);
            array = Nd4j.create(d, ndShape);
        }
        Nd4j.getAffinityManager().tagLocation(array, AffinityManager.Location.HOST);
        return array;
    }


    private Indexer indexerForType(DataType type, Pointer pointer) {
        switch (type) {
            case DOUBLE:
                return DoubleIndexer.create(new DoublePointer(pointer));
            case FLOAT:
                return FloatIndexer.create(new FloatPointer(pointer));
            case INT:
                return IntIndexer.create(new IntPointer(pointer));
            case LONG:
                return LongIndexer.create(new LongPointer(pointer));
            default:
                throw new IllegalArgumentException("Illegal type " + type);
        }
    }

    private DataType typeFor(int tensorflowType) {
        switch (tensorflowType) {
            case DT_DOUBLE:
                return DataType.DOUBLE;
            case DT_FLOAT:
                return DataType.FLOAT;
            case DT_INT32:
                return DataType.LONG;
            case DT_INT64:
                return DataType.LONG;
            case DT_STRING:
                return DataType.UTF8;
            default:
                throw new IllegalArgumentException("Illegal type " + tensorflowType);
        }
    }

    /**
     * Get an initialized {@link TF_Graph}
     * based on the passed in file
     * (the file must be a binary protobuf/pb file)
     * The graph will be modified to be associated
     * with the device associated with this current thread.
     * <p>
     * Depending on the active {@link Nd4j#getBackend()}
     * the device will either be the gpu pinned to the current thread
     * or the cpu
     *
     * @param filePath the path to the file to read
     * @param status   the status to check if an exception is thrown in c++
     * @return the initialized graph
     * @throws IOException if an error occurs loading the graph
     */
    public TF_Graph loadGraph(String filePath, TF_Status status) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return loadGraph(bytes, status);
    }


    /**
     * Get an initialized {@link TF_Graph}
     * based on the passed in byte array content
     * (the content must be a binary protobuf/pb file)
     * The graph will be modified to be associated
     * with the device associated with this current thread.
     * <p>
     * Depending on the active {@link Nd4j#getBackend()}
     * the device will either be the gpu pinned to the current thread
     * or the content
     *
     * @param content the path to the file to read
     * @param status  the status object to use to throw an exception in java if one occurs
     * @return the initialized graph
     */

    public TF_Graph loadGraph(byte[] content, TF_Status status) {
        byte[] toLoad = content;
        TF_Buffer graph_def = TF_NewBufferFromString(new BytePointer(toLoad), content.length);
        TF_Graph graphC = TF_NewGraph();
        TF_ImportGraphDefOptions opts = TF_NewImportGraphDefOptions();
        TF_GraphImportGraphDef(graphC, graph_def, opts, status);
        if (TF_GetCode(status) != TF_OK) {
            throw new IllegalStateException("ERROR: Unable to import graph " + TF_Message(status).getString());
        }


        TF_DeleteImportGraphDefOptions(opts);

        return graphC;
    }

    /**
     * Load a session based on the saved model
     *
     * @param savedModelConfig the configuration for the saved model
     * @param options          the session options to use
     * @param runOptions       the run configuration to use
     * @param graph            the tf graph to use
     * @param inputsMap        the input map
     * @param outputsMap       the output names
     * @param status           the status object to use for verifying the results
     * @return the created session
     */
    public TF_Session loadSavedModel(SavedModelConfig savedModelConfig, TF_SessionOptions options, TF_Buffer runOptions, TF_Graph graph, Map<String, String> inputsMap, Map<String, String> outputsMap, TF_Status status) {
        TF_Buffer metaGraph = TF_Buffer.newBuffer();
        TF_Session session = TF_LoadSessionFromSavedModel(options, runOptions, new BytePointer(savedModelConfig.getSavedModelPath()),
                new BytePointer(savedModelConfig.getModelTag()), 1, graph, metaGraph, status);
        if (TF_GetCode(status) != TF_OK) {
            throw new IllegalStateException("ERROR: Unable to import model " + TF_Message(status).getString());
        }

        MetaGraphDef metaGraphDef;
        try {
            metaGraphDef = MetaGraphDef.parseFrom(metaGraph.data().capacity(metaGraph.length()).asByteBuffer());
        } catch (org.nd4j.shade.protobuf.InvalidProtocolBufferException ex) {
            throw new IllegalStateException("ERROR: Unable to import model " + ex);
        }
        Map<String, SignatureDef> signatureDefMap = metaGraphDef.getSignatureDefMap();
        SignatureDef signatureDef = signatureDefMap.get(savedModelConfig.getSignatureKey());

        Map<String, TensorInfo> inputs = signatureDef.getInputsMap();
        for (Map.Entry<String, TensorInfo> e : inputs.entrySet()) {
            inputsMap.put(e.getKey(), e.getValue().getName());
        }

        Map<String, TensorInfo> outputs = signatureDef.getOutputsMap();
        for (Map.Entry<String, TensorInfo> e : outputs.entrySet()) {
            outputsMap.put(e.getKey(), e.getValue().getName());
        }

        return session;
    }

    private TF_Tensor createTensorWithDataBuffer(INDArray ndArray, int type, long[] tfShape, LongPointer longPointer, DataBuffer data) {
        TF_Tensor tfTensor;
        if (type == DT_STRING) {
            long size = 0;
            long length = ndArray.length();
            BytePointer[] strings = new BytePointer[(int) length];
            for (int i = 0; i < length; i++) {
                strings[i] = new BytePointer(ndArray.getString(i));
                size += TF_StringEncodedSize(strings[i].capacity());
            }

            tfTensor = TF_AllocateTensor(
                    type,
                    longPointer,
                    tfShape.length,
                    8 * length + size);

            long offset = 0;
            BytePointer tf_data = new BytePointer(TF_TensorData(tfTensor)).capacity(TF_TensorByteSize(tfTensor));
            TF_Status status = TF_NewStatus();
            for (int i = 0; i < length; i++) {
                tf_data.position(8 * i).putLong(offset);
                offset += TF_StringEncode(strings[i], strings[i].capacity() - 1, tf_data.position(8 * length + offset), tf_data.capacity() - tf_data.position(), status);
                if (TF_GetCode(status) != TF_OK) {
                    throw new IllegalStateException("ERROR: Unable to convert tensor " + TF_Message(status).getString());
                }
            }

            TF_DeleteStatus(status);

        } else {
            tfTensor = TF_NewTensor(
                    type,
                    longPointer,
                    tfShape.length,
                    data.pointer(),
                    data.length() * data.getElementSize(),
                    calling, null);
        }

        return tfTensor;
    }


    private int tfDataTypeFromNd4jDataType(DataBuffer data) {
        DataType dataType = data.dataType();

        int type;
        switch (dataType) {
            case DOUBLE:
                type = DT_DOUBLE;
                break;
            case FLOAT:
                type = DT_FLOAT;
                break;
            case INT:
                type = DT_INT32;
                break;
            case HALF:
                type = DT_HALF;
                break;
            case COMPRESSED:
                CompressedDataBuffer compressedData = (CompressedDataBuffer) data;
                CompressionDescriptor desc = compressedData.getCompressionDescriptor();
                String algo = desc.getCompressionAlgorithm();
                switch (algo) {
                    case "FLOAT16":
                        type = DT_HALF;
                        break;
                    case "INT8":
                        type = DT_INT8;
                        break;
                    case "UINT8":
                        type = DT_UINT8;
                        break;
                    case "INT16":
                        type = DT_INT16;
                        break;
                    case "UINT16":
                        type = DT_UINT16;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported compression algorithm: " + algo);
                }
                break;
            case LONG:
                type = DT_INT64;
                break;
            case UTF8:
                type = DT_STRING;
                break;
            default:
                throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }


        return type;

    }

}
