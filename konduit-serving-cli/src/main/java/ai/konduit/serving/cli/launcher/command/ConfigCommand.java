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
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataIntSwitchFn;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataStringSwitchFn;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.SSDToBoundingBoxStep;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.ServerProtocol;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.launcher.DefaultCommand;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Name("config")
@Summary("A helper command for creating boiler plate json/yaml for inference configuration")
@Description("This command is a utility to create boilerplate json/yaml configurations that can be conveniently modified to start konduit servers.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "                     -- FOR SEQUENCE PIPELINES--\n" +
        "- Prints 'logging -> tensorflow -> logging' config in pretty format:\n" +
        "$ konduit config -p logging,tensorflow,logging\n\n" +
        "- Prints 'logging -> tensorflow -> logging' config with gRPC protocol\n" +
        "  in pretty format:\n" +
        "$ konduit config -p logging,tensorflow,logging -pr grpc\n\n" +
        "- Prints 'dl4j -> logging' config in minified format:\n" +
        "$ konduit config -p dl4j,logging -m\n\n" +
        "- Saves 'dl4j -> logging' config in a 'config.json' file:\n" +
        "$ konduit config -p dl4j,logging -o config.json\n\n" +
        "- Saves 'dl4j -> logging' config in a 'config.yaml' file:\n" +
        "$ konduit config -p dl4j,logging -y -o config.json\n" +


        "\n\n                  -- FOR GRAPH PIPELINES --\n" +
        "- Generates a config that logs the input(1) then flow them through two \n" +
        "  tensorflow models(2,3) and merges the output(4):\n" +
        "$ konduit config -p 1=logging(input),2=tensorflow(1),3=tensorflow(1),4=merge(2,3)\n\n" +
        "- Generates a config that logs the input(1) then channels(2) them through one\n" +
        "  of the two tensorflow models(3,4) and then selects the output(5) based\n" +
        "  on the value of the selection integer field 'select'\n" +
        "$ konduit config -p 1=logging(input),[2_1,2_2]=switch(int,select,1),3=tensorflow(2_1),4=tensorflow(2_2),5=any(3,4)\n\n" +
        "- Generates a config that logs the input(1) then channels(2) them through one\n" +
        "  of the two tensorflow models(3,4) and then selects the output(5) based\n" +
        "  on the value of the selection string field 'select' in the selection map \n" +
        "  (x:0,y:1).\n" +
        "$ konduit config -p 1=logging(input),[2_1,2_2]=switch(string,select,x:0,y:1,1),3=tensorflow(2_1),4=tensorflow(2_2),5=any(3,4)\n" +
        "--------------")
public class ConfigCommand extends DefaultCommand {

    protected static final Pattern STEP_PATTERN = Pattern.compile(",?(.+?)=([^,]+?)\\(([^)]+?)\\)");
    protected static final Pattern NAME_PATTERN = Pattern.compile("([^,]+)");
    protected static final Pattern SWITCH_WHOLE_OUTPUT_PATTERN = Pattern.compile("\\[(.+)]");
    protected static final Pattern SWITCH_INPUTS_PATTERN = Pattern.compile("(int|string),([^,]+),(.+)");
    protected static final Pattern SWITCH_MAP_PATTERN = Pattern.compile("([^,]+):([0-9]+)?");

    private enum PipelineStepType {
        CROP_GRID,
        CROP_FIXED_GRID,
        DL4J,
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
        TENSORFLOW
    }

    private enum GraphStepType {
        SWITCH,
        MERGE,
        ANY
    }

    private static final List<String> reservedKeywords;

    static {
        reservedKeywords = Arrays.stream(PipelineStepType.values()).map(Enum::name).collect(Collectors.toList());
        reservedKeywords.addAll(Arrays.stream(GraphStepType.values()).map(Enum::name).collect(Collectors.toList()));
        reservedKeywords.add("INPUT");
    }

    private enum SwitchType {
        INT,
        STRING
    }

    Map<String, GraphStep> graphStepsGlobalMap = new HashMap<>();

