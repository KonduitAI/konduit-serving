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

package ai.konduit.serving.python;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import org.bytedeco.javacpp.BytePointer;
import org.nd4j.python4j.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PILImage extends PythonType<Image> {


    private static Boolean isPillowInstalled = null;

    public static final PILImage INSTANCE = new PILImage();

    private static boolean isPillowInstalled() {
        if (isPillowInstalled == null) {
            isPillowInstalled = PythonProcess.isPackageInstalled("pillow");
//            try {
//                Python.importModule("PIL.Image");
//                isPillowInstalled = true;
//            } catch (PythonException pe) {
//                isPillowInstalled = false;
//            }
        }
        return isPillowInstalled;
    }

    private static void installPillow() {
        if (!isPillowInstalled()) {
            try {
                PythonProcess.pipInstall("pillow");
            } catch (Exception e) {
                throw new PythonException("Unable to install pillow.", e);
            }

            if (!isPillowInstalled()) {
                throw new PythonException("Pillow not installed.");
            }
        }
    }

    public PILImage() {
        super("PIL.Image.Image", Image.class);
    }

    @Override
    public Image toJava(PythonObject pythonObject) {

        try (PythonGC gc = PythonGC.watch()) {
            String typeStr = Python.str(Python.type(pythonObject)).toString();
            if (!Python.isinstance(pythonObject, pythonType())){
                throw new PythonException("Expected PIL.Image.Image, received " + typeStr);
            }
            PythonObject bytesIO = Python.importModule("io").attr("BytesIO").call();
            List<PythonObject> args = Collections.singletonList(bytesIO);
            Map<String, String> kwargs = Collections.singletonMap("format", "PNG");
            pythonObject.attr("save").callWithArgsAndKwargs(args, kwargs);
            PythonObject pybytes = bytesIO.attr("getvalue").call();
            byte[] bytes = PythonTypes.BYTES.toJava(pybytes);
            return Image.create(new Png(bytes));
        }
    }

    @Override
    public PythonObject toPython(Image javaObject) {
        installPillow();
        Png png = javaObject.getAs(Png.class);
        byte[] bytes = png.getBytes();
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject pybytes = PythonTypes.BYTES.toPython(bytes);
            PythonObject bytesIO = Python.importModule("io").attr("BytesIO").call(pybytes);
            PythonObject pilImage = Python.importModule("PIL.Image").attr("open").call(bytesIO);
            PythonGC.keep(pilImage);
            return pilImage;

        }
    }

    @Override
    public Image adapt(Object javaObject) {
        if (javaObject instanceof Image) {
            return (Image) javaObject;
        }
        throw new PythonException("Cannot cast " + javaObject.getClass() + " to Image.");
    }

    @Override
    public boolean accepts(Object javaObject) {
        return javaObject instanceof Image;
    }

    @Override
    public PythonObject pythonType(){
        try(PythonGC gc = PythonGC.watch()){
            PythonObject ret = Python.importModule("PIL.Image").attr("Image");
            PythonGC.keep(ret);
            return ret;
        }
    }

}
