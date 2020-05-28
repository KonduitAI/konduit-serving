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
import java.util.*;

@SupportedAnnotationTypes("ai.konduit.serving.annotation.JsonName")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class JsonNameProcessor extends AbstractProcessor {

    private List<String> toWrite = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver()) {
            writeFile();
        } else {
            //Collect info for writing at end
            Collection<? extends Element> c = env.getElementsAnnotatedWith(JsonName.class);
            List<TypeElement> types = ElementFilter.typesIn(c);
            Element jnElement = processingEnv.getElementUtils().getTypeElement(JsonName.class.getName());

            //Get the class values
            //See https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
            TypeMirror canRunType = jnElement.asType();
            for (TypeElement annotation : types) {
                List<? extends AnnotationMirror> l = annotation.getAnnotationMirrors();
                String subtypeOf = null;
                for (AnnotationMirror am : l) {
                    if (am.getAnnotationType().equals(canRunType)) {
                        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                            if ("subclassOf".equals(entry.getKey().getSimpleName().toString())) {
                                String s = entry.getValue().toString();     //ai.konduit.something.SomeClass.class
                                if (s.endsWith(".class")) {
                                    s = s.substring(0, s.length() - 6);
                                }
                                subtypeOf = s;
                            }
                        }
                    }
                }

                String jn = annotation.getAnnotation(JsonName.class).jsonName();
                toWrite.add(jn + "," + annotation.toString() + "," + subtypeOf);      //Format: json_name,class_name,interface_name
            }

        }

        return true;
    }

    protected void writeFile() {
        Filer filer = processingEnv.getFiler();
        AnnotationUtils.writeFile(filer, JsonName.class, toWrite);
    }
}