    private ServerProtocol protocol = ServerProtocol.HTTP;
    private String pipelineString;
    private boolean minified;
    private boolean yaml;
    private File outputFile;
    @Option(longName = "pipeline", shortName = "p", argName = "config", required = true)
    @Description("A comma-separated list of sequence/graph pipeline steps to create boilerplate configuration from. " +
            "For sequences, allowed values are: " +
            "[crop_grid, crop_fixed_grid, dl4j, draw_bounding_box, draw_fixed_grid, draw_grid, " +
            "draw_segmentation, extract_bounding_box, camera_frame_capture, video_frame_capture" +
            "image_to_ndarray, logging, ssd_to_bounding_box, samediff, show_image, tensorflow]. " +
            "For graphs, the list item should be in the format '<output>=<type>(<inputs>)' or " +
            "'[outputs]=switch(<inputs>)' for switches. The pre-defined root input is named, 'input'. " +
            "Examples are ==> " +
            "Pipeline step: 'a=tensorflow(input),b=dl4j(input)' " +
            "Merge Step: 'c=merge(a,b)' " +
            "Switch Step (int): '[d1,d2,d3]=switch(int,select,input)' " +
            "Switch Step (string): '[d1,d2,d3]=switch(string,select,x:1,y:2,z:3,input)'" +
            "Any Step: 'e=any(d1,d2,d3)' " +
            "See the examples above for more usage information.")
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

    @Option(longName = "protocol", shortName = "pr")
    @Description("Protocol to use with the server. Allowed values are [http, grpc, mqtt]")
    public void setYaml(String protocol) {
        try {
            this.protocol = ServerProtocol.valueOf(protocol.toUpperCase());
        } catch (Exception exception) {
            System.out.format("Protocol can only be one of %s. Given %s%n",
                    Arrays.toString(ServerProtocol.values()), protocol);
            exception.printStackTrace();
            System.exit(1);
        }
    }

