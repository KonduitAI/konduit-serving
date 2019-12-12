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

package ai.konduit.serving.pmml.util;

import ai.konduit.serving.util.WritableValueRetriever;
import org.datavec.api.records.Record;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.serde.JsonMappers;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.*;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.*;
import org.jpmml.evaluator.clustering.ClusterAffinityDistribution;
import org.jpmml.evaluator.support_vector_machine.DistanceDistribution;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.shade.jackson.core.JsonProcessingException;

import java.util.*;

public class PmmlUtils {


    public static Map<String, Double> getResult(Evaluator evaluator, Object result) {
        MiningFunction miningFunction = evaluator.getMiningFunction();
        switch (miningFunction) {
            case CLASSIFICATION:
                if (result instanceof Map) {
                    return (Map<String, Double>) result;
                } else {
                    HasProbability probability = (HasProbability) result;
                    Map<String, Double> ret = new HashMap<>();
                    for (String category : probability.getCategories()) {
                        ret.put(category, probability.getProbability(category));
                    }

                    return ret;
                }

            case REGRESSION:

            case ASSOCIATION_RULES:
            case SEQUENCES:
            case CLUSTERING:
            case TIME_SERIES:
            default:
                throw new IllegalStateException("Unable to support non elnino or classification use cases");
        }

    }


    /**
     * Convert the input pmm output to {@link Record}
     *
     * @param pmmlInput        the input
     * @param conversionSchema the intended schema
     * @return the equivalent records based on the pmml output
     */
    public static Record[] toRecords(List<Map<FieldName, Object>> pmmlInput, Schema conversionSchema) {
        Record[] ret = new Record[pmmlInput.size()];
        for (int i = 0; i < pmmlInput.size(); i++) {
            Preconditions.checkState(conversionSchema.numColumns() == pmmlInput.get(i).size(), "Illegal pmml output. Does not match passed in schema.");
            List<Writable> record = new ArrayList<>();
            for (int j = 0; j < conversionSchema.numColumns(); j++) {
                FieldName fieldName = FieldName.create(conversionSchema.getName(j));
                Object value = pmmlInput.get(i).get(fieldName);
                Preconditions.checkNotNull(value, "Value " + fieldName.getValue() + " not found!");
                if (value instanceof HasProbability) {
                    HasProbability probabilityDistribution = (HasProbability) value;
                    switch (conversionSchema.getType(j)) {
                        case String:
                            try {
                                Text text = new Text(JsonMappers.getMapper().writeValueAsString(probabilityDistribution));
                                record.add(text);
                            } catch (JsonProcessingException e) {
                                throw new IllegalStateException("Unable to serialize value at column " + conversionSchema.getName(j) + " to json ");
                            }
                            break;
                        case NDArray:
                            double[] probabilities = new double[probabilityDistribution.getCategories().size()];
                            int count = 0;
                            for (Object catgory : probabilityDistribution.getCategories()) {
                                probabilities[count++] = probabilityDistribution.getProbability(catgory.toString());
                            }
                            record.add(new NDArrayWritable(Nd4j.createFromArray(probabilities)));
                            break;

                    }
                } else if (value instanceof Classification) {
                    Classification classification = (Classification) value;
                    switch (classification.getType()) {
                        case DISTANCE:
                            DistanceDistribution distanceDistribution = (DistanceDistribution) classification;
                            ValueMap<String, Double> values = distanceDistribution.getValues();
                            Map<String, Double> map = new LinkedHashMap<>();
                            for (Map.Entry<String, Value<Double>> entry : values.entrySet()) {
                                map.put(entry.getKey(), entry.getValue().doubleValue());
                            }


                            switch (conversionSchema.getType(j)) {
                                case String:
                                    try {
                                        Text text = new Text(JsonMappers.getMapper().writeValueAsString(map));
                                        record.add(text);
                                    } catch (JsonProcessingException e) {
                                        throw new IllegalStateException("Unable to serialize value at column " + conversionSchema.getName(j) + " to json ");
                                    }
                                    break;
                                case NDArray:
                                    double[] probabilities = new double[map.size()];
                                    int count = 0;
                                    for (Map.Entry<String, Double> category : map.entrySet()) {
                                        probabilities[count++] = category.getValue();
                                    }
                                    record.add(new NDArrayWritable(Nd4j.createFromArray(probabilities)));
                                    break;
                            }

                            Writable writable = WritableValueRetriever.writableFromValue(distanceDistribution.getPrediction());
                            record.add(writable);
                            break;
                        case SIMILARITY:
                            ClusterAffinityDistribution clusterAffinityDistribution = (ClusterAffinityDistribution) classification;
                            switch (conversionSchema.getType(j)) {
                                case String:
                                    try {
                                        Text text = new Text(JsonMappers.getMapper().writeValueAsString(clusterAffinityDistribution));
                                        record.add(text);
                                    } catch (JsonProcessingException e) {
                                        throw new IllegalStateException("Unable to serialize value at column " + conversionSchema.getName(j) + " to json ");
                                    }
                                    break;
                                case NDArray:
                                    double[] probabilities = new double[clusterAffinityDistribution.getCategories().size()];
                                    int count = 0;
                                    for (Object catgory : clusterAffinityDistribution.getCategories()) {
                                        probabilities[count++] = clusterAffinityDistribution.getAffinity(catgory.toString());
                                    }
                                    record.add(new NDArrayWritable(Nd4j.createFromArray(probabilities)));
                                    break;

                            }

                            Writable writable2 = WritableValueRetriever.writableFromValue(clusterAffinityDistribution.getPrediction());
                            record.add(writable2);
                            break;
                        case VOTE:
                        case PROBABILITY:
                        case CONFIDENCE:
                            throw new IllegalStateException("Probability case should be handled earlier");
                    }
                } else {
                    Writable writable = WritableValueRetriever.writableFromValue(value);
                    record.add(writable);
                }

            }

            ret[i] = new org.datavec.api.records.impl.Record(record, null);
        }

        return ret;
    }


