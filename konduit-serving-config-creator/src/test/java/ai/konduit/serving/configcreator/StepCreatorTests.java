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
package ai.konduit.serving.configcreator;


import ai.konduit.serving.configcreator.converter.*;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.data.Point;
import org.junit.Test;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.learning.config.*;
import org.nd4j.linalg.schedule.*;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;

public class StepCreatorTests {

    @Test
    public void testStepCreationHelp() throws Exception {
        CommandLine commandLine = new CommandLine(new StepCreator().spec());
        for(PipelineStepType pipelineStepType : PipelineStepType.values()) {
            commandLine.execute(pipelineStepType.name().toLowerCase(),"-h");
        }

    }

    @Test
    public void testPointCreator() throws Exception {
        CommandLine commandLine = new CommandLine(new StepCreator().spec());
        commandLine.execute("point-create","--probability=0.95","--label=hello","--coords=1","--coords=2");
    }

    @Test
    public void testImageNormCreator() {

    }


    @Test
    public void testImageToNDArrayConfig() throws Exception {
        ImageToNDArrayConfig noNormalization = new ImageToNDArrayConfig()
                .aspectRatioHandling(AspectRatioHandling.CENTER_CROP)
                .dataType(NDArrayType.DOUBLE)
                .format(NDFormat.CHANNELS_FIRST)
                .channelLayout(NDChannelLayout.BGR)
                .includeMinibatchDim(true)
                .listHandling(ImageToNDArrayConfig.ListHandling.LIST_OUT)
                .height(3)
                .width(3);

        ImageToNDArrayConfigTypeConverter imageToNDArrayConfigTypeConverter = new ImageToNDArrayConfigTypeConverter();
        StringBuilder command = new StringBuilder();
        command.append("aspectRatioHandling=center_crop,dataType=double,format=channels_first,channelLayout=bgr,includeMiniBatchDim=true,listHandling=list_out,height=3,width=3");
        ImageToNDArrayConfig convert = imageToNDArrayConfigTypeConverter.convert(command.toString());
        assertEquals(noNormalization,convert);

        Pair<ImageNormalization, String> imageNormalizationStringPair = imageNormalizationPair();
        command.append(",normalization=" + imageNormalizationStringPair.getSecond());
        noNormalization.normalization(imageNormalizationStringPair.getFirst());
        convert = imageToNDArrayConfigTypeConverter.convert(command.toString());
        assertEquals(noNormalization,convert);

    }


    @Test
    public void testScheduleConverter() throws Exception {
        LearningRateScheduleConverter learningRateScheduleConverter = new LearningRateScheduleConverter();
        ISchedule assertion = null;
        String command = null;
        outer: for(LearningRateScheduleConverter.Scheduletype scheduletype : LearningRateScheduleConverter.Scheduletype.values()) {
            switch(scheduletype) {
                case EXPONENTIAL:
                    assertion = new ExponentialSchedule(ScheduleType.EPOCH,1.0,1.0);
                    command = "type=exponential,scheduleType=epoch,initialValue=1.0,gamma=1.0";
                    break;
                case SIGMOID:
                    assertion = new SigmoidSchedule(ScheduleType.EPOCH,1.0,1.0,1);
                    command = "type=sigmoid,scheduleType=epoch,initialValue=1.0,gamma=1.0,stepSize=1";
                    break;
                case INVERSE:
                    assertion = new InverseSchedule(ScheduleType.EPOCH,1.0,1.0,1.0);
                    command = "type=inverse,scheduleType=epoch,initialValue=1.0,gamma=1.0,power=1.0";
                    break;
                case FIXED:
                    assertion = new FixedSchedule(1.0);
                    command = "type=fixed,value=1.0";
                    break;
                case CYCLE:
                    assertion = new CycleSchedule(ScheduleType.EPOCH,1e-1,1e-1,1,1,1.0);
                    command = "type=cycle,scheduleType=epoch,initialLearningRate=1e-1,maxLearningRate=1e-1,cycleLength=1,annealingLength=1,annealingDecay=1.0";
                    break;
                case STEP:
                    assertion = new StepSchedule(ScheduleType.EPOCH,1.0,1.0,1.0);
                    command = "type=step,scheduleType=epoch,initialValue=1.0,decayRate=1.0,step=1.0";
                    break;
                case RAMP:
                    //ignore for now
                    continue outer;
                case POLY:
                    assertion = new PolySchedule(ScheduleType.EPOCH,1.0,1.0,1);
                    command = "type=poly,scheduleType=epoch,initialValue=1.0,power=1.0,maxIter=1";
                    break;
                case MAP:
                    //ignore for now
                    continue outer;
            }

            assertEquals("Failed on case " + scheduletype,assertion,learningRateScheduleConverter.convert(command));
        }
    }

