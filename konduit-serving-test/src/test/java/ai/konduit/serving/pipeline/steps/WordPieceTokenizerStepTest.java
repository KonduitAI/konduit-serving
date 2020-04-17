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

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.pipeline.step.WordPieceTokenizerStep;
import ai.konduit.serving.util.PortUtils;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.iterator.BertIterator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nd4j.linalg.io.ClassPathResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class WordPieceTokenizerStepTest
{
    protected static InferenceConfiguration inferenceConfiguration;
    protected static WordPieceTokenizerStep wordPieceTokenizerStep;

    @BeforeClass
    public static void init() throws Exception
    {
        int port = PortUtils.getAvailablePort();

        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .build();

        ClassPathResource classPathResource = new ClassPathResource("vocab/bert-base-uncased-vocab.txt");

        wordPieceTokenizerStep = WordPieceTokenizerStep.builder()
                .vocabPath(classPathResource.getFile().getAbsolutePath())
                .sentenceMaxLen(100)
                .inputName("default")
                .inputSchema("default", Collections.singletonList(SchemaType.String))
                .inputColumnName("default", Collections.singletonList("first"))
                .outputSchema("default", Collections.singletonList(SchemaType.NDArray))
                .outputColumnName("default", Collections.singletonList("first"))
                .build();

        inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .steps(Collections.singletonList(wordPieceTokenizerStep))
                .build();
    }

    @Test
    public void testWordPieceJsonSerialization() throws Exception
    {
        assertNotNull(inferenceConfiguration.toJson());
    }

    @Test
    public void testWordPieceJsonSanityCheck() throws Exception
    {
        assertEquals(inferenceConfiguration,InferenceConfiguration.fromJson(inferenceConfiguration.toJson()));
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidVocabPath() throws Exception
    {
        WordPieceTokenizerStep invalidStep = WordPieceTokenizerStep.builder()
                .vocabPath("sample/path/vocab.txt")
                .sentenceMaxLen(100)
                .build();

        WordPieceTokenizerStepRunner step = new WordPieceTokenizerStepRunner(invalidStep);
    }

    @Test
    public void testWordPieceStepInference() throws Exception
    {
        String sampleText = "These pages provide further information about the dictionary, its content and how it's kept up-to-date.";

        WordPieceTokenizerStepRunner step = new WordPieceTokenizerStepRunner(wordPieceTokenizerStep);

        BertIterator iterator = step.getToken(sampleText);

        assertNotEquals(0, iterator.next().getFeatures(0).length());

        List<Writable> ret = new ArrayList<>();
        ret.add(new Text(sampleText));

        Record[] tokenizedSentence = step.transform(new Record[]{
                new org.datavec.api.records.impl.Record(ret, null)
        });

        assertEquals(1, tokenizedSentence.length);
    }
}
