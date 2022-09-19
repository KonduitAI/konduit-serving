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

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.common.base.Preconditions;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@Accessors(fluent = true)
@JsonName("DOCUMENTPARSER")
@Schema(description = "A pipeline step that configures a tika parser")
public class DocumentParserStep implements PipelineStep {

    @Schema(description = "A list of names of the input placeholders ( computation graph, with multiple inputs. Where values from the input data keys are mapped to " +
            "the computation graph inputs).")
    private List<String> inputNames;

    @Schema(description = "A list of names of the output placeholders (computation graph, with multiple outputs. Where the values of these output keys are mapped " +
            "from the computation graph output - INDArray[] to data keys).")
    private List<String> outputNames;
    @Schema(description = "A list of parser types: tika,pdfbox")
    private List<String> parserTypes;
    @Schema(description = "A list of parser types: tika,pdfbox")
    private List<String> tableRowExtractorTypes;
    @Schema(description = "A list of selectors for table extraction from html")
    private List<String> selectors;
    @Schema(description = "A list of field names per input/output for resolving rows where the column name is on its own with the value being in another row.")
    private List<List<String>> fieldNames;
    @Schema(description = "A list of field names per input/output for resolving rows where the column name is on the same line as the value")
    private List<List<String>> partialFieldNames;
    @Schema(description = "A list of table titles to look for")
    private List<String> tableKeys;

    public DocumentParserStep() {
    }

    public DocumentParserStep(
            @JsonProperty("inputNames") List<String> inputNames,
            @JsonProperty("outputNames") List<String> outputNames,
            @JsonProperty("parserTypes") List<String> parserTypes,
            @JsonProperty("tableRowExtractorTypes") List<String> tableRowExtractorTypes,
            @JsonProperty("selectors") List<String> selectors,
            @JsonProperty("fieldNames") List<List<String>> fieldNames,
            @JsonProperty("tableKeys") List<String> tableKeys,
            @JsonProperty("partialFieldNames") List<List<String>> partialFieldNames) {
        this.inputNames = inputNames;
        this.outputNames = outputNames;
        this.parserTypes = parserTypes;
        this.tableRowExtractorTypes = tableRowExtractorTypes;
        this.selectors = selectors;
        this.fieldNames = fieldNames;
        this.tableKeys = tableKeys;
        this.partialFieldNames = partialFieldNames;
        Preconditions.checkState(inputNames != null && outputNames != null && inputNames.size() == outputNames.size(),"Input names and output names must be specified and must be the same size.");
        /**
         * TODO: per document mappers maybe based on document type from input
         * and a map that says which one to use
         * TODO: could break down in to tables with types
         * in a similar manner where each table is an output
         */
        if(tableRowExtractorTypes != null) {
            Preconditions.checkState(selectors != null && selectors.size() == tableRowExtractorTypes.size(),"Table row extractor and selectors must be same size!");
            Preconditions.checkState(tableRowExtractorTypes.size() == inputNames.size(),"Table row extractors if defined must be equal to the number of input names.");
        }
    }
}