    @Test
    public void testUpdaterConverter() throws Exception {
        UpdaterConverter updaterConverter = new UpdaterConverter();
        for(UpdaterConverter.UpdaterTypes type : UpdaterConverter.UpdaterTypes.values()) {
            IUpdater assertion = null;
            String command = null;
            switch(type) {
                case NESTEROVS:
                    assertion = Nesterovs.builder().learningRate(1e-1).momentum(0.9)
                            .learningRateSchedule(new FixedSchedule(1.0))
                            .build();
                    command = "type=nesterovs,learningRate=1e-1,momentum=0.9,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;
                case ADABELIEF:
                    assertion = AdaBelief.builder()
                            .learningRate(1e-1)
                            .beta1(1e-1)
                            .epsilon(1e-1)
                            .learningRateSchedule(new FixedSchedule(1.0))
                            .build();
                    command = "type=adabelief,beta1=1e-1,epsilon=1e-1,learningRate=1e-1,momentum=0.9,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;
                case ADADELTA:
                    assertion = AdaDelta.builder()
                            .epsilon(1e-1)
                            .rho(1e-1)
                            .build();
                    command = "type=adadelta,rho=1e-1,epsilon=1e-1";
                    break;
                case RMSPROP:
                    assertion = RmsProp.builder()
                            .epsilon(1e-1)
                            .learningRate(1e-1)
                            .learningRateSchedule(new FixedSchedule(1.0))
                            .build();
                    command = "type=rmsprop,epsilon=1e-1,learningRate=1e-1,momentum=0.9,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;
                case AMSGRAD:
                    assertion = AMSGrad.builder()
                            .beta1(1e-1)
                            .beta2(1e-1)
                            .learningRate(1e-1)
                            .learningRateSchedule(new FixedSchedule(1.0))
                            .build();
                    command = "type=amsgrad,beta1=1e-1,beta2=1e-1,learningRate=1e-1,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;
                case ADAGRAD:
                    assertion = AdaGrad.builder()
                            .epsilon(1e-1)
                            .learningRate(1e-1)
                            .learningRateSchedule(new FixedSchedule(1.0))
                            .build();
                    command = "type=adagrad,learningRate=1e-1,epsilon=1e-1,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;
                case ADAMAX:
                    assertion = new AdaMax();
                    AdaMax adaMax = (AdaMax)  assertion;
                    adaMax.setBeta1(1e-1);
                    adaMax.setBeta2(1e-1);
                    adaMax.setEpsilon(1e-1);
                    adaMax.setLearningRate(1e-1);
                    adaMax.setLearningRateSchedule(new FixedSchedule(1.0));
                    command = "type=adamax,learningRate=1e-1,epsilon=1e-1,beta1=1e-1,beta2=1e-1,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;
                case NADAM:
                    assertion = Nadam.builder()
                            .beta1(1e-1)
                            .beta2(1e-1)
                            .learningRate(1e-1)
                            .learningRateSchedule(new FixedSchedule(1.0))
                            .build();
                    command = "type=nadam,beta1=1e-1,beta2=1e-1,learningRate=1e-1,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;
                case NOOP:
                    assertion = new NoOp();
                    command = "type=noop";
                    break;
                case ADAM:
                    assertion = Adam.builder()
                            .beta1(1e-1)
                            .beta2(1e-1)
                            .learningRateSchedule(new FixedSchedule(1.0))
                            .learningRate(1e-1)
                            .build();
                    command = "type=adam,beta1=1e-1,beta2=1e-1,learningRate=1e-1,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;
                case SGD:
                    assertion = Sgd.builder()
                            .learningRate(1e-1)
                            .learningRateSchedule(new FixedSchedule(1.0))
                            .build();
                    command = "type=sgd,learningRate=1e-1,learningRateSchedule=\"type=fixed,value=1.0\"";
                    break;

            }

            assertEquals(assertion,updaterConverter.convert(command));
        }
    }


    @Test
    public void testPointConverter() throws Exception {
        PointConverter pointConverter = new PointConverter();
        Point point = Point.create(1.0,1.0,"x",0.5);
        String input = "x=1.0,y=1.0,label=x,probability=0.5";
        assertEquals(point,pointConverter.convert(input));
    }

    @Test
    public void testImageNormalizationConfig() throws Exception {
        Pair<ImageNormalization, String> imageNormalizationStringPair = imageNormalizationPair();
        ImageNormalization imageNormalization = imageNormalizationStringPair.getFirst();
        String command = imageNormalizationStringPair.getRight();
        ImageNormalizationTypeConverter imageNormalizationTypeConverter = new ImageNormalizationTypeConverter();
        ImageNormalization convert = imageNormalizationTypeConverter.convert(command);
        assertEquals(imageNormalization,convert);
    }

    private Pair<ImageNormalization,String> imageNormalizationPair() {
        double[] mean = new double[]{3,3,3};
        double maxValue = 1.0;

        ImageNormalization imageNormalization = new ImageNormalization().maxValue(1.0)
                .type(ImageNormalization.Type.INCEPTION)
                .meanRgb(mean).stdRgb(mean);
        StringBuilder command = new StringBuilder();
        command.append(ImageNormalization.Type.INCEPTION.name().toLowerCase() + " ");
        for(int i = 0; i < mean.length; i++) {
            //append twice for mean and standard deviation
            for(int j = 0; j < 2; j++) {
                command.append(mean[i]);
                command.append(" ");
            }
        }

        command.append(maxValue);
        return Pair.of(imageNormalization,command.toString());
    }


}
