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

package ai.konduit.serving.pipeline.handlers.converter;

import ai.konduit.serving.util.ArrowUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.datavec.api.transform.ColumnType;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.arrow.ArrowConverter;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.datavec.local.transforms.LocalTransformExecutor;
import org.dmg.pmml.FieldName;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversion utility for handling converting arrays of
 * maps to feature vectors for use with inference.
 *
 * @author Adam Gibson
 */
@Slf4j
public class JsonArrayMapConverter extends BaseJsonArrayConverter {

    public static BufferAllocator bufferAllocator = new RootAllocator(Long.MAX_VALUE);

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Map<Integer, Integer>, List<? extends Map<FieldName, ?>>> convertPmmlWithErrors(Schema schema, JsonArray jsonArray, TransformProcess transformProcess, DataPipelineErrorHandler dataPipelineErrorHandler) {
        if (transformProcess != null) {
            return doTransformProcessConvertPmmlWithErrors(schema, jsonArray, transformProcess, dataPipelineErrorHandler);
        }


        List<FieldName> fieldNames = getNameRepresentationFor(schema);


        List<Map<FieldName, Object>> ret = new ArrayList<>(jsonArray.size());
        Map<Integer, Integer> mapping = new LinkedHashMap<>();
        int numSucceeded = 0;
        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                JsonObject jsonObject = jsonArray.getJsonObject(i);
                if (jsonObject.size() != schema.numColumns()) {
                    throw new IllegalArgumentException("Found illegal item at row " + i);
                }
                Map<FieldName, Object> record = new LinkedHashMap();
                for (int j = 0; j < schema.numColumns(); j++) {
                    record.put(fieldNames.get(j), jsonObject.getValue(schema.getName(j)));
                }

                mapping.put(numSucceeded, i);
                numSucceeded++;
                ret.add(record);
            } catch (Exception e) {
                dataPipelineErrorHandler.onError(e, jsonArray.getJsonObject(i), i);
            }
        }


        return Pair.of(mapping, ret);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Map<FieldName, ?>> convertPmml(Schema schema, JsonArray jsonArray, TransformProcess transformProcess) {
        if (transformProcess != null) {
            return doTransformProcessConvertPmml(schema, jsonArray, transformProcess);
        }


        List<FieldName> fieldNames = getNameRepresentationFor(schema);


        List<Map<FieldName, Object>> ret = new ArrayList<>(jsonArray.size());

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.getJsonObject(i);
            Map<FieldName, Object> record = new LinkedHashMap();
            for (int j = 0; j < schema.numColumns(); j++) {
                record.put(fieldNames.get(j), jsonObject.getValue(schema.getName(j)));

            }

            ret.add(record);
        }

        return ret;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ArrowWritableRecordBatch convert(Schema schema, JsonArray jsonArray, TransformProcess transformProcess) {
        Preconditions.checkNotNull(schema, "Please specify a schema for conversion!");
        log.info("Converting " + jsonArray);
        List<FieldVector> vectors = ArrowUtils.createFieldVectors(bufferAllocator, schema, jsonArray.size());
        //all numbers case, convert to json object
        if (jsonArray.getValue(0) instanceof JsonArray) {
            Preconditions.checkNotNull(schema, "Schema must not be null when specifying all numerical values. A schema is needed for names.");
            JsonArray newInput = new JsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = new JsonObject();
                JsonArray currInputRow = jsonArray.getJsonArray(i);
                Preconditions.checkState(currInputRow.size() == schema.numColumns(), "Invalid row " + i + " did not match schema!");
                for (int j = 0; j < currInputRow.size(); j++) {
                    jsonObject.put(schema.getName(j), currInputRow.getValue(j));
                }

                newInput.add(jsonObject);
            }

            jsonArray = newInput;
        }


        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.getJsonObject(i);
            for (int j = 0; j < schema.numColumns(); j++) {
                if (schema.getType(j) == ColumnType.NDArray) {
                    Object value = jsonObject.getValue(schema.getName(j));
                    if (value instanceof String) {
                        INDArray arr = Nd4j.scalar(Double.parseDouble(value.toString()));
                        NDArrayWritable ndArrayWritable = new NDArrayWritable(arr);
                        ArrowConverter.setValue(schema.getType(j), vectors.get(j), ndArrayWritable, i);

                    } else if (value instanceof Number) {
                        Number n = (Number) value;
                        INDArray arr = Nd4j.scalar(n.doubleValue());
                        NDArrayWritable ndArrayWritable = new NDArrayWritable(arr);
                        ArrowConverter.setValue(schema.getType(j), vectors.get(j), ndArrayWritable, i);
                    } else if (value instanceof JsonArray) {
                        JsonArray jsonArray1 = (JsonArray) value;
                        INDArray arr = Nd4j.create(jsonArray1.size(), jsonArray1.getJsonArray(0).size());
                        for (int k = 0; k < arr.rows(); k++) {
                            JsonArray row = jsonArray1.getJsonArray(k);
                            for (int l = 0; l < arr.columns(); l++) {
                                arr.putScalar(k, l, Double.parseDouble(row.getValue(l).toString()));
                            }
                        }

                        NDArrayWritable ndArrayWritable = new NDArrayWritable(arr);
                        ArrowConverter.setValue(schema.getType(j), vectors.get(j), ndArrayWritable, i);

                    } else {
                        throw new IllegalArgumentException("Illegal type found " + value.getClass());
                    }
                } else {
                    ArrowConverter.setValue(schema.getType(j), vectors.get(j), jsonObject.getValue(schema.getName(j)), i);
                }
            }
        }

        ArrowWritableRecordBatch writableRecordBatch = ArrowConverter.toArrowWritables(vectors, schema);
        if (transformProcess != null)
            return (ArrowWritableRecordBatch) LocalTransformExecutor.execute(writableRecordBatch, transformProcess);
        return writableRecordBatch;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Map<Integer, Integer>, ArrowWritableRecordBatch> convertWithErrors(Schema schema, JsonArray jsonArray, TransformProcess transformProcess, DataPipelineErrorHandler dataPipelineErrorHandler) {
        List<ArrowWritableRecordBatch> examples = new ArrayList<>();
        Map<Integer, Integer> indexMappings = new LinkedHashMap<>();
        int succeededCurrCount = 0;
        List<FieldVector> vectorsIndividual = ArrowUtils.createFieldVectors(bufferAllocator, schema, 1);

        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                JsonObject jsonObject = jsonArray.getJsonObject(i);
                for (int j = 0; j < schema.numColumns(); j++) {
                    ArrowUtils.setValue(schema.getType(j), vectorsIndividual.get(j), jsonObject.getValue(schema.getName(j)), i);
                }

                ArrowWritableRecordBatch writableRecordBatch = ArrowConverter.toArrowWritables(vectorsIndividual, schema);
                if (transformProcess != null) {
                    writableRecordBatch = (ArrowWritableRecordBatch) LocalTransformExecutor.execute(writableRecordBatch, transformProcess);
                }

                examples.add(writableRecordBatch);
                indexMappings.put(succeededCurrCount, i);
                succeededCurrCount++;

            } catch (Exception e) {
                dataPipelineErrorHandler.onError(e, jsonArray.getJsonObject(i), i);
                continue;
            }

        }

        if (transformProcess != null) {
            schema = transformProcess.getFinalSchema();
        }


        List<FieldVector> vectors = ArrowUtils.createFieldVectors(bufferAllocator, schema, indexMappings.size());
        for (int i = 0; i < examples.size(); i++) {
            for (int j = 0; j < schema.numColumns(); j++) {
                try {
                    ArrowUtils.setValue(schema.getType(j), vectors.get(j), examples.get(i).get(0).get(j), i);
                } catch (Exception e) {
                    dataPipelineErrorHandler.onError(e, examples.get(i), i);
                }
            }
        }

        ArrowWritableRecordBatch finalBatch = ArrowConverter.toArrowWritables(vectors, schema);
        return Pair.of(indexMappings, finalBatch);
    }


}
