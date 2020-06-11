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
    Corresponds to org.redline_rpm.header.Os class, mappping between KS and gradle plugins.
 */
@AllArgsConstructor
public enum Os {

    UNKNOWN("UNKNOWN"),
    LINUX("LINUX"),
    IRIX("IRIX"),
    SOLARIS("SOLARIS"),
    SUNOS("SUNOS"),
    AMIGAOS("AMIGAOS"),
    AIX("AIX"),
    HPUX10("HPUX10"),
    OSF1("OSF1"),
    FREEBSD("FREEBSD"),
    SCO("SCO"),
    IRIX64("IRIX64"),
    NEXTSTEP("NEXTSTEP"),
    BSDI("BSDI"),
    MACHTEN("MACHTEN"),
    CYGWINNT("CYGWINNT"),
    CYGWIN95("CYGWIN95"),
    UNIXSV("UNIXSV"),
    MINT("MINT"),
    OS390("OS390"),
    VMESA("VMESA"),
    LINUX390("LINUX390"),
    MACOSX("MACOSX");

    @Getter
    private String data;
}
