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

import ai.konduit.serving.util.WritableValueRetriever;
import io.vertx.core.json.JsonArray;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.dmg.pmml.FieldName;
import org.nd4j.linalg.primitives.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseJsonArrayConverter implements JsonArrayConverter {


    @Override
    public Pair<Map<Integer, Integer>, List<? extends Map<FieldName, ?>>> convertPmmlWithErrors(Schema schema, JsonArray jsonArray, DataPipelineErrorHandler dataPipelineErrorHandler) {
        return convertPmmlWithErrors(schema, jsonArray, null, dataPipelineErrorHandler);
    }

    @Override
    public Pair<Map<Integer, Integer>, ArrowWritableRecordBatch> convertWithErrors(Schema schema, JsonArray jsonArray, DataPipelineErrorHandler dataPipelineErrorHandler) {
        return convertWithErrors(schema, jsonArray, null, dataPipelineErrorHandler);
    }


    /**
     * Given a {@link Schema} transform the
     * given input (an array of json objects)
     * to a dictionary for use with the pmml evaluator
     *
     * @param schema    the input inputSchema
     * @param jsonArray an array of json objects
     * @return a list of fields for use with pmml
     */
    @Override
    public List<? extends Map<FieldName, ?>> convertPmml(Schema schema, JsonArray jsonArray) {
        return convertPmml(schema, jsonArray, null);
    }

    /**
     * Given a {@link Schema} transform the
     * given input (an array of json objects)
     * to an {@link ArrowWritableRecordBatch}
     * for use with {@link org.nd4j.linalg.api.ndarray.INDArray}
     * based konduit-serving (usually dl4j).
     * Note that since index matters for this conversion (due to {@link org.nd4j.linalg.api.ndarray.INDArray}
     * being index based) - internally, a inputSchema permutation happens to match the indices
     * appropriate for use with the given inputSchema
     *
     * @param schema    the input inputSchema
     * @param jsonArray an array of json objects
     * @return an {@link ArrowWritableRecordBatch} for use with conversion to
     * {@link org.nd4j.linalg.api.ndarray.INDArray}
     */
    @Override
    public ArrowWritableRecordBatch convert(Schema schema, JsonArray jsonArray) {
        return convert(schema, jsonArray, null);
    }


    protected Pair<Map<Integer, Integer>, List<? extends Map<FieldName, ?>>> doTransformProcessConvertPmmlWithErrors(Schema schema, JsonArray jsonArray, TransformProcess transformProcess, DataPipelineErrorHandler dataPipelineErrorHandler) {
        Schema outputSchema = transformProcess.getFinalSchema();

        if (!transformProcess.getInitialSchema().equals(schema)) {
            throw new IllegalArgumentException("Transform process specified, but does not match target input inputSchema");
        }


        List<Map<FieldName, Object>> ret = new ArrayList<>(jsonArray.size());
        List<FieldName> fieldNames = getNameRepresentationFor(outputSchema);

        Pair<Map<Integer, Integer>, ArrowWritableRecordBatch> convertWithErrors = convertWithErrors(schema, jsonArray, transformProcess, dataPipelineErrorHandler);
        ArrowWritableRecordBatch conversion = convertWithErrors.getRight();
        for (int i = 0; i < conversion.size(); i++) {
            List<Writable> recordToMap = conversion.get(i);
            Map<FieldName, Object> record = new LinkedHashMap();
            for (int j = 0; j < outputSchema.numColumns(); j++) {
                record.put(fieldNames.get(j), WritableValueRetriever.getUnderlyingValue(recordToMap.get(j)));

            }

            ret.add(record);
        }

        return Pair.of(convertWithErrors.getKey(), ret);
    }


    protected List<Map<FieldName, Object>> doTransformProcessConvertPmml(Schema schema, JsonArray jsonArray, TransformProcess transformProcess) {
        Schema outputSchema = transformProcess.getFinalSchema();

        if (!transformProcess.getInitialSchema().equals(schema)) {
            throw new IllegalArgumentException("Transform process specified, but does not match target input inputSchema");
        }


        List<Map<FieldName, Object>> ret = new ArrayList<>(jsonArray.size());
        List<FieldName> fieldNames = getNameRepresentationFor(outputSchema);

        ArrowWritableRecordBatch conversion = convert(schema, jsonArray, transformProcess);
        for (int i = 0; i < conversion.size(); i++) {
            List<Writable> recordToMap = conversion.get(i);
            Map<FieldName, Object> record = new LinkedHashMap();
            for (int j = 0; j < outputSchema.numColumns(); j++) {
                record.put(fieldNames.get(j), WritableValueRetriever.getUnderlyingValue(recordToMap.get(j)));

            }

            ret.add(record);
        }

        return ret;


    }


    protected List<FieldName> getNameRepresentationFor(Schema schema) {
        List<FieldName> fieldNames = new ArrayList<>();
        for (int i = 0; i < schema.numColumns(); i++) {
            fieldNames.add(FieldName.create(schema.getName(i)));
        }

        return fieldNames;
    }
}
