
/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */



package ai.konduit.serving.PythonWrapper;

import org.bytedeco.cpython.PyObject;
import org.tensorflow.Session;

import static org.bytedeco.cpython.global.python.*;

import java.util.*;

import static org.bytedeco.cpython.global.python.*;

/**
 * Swift like python wrapper for J
 *
 * @author Fariz Rahman
 */

public class Python {
    public static PythonObject importModule(String moduleName){
        return new PythonObject(PyImport_ImportModule(moduleName));
    }
    public static long len(PythonObject pythonObject){
        return PyObject_Size(pythonObject.getNativePythonObject());
    }
    public static String str(PythonObject pythonObject){
        return pythonObject.toString();
    }
    public static double float_(PythonObject pythonObject){
        return pythonObject.toFloat();
    }
    public static  boolean bool(PythonObject pythonObject){
        return pythonObject.toBoolean();
    }
    public static long int_(PythonObject pythonObject){
        return pythonObject.toLong();
    }
    public static ArrayList list(PythonObject pythonObject){
        throw new RuntimeException("not implemented");
    }
    public static HashMap dict(PythonObject pythonObject){
        throw new RuntimeException("not implemented");
    }
    public static HashSet set(PythonObject pythonObject){
        throw new RuntimeException("not implemented");
    }
}
