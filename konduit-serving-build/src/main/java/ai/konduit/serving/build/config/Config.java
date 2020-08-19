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

package ai.konduit.serving.build.config;

import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.dependencies.DependencyAddition;
import ai.konduit.serving.build.dependencies.ModuleRequirements;
import ai.konduit.serving.build.dependencies.nativedep.NativeDependency;
import ai.konduit.serving.build.steps.RunnerInfo;
import ai.konduit.serving.build.steps.StepId;
import ai.konduit.serving.build.util.ModuleUtils;
import ai.konduit.serving.build.validation.ValidationFailure;
import ai.konduit.serving.build.validation.ValidationResult;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.base.Preconditions;
import org.nd4j.shade.jackson.annotation.JsonIgnore;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.nd4j.shade.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Setter
@Accessors(fluent = true)
@NoArgsConstructor
@Slf4j
public class Config {

    private String pipelinePath;
    private String ksVersion;
    private Metadata metadata;

    //Target system(s) - "Linux x86_avx2 CPU", "Linux ARM64", etc)
    private Target target;      //TODO this should allow (a) N independent artifacts (one per target), and (b) N targets within one artifact

    //Konduit serving modules to include - "konduit-serving-tensorflow" etc
    private List<Serving> serving = Collections.singletonList(Serving.HTTP);
    private List<Module> modules;

    //Additional dependencies, in GAV(C) format: "group_id:artifact_id:version" / "group_id:artifact_id:version:classifier"
    private List<String> additionalDependencies;

    private List<Deployment> deployments;

    public Config(@JsonProperty("pipelinePath") String pipelinePath, @JsonProperty("ksVersion") String ksVersion,
                  @JsonProperty("metadata") Metadata metadata, @JsonProperty("target") Target target,
                  @JsonProperty("serving") List<Serving> serving, @JsonProperty("modules") List<Module> modules,
                  @JsonProperty("deployments") List<Deployment> deployments){
        this.pipelinePath = pipelinePath;
        this.ksVersion = ksVersion;
        this.metadata = metadata;
        this.target = target;
        this.serving = serving;
        this.modules = modules;
        this.deployments = deployments;
    }

    public Config modules(List<Module> modules){
        this.modules = modules;
        return this;
    }

    public Config modules(Module... modules){
        this.modules = Arrays.asList(modules);
        return this;
    }

    public Config addModules(Module... modules){
        return addModules(Arrays.asList(modules));
    }

    public Config addModules(List<Module> modules){
        if(this.modules == null){
            this.modules = modules;
        } else {
            List<Module> newList = new ArrayList<>(this.modules);   //In case currently w e have an immutable list
            newList.addAll(modules);
            this.modules = newList;
        }
        return this;
    }

    public Config serving(List<Serving> serving){
        this.serving = serving;
        return this;
    }

    public Config serving(Serving... serving){
        this.serving = Arrays.asList(serving);
        return this;
    }

    public Config deployments(List<Deployment> deployments){
        this.deployments = deployments;
        return this;
    }

    public Config deployments(Deployment... deployments){
        this.deployments = Arrays.asList(deployments);
        return this;
    }

    public List<Deployment> deployments(){
        return deployments;
    }


    public ValidationResult validate(){

        //First: check that we have a module for every step in the pipeline
        Map<StepId, List<RunnerInfo>> canRunWith = ModuleUtils.runnersForFile(new File(pipelinePath));

        List<ValidationFailure> failures = new ArrayList<>();


        //Check Target compatibility (OS/arch etc)

        return new ValidationResult(failures);
    }


    //Can't rely on lombok @Data or @EqualsAndHashCode due to bug: https://github.com/rzwitserloot/lombok/issues/2193
    @Override
    public boolean equals(Object o){
        if(!(o instanceof Config))
            return false;
        Config c = (Config)o;
        return Objects.equals(pipelinePath, c.pipelinePath) &&
                Objects.equals(ksVersion, c.ksVersion) &&
                Objects.equals(metadata, c.metadata) &&
                Objects.equals(target, c.target) &&
                Objects.equals(serving, c.serving) &&
                Objects.equals(modules, c.modules) &&
                Objects.equals(deployments, c.deployments);
    }

