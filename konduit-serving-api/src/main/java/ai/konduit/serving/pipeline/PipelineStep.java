package ai.konduit.serving.pipeline;

import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.config.TextConfig;
import ai.konduit.serving.pipeline.step.*;
import ai.konduit.serving.pipeline.step.model.*;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import org.datavec.api.transform.schema.Schema;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/** PipelineStep collects all ETL and model related properties (input schema,
 * normalization and transform steps, output schema, potential pre-
 * or post-processing etc.). This config is passed to the respective
 * verticle along with a {@link ai.konduit.serving.config.ServingConfig}.
 *
 * @author Adam Gibson
 *
 * @deprecated To be replaced by {@link PipelineStep}
 */

@JsonSubTypes({
        @JsonSubTypes.Type(value = PmmlStep.class, name = "PMML"),
        @JsonSubTypes.Type(value = PmmlStep.class, name = "PmmlConfig"),
        @JsonSubTypes.Type(value = SameDiffStep.class, name = "SAMEDIFF"),
        @JsonSubTypes.Type(value = SameDiffStep.class, name = "SameDiffConfig"),
        @JsonSubTypes.Type(value = TensorFlowStep.class, name = "TENSORFLOW"),
        @JsonSubTypes.Type(value = TensorFlowStep.class, name = "TensorFlowConfig"),
        @JsonSubTypes.Type(value = OnnxStep.class, name = "ONNX"),
        @JsonSubTypes.Type(value = OnnxStep.class, name = "OnnxConfig"),
        @JsonSubTypes.Type(value = KerasStep.class, name = "KERAS"),
        @JsonSubTypes.Type(value = KerasStep.class, name = "KerasConfig"),
        @JsonSubTypes.Type(value = Dl4jStep.class, name= "DL4J"),
        @JsonSubTypes.Type(value = Dl4jStep.class, name= "DL4JConfig"),
        @JsonSubTypes.Type(value = PythonStep.class, name = "PYTHON"),
        @JsonSubTypes.Type(value = PythonStep.class, name = "PythonStep"),
        @JsonSubTypes.Type(value = PmmlStep.class, name = "PMML"),
        @JsonSubTypes.Type(value = PmmlStep.class, name = "PmmlStep"),
        @JsonSubTypes.Type(value = TransformProcessStep.class, name = "TRANSFORM"),
        @JsonSubTypes.Type(value = TransformProcessStep.class, name = "TransformProcessStep"),
        @JsonSubTypes.Type(value = CustomPipelineStep.class, name = "CUSTOM"),
        @JsonSubTypes.Type(value = CustomPipelineStep.class, name = "CustomPipelineStep"),
        @JsonSubTypes.Type(value = ImageLoadingStep.class, name = "IMAGE"),
        @JsonSubTypes.Type(value = ImageLoadingStep.class, name = "ImageLoadingStep"),
        @JsonSubTypes.Type(value = JsonExpanderTransformStep.class, name = "JSON_EXPANDER"),
        @JsonSubTypes.Type(value = JsonExpanderTransformStep.class, name = "JsonExpanderTransformStep"),
        @JsonSubTypes.Type(value = ArrayConcatenationStep.class, name = "ARRAY_CONCAT"),
        @JsonSubTypes.Type(value = ArrayConcatenationStep.class, name = "ArrayConcatenationStep"),
        @JsonSubTypes.Type(value = WordPieceTokenizerStep.class, name = "WORDPIECE_TOKENIZER"),
        @JsonSubTypes.Type(value = WordPieceTokenizerStep.class, name = "WordPieceTokenizerStep")
})
@JsonTypeInfo(use = NAME, property = "type")
@XmlRootElement
@io.swagger.v3.oas.annotations.media.Schema(description = "Pipeline Step",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(schema = PmmlStep.class, value = "PMML"),
                @DiscriminatorMapping(schema = PmmlStep.class, value = "PmmlConfig"),
                @DiscriminatorMapping(schema = SameDiffStep.class, value = "SAMEDIFF"),
                @DiscriminatorMapping(schema = SameDiffStep.class, value = "SameDiffConfig"),
                @DiscriminatorMapping(schema = TensorFlowStep.class, value = "TENSORFLOW"),
                @DiscriminatorMapping(schema = TensorFlowStep.class, value = "TensorFlowConfig"),
                @DiscriminatorMapping(schema = OnnxStep.class, value = "ONNX"),
                @DiscriminatorMapping(schema = OnnxStep.class, value = "OnnxConfig"),
                @DiscriminatorMapping(schema = KerasStep.class, value = "KERAS"),
                @DiscriminatorMapping(schema = KerasStep.class, value = "KerasConfig"),
                @DiscriminatorMapping(schema = Dl4jStep.class, value= "DL4J"),
                @DiscriminatorMapping(schema = Dl4jStep.class, value= "DL4JConfig"),
                @DiscriminatorMapping(schema = PythonStep.class, value = "PYTHON"),
                @DiscriminatorMapping(schema = PythonStep.class, value = "PythonStep"),
                @DiscriminatorMapping(schema = PmmlStep.class, value = "PMML"),
                @DiscriminatorMapping(schema = PmmlStep.class, value = "PmmlStep"),
                @DiscriminatorMapping(schema = TransformProcessStep.class, value = "TRANSFORM"),
                @DiscriminatorMapping(schema = TransformProcessStep.class, value = "TransformProcessStep"),
                @DiscriminatorMapping(schema = CustomPipelineStep.class, value = "CUSTOM"),
                @DiscriminatorMapping(schema = CustomPipelineStep.class, value = "CustomPipelineStep"),
                @DiscriminatorMapping(schema = ImageLoadingStep.class, value = "IMAGE"),
                @DiscriminatorMapping(schema = ImageLoadingStep.class, value = "ImageLoadingStep"),
                @DiscriminatorMapping(schema = JsonExpanderTransformStep.class, value = "JSON_EXPANDER"),
                @DiscriminatorMapping(schema = JsonExpanderTransformStep.class, value = "JsonExpanderTransformStep"),
                @DiscriminatorMapping(schema = ArrayConcatenationStep.class, value = "ARRAY_CONCAT"),
                @DiscriminatorMapping(schema = ArrayConcatenationStep.class, value = "ArrayConcatenationStep"),
                @DiscriminatorMapping(schema = WordPieceTokenizerStep.class, value = "WORDPIECE_TOKENIZER"),
                @DiscriminatorMapping(schema = WordPieceTokenizerStep.class, value = "WordPieceTokenizerStep")
        }
)
@Deprecated
public interface PipelineStep<T extends PipelineStep> extends Serializable, TextConfig {