    /**
     * Auto infer a schema based on the final model
     * output from the pmml document.
     *
     * @param pmml the pmml document
     * @return the output schema relative to the output fields
     * in the pmml document
     */
    public static Schema outputSchema(PMML pmml) {
        Schema.Builder ret = new Schema.Builder();
        Preconditions.checkState(!pmml.getModels().isEmpty(), "No models found for automatic inference of output schema");
        Model model = pmml.getModels().get(pmml.getModels().size() - 1);
        Output output = model.getOutput();
        if (output != null) {
            for (OutputField outputField : output.getOutputFields()) {
                if (outputField.getDataType() != null)
                    addDataTypeForSchema(outputField.getDataType(), ret, outputField.getName().getValue());
                else {
                    addDataTypeForSchema(DataType.STRING, ret, outputField.getName().getValue());
                }
            }
        } else {
            if (model.getMiningFunction() == MiningFunction.CLASSIFICATION) {
                for (DataField dataField : pmml.getDataDictionary().getDataFields()) {
                    if (dataField.getOpType() == OpType.CATEGORICAL && model.getMiningFunction() == MiningFunction.CLASSIFICATION) {
                        addDataTypeForSchema(dataField.getDataType(), ret, dataField.getName().getValue());
                    }

                }

            } else if (model.getMiningFunction() == MiningFunction.REGRESSION) {
                for (MiningField miningField : model.getMiningSchema().getMiningFields()) {
                    if (miningField.getUsageType() == MiningField.UsageType.PREDICTED) {
                        for (DataField dataField : pmml.getDataDictionary().getDataFields()) {
                            if (dataField.getName().equals(miningField.getName())) {
                                addDataTypeForSchema(dataField.getDataType(), ret, dataField.getName().getValue());
                            }
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Unsupported mining function type " + model.getMiningFunction());
            }

        }


        return ret.build();
    }

    /**
     * Convert a {@link DataDictionary}
     * to a schema {@link Schema}
     *
     * @param pmml the target {@link PMML} document
     * @return the equivalent {@link Schema}
     */
    public static Schema inputSchema(PMML pmml) {
        DataDictionary dataDictionary = pmml.getDataDictionary();
        Schema.Builder ret = new Schema.Builder();
        MiningSchema miningSchema = pmml.getModels().get(0).getMiningSchema();
        Set<FieldName> outputNames = new HashSet<>();
        //ensure we only grab output fields
        for (MiningField miningField : miningSchema.getMiningFields()) {
            if (miningField.getUsageType() == MiningField.UsageType.PREDICTED) {
                outputNames.add(miningField.getName());
            }
        }
        for (int i = 0; i < dataDictionary.getNumberOfFields(); i++) {
            String name = dataDictionary.getDataFields().get(i).getName().getValue();
            if (!outputNames.contains(dataDictionary.getDataFields().get(i).getName()))
                addDataTypeForSchema(dataDictionary.getDataFields().get(i).getDataType(), ret, name);

        }

        return ret.build();
    }

    public static void addDataTypeForSchema(DataType dataType, Schema.Builder ret, String name) {
        Preconditions.checkNotNull(dataType, "Data type for name " + name + " is null!");
        switch (dataType) {
            case FLOAT:
                ret.addColumnFloat(name);
                break;
            case DOUBLE:
                ret.addColumnDouble(name);
                break;
            case DATE:
            case TIME:
            case TIME_SECONDS:
            case DATE_DAYS_SINCE_1960:
            case DATE_TIME_SECONDS_SINCE_1980:
            case DATE_DAYS_SINCE_0:
            case DATE_TIME:
            case DATE_DAYS_SINCE_1970:
            case DATE_DAYS_SINCE_1980:
            case DATE_TIME_SECONDS_SINCE_1970:
            case DATE_TIME_SECONDS_SINCE_0:
            case DATE_TIME_SECONDS_SINCE_1960:
                ret.addColumnTime(name, TimeZone.getDefault());
                break;
            case INTEGER:
                ret.addColumnInteger(name);
                break;
            case BOOLEAN:
                ret.addColumnBoolean(name);
                break;
            case STRING:
                ret.addColumnString(name);
                break;
            default:
                throw new IllegalArgumentException("Unable to set column type");

        }
    }

}
