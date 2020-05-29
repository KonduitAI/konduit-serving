/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.data.nd4j;

import ai.konduit.serving.annotation.module.Dependency;
import ai.konduit.serving.annotation.module.ModuleInfo;
import ai.konduit.serving.annotation.module.Requires;
import ai.konduit.serving.annotation.module.RequiresDependencies;

@ModuleInfo("konduit-serving-nd4j")
@RequiresDependencies({
        //ND4J backend - need ONE of these:
        @Requires({@Dependency(gId = "org.nd4j", aId = "nd4j-native", ver = "1.0.0-beta7"),
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.0", ver = "1.0.0-beta7"),
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.1", ver = "1.0.0-beta7"),
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.2", ver = "1.0.0-beta7")}),
        //ND4J backend classifier - need ONE of these
        @Requires({@Dependency(gId = "org.nd4j", aId = "nd4j-native", ver = "1.0.0-beta7", classifier = {
                "linux-x86_64", "linux-x86_64-avx2", "linux-x86_64-avx512", "linux-ppc64le", "linux-arm64", "linux-armhf",
                "windows-x86_64", "windows-x86_64-avx2",
                "macosx-x86_64", "macosx-x86_64-avx2"}),        //TODO 2020/05/29 AB - Not including android classifiers here as we don't yet support that for KS
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.0", ver = "1.0.0-beta7", classifier = {"linux-x86_64", "linux-ppc64le", "linux-arm64","windows-x86_64"}),
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.1", ver = "1.0.0-beta7", classifier = {"linux-x86_64", "linux-ppc64le", "linux-arm64","windows-x86_64"}),
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.2", ver = "1.0.0-beta7", classifier = {"linux-x86_64", "linux-ppc64le", "linux-arm64","windows-x86_64"})})
})
public class Nd4jModuleInfo {
    private Nd4jModuleInfo(){ }
}
