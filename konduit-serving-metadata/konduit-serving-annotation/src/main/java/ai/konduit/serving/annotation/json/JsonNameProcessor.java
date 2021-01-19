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

package ai.konduit.serving.annotation.json;

import ai.konduit.serving.annotation.AnnotationUtils;
import ai.konduit.serving.annotation.module.ModuleInfo;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({"ai.konduit.serving.annotation.json.JsonName",
        "ai.konduit.serving.annotation.module.ModuleInfo"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class JsonNameProcessor extends AbstractProcessor {
    private static final String PIPELINE_STEP = "ai.konduit.serving.pipeline.api.step.PipelineStep";
    private static final String SWITCH_FN = "ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchFn";
    private static final String GRAPH_STEP = "ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep";
    private static final String TRIGGER = "ai.konduit.serving.pipeline.api.pipeline.Trigger";

    private List<String> toWrite = new ArrayList<>();
    private List<JsonSubType> subTypes = new ArrayList<>();
    private String moduleName;
    private String moduleClass;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver()) {
            writeFile();
        } else {
            //Get module name
            if(moduleName == null){
                Collection<? extends Element> c = env.getElementsAnnotatedWith(ModuleInfo.class);
                List<TypeElement> types = ElementFilter.typesIn(c);
                for(TypeElement te : types){
                    moduleName = te.getAnnotation(ModuleInfo.class).value();
                    moduleClass = te.toString();
                    break;
                }
            }

            //Collect JSON subtype info for writing at end
            Collection<? extends Element> c = env.getElementsAnnotatedWith(JsonName.class);
            List<TypeElement> types = ElementFilter.typesIn(c);

            for (TypeElement annotation : types) {
                TypeMirror t = annotation.asType();
                TypeMirror pipelineStepTypeMirror = processingEnv.getElementUtils().getTypeElement(PIPELINE_STEP).asType();
                TypeMirror switchFnTypeMirror = processingEnv.getElementUtils().getTypeElement(SWITCH_FN).asType();
                TypeMirror graphStepTypeMirror = processingEnv.getElementUtils().getTypeElement(GRAPH_STEP).asType();
                TypeMirror triggerMirror = processingEnv.getElementUtils().getTypeElement(TRIGGER).asType();
                boolean isPS = processingEnv.getTypeUtils().isAssignable(t, pipelineStepTypeMirror);
                boolean isSF = processingEnv.getTypeUtils().isAssignable(t, switchFnTypeMirror);
                boolean isGS = processingEnv.getTypeUtils().isAssignable(t, graphStepTypeMirror);
                boolean isT = processingEnv.getTypeUtils().isAssignable(t, triggerMirror);
                if(isPS || isSF || isGS || isT){
                    String str;
                    if(isPS){
                        str = PIPELINE_STEP;
                    } else if(isSF){
                        str = SWITCH_FN;
                    } else if(isGS) {
                        str = GRAPH_STEP;
                    } else {
                        str = TRIGGER;
                    }

                    String jn = annotation.getAnnotation(JsonName.class).value();
                    toWrite.add(jn + "," + annotation.toString() + "," + str);      //Format: json_name,class_name,interface_name
                    subTypes.add(new JsonSubType(jn, annotation.toString(), str));
                }
            }
        }

        return true;
    }

    protected void writeFile() {
        Filer filer = processingEnv.getFiler();
        if(filer == null) {
            System.err.println("No filer found. Returning.");
            return;
        }
        AnnotationUtils.writeFile(filer, JsonName.class, toWrite);

        //Also write the SubTypesMapping class (to get info via service loader)
        //TODO we have 2 redundant sources of the same info here. the AnnotationUtils txt file is good for collecting info
        // for project-wide aggregation, but is bad for use in service loader etc (non-unique names)
        //This is better than manual JSON subtype mapping, but still isn't ideal

        String name = className();
        if(moduleClass == null) {
            return;
        }
        int idx = moduleClass.lastIndexOf(".");
        String fullName;
        String pkg = null;
        if (idx > 0) {
            pkg = moduleClass.substring(0, idx);
            fullName = pkg + "." + name;
        } else {
            fullName = name;
        }

        StringBuilder sb = new StringBuilder();
        if(pkg != null){
            sb.append("package ").append(pkg).append(";");
        }

        sb.append("import ai.konduit.serving.pipeline.api.serde.JsonSubType;\n")
                .append("import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;\n")
                .append("import ai.konduit.serving.pipeline.api.serde.JsonSubType;\n")
                .append("\n")
                .append("import java.util.ArrayList;\n")
                .append("import java.util.List;\n");

        sb.append("//GENERATED CLASS DO NOT EDIT\n");
        sb.append("public class ").append(name).append(" implements JsonSubTypesMapping {")
                .append("    @Override\n")
                .append("    public List<JsonSubType> getSubTypesMapping() {\n")
                .append("        List<JsonSubType> l = new ArrayList<>();\n");

        for(JsonSubType j : subTypes){
            sb.append("        l.add(new JsonSubType(\"")
                    .append(j.name).append("\", ")
                    .append(j.className).append(".class, ")
                    .append(j.subtypeOf).append(".class")
                    .append("));\n");
        }

        sb.append("        \n")
                .append("        return l;\n")
                .append("    }\n")
                .append("}");

        String s = sb.toString();

        try {
            FileObject fo = filer.createSourceFile(fullName);
            try (Writer w = fo.openWriter()) {
                w.write(s);
            }
        } catch (Throwable t){
            t.printStackTrace();
        }

        //Finally, also create the service loader file
        try {
            try{
                //Delete if it already exists
                FileObject file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping");
                file.delete();
            } catch (IOException e){ }

            FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping");
            try (Writer w = file.openWriter()) {
                w.write(fullName);
            }
        } catch (IOException e){
            throw new RuntimeException("Error writing ");
        }
    }

    private static class JsonSubType {
        private String name;
        private String className;
        private String subtypeOf;

        public JsonSubType(String name, String className, String subtypeOf){
            this.name = name;
            this.className = className;
            this.subtypeOf = subtypeOf;
        }
    }

    private String className(){
        if(moduleName == null) {
          return "";
        }

        String[] split = moduleName.split("-");
        StringBuilder sb = new StringBuilder();
        for(String s : split){
            sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        }
        sb.append("JsonMapping");
        String s =  sb.toString();
        return s;
    }
}
