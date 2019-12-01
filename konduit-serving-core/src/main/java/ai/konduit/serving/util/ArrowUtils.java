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

import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.dictionary.Dictionary;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileReader;
import org.apache.arrow.vector.ipc.ArrowFileWriter;
import org.apache.arrow.vector.ipc.SeekableReadChannel;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.DictionaryEncoding;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.util.ByteArrayReadableSeekableByteChannel;
import org.datavec.api.transform.ColumnType;
import org.datavec.api.transform.metadata.*;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.schema.conversion.TypeConversion;
import org.datavec.api.util.ndarray.RecordConverter;
import org.datavec.api.writable.*;
import org.datavec.arrow.ArrowConverter;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.datavec.arrow.recordreader.ArrowWritableRecordTimeSeriesBatch;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.exception.ND4JIllegalArgumentException;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;
import org.nd4j.serde.binary.BinarySerde;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.util.*;


/**
 * Various utilities related to arrow.
 * Heavily overlaps with {@link ArrowConverter}
 *
 * @author Adam Gibson
 */
@Slf4j
public class ArrowUtils {


    public static INDArray toArray(ArrowWritableRecordTimeSeriesBatch arrowWritableRecordBatch) {
        return RecordConverter.toTensor(arrowWritableRecordBatch);
    }

