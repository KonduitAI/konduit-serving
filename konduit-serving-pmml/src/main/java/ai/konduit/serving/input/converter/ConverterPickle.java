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

package ai.konduit.serving.input.converter;

import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;
import org.dmg.pmml.PMML;
import org.jpmml.model.MetroJAXBUtil;
import org.jpmml.sklearn.PickleUtil;
import org.jpmml.sklearn.Storage;
import sklearn.Estimator;
import sklearn.pipeline.Pipeline;
import sklearn2pmml.pipeline.PMMLPipeline;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Collections;

/**
 * InputAdapter utility class
 * This contains many utilities
 * for interacting with pmml, including
 * unpickling and conversion to pmml
 * among other functionality.
 *
 *
 * @author Adam Gibson
 */
@Slf4j
public class ConverterPickle {


    /**
     * Write the pmml based on the given object.
     * The given object is converted to a {@link PMMLPipeline}
     *
     * @param object the input object
     * @return the object written as pmml
     * @throws Exception if an error writing the pmml occurs
     */
    public static Buffer writePmml(Object object) throws Exception {
        PMMLPipeline pmmlPipeline = null;
        if (!(object instanceof PMMLPipeline) && !(object instanceof PMML)) {

            // Create a single- or multi-step PMMLPipeline from a Pipeline
            if (object instanceof Pipeline) {
                Pipeline pipeline = (Pipeline) object;

                object = new PMMLPipeline()
                        .setSteps(pipeline.getSteps());
            } else

                // Create a single-step PMMLPipeline from an Estimator
                if (object instanceof Estimator) {
                    Estimator estimator = (Estimator) object;
                    object = new PMMLPipeline()
                            .setSteps(Collections.singletonList(new Object[]{"estimator", estimator}));

                }

            pmmlPipeline = (PMMLPipeline) object;

        }


        else if(!(object instanceof PMML)) {
            pmmlPipeline = (PMMLPipeline) object;
        }
        else if(object instanceof PMML) {
            PMML pmml = (PMML) object;
            return bufferForPmml(pmml);

        }

        PMML pmml;
        log.debug("Converting..");

        long begin = System.currentTimeMillis();
        pmml = pmmlPipeline.encodePMML();
        long end = System.currentTimeMillis();

        log.debug("Converted in {} ms.", (end - begin));

        return bufferForPmml(pmml);

    }


    /**
     * Return a {@link Buffer} containing the
     * xml of the pmml based on the
     * output of {@link MetroJAXBUtil#marshalPMML(PMML, OutputStream)}
     * @param pmml the pmml to write
     * @return the output buffer
     * @throws Exception if the buffer creation fails (such as invalid pmml)
     */
    public static Buffer bufferForPmml(PMML pmml) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            log.debug("Marshalling PMML..");
            MetroJAXBUtil.marshalPMML(pmml, os);
            Buffer writeTo = Buffer.buffer(os.toByteArray());
            return writeTo;
        }
    }


    /**
     * Unpickle the given file
     * @param tmpFile the file to unpickle
     * @return the unpickled python object
     * @throws Exception if the pickle de serialization fails
     */
    public static Object unpickle(File tmpFile) throws Exception {
        Object object = null;
        try(Storage storage = PickleUtil.createStorage(tmpFile)) {
            log.debug("Parsing PKL..");

            long start = System.currentTimeMillis();
            object = PickleUtil.unpickle(storage);
            long end = System.currentTimeMillis();

            log.debug("Parsed PKL in {} ms.", (end - start));
        }




        return object;
    }
}
