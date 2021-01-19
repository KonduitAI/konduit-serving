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

package ai.konduit.serving.annotation.module;

import ai.konduit.serving.annotation.AnnotationUtils;
import ai.konduit.serving.annotation.runner.CanRun;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({"ai.konduit.serving.annotation.module.ModuleInfo",
        "ai.konduit.serving.annotation.module.RequiresDependenciesAny",
        "ai.konduit.serving.annotation.module.RequiresDependenciesAll",
        "ai.konduit.serving.annotation.module.InheritRequiredDependencies"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class RequiresDependenciesProcessor extends AbstractProcessor {
    public static final String INHERIT_MODULE_PREFIX = "inherit:";

    private String moduleName;
    private List<String> toWrite = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        if(env.processingOver()){
            if(moduleName == null && !toWrite.isEmpty()){
                //Handle incremental build situation: usually occurs in IDEs, where the class with the annotation
                //has been modified and gets recompiled in isolation (without any of the other classes)
                //In this case, the generated file probably already exists, and we don't need to do anything
                if(AnnotationUtils.existsAndContains(processingEnv.getFiler(), "ai.konduit.serving.annotation.module.RequiresDependencies", toWrite))
                    return false;

                Collection<? extends Element> c = env.getElementsAnnotatedWith(RequiresDependenciesAll.class);
                List<TypeElement> types1 = ElementFilter.typesIn(c);
                Collection<? extends Element> c2 = env.getElementsAnnotatedWith(RequiresDependenciesAny.class);
                List<TypeElement> types2 = ElementFilter.typesIn(c2);
                Collection<? extends Element> c3 = env.getElementsAnnotatedWith(InheritRequiredDependencies.class);
                List<TypeElement> types3 = ElementFilter.typesIn(c3);

                throw new IllegalStateException("No class in this module is annotated with @ModuleInfo - a class with " +
                        "@ModuleInfo(\"your-module-name\") should be added to the module that has the @RequiresDependenciesAll or " +
                        "@RequiresDependenciesAny or @InheritRequiredDependencies annotation: " + types1 + ", " + types2 + ", " + types3);
            }
            writeFile();
        } else {
            //Get module name
            if(moduleName == null){
                Collection<? extends Element> c = env.getElementsAnnotatedWith(ModuleInfo.class);
                List<TypeElement> types = ElementFilter.typesIn(c);
                for(TypeElement te : types){
                    moduleName = te.getAnnotation(ModuleInfo.class).value();
                    break;
                }
            }

            //Get the dependency requirements for the module from @RequiredDependenciesAll
            Collection<? extends Element> c = env.getElementsAnnotatedWith(RequiresDependenciesAll.class);
            List<TypeElement> l = ElementFilter.typesIn(c);
            for(TypeElement annotation : l){
                Requires[] requires = annotation.getAnnotation(RequiresDependenciesAll.class).value();

                for (Requires require : requires) {
                    Dependency[] deps = require.value();
                    Req req = require.requires();

                    List<String> depsStrList = new ArrayList<>();
                    for(Dependency d : deps){
                        //g:a:v:(any or all of classifiers)
                        String g = d.gId();
                        String a = d.aId();
                        String v = d.ver();
                        String[] cl = d.classifier();
                        Req r = d.cReq();
                        depsStrList.add(process(g,a,v,cl,r));
                    }

                    String s;
                    if(req == Req.ALL){
                        s = "[" + String.join(",", depsStrList) + "]";
                    } else {
                        //Any
                        s = "{" + String.join(",", depsStrList) + "}";
                    }

                    toWrite.add(s);
                }
            }

            //Get the dependency requirements for the module from @RequiredDependenciesAny
            //Encode as module_name,{{Requires},{Requires},...}
            c = env.getElementsAnnotatedWith(RequiresDependenciesAny.class);
            l = ElementFilter.typesIn(c);
            for(TypeElement annotation : l){
                Requires[] requires = annotation.getAnnotation(RequiresDependenciesAny.class).value();

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                boolean first = true;
                for (Requires require : requires) {
                    if(!first)
                        sb.append(",");

                    Dependency[] deps = require.value();
                    Req req = require.requires();

                    List<String> depsStrList = new ArrayList<>();
                    for(Dependency d : deps){
                        //g:a:v:(any or all of classifiers)
                        String g = d.gId();
                        String a = d.aId();
                        String v = d.ver();
                        String[] cl = d.classifier();
                        Req r = d.cReq();
                        depsStrList.add(process(g,a,v,cl,r));
                    }

                    String s;
                    if(req == Req.ALL){
                        s = "[" + String.join(",", depsStrList) + "]";
                    } else {
                        //Any
                        s = "{" + String.join(",", depsStrList) + "}";
                    }

                    sb.append(s);
                    first = false;
                }

                sb.append("}");
                toWrite.add(sb.toString());
            }



            //Get the inherited dependency requirements for the module from @InheritRequiredDependencies
            c = env.getElementsAnnotatedWith(InheritRequiredDependencies.class);
            l = ElementFilter.typesIn(c);
            for(TypeElement annotation : l) {
                String inheritFrom = annotation.getAnnotation(InheritRequiredDependencies.class).value();
                toWrite.add(INHERIT_MODULE_PREFIX + inheritFrom);
            }
        }
        return false;   //Allow other processors to process ModuleInfo
    }

    private static String process(String g, String a, String v, String[] cl, Req r){
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        sb.append(g).append(":").append(a).append(":").append(v);
        if(cl != null && cl.length == 1){
            sb.append(":").append(cl[0]);
        } else if(cl != null && cl.length > 1){
            sb.append(":");
            if(r == Req.ALL){
                sb.append("[").append(String.join(",", cl)).append("]");
            } else {
                //Any of
                sb.append("{").append(String.join(",", cl)).append("}");
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    protected void writeFile(){
        if(toWrite.isEmpty())           //Can be empty if @ModuleInfo exists but no required dependencies
            toWrite.add("{}");          //Means "no requirements"

        Filer filer = processingEnv.getFiler();
        List<String> toWrite2 = new ArrayList<>();
        for(String s : toWrite){
            toWrite2.add(moduleName + "," + s);
        }
        AnnotationUtils.writeFile(filer, "ai.konduit.serving.annotation.module.RequiresDependencies", toWrite2);
    }
}
