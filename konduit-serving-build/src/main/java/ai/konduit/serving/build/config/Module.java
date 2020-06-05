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

import ai.konduit.serving.annotation.module.RequiresDependenciesProcessor;
import ai.konduit.serving.build.dependencies.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.common.primitives.Pair;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Data
@Accessors(fluent = true)
public class Module {
    private static final Map<String, Module> MODULES = loadModuleInfo();

    public static final Module PIPELINE = forName("konduit-serving-pipeline");
    public static final Module VERTX = forName("konduit-serving-vertx");
    public static final Module HTTP = forName("konduit-serving-http");
    public static final Module GRPC = forName("konduit-serving-grpc");
    public static final Module MQTT = forName("konduit-serving-mqtt");
    public static final Module DL4J = forName("konduit-serving-deeplearning4j");
    public static final Module SAMEDIFF = forName("konduit-serving-samediff");
    public static final Module TENSORFLOW = forName("konduit-serving-samediff");
    public static final Module IMAGE = forName("konduit-serving-image");
    public static final Module CAMERA = forName("konduit-serving-camera");

    private final String name;
    private final Dependency dependency;
    private final ModuleRequirements dependencyRequirements;
    private final ModuleRequirements dependencyOptional;

    public Module(@JsonProperty("name") String name, @JsonProperty("dependency") Dependency dependency,
                  @JsonProperty("dependencyRequirements") ModuleRequirements dependencyRequirements,
                  @JsonProperty("dependencyOptional") ModuleRequirements dependencyOptional) {
        this.name = name;
        this.dependency = dependency;
        this.dependencyRequirements = dependencyRequirements;
        this.dependencyOptional = dependencyOptional;
    }

    public Object dependenciesOptional() {
        return dependencyOptional;
    }

    /**
     * @param moduleName The full name of the module - for example "konduit-serving-tensorflow"
     * @return The module for that name; throws an exception if it does not exist
     */
    public static Module forName(String moduleName) {
        Preconditions.checkState(MODULES.containsKey(moduleName), "No module with name \"%s\" is known", moduleName);
        return MODULES.get(moduleName);
    }

    /**
     * @param moduleShortName The full name of the module - for example "tensorflow" to get the "konduit-serving-tensorflow" module
     * @return The module for that name; throws an exception if it does not exist
     */
    public static Module forShortName(String moduleShortName) {
        String name = "konduit-serving-" + moduleShortName;
        return forName(name);
    }

    /**
     * @param module    Name of the module
     * @param shortName If true: the name is a short name (as per {@link #forShortName(String)}
     * @return True if a module with that name exists
     */
    public static boolean moduleExistsForName(String module, boolean shortName) {
        if (shortName) {
            return MODULES.containsKey("konduit-serving-" + module);
        } else {
            return MODULES.containsKey(module);
        }
    }


