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

package ai.konduit.serving.cli.launcher.command;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphBuilder;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchFn;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataIntSwitchFn;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataStringSwitchFn;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.SSDToBoundingBoxStep;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.primitives.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Name("config")
@Summary("A helper command for creating boiler plate json/yaml for inference configuration")
@Description("This command is a utility to create boilerplate json/yaml configurations that can be easily modified to start konduit servers.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "                      -- FOR SEQUENCES --\n" +
        "- Prints 'logging -> tensorflow -> logging' config in pretty format:\n" +
        "$ konduit config -p logging,tensorflow,logging\n\n" +
        "- Prints 'dl4j -> logging' config in minified format:\n" +
        "$ konduit config -p dl4j,logging -m\n\n" +
        "- Saves 'dl4j -> logging' config in a 'config.json' file:\n" +
        "$ konduit config -p dl4j,logging -o config.json\n\n" +
        "- Saves 'dl4j -> logging' config in a 'config.yaml' file:\n" +
        "$ konduit config -p dl4j,logging -y -o config.json\n" +
        "\n\n                      -- FOR GRAPHS --\n" +
        "- Generates a config that logs the input(1) then flow them through two \n" +
        "  tensorflow models(2,3) and merges the output(4):\n" +
        "$ konduit config -p logging:1,tensorflow:2:1,tensorflow:3:1,merge:4:2+3\n\n" +
        "- Generates a config that logs the input(1) then channels(2) them through one\n" +
        "  of the two tensorflow models(3,4) and then selects the output(5) based\n" +
        "  on the value of the selection integer field 'select'\n" +
        "$ konduit config -p \n" +
        "  logging:1,switch:2:1|int:select|tensorflow:3+tensorflow:4,any:5:3+4\n" +
        "--------------")
public class ConfigCommand extends DefaultCommand {

    protected static final Pattern BASIC_STEP_PATTERN = Pattern.compile(
            "merge:(.+):(.+)$|" +           // Group: 1,2
            "any:(.+):(.+)$|" +             // Group: 3,4
            "switch:(.+)\\|.*$|" +          // Group: 5
            "switch:(.+):(.+)\\|.*$|" +     // Group: 6,7
            "(.+):(.+)$|" +                 // Group: 8,9
            "(.+):(.+):(.+)$");             // Group: 10,11,12
    protected static final Pattern SWITCH_STEP_OUTER_PATTERN = Pattern.compile(
            "\\|(\\(switch.*\\))+|" +
            "\\+(\\(switch.*\\))\\+|" +
            "\\+(\\(switch.*\\))$");
    protected static final Pattern SWITCH_STEP_INNER_PATTERN = Pattern.compile(
            "^\\(*switch:(.+)\\|int:(.+)\\|([^)]+)\\)*$|" +                    // Group: 1,2,3
            "^\\(*switch:(.+)\\|string:(.+)/(.+)\\|([^)]+)\\)*$|" +          // Group: 4,5,6,7
            "^\\(*switch:(.+):(.+)\\|int:(.+)\\|([^)]+)\\)*$|" +               // Group: 8,9,10,11
            "^\\(*switch:(.+):(.+)\\|string:(.+)/(.+)\\|([^)]+)\\)*$");      // Group: 12,13,14,15,16

    private enum PipelineStepType {
        CROP_GRID,
        CROP_FIXED_GRID,
        DL4J,
        DRAW_BOUNDING_BOX,
        DRAW_FIXED_GRID,
        DRAW_GRID,
        DRAW_SEGMENTATION,
        EXTRACT_BOUNDING_BOX,
        FRAME_CAPTURE,
        IMAGE_TO_NDARRAY,
        LOGGING,
        SSD_TO_BOUNDING_BOX,
        SAMEDIFF,
        SHOW_IMAGE,
        TENSORFLOW
    }

    protected static final String KONDUIT_SERVING_IMAGE_MODULE = "konduit-serving-image";

    private enum SwitchFunctionType {
        INT,
        STRING
    }

    Map<String, String> switchSymbolsMap = new HashMap<>();
    Map<String, GraphStep> graphStepsGlobalMap = new HashMap<>();

