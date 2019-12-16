package ai.konduit.serving.pipeline.generator.impl;

import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.model.PythonConfig.PythonConfigBuilder;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.generator.PipelineGenerator;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.pipeline.step.PythonStep.PythonStepBuilder;
import ai.konduit.serving.util.python.PythonVariables;
import ai.konduit.serving.util.python.PythonVariables.Type;
import com.github.javafaker.Faker;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.util.*;
import java.util.stream.Collectors;

@Builder
@Data
@Slf4j
public class PythonPipelineGenerator implements PipelineGenerator {

    @Default
    private int numNames = 1;
    @Default
    private Faker faker;

    private int numVariables;
    @Default
    private long seed;

    private static Type[] singleTypes = {
            Type.FILE,
            Type.FLOAT,
            Type.INT,
            Type.NDARRAY,
            Type.BOOL,
            Type.STR
    };

    /**
     * Builder constructor. Contains all variables
     * @param numNames the number of input names to use (
     *                 the number of configurations to generate)
     * @param faker the faker instance to use. Only a seed or faker instance maybe specified.
     * @param numVariables the number of variables for input and output
     * @param seed the random seed for use in faker. Only a seed or a faker
     *             instance maybe specified
     */
    public PythonPipelineGenerator(int numNames, Faker faker, int numVariables, long seed) {
        this.numNames = numNames;
        this.faker = faker;
        this.numVariables = numVariables;
        this.seed = seed;
        if(seed != 0) {
            if(faker != null) {
                throw new IllegalArgumentException("Please either only specify a faker instance or a non zero seed.");
            }
            //set both the random seed and the faker seed for complete
            //reproducibility
            Nd4j.getRandom().setSeed(seed);
            this.faker = new Faker(new Random(seed));
        }

        if(faker == null && seed == 0) {
            throw new IllegalStateException("Faker instance not specified and seed appears to be zero. Please specify either a faker instance or a non zero seed.");
        }
     }

    @Override
    public PipelineStep generate() {
        PythonStepBuilder<?, ?> builder = PythonStep.builder();
        //generate random python configuration
        for(int i = 0; i < numNames; i++)
            builder.pythonConfig(UUID.randomUUID().toString().replaceAll("[-0-9]+",""), createRandomConfiguration());

        return builder.build();
    }

    protected PythonConfig createRandomConfiguration() {
        if(numVariables < 1) {
            numVariables = randomNumberOfVariables();
            log.warn("No number of variables specified. Picking random number of " + numVariables);
        }

        PythonConfigBuilder<?, ?> builder = PythonConfig.builder();
        Map<String,String> inputs = randomPythonVariables();
        List<Map.Entry<String,String>> inputsOrdered = inputs.entrySet().stream().collect(Collectors.toList());
        Map<String,String> outputs = randomPythonVariables();
        List<Map.Entry<String,String>> outputsOrdered = outputs.entrySet().stream().collect(Collectors.toList());
        Preconditions.checkState(inputs.size() == outputs.size(),"Inputs and outputs must be same size!");
        builder.pythonInputs(inputs);
        builder.pythonOutputs(outputs);

        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < inputsOrdered.size(); i++) {
            Type inputType = Type.valueOf(inputsOrdered.get(i).getValue());
            Type outputType = Type.valueOf(outputsOrdered.get(i).getValue());
            sb.append(conversionCode(
                    inputType,
                    outputType,
                    randomObjectForType(inputType),
                    outputsOrdered.get(i).getKey()
            ));
        }

        //sets the code for execution
        //based on the random output
        builder.pythonCode(sb.toString());

