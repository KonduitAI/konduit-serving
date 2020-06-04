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

package ai.konduit.serving.build.config;

import ai.konduit.serving.build.deployments.UberJarDeployment;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

@JsonSubTypes({
        @JsonSubTypes.Type(value = UberJarDeployment.class, name = "uberjar"),
})
@JsonTypeInfo(use = NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public interface Deployment {

    String JAR = "JAR";
    String UBERJAR = "UBERJAR";
    String DOCKER = "DOCKER";
    String EXE = "EXE";
    String WAR = "WAR";
    String RPM = "RPM";
    String DEB = "DEB";
    String TAR = "TAR";


    List<String> propertyNames();

    Map<String,String> asProperties();

    void fromProperties(Map<String,String> props);

    /**
     * Validate the deployment configuration before the deployment build is attempted
     * Used to detect obvious problems such as "output location is not set" etc
     */
    DeploymentValidation validate();

    /**
     * Summary output string after the build completes
     * i.e., info about the output after the build has completed
     */
    String outputString();

}
