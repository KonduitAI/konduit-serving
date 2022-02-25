/* ******************************************************************************
 * Copyright (c) 2022 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.python;

import org.nd4j.python4j.Python;
import org.nd4j.python4j.PythonObject;
import org.nd4j.python4j.PythonType;

/**
 * Represents the none type in python.
 *
 * @author Adam Gibson
 */
public class NoneType extends PythonType<Object> {

    private static NoneType INSTANCE = new NoneType();

    public static NoneType instance() {
        return INSTANCE;
    }


    private NoneType() {
        super("None", Object.class);
    }

    @Override
    public Object toJava(PythonObject pythonObject) {
        return INSTANCE;
    }

    @Override
    public PythonObject toPython(Object o) {
        return Python.None();
    }
}
