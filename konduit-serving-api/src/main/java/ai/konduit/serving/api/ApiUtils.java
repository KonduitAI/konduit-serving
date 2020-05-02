/*
 * *****************************************************************************
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
 * ****************************************************************************
 */

package ai.konduit.serving.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.ws.rs.core.Response;
import java.util.function.Function;
import java.util.function.Supplier;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiUtils {

    /**
     * General response pattern with error handling that takes a {@link Function} as an executable code.
     * @param responseSupplier the function to try error handling with. This can be any executable code.
     * @return the final response
     */
    public static Response sendGeneralResponse(Supplier<Response> responseSupplier) {
        try {
            return responseSupplier.get();
        } catch (Exception exception) {
            if(exception.getMessage() != null) {
                return Response.status(INTERNAL_SERVER_ERROR.getStatusCode(), exception.getMessage()).build();
            } else {
                return Response.status(INTERNAL_SERVER_ERROR.getStatusCode(), exception.toString()).build();
            }
        }
    }

    /**
     * General response pattern with error handling.
     * @param entity The object entity to send as a response
     * @return the final response
     */
    public static Response sendGeneralResponse(Object entity) {
        return sendGeneralResponse(() -> Response.ok(entity).build());
    }
}
