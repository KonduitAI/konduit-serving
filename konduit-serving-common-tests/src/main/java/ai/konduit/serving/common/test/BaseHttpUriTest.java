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

package ai.konduit.serving.common.test;


import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

public abstract class BaseHttpUriTest {

    public TestServer server;
    public final static int PORT = 9090;
    public final static String HOST = "localhost";
    public static final String HTTP = "http://";
    public static final String HTTPS = "http://";
    public static final String FTP = "ftp://";

    public static boolean setStreamHandler = false;
    public abstract URLStreamHandlerFactory streamHandler();

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    //The directory that files can be placed in to host from
    public File httpDir;

    @Before
    public void setUp() throws Exception {
        if(!setStreamHandler){
            URL.setURLStreamHandlerFactory(streamHandler());
            setStreamHandler = true;
        }

        httpDir = testDir.newFolder();
        server = new TestServer(PORT, httpDir);
        server.start();

    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

}
