/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.vertx.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An enum that determines the server type. <br><br>" +
        "HTTP -> starts an http server, <br>" +
        "MQTT -> starts an mqtt server, <br>" +
        "GRPC -> start a grpc server, <br>" +
        "KAFKA -> connect to a kafka message queue.")
public enum ServerProtocol {
    HTTP,
    MQTT,
    GRPC,
    KAFKA
}