        return builder.build();
    }


    /**
     *
     * @param type
     * @param outputType
     * @param input
     * @param outputVariableName
     * @return
     */
    public  static String conversionCode(PythonVariables.Type type,PythonVariables.Type outputType,Object input,String outputVariableName) {
        StringBuffer sb = new StringBuffer();
        sb.append(outputVariableName  + " = ");
        switch(type) {
            case BOOL:
                Preconditions.checkState(input instanceof Boolean);
                Boolean inputBoolean = (Boolean) input;
                String pythonBoolean = inputBoolean ? "True" : "False";
                switch(outputType) {
                    case BOOL:
                        sb.append(pythonBoolean);
                        break;
                    case FLOAT:
                        sb.append(inputBoolean ? "1.0" : "0.0");
                        break;
                    case NDARRAY:
                        sb.append("np.array(" + pythonBoolean + ")");
                        break;
                    case LIST:
                        sb.append("[" + pythonBoolean + "]");
                        break;
                    case DICT:
                        sb.append("{'" + outputVariableName + "':" + pythonBoolean + "}");
                        break;
                    case INT:
                        sb.append(inputBoolean ? "1" : "0");
                        break;
                    default:
                        sb.append("'" + pythonBoolean + "'");
                        break;
                }

                break;

            case FLOAT:
            case INT:
                Preconditions.checkState(input instanceof Number,"Input must be an instance of number!");
                Number inputFloat = (Number) input;
                String boolean2 = inputFloat.floatValue() > 0 ? "True" : "False";
                switch(outputType) {
                    case BOOL:
                        sb.append(boolean2);
                        break;
                    case FLOAT:
                        sb.append(inputFloat);
                        break;
                    case NDARRAY:
                        sb.append("np.array(["  + inputFloat + "])");
                        break;
                    case LIST:
                        sb.append("[" + inputFloat + "]");
                        break;
                    case DICT:
                        sb.append("{'" + outputVariableName + "':" + inputFloat + "}");
                        break;
                    case INT:
                        sb.append(inputFloat.intValue());
                        break;
                    default:
                        sb.append("'" + inputFloat + "'");
                        break;
                }

                break;

            case NDARRAY:
                Preconditions.checkState(input instanceof INDArray,"Input not an ndarray!");
                INDArray inputArr = (INDArray) input;
                Preconditions.checkState(inputArr.length() == 1);
                switch(outputType) {
                    case BOOL:
                        Number number1 = inputArr.elementWiseStride();
                        Boolean value2 = number1.doubleValue() > 0;
                        sb.append(value2 ? "True" : "False");
                        break;
                    case FLOAT:
                        sb.append(inputArr.element());
                        break;
                    case INT:
                        Number number = (Number) inputArr.element();
                        sb.append(number.intValue());
                        break;
                    case NDARRAY:
                        break;
                    case LIST:
                        sb.append("[" + inputArr.element() + "]");
                        break;
                    case DICT:
                        sb.append("{'" + outputVariableName + "':" + inputArr.element() + "}");
                        break;
                    default:
                        sb.append("'" + inputArr.element() +  "'");
                        break;
                }
                break;

            case LIST:
                Object firstElement;
                if(input instanceof List) {
                    List arr = (List) input;
                    firstElement = arr.get(0);
                }
                else if(input instanceof Object[]) {
                    Object[] o2 = (Object[]) input;
                    firstElement = o2[0];
                }
                else
                    throw new IllegalArgumentException("Input element must be  a list or object array!");

                return conversionCode(
                        typeForObject(firstElement),
                        outputType,
                        firstElement,
                        outputVariableName);


            case DICT:
                Preconditions.checkState(input instanceof Map,"Input must be a map!");
                Map inputMap = (Map) input;
                Preconditions.checkState(inputMap.size() == 1,"Input map must not be empty and only be size 1");
                Object value = inputMap.values().iterator().next();
                return conversionCode(
                        typeForObject(value),
                        outputType,
                        value,
                        outputVariableName);

            default:
                Preconditions.checkState(input instanceof String,"Input must be a string!");
                switch(outputType) {
                    case BOOL:
                        boolean parsedBool = Boolean.parseBoolean(input.toString().toLowerCase());
                        sb.append(parsedBool ? "True" : "False");
                        break;
                    case FLOAT:
                        Float parsedValue = Float.parseFloat(input.toString());
                        sb.append(parsedValue);
                        break;
                    case NDARRAY:
                        sb.append("np.array([" + input.toString() + "])");
                        break;
                    case LIST:
                        sb.append("[" + input.toString() + "]");
                        break;
                    case DICT:
                        sb.append("{'" + outputVariableName + "':" + input.toString() + "}");
                        break;
                    case INT:
                        Integer validInteger = Integer.parseInt(input.toString());
                        sb.append(validInteger);
                        break;
                    default:
                        sb.append("'" + input + "'");
                        break;
                }
                break;
        }

        sb.append("\n");
        return sb.toString();
    }


    /**
     * Returns a {@link Type}
     * for a given input object
     * @param input the input object
     * @return the type for that object
     */
    public static Type typeForObject(Object input) {
        if(input instanceof Boolean) {
            return Type.BOOL;
        }
        else if(input instanceof Integer) {
            return Type.INT;
        }
        else if(input instanceof Float || input instanceof Double) {
            return Type.FLOAT;
        }
        else if(input instanceof Map) {
            return Type.DICT;
        }
        else if(input instanceof Object[] || input instanceof List) {
            return Type.LIST;
        }
        else if(input instanceof java.io.File) {
            return Type.FILE;
        }
        else if(input instanceof INDArray) {
            return Type.NDARRAY;
        }
        else {
            return Type.STR;
        }
    }

    protected  Object randomObjectForType(PythonVariables.Type type) {
        switch(type) {
            case LIST:
                List<Object> list = new ArrayList<>();
                list.add(randomObjectForType(randomSingleType()));
                return list;
            case DICT:
                Map<String,Object> ret = new HashMap<>();
                ret.put("output",randomObjectForType(randomSingleType()));
                return ret;

            default:
                return randomObjectForTypeSingle(type);

        }
    }

    private Type randomSingleType() {
        return singleTypes[faker.random().nextInt(singleTypes.length)];
    }

    private  Object randomObjectForTypeSingle(PythonVariables.Type type) {
        switch(type) {
            case BOOL:
                return faker.bool().bool();
            case FLOAT:
                return faker.number().randomDouble(4,0,10);
            case NDARRAY:
                //note that only scalars are allowed if we want
                //consistent end to end testing.
                //an assumption is made that all ndarrays are only scalar values
                //TODO: Figure out if this breaks when generalizing the pipeline
                //for interop
                return Nd4j.rand(1,1);
            case INT:
                return faker.number().numberBetween(0,10);
            default:
                return String.valueOf(faker.number().numberBetween(0,10));
        }
    }


    private Map<String,String> randomPythonVariables() {
        Map<String,String> ret = new LinkedHashMap<>();
        for(int i = 0; i < numVariables; i++) {
            Pair<String, String> var = randomVariable();
            ret.put(var.getKey(),var.getValue());
        }

        return ret;
    }

    private int randomNumberOfVariables() {
        return faker.number().numberBetween(1,10);
    }

    private Pair<String,String> randomVariable() {
        return Pair.of(UUID.randomUUID().toString().replaceAll("[-0-9]+",""),type().name());
    }

    private PythonVariables.Type type() {
        return Type.values()[faker.number().numberBetween(0, Type.values().length)];
    }

}
