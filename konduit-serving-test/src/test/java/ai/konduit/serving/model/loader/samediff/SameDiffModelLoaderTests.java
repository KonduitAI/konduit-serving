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

package ai.konduit.serving.model.loader.samediff;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class SameDiffModelLoaderTests {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();


    @Test(timeout = 60000)

    @Ignore
    public void testSameDiffModels() throws Exception {
        File tfGraphFolder = temporary.newFolder();
        // TODO: tf_graphs does not exist
        ClassPathResource graphFolder = new ClassPathResource("/tf_graphs");
        assertThat(graphFolder.exists(),is(true));
        graphFolder.copyDirectory(tfGraphFolder);

        String[] filesToLoad = {
                "lenet_frozen.pb",
                "/examples/lstm_mnist/frozen_model.pb",
                //"examples/yolov2_608x608/frozen_model.pb",
                //"examples/ssd_inception_v2_coco_2018_01_28/frozen_inference_graph.pb"
                //"examples/ssd_mobilenet_v1_coco/frozen_model.pb"


        };

        for(String file : filesToLoad) {
            File tfGraphFile = new File(tfGraphFolder,file);
            SameDiffModelLoader sameDiffModelLoader = new SameDiffModelLoader(tfGraphFile);
            SameDiff sameDiff = sameDiffModelLoader.loadModel();
            assertThat(sameDiff.variables().size(),is(greaterThan(0)));

        }

    }

}
