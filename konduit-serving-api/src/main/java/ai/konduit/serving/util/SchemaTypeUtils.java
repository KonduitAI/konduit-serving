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

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.SchemaType;
import org.datavec.api.records.Record;
import org.datavec.api.transform.ColumnType;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

/**
 * Utils for a mix of data vec {@link Schema} manipulation
 * and configuration for {@link InferenceConfiguration}
 *
 */
public class SchemaTypeUtils {


    /**
     * Get record for all values
     * @param values the record to get
     * @return the record
     */
    public static List<Writable> getRecord(double...values) {
        List<Writable> ret = new ArrayList<>();
        for(int i = 0; i < values.length; i++) {
            ret.add(new DoubleWritable(values[i]));
        }

        return ret;
    }

    /**
     * Get the column names for the input schema
     * @param schema the schema to get the names for
     * @return list of column names
     */
    public static List<String> columnNames(Schema schema) {
        return schema.getColumnNames();
    }

    /**
     * Compute input types for the given set of transform processes
     * @param transformProcesses the input transform processes
     * @return the input types for the given transform processes
     */
    public static SchemaType[][] inputTypes(TransformProcess[] transformProcesses) {
        SchemaType[][] types = new SchemaType[transformProcesses.length][];
        for(int i = 0; i < types.length; i++) {
            types[i] = SchemaTypeUtils.typesForSchema(transformProcesses[i].getInitialSchema());
        }

        return types;
    }

    /**
     * Compute output types for the given set of transform processes
     * @param transformProcesses the input transform processes
     * @return the input types for the given transform processes
     */
    public static SchemaType[][] outputTypes(TransformProcess[] transformProcesses) {
        SchemaType[][] types = new SchemaType[transformProcesses.length][];
        for(int i = 0; i < types.length; i++) {
            types[i] = SchemaTypeUtils.typesForSchema(transformProcesses[i].getFinalSchema());
        }

        return types;
    }


    /**
     * Extract an ordered list
     * of the types in a given {@link Schema}
     * @param schema the schema to get the types for
     * @return the schema types based on the ordering
     * of the columns in the schema
     */
    public static SchemaType[] typesForSchema(Schema schema) {
        SchemaType[] ret = new SchemaType[schema.numColumns()];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = schemaTypeForColumnType(schema.getType(i));
        }

        return ret;
    }

    /**
     * Create a mapping of name {@link SchemaType}
     * based on the {@link Schema}
     * @param schema the schema to decompose
     * @return the map of name to {@link SchemaType}
     */
    public static Map<String,SchemaType> typeMappingsForSchema(Schema schema) {
        Map<String,SchemaType> ret = new LinkedHashMap<>();
        for(int i = 0; i < schema.numColumns(); i++) {
            ret.put(schema.getName(i),schemaTypeForColumnType(schema.getType(i)));
        }


        return ret;
    }

    /**
     * Convert a {@link ColumnType}
     * to the equivalent {@link SchemaType}
     * @param columnType the column type to convert
     * @return the schema type for the given column type
     */
    public static SchemaType schemaTypeForColumnType(ColumnType columnType) {
        return SchemaType.valueOf(columnType.name());
    }

    /**
     * Create a {@link Schema}
     * from the given {@link SchemaType}
     * and the names.
     * Note that exceptions are thrown
     * when the types are null, names are null,
     * or the 2 arguments are not the same length
     * @param types the type
     * @param names the names of each column
     * @return the equivalent {@link Schema} given the types
     * and names
     */
    public static Schema toSchema(SchemaType[] types, List<String> names) {
        Preconditions.checkNotNull(types,"Please specify types");
        Preconditions.checkNotNull(names,"Please specify names.");
        Preconditions.checkState(types.length == names.size(),"Types and names must be the same length");
        Schema.Builder builder = new Schema.Builder();
        for(int i = 0; i < types.length; i++) {
            Preconditions.checkNotNull(types[i],"Type " + i + " was null!");
            switch(types[i]) {
                case NDArray:
                    builder.addColumnNDArray(names.get(i),new long[]{1,1});
                    break;
                case String:
                    builder.addColumnString(names.get(i));
                    break;
                case Boolean:
                    builder.addColumnBoolean(names.get(i));
                    break;
                case Categorical:
                    builder.addColumnCategorical(names.get(i));
                    break;
                case Float:
                    builder.addColumnFloat(names.get(i));
                    break;
                case Double:
                    builder.addColumnDouble(names.get(i));
                    break;
                case Integer:
                    builder.addColumnInteger(names.get(i));
                    break;
                case Long:
                    builder.addColumnLong(names.get(i));
                    break;
                case Bytes:
                    throw new UnsupportedOperationException();
                default:
                    throw new UnsupportedOperationException("Unknown type " + types[i]);

            }
        }

        return builder.build();
    }

    /**
     * Convert a set of {@link INDArray}
     * to an equivalent {@link NDArrayWritable}
     * @param writables the writables to convert
     * @return the underlying {@link INDArray}
     */
    public static Writable[] fromArrays(INDArray[] writables) {
        Writable[] ret = new Writable[writables.length];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = new NDArrayWritable(writables[i]);
        }

        return ret;
    }

    /**
     * Convert a set of {@link NDArrayWritable}
     * to their underlying ndarrays
     * @param writables the writables to convert
     * @return the underlying {@link INDArray}
     */
    public static INDArray[] fromWritables(Writable[] writables) {
        INDArray[] ret = new INDArray[writables.length];
        for(int i = 0; i < ret.length; i++) {
            NDArrayWritable ndArrayWritable = (NDArrayWritable) writables[i];
            ret[i] = ndArrayWritable.get();
        }

        return ret;
    }

    /**
     * Convert an {@link INDArray}
     * batch to {@link Record}
     * input comprising of a single {@link NDArrayWritable}
     * @param input the input
     * @return the equivalent output records
     */
    public static Record[] toRecords(INDArray[] input) {
        Record[] ret = new Record[input.length];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = new org.datavec.api.records.impl.Record(
                    Arrays.asList(new NDArrayWritable(input[i]))
                    ,null);
        }

        return ret;
    }

    /**
     * Convert a set of {@link Record}
     * to {@link INDArray}
     * this assumes that each "record" is
     * actually a size 1 {@link Writable} of type
     * {@link NDArrayWritable}
     * @param records the records to convert
     * @return the extracted {@link INDArray}
     */
    public static INDArray[] toArrays(Record[] records) {
        INDArray[] ret = new INDArray[records[0].getRecord().size()];
        int initialLength = ret.length;
        //each ndarray
        for(int i = 0; i < initialLength; i++) {
            List<INDArray> accum = new ArrayList<>();
            //for each record
            for(Record record : records) {
                Writable curr = record.getRecord().get(i);
                NDArrayWritable writable = (NDArrayWritable) record.getRecord().get(i);
                accum.add(writable.get());

            }

            ret[i] = Nd4j.concat(0,accum.toArray(new INDArray[0]));

        }

        return ret;
    }

}
