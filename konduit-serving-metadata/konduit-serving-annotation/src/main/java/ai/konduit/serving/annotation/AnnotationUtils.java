/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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
import java.io.*;
import java.util.List;

public class AnnotationUtils {

    private AnnotationUtils(){ }

    public static void writeFile(Filer filer, Class<?> c, List<String> lines) {
        writeFile(filer, c.getName(), lines);
    }

    public static void writeFile(Filer filer, String c, List<String> lines){
        if(lines.isEmpty())
            return;
        try {
            String outputFile = "META-INF/konduit-serving/" + c;
            FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", outputFile);

            try (Writer w = file.openWriter()) {
                w.write(String.join("\n", lines));
            }

        } catch (Throwable t) {
            throw new RuntimeException("Error in annotation processing", t);
        }
    }

    public static boolean existsAndContains(Filer filer, String c, List<String> lines){
        String outputFile = "META-INF/konduit-serving/" + c;
        if(!fileExists(filer, c))
            return false;
        String content = getContent(filer, c);
        for(String s : lines){
            if(!content.contains(s)){
                return false;
            }
        }
        return true;
    }

    public static boolean fileExists(Filer filer, String c){
        String outputFile = "META-INF/konduit-serving/" + c;
        try {
            FileObject file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", outputFile);
            return file != null;
        } catch (IOException e){
            return false;
        }
    }

    public static String getContent(Filer filer, String c){
        String outputFile = "META-INF/konduit-serving/" + c;
        try {
            FileObject file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", outputFile);
            InputStream is = file.openInputStream();
            StringBuilder sb = new StringBuilder();
            try (Reader r = new BufferedReader(new InputStreamReader(is))) {
                int ch = 0;
                while ((ch = r.read()) != -1) {
                    sb.append((char) ch);
                }
            }
            return sb.toString();
        } catch (IOException e){
            throw new RuntimeException("ERROR READING FILE", e);
        }
    }
}
