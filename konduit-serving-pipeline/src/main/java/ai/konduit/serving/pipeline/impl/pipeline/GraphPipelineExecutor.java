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

package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.context.Profiler;
import ai.konduit.serving.pipeline.api.context.ProfilerConfig;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.pipeline.graph.*;
import ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchOutput;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.common.base.Preconditions;
import org.slf4j.Logger;

import java.util.*;

/**
 * An executer for {@link GraphPipeline} instances
 *
 * @author Alex Black
 */
@Slf4j
@AllArgsConstructor
public class GraphPipelineExecutor extends BasePipelineExecutor {

    private final GraphPipeline pipeline;
    private Map<String,PipelineStepRunner> runners;
    private Map<String,List<String>> inputsFor;     //Key: a step. Value: The steps that this is an input for: i.e., key -> X exists
    private ProfilerConfig profilerConfig;

    public GraphPipelineExecutor(GraphPipeline pipeline){
        this.pipeline = pipeline;

        Map<String, GraphStep> steps = pipeline.steps();

        inputsFor = new HashMap<>();
        for(Map.Entry<String, GraphStep> e : steps.entrySet()){
            GraphStep g = e.getValue();
            List<String> inputs = g.inputs();
            for(String s : inputs){
                List<String> l = inputsFor.computeIfAbsent(s, x -> new ArrayList<>());
                l.add(e.getKey());
            }
        }

        //Initialize runners:
        runners = new HashMap<>();
        for(Map.Entry<String, GraphStep> e : steps.entrySet()){
            GraphStep g = e.getValue();
            if(g.hasStep()){
                PipelineStep s = g.getStep();
                PipelineStepRunner r = getRunner(s);
                runners.put(e.getKey(), r);
            }

            if(g instanceof MergeStep || g instanceof SwitchStep || g instanceof AnyStep){
                runners.put(e.getKey(), null);
            }
        }
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public List<PipelineStepRunner> getRunners() {
        return new ArrayList<>(runners.values());
    }

    @Override
    public Data exec(Data in) {

        Queue<String> canExec = new LinkedList<>();
        Set<String> canExecSet = new HashSet<>();

        Map<String,Data> stepOutputData = new HashMap<>();
        stepOutputData.put(GraphPipeline.INPUT_KEY, in);

        Map<String,GraphStep> m = pipeline.steps();

        for(Map.Entry<String, GraphStep> e : m.entrySet()){
            List<String> inputs = e.getValue().inputs();
            if(inputs != null && inputs.size() == 1 && inputs.get(0).equals(GraphPipeline.INPUT_KEY)){
                canExec.add(e.getKey());
                canExecSet.add(e.getKey());
            }
        }

        Data out = null;

        if(m.size() == 1){
            //No steps other than input - no-op
            out = in;
        }

        while(!canExec.isEmpty()){
            String next = canExec.remove();

            log.trace("Executing step: {}", next);

            canExecSet.remove(next);
            GraphStep gs = m.get(next);
            List<String> inputs = gs.inputs();

            int switchOut = -1;
            Data stepOut = null;
            if(gs instanceof MergeStep) {
                stepOut = Data.empty();
                for (String s : inputs) {
                    Data d = stepOutputData.get(s);
                    stepOut.merge(false, d);
                }
            } else if(gs instanceof SwitchStep) {
                SwitchStep s = (SwitchStep)gs;
                Data inData = stepOutputData.get(inputs.get(0));
                switchOut = s.switchFn().selectOutput(inData);
                stepOut = inData;
            } else if(gs instanceof AnyStep) {
                List<String> stepInputs = gs.inputs();
                for (String s : stepInputs) {
                    if (stepOutputData.containsKey(s)) {
                        stepOut = stepOutputData.get(s);
                        break;
                    }
                }
                System.out.println();
            } else if(gs instanceof SwitchOutput){
                stepOut = stepOutputData.get(gs.input());
            } else if(gs instanceof PipelineGraphStep){
                Preconditions.checkState(inputs.size() == 1, "PipelineSteps should only have 1 input: got inputs %s", inputs);;
                if (inputs.size() != 1 && !GraphPipeline.INPUT_KEY.equals(next))
                    throw new IllegalStateException("Execution of steps with numInputs != 1 is not supported: " + next + ".numInputs=" + inputs.size());

                PipelineStepRunner exec = runners.get(next);
                Data inData = stepOutputData.get(inputs.get(0));
                Preconditions.checkState(inData != null, "Input data is null for step %s - input %s", next, 0);
                stepOut = exec.exec(null, inData);
            } else {
                throw new UnsupportedOperationException("Execution support not yet implemented: " + gs);
            }

            if(stepOut == null)
                throw new IllegalStateException("Got null output from step \"" + next + "\"");

            if(next.equals(pipeline.outputStep())){
                out = stepOut;
                break;
            }

            stepOutputData.put(next, stepOut);

            //Check what can now be executed
            //TODO This should be optimized
            Map<String, GraphStep> steps = pipeline.steps();
            for(String s : steps.keySet()){
                if(canExecSet.contains(s))
                    continue;
                if(stepOutputData.containsKey(s))
                    continue;

                GraphStep currStep = steps.get(s);
                List<String> stepInputs = currStep.inputs();
                boolean allSeen;
                if(currStep instanceof SwitchOutput) {
                    allSeen = false;
                    SwitchOutput so = (SwitchOutput) currStep;
                    String switchIn = so.input();
                    if (switchIn.equals(next) && so.outputNum() == switchOut) {
                        //Just executed the switch this iteration
                        allSeen = true;
                    }
                } else if(currStep instanceof AnyStep ){
                    //Can execute the Any step if at least one of the inputs are available
                    allSeen = false;
                    for (String inName : stepInputs) {
                        if (stepOutputData.containsKey(inName)) {
                            allSeen = true;
                            break;
                        }
                    }
                } else {
                    //StandardPipelineStep, MergeStep
                    allSeen = true;
                    for (String inName : stepInputs) {
                        if (!stepOutputData.containsKey(inName)) {
                            allSeen = false;
                            break;
                        }
                    }
                }

                if(allSeen) {
                    canExec.add(s);
                    canExecSet.add(s);
                    //log.info("Can now exec: {}", s);
                }
            }
        }

        if(out == null)
            throw new IllegalStateException("Could not get output");

        return out;
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public void profilerConfig(ProfilerConfig profilerConfig) {
		this.profilerConfig = profilerConfig;
    }

    @Override
    public Profiler profiler() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
