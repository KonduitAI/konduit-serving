package ai.konduit.serving.models.tvm;

import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.model.PythonIO;
import ai.konduit.serving.models.tvm.step.TVMStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.python.models.AppendType;
import ai.konduit.serving.pipeline.api.python.models.PythonConfigType;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphBuilder;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import ai.konduit.serving.python.PythonStep;
import com.sun.jna.Platform;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.bytedeco.cpython.PyObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.tvm.runner.TvmRunner;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.bytedeco.cpython.global.python.*;
import static org.bytedeco.cpython.global.python.PyErr_Print;
import static org.bytedeco.cpython.helper.python.Py_AddPath;
import static org.bytedeco.numpy.global.numpy._import_array;
import static org.junit.jupiter.api.Assertions.*;

public class TVMRunnerTest {
    static String pythonPath = tvmPythonPath();
    static String pythonScript(String libPath) {
        return "\"\"\"Script to prepare test_relay_add.so\"\"\"\n"
                + "import tvm\n"
                + "import numpy as np\n"
                + "from tvm import relay\n"
                + "import os,sys\n"

                + "x_var = relay.var(\"x\", shape=(1, 1), dtype=\"float32\")\n"
                + "y_var = relay.var(\"y\", shape=(1, 1), dtype=\"float32\")\n"
                + "params = {\"y\": np.ones((1, 1), dtype=\"float32\")}\n"
                + "mod = tvm.IRModule.from_expr(relay.Function([x_var, y_var], x_var + y_var))\n"
                + "# build a module\n"
                + "compiled_lib = relay.build(mod, tvm.target.create(\"llvm\"), params=params)\n"
                + "# export it as a shared library\n"
                + String.format("dylib_path='%s' \n",StringEscapeUtils.escapeJava(new File(libPath,binaryFileName()).getAbsolutePath()))
                + "print(dylib_path);sys.stdout.flush()\n"
                + "compiled_lib.export_library(dylib_path)\n"
                + "x = x_pass_through; y = y_pass_through\n";
    }

    static void PrepareTestLibs(String libPath) throws Exception {
        Py_AddPath(pythonPath);
        Py_Initialize();
        if (_import_array() < 0) {
            System.err.println("numpy.core.multiarray failed to import");
            PyErr_Print();
            System.exit(-1);
        }

        PyObject globals = PyModule_GetDict(PyImport_AddModule("__main__"));

        PyRun_StringFlags(pythonScript(libPath),

                Py_file_input, globals, globals, null);

        if (PyErr_Occurred() != null) {
            System.err.println("Python error occurred");
            PyErr_Print();
            System.exit(-1);
        }



    }

    @SneakyThrows
    private static String tvmPythonPath() {
        String pythonPath = StringUtils.join(org.bytedeco.tvm.presets.tvm.cachePackages(),File.pathSeparator);
        return pythonPath;
    }

    private static String binaryFileName() {
        String binaryName = "test_relay_add" + (Platform.isWindows() ? ".dll" : Platform.isMac() ? ".dylib" : ".so");
        return binaryName;
    }

    @Test
    public void testAdd2(@TempDir Path tempDir) throws Exception {
        /* try to use MKL when available */
        System.setProperty("org.bytedeco.openblas.load", "mkl");

        File libPath = tempDir.resolve("lib").toFile();
        assertTrue(libPath.mkdirs());
        String pythonScript = pythonScript(libPath.getAbsolutePath().replace(File.separatorChar, '/'));
        PythonConfig pythonConfig = PythonConfig.builder()
                .appendType(AppendType.NONE)
                .pythonLibrariesPath(pythonPath)
                .pythonConfigType(PythonConfigType.JAVACPP)
                .pythonPath(pythonPath)
                .ioInput("x_pass_through", PythonIO.builder().type(ValueType.NDARRAY).build())
                .ioInput("y_pass_through", PythonIO.builder().type(ValueType.NDARRAY).build())
                .ioOutput("x", PythonIO.builder().type(ValueType.NDARRAY).pythonType("numpy.ndarray").build())
                .ioOutput("y",PythonIO.builder().type(ValueType.NDARRAY).pythonType("numpy.ndarray").build())
                .pythonCode(pythonScript).build();

        PythonStep pythonStep = new PythonStep().pythonConfig(pythonConfig);
        assertTrue(libPath.exists());
        File f = new File(libPath,  binaryFileName());
        INDArray x = Nd4j.scalar(1.0f).reshape(1,1);
        TVMStep tvmStep = new TVMStep().modelUri(f.toURI().toString())
                .lazyInit(true)
                .inputNames("x","y").outputNames("0");
        SequencePipeline sequencePipeline = SequencePipeline.builder()
                .add(pythonStep)
                .add(tvmStep)
                .build();
        assertNotNull(sequencePipeline);
        assertNotNull(sequencePipeline.executor());
        PipelineExecutor executor = sequencePipeline.executor();
        Data inputData = Data.empty();
        inputData.put("x_pass_through",NDArray.create(x));
        inputData.put("y_pass_through",NDArray.create(x.dup()));
        Data x1 = executor.exec(inputData);
        assertEquals(x1.getNDArray("0").getAs(INDArray.class).sumNumber().doubleValue(),2.0);


    }

}
