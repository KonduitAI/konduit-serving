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

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.List;

public class AnnotationUtils {

    private AnnotationUtils(){ }

    public static void writeFile(Filer filer, Class<?> c, List<String> lines){
        try {
            String outputFile = "META-INF/konduit-serving/" + c.getName();
            FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", outputFile);

            try (Writer w = file.openWriter()) {
                w.write(String.join("\n", lines));
            }

        } catch (Throwable t) {
            throw new RuntimeException("Error in annotation processing", t);
        }
    }
}
