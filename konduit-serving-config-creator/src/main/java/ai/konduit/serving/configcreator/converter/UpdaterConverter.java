/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator.converter;

import ai.konduit.serving.configcreator.StringSplitter;
import org.nd4j.linalg.learning.config.*;
import org.nd4j.linalg.schedule.ISchedule;
import picocli.CommandLine;

import java.util.Map;

public class UpdaterConverter implements CommandLine.ITypeConverter<IUpdater> {
    public final static String DELIMITER = ",";
    public final static String UPDATER_TYPE_KEY = "type";

    public enum UpdaterTypes {
        AMSGRAD,
        ADABELIEF,
        ADAGRAD,
        ADADELTA,
        ADAMAX,
        ADAM,
        NADAM,
        NESTEROVS,
        NOOP,
        RMSPROP,
        SGD

    }

    @Override
    public IUpdater convert(String value) throws Exception {
        StringSplitter stringSplitter = new StringSplitter(DELIMITER);
        Map<String,String> result = stringSplitter.splitResult(value);

        if(!result.containsKey(UPDATER_TYPE_KEY)) {
            throw new IllegalArgumentException("Please specify an updater type for proper creation.");
        }


        IUpdater updater = instanceForName(result.get(UPDATER_TYPE_KEY));
        setValuesFor(updater,result);
        return updater;
    }

    private void setValuesFor(IUpdater updater,Map<String,String> valuesToSet) throws Exception {
        for(Map.Entry<String,String> field : valuesToSet.entrySet()) {
            if(updater instanceof Sgd) {
                Sgd sgd = (Sgd) updater;
                if(field.getKey().equals("learningRate")) {
                    Double rate = Double.parseDouble(field.getValue());
                    sgd.setLearningRate(rate);
                }

                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    sgd.setLearningRateSchedule(convert);
                }

            } else if(updater instanceof RmsProp) {
                RmsProp rmsProp = (RmsProp) updater;
                if(field.getKey().equals("epsilon")) {
                    rmsProp.setEpsilon(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("learningRate")) {
                    rmsProp.setLearningRate(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("rmsDecay")) {
                    rmsProp.setRmsDecay(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    rmsProp.setLearningRateSchedule(convert);
                }

            } else if(updater instanceof AMSGrad) {
                AMSGrad amsGrad = (AMSGrad) updater;
                if(field.getKey().equals("beta1")) {
                    amsGrad.setBeta1(Double.parseDouble(field.getValue()));
                }
                if(field.getKey().equals("beta2")) {
                    amsGrad.setBeta2(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("epsilon")) {
                    amsGrad.setEpsilon(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("learningRate")) {
                    Double rate = Double.parseDouble(field.getValue());
                    amsGrad.setLearningRate(rate);
                }

                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    amsGrad.setLearningRateSchedule(convert);
                }

            } else if(updater instanceof AdaDelta) {
                AdaDelta adaDelta = (AdaDelta) updater;
                if(field.getKey().equals("epsilon")) {
                    adaDelta.setEpsilon(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("rho")) {
                    adaDelta.setRho(Double.parseDouble(field.getValue()));
                }



            } else if(updater instanceof NoOp) {
                NoOp noOp = (NoOp) updater;
            } else if(updater instanceof AdaGrad) {
                AdaGrad adaGrad = (AdaGrad) updater;


                if(field.getKey().equals("learningRate")) {
                    Double rate = Double.parseDouble(field.getValue());
                    adaGrad.setLearningRate(rate);
                }

                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    adaGrad.setLearningRateSchedule(convert);
                }

                if(field.getKey().equals("epsilon")) {
                    adaGrad.setEpsilon(Double.parseDouble(field.getValue()));
                }

            } else if(updater instanceof Adam) {
                Adam adam = (Adam) updater;
                if(field.getKey().equals("beta1")) {
                    adam.setBeta1(Double.parseDouble(field.getValue()));
                }
                if(field.getKey().equals("beta2")) {
                    adam.setBeta2(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("epsilon")) {
                    adam.setEpsilon(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("learningRate")) {
                    Double rate = Double.parseDouble(field.getValue());
                    adam.setLearningRate(rate);
                }

                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    adam.setLearningRateSchedule(convert);
                }

            } else if(updater instanceof AdaMax) {
                AdaMax adaMax = (AdaMax) updater;
                if(field.getKey().equals("beta1")) {
                    adaMax.setBeta1(Double.parseDouble(field.getValue()));
                }
                if(field.getKey().equals("beta2")) {
                    adaMax.setBeta2(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("epsilon")) {
                    adaMax.setEpsilon(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("learningRate")) {
                    Double rate = Double.parseDouble(field.getValue());
                    adaMax.setLearningRate(rate);
                }

                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    adaMax.setLearningRateSchedule(convert);
                }


            } else if(updater instanceof AdaBelief) {
                AdaBelief adaBelief = (AdaBelief) updater;
                if(field.getKey().equals("beta1")) {
                    adaBelief.setBeta1(Double.parseDouble(field.getValue()));
                }
                if(field.getKey().equals("beta2")) {
                    adaBelief.setBeta2(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("epsilon")) {
                    adaBelief.setEpsilon(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("learningRate")) {
                    Double rate = Double.parseDouble(field.getValue());
                    adaBelief.setLearningRate(rate);
                }

                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    adaBelief.setLearningRateSchedule(convert);
                }

            } else if(updater instanceof Nesterovs) {
                Nesterovs nesterovs = (Nesterovs) updater;
                if(field.getKey().equals("learningRate")) {
                    Double rate = Double.parseDouble(field.getValue());
                    nesterovs.setLearningRate(rate);
                }

                if(field.getKey().equals("momentum")) {
                    Double rate = Double.parseDouble(field.getValue());
                    nesterovs.setMomentum(rate);
                }


                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    nesterovs.setLearningRateSchedule(convert);
                }

                if(field.getKey().equals("momentumISchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    nesterovs.setMomentumISchedule(convert);
                }


            } else if(updater instanceof Nadam) {
                Nadam nadam = (Nadam) updater;
                if(field.getKey().equals("beta1")) {
                    nadam.setBeta1(Double.parseDouble(field.getValue()));
                }
                if(field.getKey().equals("beta2")) {
                    nadam.setBeta2(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("epsilon")) {
                    nadam.setEpsilon(Double.parseDouble(field.getValue()));
                }

                if(field.getKey().equals("learningRate")) {
                    Double rate = Double.parseDouble(field.getValue());
                    nadam.setLearningRate(rate);
                }

                if(field.getKey().equals("learningRateSchedule")) {
                    LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
                    ISchedule convert = learningRateScheduleConverter.convert(field.getValue());
                    nadam.setLearningRateSchedule(convert);
                }
            }
        }
    }



    private IUpdater instanceForName(String name) {
        switch(UpdaterTypes.valueOf(name.toUpperCase())) {
            case SGD:
                return new Sgd();
            case ADAM:
                return new Adam();
            case NOOP:
                return new NoOp();
            case NADAM:
                return new Nadam();
            case ADAMAX:
                return new AdaMax();
            case ADAGRAD:
                return new AdaGrad();
            case AMSGRAD:
                return new AMSGrad();
            case RMSPROP:
                return new RmsProp();
            case ADADELTA:
                return new AdaDelta();
            case ADABELIEF:
                return new AdaBelief();
            case NESTEROVS:
                return new Nesterovs();
            default:
                throw new IllegalArgumentException("Illegal type " + name);

        }
    }

}