    @Override
    public int hashCode(){
        return Objects.hashCode(pipelinePath) ^
                Objects.hashCode(ksVersion) ^
                Objects.hashCode(metadata) ^
                Objects.hashCode(target) ^
                Objects.hashCode(serving) ^
                Objects.hashCode(modules) ^
                Objects.hashCode(deployments);
    }


    public String toJson(){
        try {
            return ObjectMappers.json().writeValueAsString(this);
        } catch (IOException e){
            throw new RuntimeException("Error converting Config to JSON", e);   //Should never happen
        }
    }

    public synchronized String toJsonMinimal(){
        try {
            ObjectMapper m = ObjectMappers.jsonMinimal();
            if(!registeredMixin){
                m.addMixIn(Config.class, ConfigJsonMixin.class);
            }
            return m.writeValueAsString(this);
        } catch (IOException e){
            throw new RuntimeException("Error converting Config to JSON", e);   //Should never happen
        }
    }


    public String toYaml(){
        try {
            return ObjectMappers.yaml().writeValueAsString(this);
        } catch (IOException e){
            throw new RuntimeException("Error converting Config to JSON", e);   //Should never happen
        }
    }

    public static Config fromJson(String json){
        try {
            return ObjectMappers.json().readValue(json, Config.class);
        } catch (IOException e){
            throw new RuntimeException("Error deserializing JSON configuration", e);
        }
    }

    public static Config fromYaml(String yaml){
        try {
            return ObjectMappers.yaml().readValue(yaml, Config.class);
        } catch (IOException e){
            throw new RuntimeException("Error deserializing YAML configuration", e);
        }
    }