    public static INDArray toArray(ArrowWritableRecordBatch arrowWritableRecordBatch) {
        List<FieldVector> columnVectors = arrowWritableRecordBatch.getList();
        Schema schema = arrowWritableRecordBatch.getSchema();
        int rows = 0;

        while (rows < schema.numColumns()) {
            switch (schema.getType(rows)) {
                case Integer:
                case Float:
                case Double:
                case Long:
                case NDArray:
                    ++rows;
                    break;
                default:
                    throw new ND4JIllegalArgumentException("Illegal data type found for column " + schema.getName(rows) + " of type " + schema.getType(rows));
            }
        }

        rows = arrowWritableRecordBatch.getList().get(0).getValueCount();
        int i;
        if (schema.numColumns() == 1 && schema.getMetaData(0).getColumnType() == ColumnType.NDArray) {
            INDArray[] toConcat = new INDArray[rows];
            VarBinaryVector valueVectors = (VarBinaryVector) arrowWritableRecordBatch.getList().get(0);

            for (i = 0; i < rows; ++i) {
                byte[] bytes = valueVectors.get(i);
                ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length);
                direct.put(bytes);
                INDArray fromTensor = BinarySerde.toArray(direct);
                toConcat[i] = fromTensor;
            }

            return Nd4j.concat(0, toConcat);
        } else {
            int cols = schema.numColumns();
            INDArray arr = Nd4j.create(rows, cols);

            for (i = 0; i < cols; ++i) {
                INDArray put = convertArrowVector((FieldVector) columnVectors.get(i), schema.getType(i));
                switch (arr.data().dataType()) {
                    case FLOAT:
                        arr.putColumn(i, Nd4j.create(put.data().asFloat()).reshape((long) rows, 1L));
                        break;
                    case DOUBLE:
                        arr.putColumn(i, Nd4j.create(put.data().asDouble()).reshape((long) rows, 1L));
                }
            }

            return arr;
        }
    }

    public static INDArray convertArrowVector(FieldVector fieldVector, ColumnType type) {
        DataBuffer buffer = null;
        int cols = fieldVector.getValueCount();
        ByteBuffer direct = ByteBuffer.allocateDirect(fieldVector.getDataBuffer().capacity());
        direct.order(ByteOrder.nativeOrder());
        fieldVector.getDataBuffer().getBytes(0, direct);
        direct.rewind();
        switch (type) {
            case Integer:
                buffer = Nd4j.createBuffer(direct, DataType.INT, cols, 0L);
                break;
            case Float:
                buffer = Nd4j.createBuffer(direct, DataType.FLOAT, cols);
                break;
            case Double:
                buffer = Nd4j.createBuffer(direct, DataType.DOUBLE, cols);
                break;
            case Long:
                buffer = Nd4j.createBuffer(direct, DataType.LONG, cols);
        }

        return Nd4j.create(buffer, new int[]{cols, 1});
    }


    public static List<FieldVector> convertToArrowVector(INDArray from, List<String> name, ColumnType type, BufferAllocator bufferAllocator) {
        List<FieldVector> ret = new ArrayList();
        long cols;
        if (from.isVector()) {
            cols = from.length();
            switch (type) {
                case Integer:
                    int[] fromDataInt = from.isView() ? from.dup().data().asInt() : from.data().asInt();
                    ret.add(vectorFor(bufferAllocator, (String) name.get(0), fromDataInt));
                    break;
                case Float:
                    float[] fromDataFloat = from.isView() ? from.dup().data().asFloat() : from.data().asFloat();
                    ret.add(vectorFor(bufferAllocator, (String) name.get(0), fromDataFloat));
                    break;
                case Double:
                    double[] fromData = from.isView() ? from.dup().data().asDouble() : from.data().asDouble();
                    ret.add(vectorFor(bufferAllocator, (String) name.get(0), fromData));
                    break;
                default:
                    throw new IllegalArgumentException("Illegal type " + type);
            }
        } else {
            cols = from.size(1);

            for (int i = 0; (long) i < cols; ++i) {
                INDArray column = from.getColumn((long) i);
                switch (type) {
                    case Integer:
                        int[] fromDataInt = column.isView() ? column.dup().data().asInt() : from.data().asInt();
                        ret.add(vectorFor(bufferAllocator, (String) name.get(i), fromDataInt));
                        break;
                    case Float:
                        float[] fromDataFloat = column.isView() ? column.dup().data().asFloat() : from.data().asFloat();
                        ret.add(vectorFor(bufferAllocator, (String) name.get(i), fromDataFloat));
                        break;
                    case Double:
                        double[] fromData = column.isView() ? column.dup().data().asDouble() : from.data().asDouble();
                        ret.add(vectorFor(bufferAllocator, (String) name.get(i), fromData));
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal type " + type);
                }
            }
        }

        return ret;
    }

    public static void writeRecordBatchTo(List<List<Writable>> recordBatch, Schema inputSchema, OutputStream outputStream) {
        BufferAllocator bufferAllocator = new RootAllocator(9223372036854775807L);
        writeRecordBatchTo(bufferAllocator, recordBatch, inputSchema, outputStream);
    }

    public static void writeRecordBatchTo(BufferAllocator bufferAllocator, List<List<Writable>> recordBatch, Schema inputSchema, OutputStream outputStream) {
        if (!(recordBatch instanceof ArrowWritableRecordBatch)) {
            convertWritables(bufferAllocator, recordBatch, inputSchema, outputStream);
        } else {
            convertWritables(bufferAllocator, recordBatch, inputSchema, outputStream);
        }

    }

    private static void convertWritables(BufferAllocator bufferAllocator, List<List<Writable>> recordBatch, Schema inputSchema, OutputStream outputStream) {
        org.apache.arrow.vector.types.pojo.Schema convertedSchema;
        List columns;
        VectorSchemaRoot root;
        ArrowFileWriter writer;
        convertedSchema = toArrowSchema(inputSchema);
        columns = toArrowColumns(bufferAllocator, inputSchema, recordBatch);

        try {
            root = new VectorSchemaRoot(convertedSchema, columns, recordBatch.size());
            writer = new ArrowFileWriter(root, providerForVectors(columns, convertedSchema.getFields()), Channels.newChannel(outputStream));
            writer.start();
            writer.writeBatch();
            writer.end();
        } catch (IOException var9) {
            throw new IllegalStateException(var9);
        }
    }

    public static List<List<List<Writable>>> toArrowWritablesTimeSeries(List<FieldVector> fieldVectors, Schema schema, int timeSeriesLength) {
        ArrowWritableRecordTimeSeriesBatch arrowWritableRecordBatch = new ArrowWritableRecordTimeSeriesBatch(fieldVectors, schema, timeSeriesLength);
        return arrowWritableRecordBatch;
    }

    public static ArrowWritableRecordBatch toArrowWritables(List<FieldVector> fieldVectors, Schema schema) {
        ArrowWritableRecordBatch arrowWritableRecordBatch = new ArrowWritableRecordBatch(fieldVectors, schema);
        return arrowWritableRecordBatch;
    }

    public static List<Writable> toArrowWritablesSingle(List<FieldVector> fieldVectors, Schema schema) {
        return toArrowWritables(fieldVectors, schema).get(0);
    }

    public static Pair<Schema, ArrowWritableRecordBatch> readFromFile(FileInputStream input) throws IOException {
        BufferAllocator allocator = new RootAllocator(9223372036854775807L);
        Schema retSchema = null;
        ArrowWritableRecordBatch ret = null;
        SeekableReadChannel channel = new SeekableReadChannel(input.getChannel());
        ArrowFileReader reader = new ArrowFileReader(channel, allocator);
        reader.loadNextBatch();
        retSchema = toDatavecSchema(reader.getVectorSchemaRoot().getSchema());
        VectorUnloader unloader = new VectorUnloader(reader.getVectorSchemaRoot());
        VectorLoader vectorLoader = new VectorLoader(reader.getVectorSchemaRoot());
        ArrowRecordBatch recordBatch = unloader.getRecordBatch();
        vectorLoader.load(recordBatch);
        ret = asDataVecBatch(recordBatch, retSchema, reader.getVectorSchemaRoot());
        ret.setUnloader(unloader);
        return Pair.of(retSchema, ret);
    }

    public static Pair<Schema, ArrowWritableRecordBatch> readFromFile(File input) throws IOException {
        return readFromFile(new FileInputStream(input));
    }

    public static Pair<Schema, ArrowWritableRecordBatch> readFromBytes(byte[] input) throws IOException {
        BufferAllocator allocator = new RootAllocator(9223372036854775807L);
        Schema retSchema = null;
        ArrowWritableRecordBatch ret = null;
        SeekableReadChannel channel = new SeekableReadChannel(new ByteArrayReadableSeekableByteChannel(input));
        ArrowFileReader reader = new ArrowFileReader(channel, allocator);
        reader.loadNextBatch();
        retSchema = toDatavecSchema(reader.getVectorSchemaRoot().getSchema());
        VectorUnloader unloader = new VectorUnloader(reader.getVectorSchemaRoot());
        VectorLoader vectorLoader = new VectorLoader(reader.getVectorSchemaRoot());
        ArrowRecordBatch recordBatch = unloader.getRecordBatch();
        vectorLoader.load(recordBatch);
        ret = asDataVecBatch(recordBatch, retSchema, reader.getVectorSchemaRoot());
        ret.setUnloader(unloader);
        return Pair.of(retSchema, ret);
    }

    public static org.apache.arrow.vector.types.pojo.Schema toArrowSchema(Schema schema) {
        List<Field> fields = new ArrayList(schema.numColumns());

        for (int i = 0; i < schema.numColumns(); ++i) {
            fields.add(getFieldForColumn(schema.getName(i), schema.getType(i)));
        }

        return new org.apache.arrow.vector.types.pojo.Schema(fields);
    }

    public static Schema toDatavecSchema(org.apache.arrow.vector.types.pojo.Schema schema) {
        Schema.Builder schemaBuilder = new Schema.Builder();

        for (int i = 0; i < schema.getFields().size(); ++i) {
            schemaBuilder.addColumn(metaDataFromField((Field) schema.getFields().get(i)));
        }

        return schemaBuilder.build();
    }

    public static Field field(String name, ArrowType arrowType) {
        return new Field(name, FieldType.nullable(arrowType), new ArrayList());
    }

    public static Field getFieldForColumn(String name, ColumnType columnType) {
        switch (columnType) {
            case Integer:
                return field(name, new ArrowType.Int(32, false));
            case Float:
                return field(name, new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE));
            case Double:
                return field(name, new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE));
            case Long:
                return field(name, new ArrowType.Int(64, false));
            case NDArray:
                return field(name, new ArrowType.Binary());
            case Boolean:
                return field(name, new ArrowType.Bool());
            case Categorical:
                return field(name, new ArrowType.Utf8());
            case Time:
                return field(name, new ArrowType.Date(DateUnit.MILLISECOND));
            case Bytes:
                return field(name, new ArrowType.Binary());
            case String:
                return field(name, new ArrowType.Utf8());
            default:
                throw new IllegalArgumentException("Column type invalid " + columnType);
        }
    }

    public static Field doubleField(String name) {
        return getFieldForColumn(name, ColumnType.Double);
    }

    public static Field floatField(String name) {
        return getFieldForColumn(name, ColumnType.Float);
    }

    public static Field intField(String name) {
        return getFieldForColumn(name, ColumnType.Integer);
    }

    public static Field longField(String name) {
        return getFieldForColumn(name, ColumnType.Long);
    }

    public static Field stringField(String name) {
        return getFieldForColumn(name, ColumnType.String);
    }

    public static Field booleanField(String name) {
        return getFieldForColumn(name, ColumnType.Boolean);
    }

    public static DictionaryProvider providerForVectors(List<FieldVector> vectors, List<Field> fields) {
        Dictionary[] dictionaries = new Dictionary[vectors.size()];

        for (int i = 0; i < vectors.size(); ++i) {
            DictionaryEncoding dictionary = ((Field) fields.get(i)).getDictionary();
            if (dictionary == null) {
                dictionary = new DictionaryEncoding((long) i, true, (ArrowType.Int) null);
            }

            dictionaries[i] = new Dictionary((FieldVector) vectors.get(i), dictionary);
        }

        return new DictionaryProvider.MapDictionaryProvider(dictionaries);
    }


    public static List<FieldVector> toArrowColumns(BufferAllocator bufferAllocator, Schema schema, List<List<Writable>> dataVecRecord) {
        int numRows = dataVecRecord.size();
        List<FieldVector> ret = createFieldVectors(bufferAllocator, schema, numRows);

        for (int j = 0; j < schema.numColumns(); ++j) {
            FieldVector fieldVector = (FieldVector) ret.get(j);
            int row = 0;

            for (Iterator var8 = dataVecRecord.iterator(); var8.hasNext(); ++row) {
                List<Writable> record = (List) var8.next();
                Writable writable = (Writable) record.get(j);
                setValue(schema.getType(j), fieldVector, writable, row);
            }
        }

        return ret;
    }

    public static List<FieldVector> toArrowColumnsTimeSeries(BufferAllocator bufferAllocator, Schema schema, List<List<List<Writable>>> dataVecRecord) {
        return toArrowColumnsTimeSeriesHelper(bufferAllocator, schema, dataVecRecord);
    }

    public static <T> List<FieldVector> toArrowColumnsTimeSeriesHelper(BufferAllocator bufferAllocator, Schema schema, List<List<List<T>>> dataVecRecord) {
        int numRows = 0;

        List timeStep;
        for (Iterator var4 = dataVecRecord.iterator(); var4.hasNext(); numRows += ((List) timeStep.get(0)).size() * timeStep.size()) {
            timeStep = (List) var4.next();
        }

        numRows /= schema.numColumns();
        List<FieldVector> ret = createFieldVectors(bufferAllocator, schema, numRows);
        Map<Integer, Integer> currIndex = new HashMap(ret.size());

        int i;
        for (i = 0; i < ret.size(); ++i) {
            currIndex.put(i, 0);
        }

        for (i = 0; i < dataVecRecord.size(); ++i) {
            List<List<T>> record = (List) dataVecRecord.get(i);

            for (int j = 0; j < record.size(); ++j) {
                List<T> curr = (List) record.get(j);

                for (int k = 0; k < curr.size(); ++k) {
                    Integer idx = (Integer) currIndex.get(k);
                    FieldVector fieldVector = (FieldVector) ret.get(k);
                    T writable = curr.get(k);
                    setValue(schema.getType(k), fieldVector, writable, idx);
                    currIndex.put(k, idx + 1);
                }
            }
        }

        return ret;
    }

    public static List<FieldVector> toArrowColumnsStringSingle(BufferAllocator bufferAllocator, Schema schema, List<String> dataVecRecord) {
        return toArrowColumnsString(bufferAllocator, schema, Arrays.asList(dataVecRecord));
    }

    public static List<FieldVector> toArrowColumnsStringTimeSeries(BufferAllocator bufferAllocator, Schema schema, List<List<List<String>>> dataVecRecord) {
        return toArrowColumnsTimeSeriesHelper(bufferAllocator, schema, dataVecRecord);
    }

    public static List<FieldVector> toArrowColumnsString(BufferAllocator bufferAllocator, Schema schema, List<List<String>> dataVecRecord) {
        int numRows = dataVecRecord.size();
        List<FieldVector> ret = createFieldVectors(bufferAllocator, schema, numRows);

        for (int j = 0; j < schema.numColumns(); ++j) {
            FieldVector fieldVector = (FieldVector) ret.get(j);

            for (int row = 0; row < numRows; ++row) {
                String writable = (String) ((List) dataVecRecord.get(row)).get(j);
                setValue(schema.getType(j), fieldVector, writable, row);
            }
        }

        return ret;
    }

    public static List<FieldVector> createFieldVectors(BufferAllocator bufferAllocator, Schema schema, int numRows) {
        List<FieldVector> ret = new ArrayList(schema.numColumns());

        for (int i = 0; i < schema.numColumns(); ++i) {
            switch (schema.getType(i)) {
                case Integer:
                    ret.add(intVectorOf(bufferAllocator, schema.getName(i), numRows));
                    break;
                case Float:
                    ret.add(floatVectorOf(bufferAllocator, schema.getName(i), numRows));
                    break;
                case Double:
                    ret.add(doubleVectorOf(bufferAllocator, schema.getName(i), numRows));
                    break;
                case Long:
                    ret.add(longVectorOf(bufferAllocator, schema.getName(i), numRows));
                    break;
                case NDArray:
                    ret.add(ndarrayVectorOf(bufferAllocator, schema.getName(i), numRows));
                    break;
                case Boolean:
                    ret.add(booleanVectorOf(bufferAllocator, schema.getName(i), numRows));
                    break;
                case Categorical:
                    ret.add(stringVectorOf(bufferAllocator, schema.getName(i), numRows));
                    break;
                case Time:
                    ret.add(timeVectorOf(bufferAllocator, schema.getName(i), numRows));
                    break;
                case Bytes:
                default:
                    throw new IllegalArgumentException("Illegal type found for creation of field vectors" + schema.getType(i));
                case String:
                    ret.add(stringVectorOf(bufferAllocator, schema.getName(i), numRows));
            }
        }

        return ret;
    }

    public static void setValue(ColumnType columnType, FieldVector fieldVector, Object value, int row) {
        if (!(value instanceof NullWritable)) {
            try {
                switch (columnType) {
                    case Integer:
                        int set;
                        if (fieldVector instanceof IntVector) {
                            IntVector intVector = (IntVector) fieldVector;
                            set = TypeConversion.getInstance().convertInt(value);
                            intVector.set(row, set);
                        } else {
                            if (!(fieldVector instanceof UInt4Vector)) {
                                throw new UnsupportedOperationException("Illegal type " + fieldVector.getClass() + " for int type");
                            }

                            UInt4Vector uInt4Vector = (UInt4Vector) fieldVector;
                            set = TypeConversion.getInstance().convertInt(value);
                            uInt4Vector.set(row, set);
                        }
                        break;
                    case Float:
                        Float4Vector float4Vector = (Float4Vector) fieldVector;
                        float set2 = TypeConversion.getInstance().convertFloat(value);
                        float4Vector.set(row, set2);
                        break;
                    case Double:
                        double set3 = TypeConversion.getInstance().convertDouble(value);
                        Float8Vector float8Vector = (Float8Vector) fieldVector;
                        float8Vector.set(row, set3);
                        break;
                    case Long:
                        if (fieldVector instanceof BigIntVector) {
                            BigIntVector largeIntVector = (BigIntVector) fieldVector;
                            largeIntVector.set(row, TypeConversion.getInstance().convertLong(value));
                        } else {
                            if (!(fieldVector instanceof UInt8Vector)) {
                                throw new UnsupportedOperationException("Illegal type " + fieldVector.getClass() + " for long type");
                            }

                            UInt8Vector uInt8Vector = (UInt8Vector) fieldVector;
                            uInt8Vector.set(row, TypeConversion.getInstance().convertLong(value));
                        }
                        break;
                    case NDArray:
                        NDArrayWritable arr = (NDArrayWritable) value;
                        VarBinaryVector nd4jArrayVector = (VarBinaryVector) fieldVector;
                        ByteBuffer byteBuffer = BinarySerde.toByteBuffer(arr.get());
                        nd4jArrayVector.setSafe(row, byteBuffer, 0, byteBuffer.capacity());
                    case Boolean:
                    case Bytes:
                    default:
                        break;
                    case Categorical:
                    case String:
                        String stringSet = TypeConversion.getInstance().convertString(value);
                        VarCharVector textVector = (VarCharVector) fieldVector;
                        textVector.setSafe(row, stringSet.getBytes());
                        break;
                    case Time:
                        long timeSet = TypeConversion.getInstance().convertLong(value);
                        setLongInTime(fieldVector, row, timeSet);
                }
            } catch (Exception var16) {
                log.warn("Unable to set value at row " + row);
            }

        }
    }

    public static void setLongInTime(FieldVector fieldVector, int index, long value) {
        TimeStampMilliVector timeStampMilliVector;
        if (fieldVector instanceof TimeStampMilliVector) {
            timeStampMilliVector = (TimeStampMilliVector) fieldVector;
            timeStampMilliVector.set(index, value);
        } else if (fieldVector instanceof TimeMilliVector) {
            TimeMilliVector timeMilliVector = (TimeMilliVector) fieldVector;
            timeMilliVector.set(index, (int) value);
        } else if (fieldVector instanceof TimeStampMicroVector) {
            TimeStampMicroVector timeStampMicroVector = (TimeStampMicroVector) fieldVector;
            timeStampMicroVector.set(index, value);
        } else if (fieldVector instanceof TimeSecVector) {
            TimeSecVector timeSecVector = (TimeSecVector) fieldVector;
            timeSecVector.set(index, (int) value);
        } else if (fieldVector instanceof TimeStampMilliVector) {
            timeStampMilliVector = (TimeStampMilliVector) fieldVector;
            timeStampMilliVector.set(index, value);
        } else if (fieldVector instanceof TimeStampMilliTZVector) {
            TimeStampMilliTZVector timeStampMilliTZVector = (TimeStampMilliTZVector) fieldVector;
            timeStampMilliTZVector.set(index, value);
        } else if (fieldVector instanceof TimeStampNanoTZVector) {
            TimeStampNanoTZVector timeStampNanoTZVector = (TimeStampNanoTZVector) fieldVector;
            timeStampNanoTZVector.set(index, value);
        } else {
            if (!(fieldVector instanceof TimeStampMicroTZVector)) {
                throw new UnsupportedOperationException();
            }

            TimeStampMicroTZVector timeStampMicroTZVector = (TimeStampMicroTZVector) fieldVector;
            timeStampMicroTZVector.set(index, value);
        }

    }

    public static TimeStampMilliVector vectorFor(BufferAllocator allocator, String name, java.util.Date[] data) {
        TimeStampMilliVector float4Vector = new TimeStampMilliVector(name, allocator);
        float4Vector.allocateNew(data.length);

        for (int i = 0; i < data.length; ++i) {
            float4Vector.setSafe(i, data[i].getTime());
        }

        float4Vector.setValueCount(data.length);
        return float4Vector;
    }

    public static TimeStampMilliVector timeVectorOf(BufferAllocator allocator, String name, int length) {
        TimeStampMilliVector float4Vector = new TimeStampMilliVector(name, allocator);
        float4Vector.allocateNew(length);
        float4Vector.setValueCount(length);
        return float4Vector;
    }

    public static VarBinaryVector vectorFor(BufferAllocator bufferAllocator, String name, INDArray[] data) {
        VarBinaryVector ret = new VarBinaryVector(name, bufferAllocator);
        ret.allocateNew();

        for (int i = 0; i < data.length; ++i) {
            ByteBuffer byteBuffer = BinarySerde.toByteBuffer(data[i]);
            ret.set(i, byteBuffer, 0, byteBuffer.capacity());
        }

        return ret;
    }

    public static VarCharVector vectorFor(BufferAllocator allocator, String name, String[] data) {
        VarCharVector float4Vector = new VarCharVector(name, allocator);
        float4Vector.allocateNew();

        for (int i = 0; i < data.length; ++i) {
            float4Vector.setSafe(i, data[i].getBytes());
        }

        float4Vector.setValueCount(data.length);
        return float4Vector;
    }

    public static VarBinaryVector ndarrayVectorOf(BufferAllocator allocator, String name, int length) {
        VarBinaryVector ret = new VarBinaryVector(name, allocator);
        ret.allocateNewSafe();
        ret.setValueCount(length);
        return ret;
    }

    public static VarCharVector stringVectorOf(BufferAllocator allocator, String name, int length) {
        VarCharVector float4Vector = new VarCharVector(name, allocator);
        float4Vector.allocateNew();
        float4Vector.setValueCount(length);
        return float4Vector;
    }

    public static Float4Vector vectorFor(BufferAllocator allocator, String name, float[] data) {
        Float4Vector float4Vector = new Float4Vector(name, allocator);
        float4Vector.allocateNew(data.length);

        for (int i = 0; i < data.length; ++i) {
            float4Vector.setSafe(i, data[i]);
        }

        float4Vector.setValueCount(data.length);
        return float4Vector;
    }

    public static Float4Vector floatVectorOf(BufferAllocator allocator, String name, int length) {
        Float4Vector float4Vector = new Float4Vector(name, allocator);
        float4Vector.allocateNew(length);
        float4Vector.setValueCount(length);
        return float4Vector;
    }

    public static Float8Vector vectorFor(BufferAllocator allocator, String name, double[] data) {
        Float8Vector float8Vector = new Float8Vector(name, allocator);
        float8Vector.allocateNew(data.length);

        for (int i = 0; i < data.length; ++i) {
            float8Vector.setSafe(i, data[i]);
        }

        float8Vector.setValueCount(data.length);
        return float8Vector;
    }

    public static Float8Vector doubleVectorOf(BufferAllocator allocator, String name, int length) {
        Float8Vector float8Vector = new Float8Vector(name, allocator);
        float8Vector.allocateNew();
        float8Vector.setValueCount(length);
        return float8Vector;
    }

    public static BitVector vectorFor(BufferAllocator allocator, String name, boolean[] data) {
        BitVector float8Vector = new BitVector(name, allocator);
        float8Vector.allocateNew(data.length);

        for (int i = 0; i < data.length; ++i) {
            float8Vector.setSafe(i, data[i] ? 1 : 0);
        }

        float8Vector.setValueCount(data.length);
        return float8Vector;
    }

    public static BitVector booleanVectorOf(BufferAllocator allocator, String name, int length) {
        BitVector float8Vector = new BitVector(name, allocator);
        float8Vector.allocateNew(length);
        float8Vector.setValueCount(length);
        return float8Vector;
    }

    public static IntVector vectorFor(BufferAllocator allocator, String name, int[] data) {
        IntVector float8Vector = new IntVector(name, FieldType.nullable(new ArrowType.Int(32, true)), allocator);
        float8Vector.allocateNew(data.length);

        for (int i = 0; i < data.length; ++i) {
            float8Vector.setSafe(i, data[i]);
        }

        float8Vector.setValueCount(data.length);
        return float8Vector;
    }

    public static IntVector intVectorOf(BufferAllocator allocator, String name, int length) {
        IntVector float8Vector = new IntVector(name, FieldType.nullable(new ArrowType.Int(32, true)), allocator);
        float8Vector.allocateNew(length);
        float8Vector.setValueCount(length);
        return float8Vector;
    }

    public static BigIntVector vectorFor(BufferAllocator allocator, String name, long[] data) {
        BigIntVector float8Vector = new BigIntVector(name, FieldType.nullable(new ArrowType.Int(64, true)), allocator);
        float8Vector.allocateNew(data.length);

        for (int i = 0; i < data.length; ++i) {
            float8Vector.setSafe(i, data[i]);
        }

        float8Vector.setValueCount(data.length);
        return float8Vector;
    }

    public static BigIntVector longVectorOf(BufferAllocator allocator, String name, int length) {
        BigIntVector float8Vector = new BigIntVector(name, FieldType.nullable(new ArrowType.Int(64, true)), allocator);
        float8Vector.allocateNew(length);
        float8Vector.setValueCount(length);
        return float8Vector;
    }

    public static ColumnMetaData metaDataFromField(Field field) {
        ArrowType arrowType = field.getFieldType().getType();
        if (arrowType instanceof ArrowType.Int) {
            ArrowType.Int intType = (ArrowType.Int) arrowType;
            return (ColumnMetaData) (intType.getBitWidth() == 32 ? new IntegerMetaData(field.getName()) : new LongMetaData(field.getName()));
        } else if (arrowType instanceof ArrowType.Bool) {
            return new BooleanMetaData(field.getName());
        } else if (arrowType instanceof ArrowType.FloatingPoint) {
            ArrowType.FloatingPoint floatingPointType = (ArrowType.FloatingPoint) arrowType;
            return (ColumnMetaData) (floatingPointType.getPrecision() == FloatingPointPrecision.DOUBLE ? new DoubleMetaData(field.getName()) : new FloatMetaData(field.getName()));
        } else if (arrowType instanceof ArrowType.Binary) {
            return new BinaryMetaData(field.getName());
        } else if (arrowType instanceof ArrowType.Utf8) {
            return new StringMetaData(field.getName());
        } else if (arrowType instanceof ArrowType.Date) {
            return new TimeMetaData(field.getName());
        } else {
            throw new IllegalStateException("Illegal type " + field.getFieldType().getType());
        }
    }

    public static Writable fromEntry(int item, FieldVector from, ColumnType columnType) {
        if (from.getValueCount() < item) {
            throw new IllegalArgumentException("Index specified greater than the number of items in the vector with length " + from.getValueCount());
        } else {
            switch (columnType) {
                case Integer:
                    return new IntWritable(getIntFromFieldVector(item, from));
                case Float:
                    return new FloatWritable(getFloatFromFieldVector(item, from));
                case Double:
                    return new DoubleWritable(getDoubleFromFieldVector(item, from));
                case Long:
                    return new LongWritable(getLongFromFieldVector(item, from));
                case NDArray:
                    VarBinaryVector valueVector = (VarBinaryVector) from;
                    byte[] bytes = valueVector.get(item);
                    ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length);
                    direct.put(bytes);
                    INDArray fromTensor = BinarySerde.toArray(direct);
                    return new NDArrayWritable(fromTensor);
                case Boolean:
                    BitVector bitVector = (BitVector) from;
                    return new BooleanWritable(bitVector.get(item) > 0);
                case Categorical:
                    VarCharVector varCharVector = (VarCharVector) from;
                    return new Text(varCharVector.get(item));
                case Time:
                    return new LongWritable(getLongFromFieldVector(item, from));
                case Bytes:
                default:
                    throw new IllegalArgumentException("Illegal type " + from.getClass().getName());
                case String:
                    VarCharVector varCharVector2 = (VarCharVector) from;
                    return new Text(varCharVector2.get(item));
            }
        }
    }

    public static int getIntFromFieldVector(int row, FieldVector fieldVector) {
        if (fieldVector instanceof UInt4Vector) {
            UInt4Vector uInt4Vector = (UInt4Vector) fieldVector;
            return uInt4Vector.get(row);
        } else if (fieldVector instanceof IntVector) {
            IntVector intVector = (IntVector) fieldVector;
            return intVector.get(row);
        } else {
            throw new IllegalArgumentException("Illegal vector type for int " + fieldVector.getClass().getName());
        }
    }

    public static long getLongFromFieldVector(int row, FieldVector fieldVector) {
        if (fieldVector instanceof UInt8Vector) {
            UInt8Vector uInt4Vector = (UInt8Vector) fieldVector;
            return uInt4Vector.get(row);
        } else {
            BigIntVector bigIntVector;
            if (fieldVector instanceof IntVector) {
                bigIntVector = (BigIntVector) fieldVector;
                return bigIntVector.get(row);
            } else {
                TimeStampMilliVector timeStampMilliVector;
                if (fieldVector instanceof TimeStampMilliVector) {
                    timeStampMilliVector = (TimeStampMilliVector) fieldVector;
                    return timeStampMilliVector.get(row);
                } else if (fieldVector instanceof BigIntVector) {
                    bigIntVector = (BigIntVector) fieldVector;
                    return bigIntVector.get(row);
                } else if (fieldVector instanceof DateMilliVector) {
                    DateMilliVector dateMilliVector = (DateMilliVector) fieldVector;
                    return dateMilliVector.get(row);
                } else if (fieldVector instanceof TimeStampMilliVector) {
                    timeStampMilliVector = (TimeStampMilliVector) fieldVector;
                    return timeStampMilliVector.get(row);
                } else if (fieldVector instanceof TimeMilliVector) {
                    TimeMilliVector timeMilliVector = (TimeMilliVector) fieldVector;
                    return (long) timeMilliVector.get(row);
                } else if (fieldVector instanceof TimeStampMicroVector) {
                    TimeStampMicroVector timeStampMicroVector = (TimeStampMicroVector) fieldVector;
                    return timeStampMicroVector.get(row);
                } else if (fieldVector instanceof TimeSecVector) {
                    TimeSecVector timeSecVector = (TimeSecVector) fieldVector;
                    return (long) timeSecVector.get(row);
                } else if (fieldVector instanceof TimeStampMilliVector) {
                    timeStampMilliVector = (TimeStampMilliVector) fieldVector;
                    return timeStampMilliVector.get(row);
                } else if (fieldVector instanceof TimeStampMilliTZVector) {
                    TimeStampMilliTZVector timeStampMilliTZVector = (TimeStampMilliTZVector) fieldVector;
                    return timeStampMilliTZVector.get(row);
                } else if (fieldVector instanceof TimeStampNanoTZVector) {
                    TimeStampNanoTZVector timeStampNanoTZVector = (TimeStampNanoTZVector) fieldVector;
                    return timeStampNanoTZVector.get(row);
                } else if (fieldVector instanceof TimeStampMicroTZVector) {
                    TimeStampMicroTZVector timeStampMicroTZVector = (TimeStampMicroTZVector) fieldVector;
                    return timeStampMicroTZVector.get(row);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }
    }

    public static double getDoubleFromFieldVector(int row, FieldVector fieldVector) {
        if (fieldVector instanceof Float8Vector) {
            Float8Vector uInt4Vector = (Float8Vector) fieldVector;
            return uInt4Vector.get(row);
        } else {
            throw new IllegalArgumentException("Illegal vector type for int " + fieldVector.getClass().getName());
        }
    }

    public static float getFloatFromFieldVector(int row, FieldVector fieldVector) {
        if (fieldVector instanceof Float4Vector) {
            Float4Vector uInt4Vector = (Float4Vector) fieldVector;
            return uInt4Vector.get(row);
        } else {
            throw new IllegalArgumentException("Illegal vector type for int " + fieldVector.getClass().getName());
        }
    }

    public static ArrowWritableRecordBatch asDataVecBatch(ArrowRecordBatch arrowRecordBatch, Schema schema, VectorSchemaRoot vectorLoader) {
        List<FieldVector> fieldVectors = new ArrayList();

        for (int j = 0; j < schema.numColumns(); ++j) {
            String name = schema.getName(j);
            FieldVector fieldVector = vectorLoader.getVector(name);
            fieldVectors.add(fieldVector);
        }

        ArrowWritableRecordBatch ret = new ArrowWritableRecordBatch(fieldVectors, schema);
        ret.setArrowRecordBatch(arrowRecordBatch);
        return ret;
    }
}