    private String pipelineString;
    private boolean minified;
    private boolean yaml;
    private File outputFile;

    @Option(longName = "pipeline", shortName = "p", argName = "config", required = true)
    @Description("A comma-separated list of sequence/graph pipeline steps to create boilerplate configuration from. " +
            "For sequences, allowed values are: " +
            "[crop_grid, crop_fixed_grid, dl4j, draw_bounding_box, draw_fixed_grid, draw_grid, " +
            "draw_segmentation, extract_bounding_box, frame_capture, image_to_ndarray, logging, " +
            "ssd_to_bounding_box, samediff, show_image, tensorflow]. " +
            "For graphs, the list item should be in the format '<step_type>:<step_name>' for root inputs and " +
            "'<step_type>:<step_name>:<input_name>' for specified input ('<step_type>:<step_name>:<input1_name>+<input2_name>+...' for multiple inputs. Multiple inputs are only applicable to 'merge' and 'any' graph step types). " +
            "For switch type step the formats are: " +
            "1: 'switch:<step_name>|int:<field_name>|<step1_type>:<step1_name>+<step2_type>:<step2_name>+...' (for 'int' type switch), " +
            "2: 'switch:<step_name>:<input_name>|int:<field_name>|<step1_type>:<step1_name>+<step2_type>:<step2_name>+...' (for 'int' type switch with input specified), " +
            "3: 'switch:<step_name>|string:<field_name>/key1:value1+key2:value2+...|<step1_type>:<step1_name>+<step2_type>:<step2_name>+...' (for 'string' type switch), " +
            "4: 'switch:<step_name>:<input_name>|string:<field_name>/key1:value1+key2:value2+...|<step1_type>:<step1_name>+<step2_type>:<step2_name>+...' (for 'string' type switch with input specified)." +
            "See the examples above for the usage.")
    public void setPipeline(String pipelineString) {
        this.pipelineString = pipelineString;
    }

    @Option(longName = "minified", shortName = "m", flag = true)
    @Description("If set, the output json will be printed in a single line, without indentations. (Ignored for yaml configuration output)")
    public void setMinified(boolean minified) {
        this.minified = minified;
    }

    @Option(longName = "yaml", shortName = "y", flag = true)
    @Description("Set if you want the output to be a yaml configuration.")
    public void setYaml(boolean yaml) { this.yaml = yaml; }

