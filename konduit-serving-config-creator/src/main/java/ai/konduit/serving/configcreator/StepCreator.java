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
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.tensorrt.NamedDimensionList;
import io.swagger.v3.oas.annotations.media.Schema;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.schedule.ISchedule;
import picocli.CommandLine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static ai.konduit.serving.configcreator.PipelineStepType.*;


@CommandLine.Command(name = "step-create")
public class StepCreator implements CommandLine.IModelTransformer, Callable<Integer> {

    private Map<String, CommandLine.ITypeConverter> converters = new HashMap<>();
    @CommandLine.Parameters
    private List<String> params;
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec; // injected by picocli
    @CommandLine.Unmatched
    private List<String> unknownParams = new ArrayList<>();
    private CommandLine subCli;
    @Override
    public CommandLine.Model.CommandSpec transform(CommandLine.Model.CommandSpec commandSpec) {
        try {
            addSubCommandForSteps(commandSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return commandSpec;
    }

    @Override
    public Integer call() throws Exception {
        if(subCli == null) {
            subCli = new CommandLine(spec());
        }
        //work around improper sub command parsing by parsing the passed in arguments manually
        List<String> args = new ArrayList<>();
        args.addAll(this.params);
        args.addAll(this.unknownParams);
        CommandLine.ParseResult parseResult = subCli.parseArgs(args.toArray(new String[args.size()]));
        return run(parseResult);
    }

    public static int run(CommandLine.ParseResult parseResult) throws Exception {
        PipelineStep stepFromResult = createStepFromResult(parseResult);
        //same as above: if a user passes the help signal, this method returns null
        if(stepFromResult == null) {
            parseResult.subcommand().commandSpec().commandLine().usage(System.err);
            return 1;
        }
        CommandLine.Model.OptionSpec optionSpec = parseResult.matchedOption("--fileFormat");
        String fileFormat = optionSpec == null ? "json" : optionSpec.getValue();
        if(fileFormat.equals("json")) {
            System.out.println(stepFromResult.toJson());
        } else if(fileFormat.equals("yaml") || fileFormat.equals("yml")) {
            System.out.println(stepFromResult.toYaml());
        }

        return 0;
    }

    private enum GraphStepType {
        SWITCH,
        MERGE,
        ANY
    }


    private void registerConverters() {
        converters.put(ImageToNDArrayConfig.class.getName(),new ImageToNDArrayConfigTypeConverter());
        converters.put(Point.class.getName(),new PointConverter());
        converters.put(PythonConfig.class.getName(),new PythonConfigTypeConverter());
        converters.put(NamedDimensionList.class.getName(),new NameDimensionConverter());
        converters.put(IUpdater.class.getName(),new UpdaterConverter());
        converters.put(ISchedule.class.getName(),new LearningRateScheduleConverter());
    }


    private void addSubCommandForSteps(CommandLine.Model.CommandSpec ret) throws ClassNotFoundException {
        PipelineStepType[] values = null;
        if(System.getProperty("os.arch").contains("amd")) {
            values = PipelineStepType.values();
        }//non amd, probably arm, pick steps we can load on non intel/amd devices
        else {
            values = new PipelineStepType[] {
                    CROP_GRID,
                    CROP_FIXED_GRID,
                    DL4J,
                    KERAS,
                    DRAW_BOUNDING_BOX,
                    DRAW_FIXED_GRID,
                    DRAW_GRID,
                    DRAW_SEGMENTATION,
                    EXTRACT_BOUNDING_BOX,
                    CAMERA_FRAME_CAPTURE,
                    VIDEO_FRAME_CAPTURE,
                    IMAGE_TO_NDARRAY,
                    LOGGING,
                    SSD_TO_BOUNDING_BOX,
                    SAMEDIFF,
                    SHOW_IMAGE,
                    PYTHON,
                    ONNX,
                    CLASSIFIER_OUTPUT,
                    IMAGE_RESIZE,
                    RELATIVE_TO_ABSOLUTE,
                    DRAW_POINTS,
                    DRAW_HEATMAP,
                    PERSPECTIVE_TRANSFORM,
                    IMAGE_CROP,
                    GRAY_SCALE,
                    TENSORRT,

            };

        }
        for(PipelineStepType pipelineStepType : values) {
            Class<? extends PipelineStep> aClass = PipelineStepType.clazzForType(pipelineStepType);
            if(aClass != null) {
                CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.create();
                spec.mixinStandardHelpOptions(true); // usageHelp and versionHelp option
                addStep(PipelineStepType.clazzForType(pipelineStepType),spec);
                spec.name(pipelineStepType.name().toLowerCase());
                spec.addOption(CommandLine.Model.OptionSpec.builder("--fileFormat")
                        .type(String.class)
                        .required(true)
                        .description("The file format (either json or yaml/yml) to output the pipeline step in")
                        .build());

                ret.addSubcommand(pipelineStepType.name().toLowerCase(),spec);
            } else {
                System.err.println("No class found for " + pipelineStepType);
            }
        }
    }


    public CommandLine.Model.CommandSpec spec() throws Exception {
        registerConverters();
        CommandLine.Model.CommandSpec ret = CommandLine.Model.CommandSpec.create();
        ret = CommandLine.Model.CommandSpec.wrapWithoutInspection(this);
        addSubCommandForSteps(ret);
        ret.name("step-create");
        ret.mixinStandardHelpOptions(true);
        return ret;
    }

    public  void addStep(Class<? extends PipelineStep> clazz,CommandLine.Model.CommandSpec spec) {
        for(Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            CommandLine.Model.OptionSpec.Builder builder = CommandLine
                    .Model.OptionSpec.builder("--" + field.getName())
                    .type(field.getType());
            StringBuilder description = new StringBuilder();
            if(field.isAnnotationPresent(Schema.class)) {
                Schema annotation = field.getAnnotation(Schema.class);
                description.append(annotation.description());
                builder.description(annotation.description());
                appendEnumTypesIfApplicable(description, field);

            }


            if(converters.containsKey(field.getType().getName())) {
                for(Field f : field.getType().getDeclaredFields()) {
                    if(f.isAnnotationPresent(Schema.class)) {
                        Schema annotation = f.getAnnotation(Schema.class);
                        description.append("\n");
                        description.append("\n Parameter value of name " + f.getName() + " " + annotation.description() + " \n");
                        appendEnumTypesIfApplicable(description, f);

                    }
                }

                for(Field f : field.getType().getDeclaredFields()) {
                    if(f.isAnnotationPresent(Schema.class)) {
                        Schema annotation = f.getAnnotation(Schema.class);
                        description.append("\n");
                        description.append("\nParameter value of name " + f.getName() + " for value " + field.getName() + " " + annotation.description() + "\n");
                        appendEnumTypesIfApplicable(description, f);
                    }
                }


                builder.converters(converters.get(field.getType().getName()));
            }


            builder.description(description.toString());
            spec.addOption(builder.build());
        }

    }

    private void appendEnumTypesIfApplicable(StringBuilder description, Field f) {
        if(Enum.class.isAssignableFrom(f.getType())) {
            description.append("\n Possible values are: ");
            Object[] values = f.getType().getEnumConstants();
            for(Object value : values) {
                description.append(value.toString());
                description.append(",");
            }

            description.append("\n");
        }
    }

    public static PipelineStep createStepFromResult(CommandLine.ParseResult parseResult) throws Exception {
        CommandLine.ParseResult subcommand = parseResult.subcommand();
        String name = null;
        if(subcommand.subcommand() == null) {
            name = subcommand.commandSpec().name();
            return getPipelineStep(subcommand, name);

        } else {
            name = subcommand.commandSpec().name();
            return getPipelineStep(subcommand.subcommand(), name);
        }

    }

    private static PipelineStep getPipelineStep(CommandLine.ParseResult subcommand, String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        PipelineStepType pipelineStepType = PipelineStepType.valueOf(name.toUpperCase());
        Class<? extends PipelineStep> aClass = PipelineStepType.clazzForType(pipelineStepType);
        PipelineStep ret =  aClass.newInstance();
        for(Field field : aClass.getDeclaredFields()) {
            field.setAccessible(true);
            if(subcommand.hasMatchedOption("--" + field.getName())) {
                CommandLine.Model.OptionSpec optionSpec = subcommand.matchedOption("--" + field.getName());
                Object value = optionSpec.getValue();
                field.set(ret,value);
            }
        }

        return ret;
    }



}
