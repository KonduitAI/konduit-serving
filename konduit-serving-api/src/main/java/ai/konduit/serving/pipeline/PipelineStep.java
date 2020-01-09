package ai.konduit.serving.pipeline;

import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.pipeline.step.*;
import org.datavec.api.transform.schema.Schema;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/** PipelineStep collects all ETL and model related properties (input schema,
 * normalization and transform steps, output schema, potential pre-
 * or post-processing etc.). This config is passed to the respective
 * verticle along with a {@link ai.konduit.serving.config.ServingConfig}.
 *
 * @author Adam Gibson
 */

@JsonSubTypes({
        @JsonSubTypes.Type(value = ModelStep.class, name = "ModelStep"),
        @JsonSubTypes.Type(value = PythonStep.class, name = "PythonStep"),
        @JsonSubTypes.Type(value = PmmlStep.class, name = "PmmlStep"),
        @JsonSubTypes.Type(value = TransformProcessStep.class, name = "TransformProcessStep"),
        @JsonSubTypes.Type(value = CustomStep.class, name = "CustomStep"),
        @JsonSubTypes.Type(value = ImageLoadingStep.class, name = "ImageLoadingStep"),
        @JsonSubTypes.Type(value = JsonExpanderTransformStep.class, name = "JsonExpanderTransformStep"),
        @JsonSubTypes.Type(value = ArrayConcatenationStep.class, name = "ArrayConcatenationStep"),
})
@JsonTypeInfo(use = NAME, include = PROPERTY)
public interface PipelineStep<T extends PipelineStep> extends Serializable {


    /**
     * Returns true if the input data format
     * is valid or not. Have {@link #validInputTypes()}
     * return null or an empty array if you would like any type to be valid.
     * @param dataFormat the {@link ai.konduit.serving.config.Input.DataFormat} to test
     * @return true if the input format is valid or false otherwise
     */
    default  boolean isValidInputType(Input.DataFormat dataFormat) {
        if(validInputTypes() == null || validInputTypes().length < 1) {
            return true;
        }

        boolean ret =  Arrays.stream(validInputTypes()).anyMatch(input -> dataFormat.equals(input));
        return ret;
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
    default  boolean isValidOutputType(Output.DataFormat dataFormat) {
        if(validOutputTypes() == null || validOutputTypes().length < 1) {
            return true;
        }

        boolean ret =  Arrays.stream(validOutputTypes()).anyMatch(input -> dataFormat.equals(input));
        return ret;
    }


    /**
     * Valid {@link PredictionType}s
     * if this {@link PipelineStep} is the last step
     * in a pipeline.
     * Have {@link #validPredictionTypes()} return null or empty array
     * if you would like any type to be valid.
     *
     * @return the valid prediction type
     */
    PredictionType[] validPredictionTypes();

    /**
     * Returns true if the {@link #validPredictionTypes()}
     * is contained within the input or if {@link #validPredictionTypes()}
     * is null or empty
     * @param predictionType the prediction type
     * @return
     */
    default boolean isValidPredictionType(PredictionType predictionType) {
        if(validPredictionTypes() == null || validPredictionTypes().length < 1)
            return true;
        boolean ret =  Arrays.stream(validPredictionTypes()).anyMatch(input -> predictionType.equals(input));
        return ret;
    }


    /**
     * Returns the valid {@link Input.DataFormat}
     * when the input is the beginning of a pipeline
     * @return
     */
    Input.DataFormat[] validInputTypes();

    /**
     * Returns the valid {@link Output.DataFormat}
     * when the output is the end of a pipeline
     * @return
     */
    Output.DataFormat[] validOutputTypes();

    /**
     * Getter for the input column names
     * @return
     */
    Map<String,List<String>> getInputColumnNames();

    /**
     * Getter for the input column names
     * @return
     */
    Map<String,List<String>> getOutputColumnNames();

    /**
     * Getter for the input schema
     * @return
     */
    Map<String,SchemaType[]> getInputSchemas();

    /**
     * Getter for the input schema
     * @return
     */
    Map<String,SchemaType[]> getOutputSchemas();

    /**
     * Getter for the output names for this
     * pipeline step
     * @return
     */
    List<String> getOutputNames();

    /**
     *
     * Getter for the input names for this pipeline step
     * @return
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
    T setInput(String inputName, String[] columnNames, SchemaType[] types)
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
    T setOutput(String outputName, String[] columnNames, SchemaType[] types)
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
    default T setInput(String[] columnNames, SchemaType[] types) throws Exception {
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
    default T setOutput(String[] columnNames, SchemaType[] types) throws Exception {
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
    SchemaType[] inputTypesForName(String name);

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
