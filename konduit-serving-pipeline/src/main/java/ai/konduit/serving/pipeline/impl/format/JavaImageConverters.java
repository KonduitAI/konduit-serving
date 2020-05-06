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

package ai.konduit.serving.pipeline.impl.format;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.format.ImageConverter;
import ai.konduit.serving.pipeline.api.format.ImageFormat;
import org.nd4j.common.base.Preconditions;

public class JavaImageConverters {

    private JavaImageConverters(){ }

    public static class IdentityConverter implements ImageConverter {

        @Override
        public boolean canConvert(Image from, ImageFormat<?> to) {
            return false;
        }

        @Override
        public boolean canConvert(Image from, Class<?> to) {
            return to.isAssignableFrom(from.get().getClass());
        }

        @Override
        public <T> T convert(Image from, ImageFormat<T> to) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public <T> T convert(Image from, Class<T> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert %s to %s", from.get().getClass(), to);
            return (T)from.get();
        }
    }

}
