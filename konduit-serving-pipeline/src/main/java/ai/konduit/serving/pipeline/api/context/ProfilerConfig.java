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
package ai.konduit.serving.pipeline.api.context;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.File;
import java.nio.file.Path;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
public class ProfilerConfig {
    @Getter
    private Path outputFile;
    @Getter
    private long splitSize;

    public ProfilerConfig outputFile(File f){
        this.outputFile = f.toPath();
        return this;
    }

    public ProfilerConfig outputFile(Path p){
        this.outputFile = p;
        return this;
    }
}
