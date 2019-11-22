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

package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.PipelineStepRunner;
import ai.konduit.serving.util.WritableValueRetriever;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.nd4j.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes expansion of json objects in to "real" objects.
 * This is needed when integrating with {@link PipelineStepRunner}
 * that may output {@link Text} with json arrays or json objects.
 * This kind of output is generally expected from python or pmml based pipelines
 * which have a lot more complicated output and schema based values
 * rather than straight {@link org.nd4j.linalg.api.ndarray.INDArray} like
 * most deep learning pipelines will be.
 *
 * @author Adam Gibson
 *
 */
public class JsonExpanderTransformStepRunner extends BaseStepRunner {

    public JsonExpanderTransformStepRunner(PipelineStep pipelineStep) {
        super(pipelineStep);
    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs) {

    }

    @Override
    public Record[] transform(Record[] input) {
        Preconditions.checkNotNull(input,"Input  records were null!");
        List<Record> recordList = new ArrayList<>();
        for(Record record : input) {
            Text text = (Text) record.getRecord().get(0);
            if(text.toString().charAt(0) == '[') {
                JsonArray arr = new JsonArray(text.toString());
                for(int i  = 0; i < arr.size(); i++) {
                    List<Writable> writables = new ArrayList<>();
                    JsonObject object = arr.getJsonObject(i);
                    for(String field : object.fieldNames()) {
                        writables.add(WritableValueRetriever.writableFromValue(object.getValue(field)));
                    }

                    recordList.add(new org.datavec.api.records.impl.Record(writables,null));
                }
            }
            else if(text.toString().charAt(0) == '{') {
                JsonObject jsonObject = new JsonObject(text.toString());
                List<Writable> writables = new ArrayList<>();
                for(String field : jsonObject.fieldNames()) {
                    writables.add(WritableValueRetriever.writableFromValue(jsonObject.getValue(field)));
                }

                recordList.add(new org.datavec.api.records.impl.Record(writables,null));

            }

        }

        return recordList.toArray(new Record[recordList.size()]);
    }
}