    @Option(longName = "output", shortName = "o", argName = "output-file")
    @Description("Optional: If set, the generated json/yaml will be saved here. Otherwise, it's printed on the console.")
    public void setOutputFile(String output) {
        outputFile = new File(output);
        if(outputFile.exists()) {
            if(!outputFile.isFile()) {
                System.out.format("'%s' is not a valid file location%n", outputFile);
                System.exit(1);
            }
        } else {
            try {
                if(!outputFile.createNewFile()) {
                    System.out.format("'%s' is not a valid file location%n", outputFile);
                    System.exit(1);
                }
            } catch (Exception exception) {
                System.out.format("Error while creating file: '%s'%n", outputFile);
                exception.printStackTrace();
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {
        Pipeline pipeline;

        if(pipelineString.contains("=")) {
            pipeline = getGraph(pipelineString);
        } else {
            pipeline = getSequence(pipelineString);
        }

        InferenceConfiguration inferenceConfiguration =
                InferenceConfiguration.builder()
                        .protocol(protocol)
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
        graphStepsGlobalMap.put("input", builder.input());

        Matcher stepMatcher = STEP_PATTERN.matcher(pipelineString);
        String lastOutputName = null;
        int stepIndex = 0;
        while (stepMatcher.find()) {
            stepIndex++;

            String outputs = stepMatcher.group(1);
            String type = stepMatcher.group(2);
            String inputs = stepMatcher.group(3);

             if (type.equalsIgnoreCase(GraphStepType.SWITCH.name())) {
                Matcher switchWholeOutputMatcher = SWITCH_WHOLE_OUTPUT_PATTERN.matcher(outputs);
                if(switchWholeOutputMatcher.find()) {
                    Matcher switchOutputsMatcher = NAME_PATTERN.matcher(switchWholeOutputMatcher.group(1));
                    List<String> switchOutputs = new ArrayList<>();
                    while (switchOutputsMatcher.find()) {
                        String switchOutputName = switchOutputsMatcher.group(1);
                        if(reservedKeywords.contains(switchOutputName)) {
                            out.format("Output name '%s' should be other than one of the reserved keywords: %s%n", switchOutputName, reservedKeywords);
                            System.exit(1);
                        } else {
                            switchOutputs.add(switchOutputsMatcher.group(1));
                        }
                    }

                    if(switchOutputs.size() < 2) {
                        out.format("Switch outputs (%s) should be more than 1%n", switchOutputs.size());
                        System.exit(1);
                    }

                    Matcher switchInputsMatcher = SWITCH_INPUTS_PATTERN.matcher(inputs);
                    if(switchInputsMatcher.find()) {
                        String switchType = switchInputsMatcher.group(1);
                        String selectField = switchInputsMatcher.group(2);
                        String otherSwitchInputs = switchInputsMatcher.group(3);

                        if(switchType.equalsIgnoreCase(SwitchType.INT.name())) {
                            String switchName = String.format("%s_switch_%s", otherSwitchInputs, UUID.randomUUID().toString().substring(0, 8));
                            if(graphStepsGlobalMap.containsKey(otherSwitchInputs)) {
                                GraphStep[] switchOutputSteps = builder.switchOp(switchName, new DataIntSwitchFn(switchOutputs.size(), selectField), graphStepsGlobalMap.get(otherSwitchInputs));
                                for(int i = 0; i < switchOutputs.size(); i++) {
                                    switchOutputSteps[i].name(switchOutputs.get(i));

                                    if(graphStepsGlobalMap.containsKey(switchOutputSteps[i].name())) {
                                        out.format("Output '%s' is already defined in a previous step from the current step %s%n",
                                                switchOutputSteps[i].name(), stepIndex);
                                        System.exit(1);
                                    }

                                    graphStepsGlobalMap.put(switchOutputSteps[i].name(), switchOutputSteps[i]);
                                    lastOutputName = switchOutputs.get(i);
                                }
                            } else {
                                out.format("Undefined input name '%s' for switch step '%s' at step %s. Make sure that the input name '%s' is defined in a previous step%n",
                                        otherSwitchInputs, stepMatcher.group(), stepIndex, otherSwitchInputs);
                                System.exit(1);
                            }
                        } else {
                            int lastIndexOfComma = otherSwitchInputs.lastIndexOf(',');
                            String inputName = otherSwitchInputs.substring(lastIndexOfComma + 1);
                            if(!graphStepsGlobalMap.containsKey(inputName)) {
                                out.format("Undefined input name '%s' for switch step '%s' at step %s. Make sure that the input name '%s' is defined in a previous step%n",
                                        inputName, stepMatcher.group(), stepIndex, inputName);
                                System.exit(1);
                            }
                            String mapInput = otherSwitchInputs.substring(0, lastIndexOfComma);
                            Matcher switchMapMatcher = SWITCH_MAP_PATTERN.matcher(mapInput);
                            Map<String, Integer> switchMap = new HashMap<>();
                            while (switchMapMatcher.find()) {
                                String key = switchMapMatcher.group(1);
                                if(switchMap.containsKey(key)) {
                                    out.format("Switch map key '%s' is already defined%n", key);
                                    System.exit(1);
                                }

                                int channel = Integer.parseInt(switchMapMatcher.group(2));
                                if(channel > switchOutputs.size() - 1) {
                                    out.format("The switch channel (%s) in the switch map should not be greater " +
                                            "than the number of switch outputs minus one (%s)%n", channel, switchOutputs.size() - 1);
                                    System.exit(1);
                                } else {
                                    switchMap.put(key, channel);
                                }
                            }
                            if(switchMap.size() != switchOutputs.size()) {
                                out.format("Switch map size (%s) should be equal to switch outputs size (%s)%n",
                                        switchMap.size(), switchOutputs.size());
                                System.exit(1);
                            }
                            String switchName = String.format("%s_switch_%s", inputName, UUID.randomUUID().toString().substring(0, 8));
                            GraphStep[] switchOutputSteps = builder.switchOp(switchName, new DataStringSwitchFn(switchOutputs.size(), selectField, switchMap), graphStepsGlobalMap.get(inputName));
                            for(int i = 0; i < switchOutputs.size(); i++) {
                                switchOutputSteps[i].name(switchOutputs.get(i));

                                if(graphStepsGlobalMap.containsKey(switchOutputSteps[i].name())) {
                                    out.format("Output '%s' is already defined in a previous step from the current step %s%n",
                                            switchOutputSteps[i].name(), stepIndex);
                                    System.exit(1);
                                }

                                graphStepsGlobalMap.put(switchOutputSteps[i].name(), switchOutputSteps[i]);
                                lastOutputName = switchOutputSteps[i].name();
                            }
                        }
                    } else {
                        out.format("Invalid switch input pattern '%s' at step %s. The format should be int,<select_field>,<input_name> " +
                                "or string,<select_field>,<map_keys_and_values>,<input_name>. " +
                                "Where 'map_keys_and_values' should be in the form of '<key1>:<switch1_number>,<key2>:<switch2_number>,...'.%n" +
                                "Examples are:%n" +
                                "---------------------------------%n" +
                                "01. int,select,input%n" +
                                "02. string,select,x:0,y:1,input%n" +
                                "---------------------------------%n",
                                inputs, stepIndex);
                        System.exit(1);
                    }
                } else {
                    out.format("Invalid switch output pattern '%s' at step %s. Should be a comma-separated list of output names. For example: [s1,s2,...]%n", outputs, stepIndex);
                    System.exit(1);
                }
             } else if(type.equalsIgnoreCase(GraphStepType.ANY.name()) || type.equalsIgnoreCase(GraphStepType.MERGE.name())) {
                 Matcher inputsMatcher = NAME_PATTERN.matcher(inputs);
                 List<GraphStep> inputGraphSteps = new ArrayList<>();
                 while (inputsMatcher.find()) {
                     String inputName = inputsMatcher.group(1);
                     if(graphStepsGlobalMap.containsKey(inputName)) {
                         inputGraphSteps.add(graphStepsGlobalMap.get(inputName));
                     } else {
                         out.format("Undefined input name '%s' for '%s' step '%s' at step %s. Make sure that the input name '%s' is defined in a previous step%n",
                                 inputName, type.toLowerCase(), stepMatcher.group(), stepIndex, inputName);
                         System.exit(1);
                     }
                 }

                 if(inputGraphSteps.size() < 2) {
                     out.format("Number of inputs for '%s' step should be more than 1%n", type.toLowerCase());
                     System.exit(1);
                 }

                 if(type.equalsIgnoreCase(GraphStepType.ANY.name())) {
                     GraphStep anyOutput = builder.any(outputs, inputGraphSteps.toArray(new GraphStep[inputGraphSteps.size()]));

                     if(graphStepsGlobalMap.containsKey(anyOutput.name())) {
                         out.format("Output '%s' is already defined in a previous step from the current step %s%n",
                                 anyOutput.name(), stepIndex);
                         System.exit(1);
                     }

                     graphStepsGlobalMap.put(anyOutput.name(), anyOutput);
                     lastOutputName = anyOutput.name();
                 } else {
                     GraphStep mergeOutput = inputGraphSteps.get(0).mergeWith(outputs, inputGraphSteps.subList(1, inputGraphSteps.size()).toArray(new GraphStep[inputGraphSteps.size() - 1]));

                     if(graphStepsGlobalMap.containsKey(mergeOutput.name())) {
                         out.format("Output '%s' is already defined in a previous step from the current step %s%n",
                                 mergeOutput.name(), stepIndex);
                         System.exit(1);
                     }

                     graphStepsGlobalMap.put(mergeOutput.name(), mergeOutput);
                     lastOutputName = mergeOutput.name();
                 }
             } else {
                 if(type.equalsIgnoreCase("input")) {
                     out.format("The step type cannot be 'input'. Should be either one of the pipeline step types %sor the graph step types %s%n",
                             Arrays.toString(PipelineStepType.values()), Arrays.toString(GraphStepType.values()));
                     System.exit(1);
                 }

                 if(outputs.contains(",")) {
                     out.format("Number of outputs (%s) in step %s can only be 1%n", outputs.split(",").length, stepIndex);
                     System.exit(1);
                 }

                 if(inputs.contains(",")) {
                     out.format("Number of inputs (%s) in step %s can only be 1%n", outputs.split(",").length, stepIndex);
                     System.exit(1);
                 }

                 if(reservedKeywords.contains(outputs)) {
                     out.format("Output name '%s' should be other than one of the reserved keywords: %s%n", outputs, reservedKeywords);
                     System.exit(1);
                 } else {
                     if(graphStepsGlobalMap.containsKey(inputs)) {
                         if(Arrays.stream(PipelineStepType.values()).map(Enum::name).collect(Collectors.toList()).contains(type.toUpperCase())) {

                             if(graphStepsGlobalMap.containsKey(outputs)) {
                                 out.format("Output '%s' is already defined in a previous step from the current step %s%n",
                                         outputs, stepIndex);
                                 System.exit(1);
                             }

                             graphStepsGlobalMap.put(outputs, graphStepsGlobalMap.get(inputs).then(outputs, getPipelineStep(type)));
                            lastOutputName = outputs;
                         } else {
                             out.format("Invalid step type '%s'. Should be either one of the pipeline step types %sor the graph step types %s%n",
                                     type, Arrays.toString(PipelineStepType.values()), Arrays.toString(GraphStepType.values()));
                             System.exit(1);
                         }
                     } else {
                         out.format("Undefined input name '%s' for %s step '%s' at step %s. Make sure that the input name '%s' is defined in a previous step%n",
                                 inputs, type.toLowerCase(), stepMatcher.group(), stepIndex, inputs);
                         System.exit(1);
                     }
                 }
             }
        }

        if (lastOutputName == null) {
            out.format("Invalid graph pipeline format %s. Should be a comma-separated list of the format: " +
                    "'<output>=<type>(<inputs>)' or '[outputs]=switch(<inputs>)' for switches%n", pipelineString);
            System.exit(1);
        }

        return builder.build(graphStepsGlobalMap.get(lastOutputName));
    }

    private PipelineStep getPipelineStep(String type) {
        String moduleName = null;
        Class<?> clazz;

        try {
            switch (PipelineStepType.valueOf(type.toUpperCase())) {
                case CROP_GRID:
                    moduleName = "konduit-serving-image";
                    clazz = Class.forName("ai.konduit.serving.data.image.step.grid.crop.CropGridStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class, String.class, int.class, int.class,
                                    boolean.class, String.class, boolean.class, boolean.class, Double.class, String.class)
                            .newInstance("image1", "x", "y", 10, 10, true, "box", false, false, 1.33,
                                    (String) clazz.getField("DEFAULT_OUTPUT_NAME").get(null));
                case CROP_FIXED_GRID:
                    moduleName = "konduit-serving-image";
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
                    moduleName = "konduit-serving-image";
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
                    moduleName = "konduit-serving-image";
                    clazz = Class.forName("ai.konduit.serving.data.image.step.grid.draw.DrawFixedGridStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, double[].class, double[].class, int.class, int.class,
                                    boolean.class, String.class, String.class, int.class, Integer.class)
                            .newInstance("image4", new double[]{ 1, 2 }, new double[]{ 1, 2 }, 10, 10, true,
                                    "blue", "red", 1, 1);
                case DRAW_GRID:
                    moduleName = "konduit-serving-image";
                    clazz = Class.forName("ai.konduit.serving.data.image.step.grid.draw.DrawGridStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class, String.class, int.class, int.class,
                                    boolean.class, String.class, String.class, int.class, Integer.class)
                            .newInstance("image1", "x", "y", 10, 10, true, "blue", "red", 1, 1);
                case DRAW_SEGMENTATION:
                    moduleName = "konduit-serving-image";
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
                    moduleName = "konduit-serving-image";
                    clazz = Class.forName("ai.konduit.serving.data.image.step.bb.extract.ExtractBoundingBoxStep");
                    Class<?> imageToNDArrayConfigClass2 = Class.forName("ai.konduit.serving.data.image.convert.ImageToNDArrayConfig");
                    Object imageToNDArrayConfigObject2 = imageToNDArrayConfigClass2.getConstructor().newInstance();
                    imageToNDArrayConfigObject2.getClass().getMethod("height", Integer.class).invoke(imageToNDArrayConfigObject2, 100);
                    imageToNDArrayConfigObject2.getClass().getMethod("width", Integer.class).invoke(imageToNDArrayConfigObject2, 100);
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class, String.class, boolean.class, Double.class,
                                    Integer.class, Integer.class, imageToNDArrayConfigClass2)
                            .newInstance("image7", "box2", "image8", true, 1.33, 10, 10, imageToNDArrayConfigObject2);
                case CAMERA_FRAME_CAPTURE:
                    moduleName = "konduit-serving-camera";
                    clazz = Class.forName("ai.konduit.serving.camera.step.capture.CameraFrameCaptureStep");
                    return (PipelineStep) clazz
                            .getConstructor(int.class, int.class, int.class, String.class)
                            .newInstance(0, 640, 480, "image");
                case VIDEO_FRAME_CAPTURE:
                    moduleName = "konduit-serving-camera";
                    clazz = Class.forName("ai.konduit.serving.camera.step.capture.VideoFrameCaptureStep");
                    return (PipelineStep) clazz
                            .getConstructor(String.class, String.class)
                            .newInstance("<video_file_path>", "image");
                case IMAGE_TO_NDARRAY:
                    moduleName = "konduit-serving-image";
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
                    moduleName = "konduit-serving-image";
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
            } else if (exception instanceof IllegalArgumentException) {
                out.format("Invalid step type '%s'. Allowed values are %s%n", type, Arrays.asList(PipelineStepType.values()));
            } else {
                out.format("No pipeline step found for %s%n", type);
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
}