    private static Map<String, Module> loadModuleInfo() {
        //Load module info
        String s;
        try {
            File f = new ClassPathResource("META-INF/konduit-serving/ModuleRequiresDependencies").getFile();
            s = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, Module> modulesInner = new LinkedHashMap<>();
        Map<String, Module> modules = Collections.unmodifiableMap(modulesInner);

        String[] lines = s.split("\n");
        Map<String, String> inherit = new HashMap<>();
        for (String line : lines) {
            int idx = line.indexOf(',');
            String module = line.substring(0, idx);
            String deps = line.substring(idx + 1);

            if (deps.startsWith(RequiresDependenciesProcessor.INHERIT_MODULE_PREFIX)) {
                String inheritFrom = deps.substring(RequiresDependenciesProcessor.INHERIT_MODULE_PREFIX.length());
                inherit.put(module, inheritFrom);
                continue;
            }

            //First: need to work out if an "Any of" dependency, or just one instance of an ALL requirement
            //Note that ALL requirements are on separate lines, whereas ANY are on one line
            boolean isAny = deps.startsWith("{{") || deps.startsWith("{[");

            ModuleRequirements r = null;
            if (isAny) {
                //Example format: {["org.nd4j:nd4j-native:1.0.0-beta7","org.nd4j:nd4j-native:1.0.0-beta7:{linux-x86_64,...}"],["org.nd4j:nd4j-cuda-10.0:1.0.0-beta7","org.nd4j:nd4j-cuda-10.0:1.0.0-beta7:{linux-x86_64,...}"]}
                //This should be interpreted to mean: "We need ANY ONE of the [...] blocks, for which we need all of inner dependencies
                //In this instance, we need nd4j-native AND its classifier - OR - we need nd4j-cuda-10.x AND its classifier
                String before = deps;
                deps = deps.substring(1, deps.length() - 1);  //Strip first/last bracket
                List<DependencyRequirement> toCombine = new ArrayList<>();
                boolean thisAll = deps.startsWith("[");
                String[] reqSplit = deps.split("[]}],[\\[{]");  //Split on: "],[" or "],{" or "},[" or "},{;
                reqSplit[0] = reqSplit[0].substring(1); //Strip leading "["
                reqSplit[reqSplit.length - 1] = reqSplit[reqSplit.length - 1].substring(0, reqSplit[reqSplit.length - 1].length() - 1);     //Strip trainig "]"
                for (String req : reqSplit) {
                    //req = req.substring(1);     //Strip leading bracket; trailing bracket
                    if (req.endsWith("]") || req.endsWith("}"))
                        req = req.substring(0, req.length() - 1);
                    if (req.isEmpty())
                        continue;       //Shouldn't happen except for malformed annotation (no @Dependency in block)

                    DependencyRequirement parse = parseDependenciesLine(req, !thisAll);
                    toCombine.add(parse);
                }

                DependencyRequirement req = new CompositeRequirement(CompositeRequirement.Type.ANY, toCombine);
                r = new ModuleRequirements(Collections.singletonList(req));
            } else {
                //Example format: "org.nd4j:nd4j-native:1.0.0-beta7:{linux-x86_64,linux-x86_64-avx2,linux-x86_64-avx512,linux-ppc64le,linux-arm64,linux-armhf,windows-x86_64,windows-x86_64-avx2,macosx-x86_64,macosx-x86_64-avx2}","org.nd4j:nd4j-cuda-10.0:1.0.0-beta7:{linux-x86_64,linux-ppc64le,linux-arm64,windows-x86_64}","org.nd4j:nd4j-cuda-10.1:1.0.0-beta7:{linux-x86_64,linux-ppc64le,linux-arm64,windows-x86_64}","org.nd4j:nd4j-cuda-10.2:1.0.0-beta7:{linux-x86_64,linux-ppc64le,linux-arm64,windows-x86_64}"
                //This should be interpreted as "any of the following"
                deps = deps.substring(1, deps.length() - 1);  //Strip first/last bracket

                List<DependencyRequirement> reqs = new ArrayList<>();
                List<Dependency> depsForReq = new ArrayList<>();
                if (!deps.isEmpty()) {   //Can be empty if there are no requirements for this module
                    DependencyRequirement req = parseDependenciesLine(deps, true);
                    reqs.add(req);
                }

                if (!reqs.isEmpty()) {
                    r = new ModuleRequirements(reqs);
                }
            }


            if (modulesInner.containsKey(module)) {
                Module mod = modulesInner.get(module);
                List<DependencyRequirement> currReqs = mod.dependencyRequirements().reqs();
                if (currReqs == null) {
                    mod = new Module(module, ksModule(module), r, null);
                    modulesInner.put(module, mod);
                } else if (r != null) {
                    List<DependencyRequirement> newRews = mod.dependencyRequirements().reqs();
                    newRews.addAll(currReqs);
                    mod = new Module(module, ksModule(module), r, null);
                    modulesInner.put(module, mod);
                }
            } else {
                Module mod = new Module(module, ksModule(module), r, null);
                modulesInner.put(module, mod);
            }

        }

        //Handle dependency inheritence
        //Note that we need to ALSO take into account transitive: x -> y -> z
        if (!inherit.isEmpty()) {
            Set<Pair<String, String>> toProcess = new HashSet<>();
            for (Map.Entry<String, String> e : inherit.entrySet()) {
                toProcess.add(Pair.of(e.getKey(), e.getValue()));
            }

            while (!toProcess.isEmpty()) {
                Iterator<Pair<String, String>> iter = toProcess.iterator();
                boolean anyRemoved = false;
                while (iter.hasNext()) {
                    Pair<String, String> next = iter.next();
                    if (modulesInner.containsKey(next.getSecond())) {
                        //Already processed the module we want to inherit from
                        String m = next.getFirst();
                        String from = next.getSecond();
                        Module fromM = modulesInner.get(from);
                        Module mod = modulesInner.get(m);
                        if (mod == null) {
                            mod = new Module(m, ksModule(m), fromM.dependencyRequirements(), fromM.dependencyOptional());
                            modulesInner.put(m, mod);
                        } else {
                            ModuleRequirements reqs = mod.dependencyRequirements();
                            List<DependencyRequirement> toAdd = fromM.dependencyRequirements().reqs();
                            List<DependencyRequirement> l = reqs.reqs();
                            if (toAdd != null) {
                                if (l == null) {
                                    reqs.reqs(toAdd);
                                } else {
                                    //Add
                                    for (DependencyRequirement dr : toAdd) {
                                        if (!l.contains(dr)) {
                                            l.add(dr);
                                        }
                                    }
                                }
                            }
                        }

                        iter.remove();
                        anyRemoved = true;
                    }
                }
                if (!anyRemoved) {
                    throw new IllegalStateException("Unable to resolve inherited dependencies: unknown modules or cyclical" +
                            "inheritance situation?\n" + toProcess);
                }
            }
        }

        return modules;
    }

    protected static DependencyRequirement parseDependenciesLine(String line, boolean any) {
        String[] depsSplit = line.split("\",\"");
        depsSplit[0] = depsSplit[0].substring(1);   //Remove leading quote
        depsSplit[depsSplit.length - 1] = depsSplit[depsSplit.length - 1].substring(0, depsSplit[depsSplit.length - 1].length() - 1);   //Remove training quote

        List<DepSet> set = new ArrayList<>();
        for (String d : depsSplit) {
            String[] split = d.split(":");

            if (split.length == 4) {
                String classifiers = split[3];
                if (classifiers.startsWith("{") || classifiers.startsWith("[")) {
                    boolean allClassifier = classifiers.startsWith("[");            //{any} vs. [all]
                    classifiers = classifiers.substring(1, classifiers.length() - 1); //Strip brackets
                    String[] cs = classifiers.split(",");
                    List<Dependency> dList = new ArrayList<>();
                    List<Dependency> classifierSet = new ArrayList<>();
                    for (String c : cs) {
                        classifierSet.add(new Dependency(split[0], split[1], split[2], c));
                    }
                    if (allClassifier) {
                        //All classifiers are needed
                        throw new UnsupportedOperationException("Not yet implemented");
                    } else {
                        //Only one of the classifiers are needed (usual case)
                        set.add(new DepSet(classifierSet));
                    }
                } else {
                    //Single classifier
                    set.add(new DepSet(Collections.singletonList(new Dependency(split[0], split[1], split[2]))));
                }
            } else {
                //GAV only
                set.add(new DepSet(Collections.singletonList(new Dependency(split[0], split[1], split[2]))));
            }
        }


        boolean allSingle = true;
        for (DepSet s : set) {
            allSingle = s.list.size() == 1;
            if (!allSingle)
                break;
        }

        if (allSingle) {
            //Combine into a single AllRequirement
            List<Dependency> finalDeps = new ArrayList<>();
            for (DepSet s : set) {
                finalDeps.addAll(s.list);
            }
            return new AllRequirement("", finalDeps);
        } else {
            //Combine into a composite requirement
            List<DependencyRequirement> reqs = new ArrayList<>();
            for (DepSet s : set) {
                if (s.list.size() == 1) {
                    reqs.add(new AllRequirement("", s.list));
                } else {
                    //Multiple classifiers
                    reqs.add(new AnyRequirement("", s.list));
                }
            }
            return new CompositeRequirement(any ? CompositeRequirement.Type.ANY : CompositeRequirement.Type.ALL, reqs);
        }
    }

    @AllArgsConstructor
    @Data
    private static class DepSet {
        private List<Dependency> list;
    }


    protected static Dependency ksModule(String name) {
        //TODO don't hardcode versions
        return new Dependency("ai.konduit.serving", name, "0.1.0-SNAPSHOT", null);
    }
}
