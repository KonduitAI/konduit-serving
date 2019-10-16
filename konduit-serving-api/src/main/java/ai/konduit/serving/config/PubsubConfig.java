/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * This configuration is for handling
 * endpoints that allow asynchronous responses
 * via a publish subscribe pattern.
 *
 * The basic idea is simple: You have an endpoint
 * that receives input and a url you send a post request to
 * with the expected output.
 *
 * The {@link #httpMethod} allow customization but generally should be set to POST.
 * {@link #contentType} is the expected content type of the input (usually application/json)
 * {@link #pubsubUrl} is the raw url (hostname, port, routes and all) that the data should be sent to.
 *
 * @author Adam Gibson
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PubsubConfig implements Serializable  {
    @Builder.Default
    private String httpMethod = "POST";
    private String pubsubUrl;
    @Builder.Default
    private String contentType = "application/json";

}
