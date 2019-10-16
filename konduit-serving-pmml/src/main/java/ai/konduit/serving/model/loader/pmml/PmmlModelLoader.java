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

package ai.konduit.serving.model.loader.pmml;

import ai.konduit.serving.model.loader.ModelLoader;
import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.model.PMMLUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Data
@AllArgsConstructor

public class PmmlModelLoader implements ModelLoader<Evaluator> {

    private ModelEvaluatorFactory modelEvaluatorFactory;
    private File pmmlFile;


    @Override
    public Buffer saveModel(Evaluator model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Evaluator loadModel() throws Exception {
        PMML pmml;
        try(InputStream is = new FileInputStream(pmmlFile)) {
            pmml =  PMMLUtil.unmarshal(is);
        }


        Evaluator evaluator = modelEvaluatorFactory.newModelEvaluator(pmml);
        return evaluator;
    }
}
