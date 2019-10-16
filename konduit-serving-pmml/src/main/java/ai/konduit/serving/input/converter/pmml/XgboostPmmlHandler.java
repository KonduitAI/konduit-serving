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

package ai.konduit.serving.input.converter.pmml;

import ai.konduit.serving.input.converter.ConverterPickle;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.io.FileUtils;
import org.dmg.pmml.PMML;
import org.jpmml.xgboost.FeatureMap;
import org.jpmml.xgboost.Learner;
import org.jpmml.xgboost.XGBoostUtil;
import org.nd4j.util.ArchiveUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * PMML handler for xgboost
 * An xgboost pmml handler takes in 2 args:
 * A  zip file containing:
 *     A file for the {@link Learner} named learner.model
 *    A file for the     {@link FeatureMap} named features.fmap
 *
 * and a configuration of an integer representing
 * the tree limit.
 *
 * The handler should take in an extract directory
 * where the contents of the zip file will be extracted
 * to temporarily for loading.
 *
 * @author Adam Gibson
 */
public class XgboostPmmlHandler extends BasePmmlHandler {

    private File extractDir;
    private int nTreeLimit;

    public XgboostPmmlHandler(File extractDir) {
        this.extractDir = extractDir;
    }

    @Override
    public Buffer getPmmlBuffer(RoutingContext routingContext, Object... otherInputs) throws Exception {
        Learner learner;
        File tmpFile = (File) otherInputs[0];
        String tmpDirId = UUID.randomUUID().toString();
        File tmpDir = new File(extractDir,tmpDirId);
        tmpDir.mkdirs();
        tmpFile.renameTo(new File(tmpFile.getParent(),tmpDir.getName() + ".zip"));
        tmpFile = new File(tmpFile.getParent(),tmpDir.getName() + ".zip");

        ArchiveUtils.unzipFileTo(tmpFile.getAbsolutePath(),tmpDir.getAbsolutePath());

        try(InputStream is = new FileInputStream(new File(tmpDir,"learner.model"))) {
            learner = XGBoostUtil.loadLearner(is);
        }

        FeatureMap featureMap;

        try(InputStream is = new FileInputStream(new File(tmpDir,"features.fmap"))) {
            featureMap = XGBoostUtil.loadFeatureMap(is);
        }

        Integer ntreeLimit = otherInputs.length >= 2 ? (Integer) otherInputs[1] : null;

        PMML pmml = learner.encodePMML(null, null, featureMap, ntreeLimit, (ntreeLimit != null));
        FileUtils.deleteDirectory(tmpDir);
        return ConverterPickle.bufferForPmml(pmml);

    }

    @Override
    public Object[] getExtraArgs(RoutingContext req) {
        return new Object[]{getTmpFileWithContext(req),nTreeLimit};
    }
}
