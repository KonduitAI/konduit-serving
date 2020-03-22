/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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
import ai.konduit.serving.pipeline.step.WordTokenizerStep;
import ai.konduit.serving.util.WritableValueRetriever;
import io.vertx.core.json.JsonObject;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.iterator.BertIterator;
import org.deeplearning4j.iterator.LabeledSentenceProvider;
import org.deeplearning4j.iterator.provider.CollectionLabeledSentenceProvider;
import org.deeplearning4j.text.tokenization.tokenizerfactory.BertWordPieceTokenizerFactory;
import org.nd4j.base.Preconditions;
import org.datavec.api.writable.Text;
import org.nd4j.linalg.dataset.api.MultiDataSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WordTokenizerStepRunner extends BaseStepRunner
{
    private BertWordPieceTokenizerFactory tokenizer;
    private WordTokenizerStep tokenizerStep;
    private int sentenceMaxLen;

    public WordTokenizerStepRunner(PipelineStep pipelineStep)
    {
        super(pipelineStep);

        this.tokenizerStep = (WordTokenizerStep) pipelineStep;

        String vocabPath = this.tokenizerStep.getVocabPath();
        this.sentenceMaxLen = this.tokenizerStep.getSentenceMaxLen();

        //load tokenizer
        try {

            this.tokenizer = new BertWordPieceTokenizerFactory(new File(vocabPath), true, true, StandardCharsets.UTF_8);

        } catch (IOException e)
        {
            throw new IllegalStateException("Failed to create BertWordPieceTokenizerFactory", e);
        }

    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs){
        throw new UnsupportedOperationException();
    }

    public BertIterator getToken(String input)
    {
        List<String> inputList = Collections.singletonList(input);
        List<String> labelList = Collections.singletonList("default");

        LabeledSentenceProvider provider = new CollectionLabeledSentenceProvider(inputList, labelList, new Random(123));

        return BertIterator.builder()
                .tokenizer(this.tokenizer)
                .lengthHandling(BertIterator.LengthHandling.FIXED_LENGTH, this.sentenceMaxLen)
                .minibatchSize(1)
                .sentenceProvider(provider)
                .featureArrays(BertIterator.FeatureArrays.INDICES_MASK)
                .vocabMap(tokenizer.getVocab())
                .task(BertIterator.Task.SEQ_CLASSIFICATION)
                .build();
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
                List<Writable> featureWritable = new ArrayList<>();
                List<Writable> featureMaskWritable = new ArrayList<>();

                for (String field : jsonObject.fieldNames())
                {
                    String sentence = (String) jsonObject.getValue(field);

                    MultiDataSet mds = this.getToken(sentence).next();
                    featureWritable.add(WritableValueRetriever.writableFromValue(mds.getFeatures(0)));
                    featureMaskWritable.add(WritableValueRetriever.writableFromValue(mds.getFeaturesMaskArray(0)));
                }

                recordList.add(new org.datavec.api.records.impl.Record(featureWritable, null));
                recordList.add(new org.datavec.api.records.impl.Record(featureMaskWritable, null));
            }

        }

        return recordList.toArray(new Record[recordList.size()]);
    }

}
