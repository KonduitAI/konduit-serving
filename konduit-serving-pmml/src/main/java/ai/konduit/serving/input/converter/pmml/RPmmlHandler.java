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
import org.dmg.pmml.PMML;
import org.jpmml.rexp.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Pmml converter for r.
 * Current types included:
 *      ADA,
 ADABAG,
 BAGGING,
 BINARYTREE,
 BOOSTING,
 CARET,
 CHAID,
 CROSSVALGLMNET,
 EARTH,
 ELMNN,
 EVTREE,
 GBM,
 GLM,
 GLMNET,
 IFOREST,
 KMEANS,
 LM,
 LRM,
 MVR,
 MULTINOM,
 NN,
 NNet,
 NAIVEBAYES,
 OLS,
 PARTY,
 RANDOMFOREST,
 SVM,
 XGBOOST


 @author Adam Gibson
 */
public class RPmmlHandler extends BasePmmlHandler {



    public final static String CONVERTER_TYPE = "converterType";

    public enum RConverterType {
        ADA,
        ADABAG,
        BAGGING,
        BINARYTREE,
        BOOSTING,
        CARET,
        CHAID,
        CROSSVALGLMNET,
        EARTH,
        ELMNN,
        EVTREE,
        GBM,
        GLM,
        GLMNET,
        IFOREST,
        KMEANS,
        LM,
        LRM,
        MVR,
        MULTINOM,
        NN,
        NNet,
        NAIVEBAYES,
        OLS,
        PARTY,
        RANDOMFOREST,
        SVM,
        XGBOOST
    }

    private  Class<? extends Converter<? extends RExp>> converterForType(RConverterType converterType) {
        switch(converterType) {
            case ADA: return AdaConverter.class;
            case ADABAG: return AdaBagConverter.class;
            case BAGGING: return BaggingConverter.class;
            case BINARYTREE: return BinaryTreeConverter.class;
            case BOOSTING: return BoostingConverter.class;
            case CARET: return CaretEnsembleConverter.class;
            case CROSSVALGLMNET: return CrossValGLMNetConverter.class;
            case EARTH: return EarthConverter.class;
            case ELMNN: return ElmNNConverter.class;
            case GBM: return GBMConverter.class;
            case GLM: return GLMConverter.class;
            case GLMNET: return GLMNetConverter.class;
            case IFOREST: return IForestConverter.class;
            case KMEANS: return KMeansConverter.class;
            case LM: return LMConverter.class;
            case LRM: return LRMConverter.class;
            case MVR: return MVRConverter.class;
            case MULTINOM: return MultinomConverter.class;
            case NN: return NNConverter.class;
            case NNet: return NNetConverter.class;
            case NAIVEBAYES: return NaiveBayesConverter.class;
            case OLS: return OLSConverter.class;
            case PARTY: return PartyConverter.class;
            case RANDOMFOREST: return RandomForestConverter.class;
            case SVM: return SVMConverter.class;
            case XGBOOST: return XGBoostConverter.class;
            default: throw new IllegalArgumentException("Illegal converter type");

        }
    }



    @Override
    public Buffer getPmmlBuffer(RoutingContext routingContext, Object... otherInputs) throws Exception {
        File inputFile = (File) otherInputs[0];
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile))) {
            RExpParser parser = new RExpParser(bis);
            RExp rexp = parser.parse();
            ConverterFactory converterFactory = ConverterFactory.newInstance();
            String converterName = otherInputs.length > 1 ? (String) otherInputs[1] : null;
            Converter<RExp> converter;
            Class<? extends Converter<? extends RExp>> clazz = converterName != null ?
                    converterForType(RConverterType.valueOf(converterName.toUpperCase()))
                    : null;

            if(clazz != null) {
                converter = converterFactory.newConverter(clazz, rexp);
            } else {
                converter = converterFactory.newConverter(rexp);
            }

            PMML pmml =  converter.encodePMML();
            Buffer writeBuffer = ConverterPickle.writePmml(pmml);
            return  writeBuffer;
        }
    }

    @Override
    public Object[] getExtraArgs(RoutingContext req) {
        String param = req.pathParam(CONVERTER_TYPE);
        return new Object[]{getTmpFileWithContext(req),param};
    }
}