    @Option(longName = "output", shortName = "o", argName = "output-file")
    @Description("Optional: If set, the generated json/yaml will be saved here. Otherwise, it's printed on the console.")
    public void setOutputFile(String output) {
        outputFile = new File(output);
        if(outputFile.exists()) {
            if(!outputFile.isFile()) {
                System.out.format("'%s' is not a valid file location%n", outputFile);
            }
        } else {
            try {
                if(!outputFile.createNewFile()) {
                    System.out.format("'%s' is not a valid file location%n", outputFile);
                }
            } catch (Exception exception) {
                System.out.format("Error while creating file: '%s'%n", outputFile);
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        Pipeline pipeline;

        if(pipelineString.contains(":")) {
            pipeline = getGraph(pipelineString);
        } else {
            pipeline = getSequence(pipelineString);
        }

        InferenceConfiguration inferenceConfiguration =
                InferenceConfiguration.builder()
                        .pipeline(pipeline).build();

        if(yaml) {
            printOrSave(inferenceConfiguration.toYaml());
        } else {
            JsonObject output = new JsonObject(inferenceConfiguration.toJson());

            if (minified) {
                printOrSave(output.encode());
            } else {
                printOrSave(output.encodePrettily());
            }
        }
    }

    private SequencePipeline getSequence(String pipelineString) {
        SequencePipeline.Builder builder = SequencePipeline.builder();
        for(String stepType : pipelineString.split(",")) {
            builder.add(getPipelineStep(stepType));
        }
        return builder.build();
    }

    private GraphPipeline getGraph(String pipelineString) {
        GraphBuilder builder = new GraphBuilder();
        GraphStep inputGraphStep = builder.input();
        String stepName = null;    // this will keep track of the last step name in the loop
        for(String stepInfo : pipelineString.split(",")) {
            Matcher matcher = BASIC_STEP_PATTERN.matcher(stepInfo);
            if(matcher.find()) {
                if(matcher.group(1) != null) {
                    stepName = matcher.group(1);
                    List<String> mergeNames = Arrays.asList(matcher.group(2).split("\\+"));
                    int inputsToMerge = mergeNames.size();
                    if(inputsToMerge > 1) {
                        GraphStep first = graphStepsGlobalMap.get(mergeNames.get(0));
                        graphStepsGlobalMap.put(stepName, first.mergeWith(stepName,
                                mergeNames.subList(1, inputsToMerge)
                                        .stream().map(graphStepsGlobalMap::get).toArray(GraphStep[]::new)
                                )
                        );
                    } else {
                        throw new CLIException(String.format("Graph steps to be merged must be more than 1. " +
                                "Current it is %s", inputsToMerge));
                    }
                } else if(matcher.group(3) != null) {
                    stepName = matcher.group(3);
                    List<String> anyNames = Arrays.asList(matcher.group(4).split("\\+"));
                    int inputsForAny = anyNames.size();
                    if(inputsForAny > 1) {
                        graphStepsGlobalMap.put(stepName, builder.any(stepName,
                                anyNames.stream().map(graphStepsGlobalMap::get).toArray(GraphStep[]::new))
                        );
                    } else {
                        throw new CLIException(String.format("Graph steps for 'any' step must be more than 1. " +
                                "Current it is %s", inputsForAny));
                    }
                } else if(matcher.group(5) != null) {
                    SwitchStepDetails mainSwitchStepDetails = parseSwitchString(stepInfo);
                    stepName = mainSwitchStepDetails.getName();
                    switchStep(builder, stepName, mainSwitchStepDetails.getSteps(),
                            mainSwitchStepDetails.getSwitchFn(), inputGraphStep);
                } else if(matcher.group(6) != null) {
                    SwitchStepDetails mainSwitchStepDetails = parseSwitchString(stepInfo);
                    stepName = mainSwitchStepDetails.getName();
                    switchStep(builder, stepName, mainSwitchStepDetails.getSteps(),
                            mainSwitchStepDetails.getSwitchFn(),
                            graphStepsGlobalMap.get(mainSwitchStepDetails.inputName));
                } else if(matcher.group(8) != null) {
                    String stepType = matcher.group(8);
                    stepName = matcher.group(9);
                    graphStepsGlobalMap.put(stepName, inputGraphStep.then(stepName, getPipelineStep(stepType)));
                } else {
                    String stepType = matcher.group(10);
                    stepName = matcher.group(11);
                    String stepInput = matcher.group(12);
                    graphStepsGlobalMap.put(stepName,
                            graphStepsGlobalMap.get(stepInput).then(stepName, getPipelineStep(stepType))
                    );
                }
            } else {
                throw new CLIException(String.format("Invalid steps format in %s", stepInfo));
            }
        }
        return builder.build(graphStepsGlobalMap.get(stepName));
    }

    private String symbolize(String switchStepString) {
        Matcher matcher = SWITCH_STEP_OUTER_PATTERN.matcher(switchStepString);
        if(matcher.find()) {
            String symbolId = UUID.randomUUID().toString();
            String internalSwitchString;
            if(matcher.group(1) != null) {
                internalSwitchString = matcher.group(1);
            } else if(matcher.group(2) != null) {
                internalSwitchString = matcher.group(2);
            } else {
                internalSwitchString = matcher.group(3);
            }
            switchSymbolsMap.put(symbolId, internalSwitchString);
            return switchStepString.replace(internalSwitchString, symbolId);
        } else {
            return switchStepString;
        }
    }

    private void switchStep(GraphBuilder builder, String name, String[] steps, SwitchFn fn, GraphStep input) {
        GraphStep[] graphSteps = builder.switchOp(name, fn, input);
        for(int i = 0; i < steps.length; i++) {
            String[] stepSplits = steps[i].split(":");
            String stepType = stepSplits[0];
            if(switchSymbolsMap.containsKey(stepType)) {
                SwitchStepDetails switchStepDetails = parseSwitchString(switchSymbolsMap.get(stepType));
                switchStep(builder,
                        switchStepDetails.getName(),
                        switchStepDetails.getSteps(),
                        switchStepDetails.getSwitchFn(),
                        graphSteps[i]);
            } else {
                String stepName = stepSplits[1];
                this.graphStepsGlobalMap.put(stepName, graphSteps[i].then(stepName, getPipelineStep(stepType)));
            }
        }
    }

    private SwitchStepDetails parseSwitchString(String switchStepString) {
        String symbolized = symbolize(switchStepString);
        Matcher matcher = SWITCH_STEP_INNER_PATTERN.matcher(symbolized);
        if(matcher.find()) {
            if(matcher.group(1) != null) {
                String name = matcher.group(1);
                String fieldName = matcher.group(2);
                String[] steps = matcher.group(3).split("\\+");
                return new SwitchStepDetails(name,
                        steps,
                        parseSwitchFunction(steps.length, "int", fieldName, null),
                        null);
            } else if (matcher.group(4) != null){
                String name = matcher.group(4);
                String fieldName = matcher.group(5);
                String mappings = matcher.group(6);
                String[] steps = matcher.group(7).split("\\+");
                return new SwitchStepDetails(name,
                        steps,
                        parseSwitchFunction(steps.length, "string", fieldName, mappings),
                        null);
            } else if (matcher.group(8) != null){
                String name = matcher.group(8);
                String inputName = matcher.group(9);
                String fieldName = matcher.group(10);
                String[] steps = matcher.group(11).split("\\+");
                return new SwitchStepDetails(name,
                        steps,
                        parseSwitchFunction(steps.length, "int", fieldName, null),
                        inputName);
            } else {
                String name = matcher.group(12);
                String inputName = matcher.group(13);
                String fieldName = matcher.group(14);
                String mappings = matcher.group(15);
                String[] steps = matcher.group(16).split("\\+");
                return new SwitchStepDetails(name,
                        steps,
                        parseSwitchFunction(steps.length, "string", fieldName, mappings),
                        inputName);
            }
        } else {
            throw new CLIException(String.format("Invalid switch step format in: '%s'", switchStepString));
        }
    }

    private SwitchFn parseSwitchFunction(int numberOfOutputs, String type, String fieldName, String mappings) {
        if(SwitchFunctionType.INT.name().equalsIgnoreCase(type)) {
            return new DataIntSwitchFn(numberOfOutputs, fieldName);
        } else if (SwitchFunctionType.STRING.name().equalsIgnoreCase(type)) {
            if(mappings == null) {
                throw new CLIException("Mappings should not be null for STRING type switch function");
            }
            return new DataStringSwitchFn(numberOfOutputs,
                    fieldName,
                    Arrays.stream(mappings.split("\\+"))
                            .map(split -> {
                                String[] keyValue = split.split(":");
                                return new Pair<>(keyValue[0], Integer.parseInt(keyValue[1]));
                            })
                            .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
        } else {
            throw new CLIException(String.format("Invalid switch function type: %s. Should be one of %s",
                    type,
                    Arrays.asList(SwitchFunctionType.values())));
        }
    }

    private PipelineStep getPipelineStep(String type) {
        String moduleName = null;
        Class<?> clazz;

        try {
            switch (PipelineStepType.valueOf(type.toUpperCase())) {
                case CROP_GRID:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.grid.crop.CropGridStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class, String.class, int.class, int.class,
                                    boolean.class, String.class, boolean.class, boolean.class, Double.class, String.class)
                            .newInstance("image1", "x", "y", 10, 10, true, "box", false, false, 1.33,
                                    (String) clazz.getField("DEFAULT_OUTPUT_NAME").get(null));
                case CROP_FIXED_GRID:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.grid.crop.CropFixedGridStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, double[].class, double[].class, int.class, int.class,
                                    boolean.class, String.class, boolean.class, boolean.class, Double.class, String.class)
                            .newInstance("image2", new double[]{ 1, 2 }, new double[]{ 1, 2 }, 100, 100, true, "box",
                                    false, false, 1.33, "crop");
                case DL4J:
                    moduleName = "konduit-serving-deeplearning4j";
                    clazz = Class.forName("ai.konduit.serving.models.deeplearning4j.step.DL4JModelPipelineStep");
                    Class<?> dl4jConfigClazz = Class.forName("ai.konduit.serving.models.deeplearning4j.DL4JConfiguration");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, dl4jConfigClazz, List.class, List.class)
                            .newInstance("<path_to_model>", dl4jConfigClazz.getConstructor().newInstance(),
                                    Arrays.asList("1", "2"), Arrays.asList("11", "22"));
                case DRAW_BOUNDING_BOX:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.bb.draw.DrawBoundingBoxStep");
                    Class<?> scaleClass = Class.forName("ai.konduit.serving.data.image.step.bb.draw.DrawBoundingBoxStep$Scale");
                    Class<?> imageToNDArrayConfigClass = Class.forName("ai.konduit.serving.data.image.convert.ImageToNDArrayConfig");
                    Object imageToNDArrayConfigObject = imageToNDArrayConfigClass.getConstructor().newInstance();
                    imageToNDArrayConfigObject.getClass().getMethod("height", Integer.class).invoke(imageToNDArrayConfigObject, 100);
                    imageToNDArrayConfigObject.getClass().getMethod("width", Integer.class).invoke(imageToNDArrayConfigObject, 100);
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class, boolean.class, boolean.class, Map.class,
                                    String.class, int.class, scaleClass, int.class, int.class,
                                    imageToNDArrayConfigClass, boolean.class, String.class)
                            .newInstance("image3", "box", false, false, new HashMap<>(), "blue", 1,
                                    scaleClass.getField("NONE").get(null), 10, 10,
                                    imageToNDArrayConfigObject, false, "red");
                case DRAW_FIXED_GRID:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.grid.draw.DrawFixedGridStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, double[].class, double[].class, int.class, int.class,
                                    boolean.class, String.class, String.class, int.class, Integer.class)
                            .newInstance("image4", new double[]{ 1, 2 }, new double[]{ 1, 2 }, 10, 10, true,
                                    "blue", "red", 1, 1);
                case DRAW_GRID:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.grid.draw.DrawGridStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class, String.class, int.class, int.class,
                                    boolean.class, String.class, String.class, int.class, Integer.class)
                            .newInstance("image1", "x", "y", 10, 10, true, "blue", "red", 1, 1);
                case DRAW_SEGMENTATION:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.segmentation.index.DrawSegmentationStep");
                    Class<?> imageToNDArrayConfigClass1 = Class.forName("ai.konduit.serving.data.image.convert.ImageToNDArrayConfig");
                    Object imageToNDArrayConfigObject1 = imageToNDArrayConfigClass1.getConstructor().newInstance();
                    imageToNDArrayConfigObject1.getClass().getMethod("height", Integer.class).invoke(imageToNDArrayConfigObject1, 100);
                    imageToNDArrayConfigObject1.getClass().getMethod("width", Integer.class).invoke(imageToNDArrayConfigObject1, 100);
                    return (PipelineStep) clazz
                            .getConstructor(List.class, String.class, String.class, String.class, Double.class,
                                    Integer.class, imageToNDArrayConfigClass1)
                            .newInstance(Arrays.asList("red", "blue"), "[]", "image5", "image6", 0.5, 1,
                                    imageToNDArrayConfigObject1);
                case EXTRACT_BOUNDING_BOX:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.bb.extract.ExtractBoundingBoxStep");
                    Class<?> imageToNDArrayConfigClass2 = Class.forName("ai.konduit.serving.data.image.convert.ImageToNDArrayConfig");
                    Object imageToNDArrayConfigObject2 = imageToNDArrayConfigClass2.getConstructor().newInstance();
                    imageToNDArrayConfigObject2.getClass().getMethod("height", Integer.class).invoke(imageToNDArrayConfigObject2, 100);
                    imageToNDArrayConfigObject2.getClass().getMethod("width", Integer.class).invoke(imageToNDArrayConfigObject2, 100);
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class, String.class, boolean.class, Double.class,
                                    Integer.class, Integer.class, imageToNDArrayConfigClass2)
                            .newInstance("image7", "box2", "image8", true, 1.33, 10, 10, imageToNDArrayConfigObject2);
                case FRAME_CAPTURE:
                    moduleName = "konduit-serving-camera";
                    clazz = Class.forName("ai.konduit.serving.camera.step.capture.FrameCapturePipelineStep");
                    return (PipelineStep) clazz
                            .getConstructor(int.class, int.class, int.class, String.class)
                            .newInstance(0, 640, 480, "image");
                case IMAGE_TO_NDARRAY:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep");
                    Class<?> imageToNDArrayConfigClass3 = Class.forName("ai.konduit.serving.data.image.convert.ImageToNDArrayConfig");
                    Object imageToNDArrayConfigObject3 = imageToNDArrayConfigClass3.getConstructor().newInstance();
                    imageToNDArrayConfigObject3.getClass().getMethod("height", Integer.class).invoke(imageToNDArrayConfigObject3, 100);
                    imageToNDArrayConfigObject3.getClass().getMethod("width", Integer.class).invoke(imageToNDArrayConfigObject3, 100);
                    return (PipelineStep) clazz
                            .getConstructor(imageToNDArrayConfigClass3, List.class, List.class, boolean.class, boolean.class,
                                    String.class)
                            .newInstance(imageToNDArrayConfigObject3, Arrays.asList("key1", "key2"),
                                    Arrays.asList("output1", "output2"), true, false, "@ImageToNDArrayStepMetadata");
                case LOGGING:
                    return LoggingPipelineStep.builder()
                            .log(LoggingPipelineStep.Log.KEYS_AND_VALUES)
                            .build();
                case SSD_TO_BOUNDING_BOX:
                    return SSDToBoundingBoxStep.builder().build();
                case SAMEDIFF:
                    moduleName = "konduit-serving-samediff";
                    clazz = Class.forName("ai.konduit.serving.models.samediff.step.SameDiffModelPipelineStep");
                    Class<?> sameDiffConfigClazz = Class.forName("ai.konduit.serving.models.samediff.SameDiffConfig");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, sameDiffConfigClazz, List.class)
                            .newInstance("<path_to_model>", sameDiffConfigClazz.getConstructor().newInstance(),
                                    Arrays.asList("11", "22"));
                case SHOW_IMAGE:
                    moduleName = KONDUIT_SERVING_IMAGE_MODULE;
                    clazz = Class.forName("ai.konduit.serving.data.image.step.show.ShowImagePipelineStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class, Integer.class, Integer.class, boolean.class)
                            .newInstance("image", "image", 1280, 720, false);
                case TENSORFLOW:
                    moduleName = "konduit-serving-tensorflow";
                    clazz = Class.forName("ai.konduit.serving.models.tensorflow.step.TensorFlowPipelineStep");
                    Class<?> tensorflowConfigClazz = Class.forName("ai.konduit.serving.models.tensorflow.TensorFlowConfiguration");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, tensorflowConfigClazz, List.class, List.class)
                            .newInstance("<path_to_model>", tensorflowConfigClazz.getConstructor().newInstance(),
                                    Arrays.asList("1", "2"), Arrays.asList("11", "22"));
                default:
                    out.format("Invalid step type '%s'. Allowed values are %s%n", type, Arrays.asList(PipelineStepType.values()));
                    System.exit(1);
            }
        } catch (Exception exception) {
            if(exception instanceof ClassNotFoundException) {
                if(moduleName == null) {
                    exception.printStackTrace(out);
                } else {
                    out.format("Please add '%s' module to the binaries to use " +
                            "'%s' step type%n", moduleName, type);
                }
            } else {
                exception.printStackTrace(out);
            }

            System.exit(1);
        }

        return null;
    }

    private void printOrSave(String output) {
        if(outputFile == null) {
            out.println(output);
        } else {
            try {
                FileUtils.writeStringToFile(outputFile, output, StandardCharsets.UTF_8);
                out.format("Config file created successfully at %s%n", outputFile.getAbsolutePath());
            } catch (IOException exception) {
                out.format("Unable to save configuration file to %s%n", outputFile.getAbsolutePath());
                exception.printStackTrace(out);
            }
        }
    }

    @Data
    @AllArgsConstructor
    private class SwitchStepDetails {
        private String name;
        private String[] steps;
        private SwitchFn switchFn;
        private String inputName;
    }
}
