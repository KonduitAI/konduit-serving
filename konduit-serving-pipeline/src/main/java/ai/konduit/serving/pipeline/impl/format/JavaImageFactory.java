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

package ai.konduit.serving.pipeline.impl.format;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.exception.DataLoadingException;
import ai.konduit.serving.pipeline.api.format.ImageFactory;
import ai.konduit.serving.pipeline.impl.data.image.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JavaImageFactory implements ImageFactory {

    private static final Set<Class<?>> supportedClasses = new HashSet<>();
    static {
        Set<Class<?>> s = supportedClasses;
        s.add(File.class);
        s.add(Path.class);
        s.add(Png.class);
        s.add(Jpeg.class);
        s.add(Bmp.class);
        s.add(BufferedImage.class);
    }

    @Override
    public Set<Class<?>> supportedTypes() {
        return Collections.unmodifiableSet(supportedClasses);
    }

    @Override
    public boolean canCreateFrom(Object o) {
        //TODO what about interfaces, subtypes etc?
        return supportedClasses.contains(o.getClass());
    }

    @Override
    public Image create(Object o) {
        if(o instanceof File || o instanceof Path){
            //Try to infer file type from
            File f;
            if(o instanceof File){
                f = (File) o;
            } else {
                f = ((Path)o).toFile();
            }
            String name = f.getName().toLowerCase();
            if(name.endsWith(".png")){
                return new PngImage(new Png(f));
            } else if(name.endsWith(".jpg") || name.endsWith(".jpeg")){
                return new JpegImage(new Jpeg(f));
            } else if(name.endsWith(".bmp")){
                return new BmpImage(new Bmp(f));
            }
            throw new DataLoadingException("Unable to create Image object: unable to guess image file format from File" +
                    " path/filename, or format not supported - " + f.getAbsolutePath());
        } else if(o instanceof Png) {
            return new PngImage((Png) o);
        } else if(o instanceof Jpeg){
            return new JpegImage((Jpeg)o);
        } else if(o instanceof Bmp){
            return new BmpImage((Bmp)o);
        } else if(o instanceof BufferedImage){
            return new BImage((BufferedImage) o);
        } else {
            throw new UnsupportedOperationException("Unable to create Image from object of type: " + o.getClass());
        }
    }
}
