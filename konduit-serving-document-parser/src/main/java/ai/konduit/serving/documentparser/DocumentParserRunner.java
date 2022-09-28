/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.documentparser;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.fit.pdfdom.PDFDomTree;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@CanRun(DocumentParserStep.class)
@Slf4j
public class DocumentParserRunner implements PipelineStepRunner {

    private DocumentParserStep tikaStep;
    private Tika tika;
    private PDFTextStripper pdfTextStripper;
    private PDFDomTree pdfDomTree;
    private List<TableExtractor> tableExtractors;

    @SneakyThrows
    public DocumentParserRunner(DocumentParserStep documentParserStep) {
        this.tikaStep = documentParserStep;
        tika = new Tika();
        pdfTextStripper = new PDFTextStripper();
        pdfDomTree = new PDFDomTree();
        if(documentParserStep.tableRowExtractorTypes() != null) {
            tableExtractors = new ArrayList<>();
            for(int i = 0; i < documentParserStep.tableRowExtractorTypes().size(); i++) {
                tableExtractors.add(new DivRowBased(documentParserStep.selectors().get(i),
                        this.tikaStep.fieldNames().get(i),
                        this.tikaStep.partialFieldNames().get(i),
                        this.tikaStep.tableSpecificFieldNames()));
            }
        }
    }



    @Override
    public void close() {
    }

    @Override
    public PipelineStep getPipelineStep() {
        return tikaStep;
    }

    @SneakyThrows
    @Override
    public Data exec(Context ctx, Data data) {
        Data ret = Data.empty();
        for(int i = 0; i < tikaStep.inputNames().size(); i++) {
            byte[] bytes = data.getBytes(tikaStep.inputNames().get(i));
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            if(tikaStep.parserTypes().get(i).equals("pdfbox")) {
                PDDocument doc = PDDocument.load(byteArrayInputStream);
                StringWriter stringWriter = new StringWriter();
                pdfDomTree.writeText(doc,stringWriter);
                String content = stringWriter.toString();
                if(tikaStep.tableRowExtractorTypes() != null)
                    ret.put(tikaStep.outputNames().get(i), ObjectMappers.toJson(tableExtractors.get(i)
                            .extract(content,tikaStep.tableKeys())));
                else
                    ret.put(tikaStep.outputNames().get(i),content);
                doc.close();
            } else {
                Tika tika = new Tika();
                String content = tika.parseToString(byteArrayInputStream);
                if(tikaStep.tableRowExtractorTypes() != null)
                    ret.put(tikaStep.outputNames().get(i), ObjectMappers.toJson(
                            tableExtractors.get(i).extract(content,
                            tikaStep.tableKeys())));
                else
                    ret.put(tikaStep.outputNames().get(i),content);
            }
        }


        return ret;
    }
}