    /**
     * Returns true if the input data format
     * is valid or not. Have {@link #validInputTypes()}
     * return null or an empty array if you would like any type to be valid.
     * @param dataFormat the {@link ai.konduit.serving.config.Input.DataFormat} to test
     * @return true if the input format is valid or false otherwise
     */
    default boolean isValidInputType(Input.DataFormat dataFormat) {
        if(validInputTypes() == null || validInputTypes().length < 1) {
            return true;
        }

        return Arrays.asList(validInputTypes()).contains(dataFormat);
    }

    /**
     * Returns true if the output data format
     * is valid or not.
     * Have {@link #validOutputTypes()} return null or an empty array
     * if you would like any type to be valid.
     *
     * @param dataFormat the {@link ai.konduit.serving.config.Output.DataFormat} to test
     * @return true if the output format is valid or false otherwise
     */
    default boolean isValidOutputType(Output.DataFormat dataFormat) {
        if(validOutputTypes() == null || validOutputTypes().length < 1) {
            return true;
        }

        return Arrays.asList(validOutputTypes()).contains(dataFormat);
    }

    /**
     * Valid {@link PredictionType}s
     * if this {@link PipelineStep} is the last step
     * in a pipeline.
     * Have this return null or empty array
     * if you would like any type to be valid.
     *
     * @return the valid prediction type
     */
    PredictionType[] validPredictionTypes();

    /**
     * Checks for the validity of {@link PredictionType} input.
     *
     *
     * is null or empty
     * @param predictionType the prediction type
     * @return Returns true if {@code predictionType} is contained within {@link #validPredictionTypes()}
     */
    default boolean isValidPredictionType(PredictionType predictionType) {
        if(validPredictionTypes() == null || validPredictionTypes().length < 1)
            return true;
        return Arrays.asList(validPredictionTypes()).contains(predictionType);
    }

    /**
     * Returns an array of valid {@link Input.DataFormat} with the current pipeline step.
     * Currently this works when the pipeline step is the beginning of the whole pipeline.
     * @return array of valid {@link Input.DataFormat}s
     */
    Input.DataFormat[] validInputTypes();

    /**
     * Returns an array of valid {@link Output.DataFormat} with the current pipeline step.
     * Currently this works when the pipeline step is at the end of the whole pipeline.
     * @return array of valid {@link Output.DataFormat}s
     */
    Output.DataFormat[] validOutputTypes();

    /**
     * Getter for the input column names
     * @return input column names
     */
    Map<String,List<String>> getInputColumnNames();

    /**
     * Getter for the output column names
     * @return output column names
     */
    Map<String,List<String>> getOutputColumnNames();

    /**
     * Getter for the input schema
     * @return the input schema
     */
    Map<String,List<SchemaType>> getInputSchemas();

    /**
     * Getter for the output schema
     * @return the output schema
     */
    Map<String,List<SchemaType>> getOutputSchemas();

