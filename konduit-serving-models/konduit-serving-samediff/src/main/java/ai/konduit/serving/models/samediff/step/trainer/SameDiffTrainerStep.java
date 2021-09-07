/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.models.samediff.step.trainer;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.experimental.Tolerate;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

@SuperBuilder
@Data
@Accessors(fluent = true)
@JsonName("SAMEDIFF_TRAIN")
@NoArgsConstructor
@Schema(description = "A pipeline step that configures a SameDiff model that is to be executed.")
public class SameDiffTrainerStep implements PipelineStep {

    @Schema(description = "Specifies the location of a saved model file.")
    private String modelUri;
    @Schema(description = "An L1 regularization coefficient for application during training.  Set this value for l1 regularization. Not applied by default.")
    private double l1 = -1.0;
    @Schema(description = "An L2 regularization coefficient for application during training. Set this value for l2 regularization. Not applied by default.")
    private double l2 = -1.0;
    @Schema(description = "A weight regularization coefficient for application during training. Set this value to enable weight decay. Disabled byd efault.")
    private double weightDecayCoefficient;
    @Schema(description = "Whether to apply learning rate during weight decay,defaults to true")
    private boolean weightDecayApplyLearningRate = true;

    @Schema(description = "Specifies the location of the model save path")
    private String modelSaveOutputPath;
    @Schema(description = "Specifies the number of epochs to run training for")
    private int numEpochs = 1;
    @Schema(description = "A list of names of the loss variables- the names of the targets to train against for the loss function")
    private List<String> lossVariables;
    @Schema(description = "The updater to use for training")
    private IUpdater updater;

    public SameDiffTrainerStep(@JsonProperty("modelUri") String modelUri,
                               @JsonProperty("l1") double l1,
                               @JsonProperty("l2") double l2,
                               @JsonProperty("modelSaveOutputPath") String modelSaveOutputPath,
                               @JsonProperty("numEpochs") int numEpochs,
                               @JsonProperty("lossVariables") List<String> lossVariables,
                               @JsonProperty("weightDecayCoefficient") double weightDecayCoefficient,
                               @JsonProperty("weightDecayApplyLearningRate") boolean weightDecayApplyLearningRate,
                               @JsonProperty("updater") IUpdater updater) {
        this.modelUri = modelUri;
        this.l1 = l1;
        this.l2 = l2;
        this.modelSaveOutputPath = modelSaveOutputPath;
        this.numEpochs = numEpochs;
        this.lossVariables = lossVariables;
        this.weightDecayApplyLearningRate = weightDecayApplyLearningRate;
        this.weightDecayCoefficient = weightDecayCoefficient;
        this.updater = updater;
    }

}
