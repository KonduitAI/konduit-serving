package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.registry.PipelineRegistry;
import org.eclipse.python4j.PythonJob;
import org.eclipse.python4j.PythonVariable;

import java.util.Collections;
import java.util.UUID;

public class PythonStep implements PipelineStep {

    private final String code;
    private final String setupMethodName;
    private final String runMethodName;

    static {
        PipelineRegistry.registerStepRunnerFactory(new Factory());
    }


    public PythonStep(String code, String setupMethodName, String runMethodName) {
        this.code = code;
        this.setupMethodName = setupMethodName;
        this.runMethodName = runMethodName;


    }

    public static class Factory implements PipelineStepRunnerFactory{

        @Override
        public boolean canRun(PipelineStep pipelineStep){
            return pipelineStep instanceof PythonStep;
        }

        @Override
        public PipelineStepRunner create(PipelineStep pipelineStep){
            return new Runner((PythonStep) pipelineStep);
        }

    }

    public static class Runner implements PipelineStepRunner{

        private final PythonStep pythonStep;
        private final PythonJob pythonJob;

        private  String resolveActualCode(String userCode, String setupF, String runF){
            StringBuilder sb = new StringBuilder();
            sb.append(userCode);
            if (!setupF.equals("setup")){
                sb.append("\n").append("setup = ").append(setupF).append("\n");
            }
            if (runF.equals("run")){
                sb.append("\n").append("runOrig = ").append("run").append("\n");
                runF = "runOrig";
            }

            sb.append("\n").append("run = lambda input: {'output:'").append(runF).append("(input)").append("}");
            return sb.toString();
        }

        public Runner(PythonStep pythonStep){
            this.pythonStep = pythonStep;
            this.pythonJob = new PythonJob("job_" + UUID.randomUUID().toString().replace('-', '_'),
                    resolveActualCode(pythonStep.code, pythonStep.setupMethodName, pythonStep.runMethodName), true);
        }

        @Override
        public Data exec(Context ctx, Data input){
            PythonVariable<Data> pyInput = new PythonVariable<>("input", PyData.INSTANCE, null);
            PythonVariable<Data> pyOutput = new PythonVariable<>("output", PyData.INSTANCE, null);
            pythonJob.exec(Collections.singletonList(pyInput), Collections.singletonList(pyOutput));
            return pyOutput.getValue();
        }

        @Override
        public PipelineStep getPipelineStep(){
            return pythonStep;
        }

        @Override
        public void close() {}
    }
}
