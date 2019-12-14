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
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@Data
public class PythonPipelineGenerator implements PipelineGenerator {

    @Default
    private int numNames = 1;
    @Default
    private Faker faker = new Faker();
    private PythonVariables inputVariables,outputVariables;

    @Override
    public PipelineStep generate() {
        PythonStepBuilder<?, ?> builder = PythonStep.builder();
        //generate random python configuration
        for(int i = 0; i < numNames; i++)
            builder.pythonConfig(UUID.randomUUID().toString(),getConfig());

        return builder.build();
    }

    private PythonConfig getConfig() {
        PythonConfigBuilder<?, ?> builder = PythonConfig.builder();
        Map<String,String> inputs = randomPythonVariables();
        Map<String,String> outputs = randomPythonVariables();
        builder.pythonInputs(inputs);
        builder.pythonOutputs(outputs);
        /**
         * Need to figure out python code relative to variables.
         *
         * Need to figure out logic we want for each kind of
         * input/output type combination
         *
         */
        StringBuffer sb = new StringBuffer();
        for(Map.Entry<String,String> entry : outputs.entrySet()) {
          //  sb.append(co);
        }

        return builder.build();
    }

    public String codeRepresentationForType(Object o,PythonVariables.Type type) {
        switch(type) {
            case STR: return "'" + o.toString() + "'";
            default: return o.toString();
        }
    }

    public  static String conversionCode(PythonVariables.Type type,PythonVariables.Type outputType,Object input,String outputVariableName) {
        StringBuffer sb = new StringBuffer();
        sb.append(outputVariableName  + " = ");
        switch(type) {
            case BOOL:
                Preconditions.checkState(input instanceof Boolean);
                Boolean inputBoolean = (Boolean) input;
                String pythonBoolean = inputBoolean ? "True" : "false";
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
                    case FILE:
                    default:
                        throw new UnsupportedOperationException();
                    case INT:
                        sb.append(inputBoolean ? "1" : "0");
                        break;
                    case STR:
                        sb.append("'" + pythonBoolean + "'");
                        break;
                }

                break;

            case FLOAT:
                Float inputFloat = (Float) input;
                String boolean2 = inputFloat > 0 ? "True" : "false";
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
                    case FILE:
                    default:
                        throw new UnsupportedOperationException();
                    case INT:
                        sb.append(inputFloat.intValue());
                        break;
                    case STR:
                        sb.append("'" + inputFloat + "'");
                        break;
                }

                break;

            case NDARRAY:
                INDArray inputArr = (INDArray) input;
                Preconditions.checkState(inputArr.length() == 1);
                switch(outputType) {
                    case BOOL:
                        Preconditions.checkState(inputArr.dataType() == DataType.BOOL);
                        Boolean value2 = (Boolean) inputArr.element();
                        sb.append(value2 ? "True" : "False");
                        break;
                    case FLOAT:
                        sb.append(inputArr.element());
                        break;
                    case NDARRAY:
                        sb.append("{'" + outputVariableName + "':" + inputArr.element() + "}");
                        break;
                    case LIST:
                        sb.append("[" + inputArr.element() + "]");
                    case DICT:
                    case FILE:
                    default:
                        throw new UnsupportedOperationException();
                    case INT:
                        sb.append(inputArr.element());
                        break;
                    case STR:
                        sb.append("str(" + inputArr.element() +  ")");
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

                switch(outputType) {
                    case BOOL:
                        break;
                    case FLOAT:
                        break;
                    case NDARRAY:
                        break;
                    case LIST:
                    case DICT:
                    case FILE:
                    default:
                        throw new UnsupportedOperationException();
                    case INT:
                        break;
                    case STR:
                        break;
                }

                break;

            case DICT:
                switch(outputType) {
                    case BOOL:
                        break;
                    case FLOAT:
                        break;
                    case NDARRAY:
                        break;
                    case LIST:
                    case DICT:
                    case FILE:
                    default:
                        throw new UnsupportedOperationException();
                    case INT:
                        break;
                    case STR:
                        break;
                }
                break;

            case FILE:
                switch(outputType) {
                    case BOOL:
                        break;
                    case FLOAT:
                        break;
                    case NDARRAY:
                        break;
                    case LIST:
                    case DICT:
                    case FILE:
                    default:
                        throw new UnsupportedOperationException();
                    case INT:
                        break;
                    case STR:
                        break;
                }
            default:
                throw new UnsupportedOperationException();

            case INT:
                Integer intCase = (Integer) input;
                String boolean3 = intCase > 0 ? "True" : "false";

                switch(outputType) {
                    case BOOL:
                        sb.append(boolean3);
                        break;
                    case FLOAT:
                        sb.append(intCase.floatValue());
                        break;
                    case NDARRAY:
                        sb.append("np.array([" + intCase  + "])");
                        break;
                    case LIST:
                        sb.append("[" + intCase + "]");
                    case DICT:
                    case FILE:
                    default:
                        throw new UnsupportedOperationException();
                    case INT:
                        sb.append(intCase);
                        break;
                    case STR:
                        sb.append("'" + intCase + "'");
                        break;
                }
                break;

            case STR:
                switch(outputType) {
                    case BOOL:
                        break;
                    case FLOAT:
                        break;
                    case NDARRAY:
                        break;
                    case LIST:
                    case DICT:
                    case FILE:
                    default:
                        throw new UnsupportedOperationException();
                    case INT:
                        break;
                    case STR:
                        break;
                }
                break;
        }

        sb.append("\n");
        return sb.toString();
    }

    public  Object randomObjectForType(PythonVariables.Type type) {
        switch(type) {
            case BOOL:
                return faker.bool().bool();
            case FLOAT:
                return faker.number().randomDouble(4,0,10);
            case NDARRAY:
                return Nd4j.rand(2,2);
            case LIST:
            case DICT:
            case FILE:
            default:
                throw new UnsupportedOperationException();
            case INT:
                return faker.number().numberBetween(0,10);
            case STR:
                return faker.esports().game();
        }
    }

    private Map<String,String> randomPythonVariables() {
        Map<String,String> ret = new HashMap<>();
        for(int i = 0; i < randomNumberOfVariables(); i++) {
            Pair<String, String> var = randomVariable();
            ret.put(var.getKey(),var.getValue());
        }

        return ret;
    }

    private int randomNumberOfVariables() {
        return faker.number().numberBetween(0,10);
    }

    private Pair<String,String> randomVariable() {
        return Pair.of(faker.ancient().god(),type().name());
    }

    private PythonVariables.Type type() {
        return Type.values()[faker.number().numberBetween(0, Type.values().length)];
    }

}