    public static Config fromFileJson(File f){
        try {
            return fromJson(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
        } catch (IOException e){
            throw new RuntimeException("Error reading JSON file configuration: " + f.getAbsolutePath(), e);
        }
    }

    public static Config fromFileYaml(File f){
        try {
            return fromYaml(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
        } catch (IOException e){
            throw new RuntimeException("Error reading YAML file configuration: " + f.getAbsolutePath(), e);
        }
    }

    public List<Module> modules(){
        return modules;
    }


    public List<Module> resolveModules(){
        Preconditions.checkState(pipelinePath != null && !pipelinePath.isEmpty(), "Pipeline past must be set before attempting" +
                " to resolve requide modules for it");
        Set<Module> modules = new LinkedHashSet<>();
        modules.add(Module.PIPELINE);       //Always include core API
        modules.add(Module.VERTX);          //Always include core Vert.x module for serving
        modules.add(Module.CLI);            //Always include CLI for launching

        for(Serving s : serving){
            switch (s){
                case HTTP:
                    modules.add(Module.HTTP);
                    break;
                case GRPC:
                    modules.add(Module.GRPC);
                    break;
                case MQTT:
                    modules.add(Module.MQTT);
                    break;
                default:
                    throw new IllegalStateException("Unknown or not supported serving type: " + s);
            }
        }

        Map<StepId, List<RunnerInfo>> m = ModuleUtils.runnersForFile(new File(pipelinePath));
        for(Map.Entry<StepId, List<RunnerInfo>> e : m.entrySet()){
            List<RunnerInfo> runners = e.getValue();
            if(runners.size() > 1){
                //TODO fix this - properly handle the case where one step can be executed by more than 1 runner
                log.warn("More than one possible runner, selecting first: {}, {}", e.getKey(), runners);
            }
            Module mod = runners.get(0).module();
            modules.add(mod);
        }
        //TODO what if user has set modules already, and they want extra modules for some reason?
        this.modules = new ArrayList<>(modules);
        return this.modules;
    }

    public List<Dependency> resolveDependencies(){
        Preconditions.checkState(target != null, "Cannot resolve dependencies: No target has been set");
        if(modules == null || modules.isEmpty())
            resolveModules();

        Set<Dependency> deps = new LinkedHashSet<>();

        //First: go through the modules needed to run this pipeline, and add those module dependencies
        for(Module m : modules){
            deps.add(m.dependency());
        }

        //Second: go through each module, and work out what optional dependencies (nd4j backends, etc) we must add
        for(Module m : modules){
            ModuleRequirements req = m.dependencyRequirements();
            if(req == null)     //Module doesn't have any configurable required dependencies
                continue;

            if(!req.satisfiedBy(target, deps)){
                List<DependencyAddition> l = req.suggestDependencies(target, deps);
                if(l != null){
                    for( DependencyAddition da : l){
                        if(da.type() == DependencyAddition.Type.ALL_OF){
                            deps.addAll(da.toAdd());
                        } else {
                            //Any of
                            List<Dependency> toAdd = da.toAdd();
                            if(toAdd.size() == 1) {
                                deps.add(toAdd.get(0));
                            } else if(toAdd.size() > 1){

                                //Perhaps this is due to classifiers - both x86 and avx2 for example
                                boolean allSameExClassifier = true;
                                Dependency first = toAdd.get(0);
                                for( int i=1; i<toAdd.size(); i++ ){
                                    Dependency d = toAdd.get(1);
                                    allSameExClassifier = first.groupId().equals(d.groupId()) &&
                                            first.artifactId().equals(d.artifactId()) &&
                                            first.version().equals(d.version()) &&
                                            (first.classifier() != null && d.classifier() != null);
                                    if(!allSameExClassifier){
                                        break;
                                    }
                                }
                                boolean resolved = false;
                                if(allSameExClassifier){
                                    boolean allNative = true;
                                    for(Dependency d : toAdd){
                                        if(!d.isNativeDependency()){
                                            allNative = false;
                                            break;
                                        }
                                    }
                                    if(allNative){
                                        //Now just select the dependency that matches our target...

                                        for(Dependency d : toAdd){
                                            NativeDependency nd = d.getNativeDependency();
                                            Set<Target> supported = nd.getSupportedTargets();
                                            //Just because it SUPPORTS this target, doesn't mean it's optimal...
                                            boolean noneLower = true;
                                            for(Target t : supported){
                                                Arch a = t.arch();
                                                if(a.isCompatibleWith(target.arch()) && t.arch().lowerThan(target.arch())){
                                                    noneLower = false;
                                                    break;
                                                }
                                            }
                                            if(noneLower){
                                                deps.add(d);
                                                resolved = true;
                                            }
                                        }
                                    }
                                }

                                if(!resolved) {
                                    //TODO Currently both nd4j-native and nd4j-cuda-10.x can be recommended when the target is CUDA
                                    //TODO we'll work out a better solution to this in the future... for now, just warn
                                    log.warn("Multiple possible dependencies for requirement, picking first: {} - {}", req, toAdd);
                                    deps.add(toAdd.get(0));
                                }
                            }
                        }
                    }
                }
            }
        }

        //Additional dependencies:
        if(additionalDependencies != null && !additionalDependencies.isEmpty()){
            for(String s : additionalDependencies){
                String[] split = s.split(":");
                Preconditions.checkState(split.length == 3 || split.length == 4, "Invalid additionalDependency setting: Dependencies must " +
                        "be specified in \"group_id:artifact_id:version\" or \"group_id:artifact_id:version:classifier\" format. Got %s", additionalDependencies);
                String c = split.length == 4 ? split[3] : null;
                Dependency d = new Dependency(split[0], split[1], split[2], c);
                deps.add(d);
            }
        }

        return new ArrayList<>(deps);
    }

    private static boolean registeredMixin = false;
    private static class ConfigJsonMixin {

        @JsonIgnore
        private List<Module> modules;
        @JsonIgnore
        private List<Deployment> deployments;
        @JsonIgnore
        private String pipelinePath;

    }
}
