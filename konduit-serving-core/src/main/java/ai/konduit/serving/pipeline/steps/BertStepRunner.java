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

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.step.BertStep;
import ai.konduit.serving.util.WritableValueRetriever;
import io.vertx.core.json.JsonObject;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.iterator.BertIterator;
import org.deeplearning4j.iterator.LabeledSentenceProvider;
import org.deeplearning4j.iterator.provider.CollectionLabeledSentenceProvider;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.text.tokenization.tokenizerfactory.BertWordPieceTokenizerFactory;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.datavec.api.writable.Text;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BertStepRunner extends BaseStepRunner
{
    private BertWordPieceTokenizerFactory tokenizer;
    private BertStep bertStep;

    private ComputationGraph bertModel;

    private final int MAX_LEN = 256;

    public BertStepRunner(PipelineStep pipelineStep)
    {
        super(pipelineStep);

        this.bertStep = (BertStep) pipelineStep;

        //delete once confirmed
        String inputName = (String) this.bertStep.getInputNames().get(0);
        String outputName = (String) this.bertStep.getOutputNames().get(0);

        String modelPath = (String) this.bertStep.getModelPath();
        String vocabPath = (String) this.bertStep.getVocabPath();

        //delete once confirmed
        System.out.println( "BertStepRunner [InputName]: " + inputName );
        System.out.println( "BertStepRunner [outputName]: " + outputName );
        System.out.println( "BertStepRunner [modelpath]: " + modelPath );
        System.out.println( "BertStepRunner [vocabPath]: " + vocabPath );

        Map<String, SchemaType[]> inputSchema = this.bertStep.getInputSchemas();

        //load tokenizer
        try {

            this.tokenizer = new BertWordPieceTokenizerFactory(new File(vocabPath), true, true, StandardCharsets.UTF_8);

        } catch (IOException e)
        {
            throw new IllegalStateException("Vocabulary file missing");
        }

        //load Bert trained model
        try {

            this.bertModel = ModelSerializer.restoreComputationGraph(modelPath);

        } catch (IOException e)
        {
            throw new IllegalStateException("Model file failed loading");
        }
    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs){
        throw new UnsupportedOperationException();
    }

    public BertIterator getToken(String input)
    {
        List<String> inputList = Arrays.asList(input);
        List<String> labelList = Arrays.asList("default");

        LabeledSentenceProvider provider = new CollectionLabeledSentenceProvider(inputList, labelList, new Random(123));

        return BertIterator.builder()
                .tokenizer(this.tokenizer)
                .lengthHandling(BertIterator.LengthHandling.FIXED_LENGTH, this.MAX_LEN)
                .minibatchSize(1)
                .sentenceProvider(provider)
                .featureArrays(BertIterator.FeatureArrays.INDICES_MASK)
                .vocabMap(tokenizer.getVocab())
                .task(BertIterator.Task.SEQ_CLASSIFICATION)
                .build();
    }

    public INDArray getOutput(BertIterator iterator)
    {
        return this.bertModel.output(iterator)[0];
    }

    @Override
    public Record[] transform(Record[] input)
    {
        Preconditions.checkNotNull(input, "Input records were null!");

        List<Record> recordList = new ArrayList<>();

        for(Record record : input)
        {
            Text text = (Text) record.getRecord().get(0);

            if (text.toString().charAt(0) == '{')
            {
                JsonObject jsonObject = new JsonObject(text.toString());
                List<Writable> writables = new ArrayList<>();

                for (String field : jsonObject.fieldNames())
                {
                    String sentence = (String) jsonObject.getValue(field);

                    writables.add(WritableValueRetriever.writableFromValue(this.getOutput(this.getToken(sentence))));
                }

                recordList.add(new org.datavec.api.records.impl.Record(writables, null));
            }

        }

        Record[] records =  recordList.toArray(new Record[recordList.size()]);

        return records;
    }

}
