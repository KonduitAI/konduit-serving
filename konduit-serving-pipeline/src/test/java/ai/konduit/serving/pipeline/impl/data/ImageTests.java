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

package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.image.PngImage;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

public class ImageTests {

    @Test
    public void testBasicLoadingConversion() throws Exception {

        File f = Resources.asFile("data/5_32x32.png");
        System.out.println(f.getAbsolutePath());

        Image i = Image.create(f);
        assertTrue(i instanceof PngImage);
        assertTrue(i.get() instanceof Png);

        Png p = i.getAs(Png.class);

        byte[] bytes = p.getBytes();
        byte[] expBytes;
        try(InputStream is = new BufferedInputStream(new FileInputStream(f))){
            expBytes = IOUtils.toByteArray(is);
        }

        assertArrayEquals(expBytes, bytes);


        //Data and JSON serialization
        Data d = Data.singleton("myImage", i);
        String json = d.toJson();
        System.out.println(json);
        Data dJson = Data.fromJson(json);
        assertEquals(d, dJson);


        //TODO BufferedImage


    }

}
