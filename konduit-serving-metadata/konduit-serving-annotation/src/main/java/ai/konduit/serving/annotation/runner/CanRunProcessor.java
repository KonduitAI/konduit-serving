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

package ai.konduit.serving.annotation.runner;

import ai.konduit.serving.annotation.AnnotationUtils;
import ai.konduit.serving.annotation.module.ModuleInfo;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.*;

/**
 * Collect runner metadata:
 * {@code @CanRun(SomePipeline.class)} annotation on a PipelineStepRunner means that the specified PipelineStep can
 * be run by the annotated PipelineStepRunner class. Note that in some cases, a given PipelineStepRunner may not be
 * able to run a particular instance of this type of PipelineStep due to some configuration or versioning issue (but it
 * must be able to run _some_ of these types of PipelineStep instances)
 * <br>
 * During processing, this processor writes a "META-INF/konduit-serving/ai.konduit.serving.annotation.runner.CanRun" file
 * with content like: ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep,ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStepRunner<br>
 * which should be interpreted as "LoggingPipelineStep can be run by LoggingPipelineStepRunner"
 *
 * @author Alex Black
 */
@SupportedAnnotationTypes({"ai.konduit.serving.annotation.runner.CanRun", "ai.konduit.serving.annotation.module.ModuleInfo"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CanRunProcessor extends AbstractProcessor {

    private List<String> toWrite = new ArrayList<>();
    private String moduleName;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        if(env.processingOver()){
            if(moduleName == null && !toWrite.isEmpty()){
                Collection<? extends Element> c = env.getElementsAnnotatedWith(CanRun.class);
                List<TypeElement> types = ElementFilter.typesIn(c);
                throw new IllegalStateException("No class in this module is annotated with @ModuleInfo - a class with " +
                        "@ModuleInfo(\"your-module-name\") should be added to the module that has the @CanRun(...) annotation: " + types + " - " + toWrite);
            }
            writeFile();
        } else {
            if(moduleName == null){
                Collection<? extends Element> c = env.getElementsAnnotatedWith(ModuleInfo.class);
                List<TypeElement> types = ElementFilter.typesIn(c);
                for(TypeElement te : types){
                    moduleName = te.getAnnotation(ModuleInfo.class).value();
                    break;
                }
            }

            //Collect info for writing at end
            Collection<? extends Element> c = env.getElementsAnnotatedWith(CanRun.class);
            List<TypeElement> types = ElementFilter.typesIn(c);
            Element canRunElement = processingEnv.getElementUtils().getTypeElement(CanRun.class.getName());

            //Get the class values
            //See https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
            TypeMirror canRunType = canRunElement.asType();
            for (TypeElement annotation : types) {
                List<? extends AnnotationMirror> l = annotation.getAnnotationMirrors();
                String[] values = null;
                for (AnnotationMirror am : l) {
                    if (am.getAnnotationType().equals(canRunType)) {
                        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                            if ("value".equals(entry.getKey().getSimpleName().toString())) {
                                String s = entry.getValue().toString();     //ai.konduit.something.SomeClass.class
                                s = s.replace("{", "").replace("}", "");

                                values = s.split(", ?");
                                for( int i=0; i<values.length; i++ ){
                                    if(values[i].endsWith(".class")){
                                        values[i] = values[i].substring(0, values[i].length()-6);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }

                if(values != null) {
                    for (String s : values) {
                        toWrite.add(s + "," + annotation.toString());   //Format: pipelineClass,runnerClass,module - i.e., "this type of pipeline step (in specified module) can be run by this type of runner"
                    }
                }
            }
        }

        return false;   //Allow other processors to process ModuleInfo
    }

    protected void writeFile(){
        if(toWrite.isEmpty())           //Can be empty if @ModuleInfo exists but no runners
            return;

        Filer filer = processingEnv.getFiler();
        List<String> toWrite2 = new ArrayList<>();
        for(String s : toWrite){
            toWrite2.add(s + "," + moduleName);
        }
        AnnotationUtils.writeFile(filer, CanRun.class, toWrite2);
    }
}
