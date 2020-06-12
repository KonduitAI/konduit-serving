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
package ai.konduit.serving.build.deployments;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
    Corresponds to org.redline_rpm.header.Architecture class,
    mappping between KS and gradle plugins.
*/
@AllArgsConstructor
public enum Architecture {
    NOARCH("NOARCH"),
    I386("I386"),
    ALPHA("ALPHA"),
    SPARC("SPARC"),
    MIPS("MIPS"),
    PPC("PPC"),
    M68K("M68K"),
    IP("IP"),
    RS6000("RS6000"),
    IA64("IA64"),
    SPARC64("SPARC64"),
    MIPSEL("MIPSEL"),
    ARM("ARM"),
    MK68KMINT("MK68KMINT"),
    S390("S390"),
    S390X("S390X"),
    PPC64("PPC64"),
    SH("SH"),
    XTENSA("XTENSA"),
    X86_64("X86_64"),
    PPC64LE("PPC64LE"),
    AARCH64("AARCH64");

    @Getter
    private String data;
}
