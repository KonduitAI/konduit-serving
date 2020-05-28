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

package ai.konduit.serving.annotation;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.*;

/**
 * Collect runner metadata:
 * {@code @CanRun(SomePipeline.class)} annotation on a PipelineStepRunner means that the specified PipelineStep can
 * be run by the annotated PipelineStepRunner class. Note that in some cases, a given PipelineStepRunner may not be
 * able to run a particular instance of this type of PipelineStep due to some configuration or versioning issue (but it
 * must be able to run _some_ of these types of PipelineStep instances)
 * <br>
 * During processing, this processor writes a "META-INF/konduit-serving/ai.konduit.serving.annotation.CanRun" file
 * with content like: ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep,ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStepRunner<br>
 * which should be interpreted as "LoggingPipelineStep can be run by LoggingPipelineStepRunner"
 *
 * @author Alex Black
 */
@SupportedAnnotationTypes("ai.konduit.serving.annotation.CanRun")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CanRunProcessor extends AbstractProcessor {

    private List<String> toWrite = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if(env.processingOver()){
            writeFile();
        } else {
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

                String moduleName = annotation.getAnnotation(CanRun.class).moduleName();

                for(String s : values) {
                    toWrite.add(s + "," + annotation.toString() + "," + moduleName);   //Format: pipelineClass,runnerClass - i.e., "this type of pipeline step can be run by this type of runner"
                }
            }
        }

        return true;
    }

    protected void writeFile(){
        Filer filer = processingEnv.getFiler();
        try {

            String outputFile = "META-INF/konduit-serving/" + CanRun.class.getName();
            FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", outputFile);

            try (Writer w = file.openWriter()) {
                boolean first = true;
                for(String s : toWrite){
                    if(!first)
                        w.write("\n");
                    w.write(s);
                    first = false;
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Error in annotation processing", t);
        }
    }
}
