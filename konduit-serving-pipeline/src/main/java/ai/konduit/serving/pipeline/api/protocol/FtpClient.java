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

package ai.konduit.serving.pipeline.api.protocol;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.PrintWriter;

public class FtpClient implements NetClient {

    private FTPClient ftp = new FTPClient();
    private int port = 21;

    @Override
    public void connect(String host) throws IOException {
        ftp.connect("localhost", port);
        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
    }

    @Override
    public boolean login(String user, String password) throws IOException {
        return ftp.login(user, password);
    }
}
