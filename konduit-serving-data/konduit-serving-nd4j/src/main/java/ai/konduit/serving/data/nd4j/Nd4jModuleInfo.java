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

import ai.konduit.serving.annotation.module.*;

@ModuleInfo("konduit-serving-nd4j")
@RequiresDependenciesAny({
        //Requires ND4J native + one of the classifiers
        @Requires(requires = Req.ALL, value = {@Dependency(gId = "org.nd4j", aId = "nd4j-native", ver = "1.0.0-beta7"),
                @Dependency(gId = "org.nd4j", aId = "nd4j-native", ver = "1.0.0-beta7", classifier = {
                        "linux-x86_64", "linux-x86_64-avx2", "linux-x86_64-avx512", "linux-ppc64le", "linux-arm64", "linux-armhf",
                        "windows-x86_64", "windows-x86_64-avx2",
                        "macosx-x86_64", "macosx-x86_64-avx2"}, cReq = Req.ANY)     //TODO 2020/05/29 AB - Not including android classifiers here as we don't yet support that for KS
        }),

        //OR it requires CUDA 10.0, 10.1 or 10.2 + one of the classifiers
        @Requires(requires = Req.ALL, value = {@Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.0", ver = "1.0.0-beta7"),
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.0", ver = "1.0.0-beta7", classifier = {"linux-x86_64", "linux-ppc64le", "linux-arm64","windows-x86_64"}),
                @Dependency(gId = "org.bytedeco", aId = "cuda", ver = "10.2-7.6-1.5.3", classifier = {"linux-x86_64", "linux-ppc64le", "windows-x86_64"}),
                @Dependency(gId = "org.bytedeco", aId = "cuda", ver = "10.0-7.4-1.5", classifier = {"linux-x86_64", "linux-ppc64le", "windows-x86_64"})}),
        @Requires(requires = Req.ALL, value = {@Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.1", ver = "1.0.0-beta7"),
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.1", ver = "1.0.0-beta7", classifier = {"linux-x86_64", "linux-ppc64le", /*"linux-arm64",*/ "windows-x86_64"}),      //Note 1.0.0-beta7 was only released for linux-arm64 for CUDA 10.0
                @Dependency(gId = "org.bytedeco", aId = "cuda", ver = "10.1-7.6-1.5.2", classifier = {"linux-x86_64", "linux-ppc64le", "windows-x86_64"})}),
        @Requires(requires = Req.ALL, value = {@Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.2", ver = "1.0.0-beta7"),
                @Dependency(gId = "org.nd4j", aId = "nd4j-cuda-10.2", ver = "1.0.0-beta7", classifier = {"linux-x86_64", "linux-ppc64le", /*"linux-arm64",*/ "windows-x86_64"}),
                @Dependency(gId = "org.bytedeco", aId = "cuda", ver = "10.2-7.6-1.5.3", classifier = {"linux-x86_64", "linux-ppc64le", "windows-x86_64"})})
})
public class Nd4jModuleInfo {
    private Nd4jModuleInfo(){ }
}