    /**
     * Getter for the output names for the current pipeline step
     * @return output names
     */
    List<String> getOutputNames();

    /**
     *
     * Getter for the input names for the current pipeline step
     * @return input names
     */
    List<String> getInputNames();

    /**
     * Define a single input for a Pipeline Step from explicit
     * column names and types for this input.
     *
     * @param inputName   input name
     * @param columnNames column names
     * @param types       schema types
     * @return this pipeline step
     * @throws Exception key error
     */
    T setInput(String inputName, String[] columnNames, List<SchemaType> types)
            throws Exception;

    /**
     * Define a single input for a TransformProcess Step from a schema.
     *
     * @param inputName   input name
     * @param inputSchema input schema
     * @return this pipeline step
     * @throws Exception key error
     */
    T setInput(String inputName, Schema inputSchema) throws Exception;

    /**
     * Define a single output for a TransformProcess Step from explicit
     * column names and types for this output.
     *
     * @param outputName  output name
     * @param columnNames column names
     * @param types       schema types
     * @return this pipeline step
     * @throws Exception key error
     */
    T setOutput(String outputName, String[] columnNames, List<SchemaType> types)
            throws Exception;

    /**
     * Define a single output for a TransformProcess Step.
     *
     * @param outputName   output name
     * @param outputSchema output schema
     * @return this pipeline step
     * @throws Exception key error
     */
    T setOutput(String outputName, Schema outputSchema) throws Exception;

    /**
     * Define a single input for a PipelineStep from column names and types.
     * The input name will be "default" when using this method.
     *
     * @param columnNames column names
     * @param types       schema types
     * @return this pipeline step
     * @throws Exception key error
     */
    default T setInput(String[] columnNames, List<SchemaType> types) throws Exception {
        return setInput("default", columnNames, types);
    }

    /**
     * Define a single output for a PipelineStep from explicit
     * column names and types for this output. The output name
     * for this step will be "default".
     *
     * @param columnNames column names
     * @param types       schema types
     * @return this pipeline step
     * @throws Exception key error
     */
    default T setOutput(String[] columnNames, List<SchemaType> types) throws Exception {
        return setOutput("default", columnNames, types);
    }

    /**
     * Define a single input for a PipelineStep from a schema.
     * The input name will be "default" when using this method.
     *
     * @param inputSchema input schema
     * @return this pipeline step
     * @throws Exception key error
     */
    default T setInput(Schema inputSchema) throws Exception {
        return setInput("default", inputSchema);
    }

    /**
     * Define a single output for a PipelineStep from a schema.
     * The output name for this step will be "default".
     *
     * @param outputSchema output schema
     * @return this pipeline step
     * @throws Exception key error
     */
    default T setOutput(Schema outputSchema) throws Exception {
        return setOutput("default", outputSchema);
    }

    /**
     * Get the respective runner for the configuration
     *
     * @return the respective step runner
     */
    PipelineStepRunner createRunner();

    /**
     * Look up the output
     * {@link Schema} for a given input name.
     * This is typically going to be the "default" name.
     * @param name the name of the schema to get the output for
     * @return the {@link Schema} for the output
     */
    Schema outputSchemaForName(String name);

    /**
     * Look up the input
     * {@link Schema} for a given input name.
     * This is typically going to be the "default" name.
     * @param name the name of the schema to get the input for
     * @return the {@link Schema} for the input
     */
    Schema inputSchemaForName(String name);

    /**
     * Look up the {@link SchemaType}
     * input types by name. This is typically
     * going to be the "default" name.
     * @param name the name of the schema types to look up
     *
     * @return the schema types ordered by column name ordering
     */
    List<SchemaType> inputTypesForName(String name);

    /**
     * Returns true if this pipeline step
     * has the given input name
     * @param name the name to check for
     * @return true if the input name exists, false otherwise
     */
    boolean hasInputName(String name);

    /**
     * The input name at a particular index
     * @param i the index
     * @return the input name at a particular index
     */
    String inputNameAt(int i);

    /**
     * Returns whether to process the column
     * at the specified index for the given input name
     * @param name the input name to get the status for
     * @param index the index of the column to test
     *              on whether to process
     * @return true if the column should be processed
     */
    boolean processColumn(String name, int index);

    /**
     * Whether the input name is valid
     * for the step
     * @param name the name to test
     * @return true if the input name is valid
     * for the given step
     */
    boolean inputNameIsValidForStep(String name);

    /**
     * The input name at the index
     * @param idx the index to get the input name of
     * @return the input name at the given index
     */
    String inputNameAtIndex(int idx);

    /**
     * Returns fully qualified class name
     * for the {@link PipelineStepRunner}
     * implementation associated with this
     * PipelineStep
     * @return the fully qualified class name as a string
     */
    String pipelineStepClazz();
}

