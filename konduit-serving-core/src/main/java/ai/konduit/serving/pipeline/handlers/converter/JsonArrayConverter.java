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

import io.vertx.core.json.JsonArray;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.dmg.pmml.FieldName;
import org.nd4j.common.primitives.Pair;

import java.util.List;
import java.util.Map;

/**
 * An interface for converting input
 * json arrays of either arrays of arrays (csvs)
 * or arrays of objects/dictionaries
 * converting the input to either pmml oriented
 * dictionary output or datavec oriented arrow batches.
 *
 * @author Adam Gibson
 */
public interface JsonArrayConverter {


    /**
     * Convert a json array of arrays to
     * the proper format for pmml.
     * PMML expects a list of named tuples
     * where each name is a {@link FieldName}
     * representing a column  in the input.
     * <p>
     * This returns a mapping of original indices
     * to the output indices.
     * If there are no errors,mappings should be 1 to 1.
     * If there were errors, the map is used to find
     * what rows in the output (the second part of the pair)
     * map to the original input rows.
     *
     * @param schema                   the inputSchema for the input
     * @param jsonArray                the array of arrays to use
     * @param dataPipelineErrorHandler the {@link DataPipelineErrorHandler}
     *                                 when an error is encountered in the conversion step
     * @return a map of the original inputs to the output rows, and the list of converted inputs
     * in pmml format
     */
    Pair<Map<Integer, Integer>, List<? extends Map<FieldName, ?>>> convertPmmlWithErrors(Schema schema, JsonArray jsonArray,
                                                                                         DataPipelineErrorHandler dataPipelineErrorHandler);

    /**
     * See {@link #convertPmmlWithErrors(Schema, JsonArray, TransformProcess, DataPipelineErrorHandler)}
     * for the documentation for this method. This method just adds a {@link TransformProcess}
     * as an option for converting the output.
     *
     * @param schema                   the input inputSchema
     * @param jsonArray                the array of arrays
     * @param transformProcess         the transform process to use to transform the data
     * @param dataPipelineErrorHandler the {@link DataPipelineErrorHandler}
     *                                 error handler to use
     * @return a map of the original inputs to the output rows, and the list of converted inputs
     * in pmml format
     */
    Pair<Map<Integer, Integer>, List<? extends Map<FieldName, ?>>> convertPmmlWithErrors(Schema schema,
                                                                                         JsonArray jsonArray,
                                                                                         TransformProcess transformProcess,
                                                                                         DataPipelineErrorHandler dataPipelineErrorHandler);


    /**
     * Convert a json array to pmml output
     * given an input {@link Schema} and {@link JsonArray}
     *
     * @param schema    the inputSchema to convert
     * @param jsonArray the json array to convert
     * @return a list of maps of {@link FieldName} to feature value
     * where each item in the list is considered a "row" or an example.
     */
    List<? extends Map<FieldName, ?>> convertPmml(Schema schema, JsonArray jsonArray);

    /**
     * See {@link #convertPmml(Schema, JsonArray)}
     * for documentation for this method.
     * This method just adds the ability to add a
     * {@link TransformProcess} to the conversion process
     * after sending in the raw input.
     *
     * @param schema           the input inputSchema
     * @param jsonArray        the input data to convert
     * @param transformProcess the {@link TransformProcess}
     *                         to convert
     * @return a list of maps of {@link FieldName} to feature
     * valueu where each item in the list is considered a "row" or an example.
     */
    List<? extends Map<FieldName, ?>> convertPmml(Schema schema,
                                                  JsonArray jsonArray,
                                                  TransformProcess transformProcess);

    /**
     * Convert a {@link JsonArray}
     * given an input inputSchema to an {@link ArrowWritableRecordBatch}
     * representing a schemaed input for use with nd4j based
     * konduit-serving where data in memory is represented by arrow.
     * <p>
     * The {@link ArrowWritableRecordBatch} is an implementation of
     * the expected input format for {@link org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator}
     * {@link org.datavec.api.records.Record}
     *
     * @param schema    the input inputSchema to use
     * @param jsonArray the json array of raw data to convert
     * @return the converted batch
     */
    ArrowWritableRecordBatch convert(Schema schema, JsonArray jsonArray);

    /**
     * See {@link #convert(Schema, JsonArray)}
     * for more context on what this method does.
     * This method just adds a transform process as
     * a conversion step.
     *
     * @param schema           the inputSchema to use
     * @param jsonArray        the json array input data
     * @param transformProcess the {@link TransformProcess}
     *                         to use as part of the conversion step
     * @return the converted batch
     */
    ArrowWritableRecordBatch convert(Schema schema, JsonArray jsonArray, TransformProcess transformProcess);


    /**
     * Convert a json array of arrays to
     * {@link ArrowWritableRecordBatch}.
     * <p>
     * This returns a mapping of original indices
     * to the output indices.
     * If there are no errors,mappings should be 1 to 1.
     * If there were errors, the map is used to find
     * what rows in the output (the second part of the pair)
     * map to the original input rows.
     *
     * @param schema                   the inputSchema for the input
     * @param jsonArray                the array of arrays to use
     * @param dataPipelineErrorHandler the {@link DataPipelineErrorHandler}
     *                                 when an error is encountered in the conversion step
     * @return a map of the original inputs to the output rows, and the {@link ArrowWritableRecordBatch}
     * resulting from the conversion
     */
    Pair<Map<Integer, Integer>, ArrowWritableRecordBatch> convertWithErrors(Schema schema,
                                                                            JsonArray jsonArray,
                                                                            DataPipelineErrorHandler dataPipelineErrorHandler);

    /**
     * See {@link #convertWithErrors(Schema, JsonArray, DataPipelineErrorHandler)}
     * for more context in to what this method does.
     * This method just adds a {@link TransformProcess}
     * manipulating the raw input data in to a state suitable
     * for conversion to {@link ArrowWritableRecordBatch}
     *
     * @param schema                   the input inputSchema of the data
     * @param jsonArray                the data to convert
     * @param transformProcess         the transform process to use for conversion
     * @param dataPipelineErrorHandler the {@link DataPipelineErrorHandler}
     *                                 to use
     * @return a map of the original inputs to the output rows, and the {@link ArrowWritableRecordBatch}
     * resulting from the conversion
     */
    Pair<Map<Integer, Integer>, ArrowWritableRecordBatch> convertWithErrors(Schema schema,
                                                                            JsonArray jsonArray,
                                                                            TransformProcess transformProcess,
                                                                            DataPipelineErrorHandler dataPipelineErrorHandler);

}
