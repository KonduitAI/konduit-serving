/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
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
package ai.konduit.serving.config;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.metrics.MetricType;
import ai.konduit.serving.model.*;
import ai.konduit.serving.pipeline.config.ObjectDetectionConfig;
import ai.konduit.serving.pipeline.step.*;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.parallelism.inference.InferenceMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nd4j.base.Preconditions;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Slf4j
public class ConfigJsonCoverageTrackingTests {

    private static final Set<Class<? extends TextConfig>> allClasses = new LinkedHashSet<>();
    private static final Set<Class<? extends TextConfig>> seen = new LinkedHashSet<>();

    @BeforeClass
    public static void before(){

        //Collect all classes implementing TextConfig interface (i.e., has JSON and YAML conversion support)

        Reflections reflections = new Reflections("ai.konduit");
        Set<Class<? extends TextConfig>> subTypes = reflections.getSubTypesOf(TextConfig.class);

        System.out.println(String.format("All subtypes of %s:", TextConfig.class.getCanonicalName()));
        for(Class<? extends TextConfig> c : subTypes ){
            int mod = c.getModifiers();
            if(Modifier.isAbstract(mod) || Modifier.isInterface(mod))
                continue;
            allClasses.add(c);
            System.out.println(c);
        }
    }

    @AfterClass
    public static void afterClass(){
        if(!seen.containsAll(allClasses)){
            List<String> notTested = new ArrayList<>();
            for(Class<?> c : allClasses){
                if(!seen.contains(c)) {
                    notTested.add(c.getName());
                }
            }

            Collections.sort(notTested);
            for(String s : notTested){
                log.warn("Class was not tested for JSON/YAML serialization/deserialization: {}", s);
            }

            fail(notTested.size() + " of " + allClasses.size() + " classes implementing TextConfig were not tested for JSON/YAML serialization and deserialization");
        }
    }

    public static void testConfigSerDe(TextConfig c) {
        try{
            testConfigSerDeHelper(c);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    protected static void testConfigSerDeHelper(TextConfig c) throws Exception {
        seen.add(c.getClass());     //Record class for coverage tracking

        String json = c.toJson();
        String yaml = c.toYaml();

        Method jsonMethod = findStaticFromJson(c);
        Method yamlMethod = findStaticFromYaml(c);

        TextConfig fromJson = (TextConfig) jsonMethod.invoke(null, json);
        TextConfig fromYaml = (TextConfig) yamlMethod.invoke(null, yaml);

        assertEquals("to/from JSON object is not equal", c, fromJson);
        assertEquals("to/from YAML object is not equal ", c, fromYaml);
    }

    private static Method findStaticFromJson(TextConfig c) {
        Method m = findStaticSingleStringArgMethodWithName(c.getClass(), "fromJson");
        Preconditions.checkState(m != null, "No fromJson(String) method is defined on class " + c.getClass().getName() +
                " or any superclasses of this class. All classes implementing TextConfig should also have a static fromJson(String) method");
        return m;
    }

    private static Method findStaticFromYaml(TextConfig c){
        Method m = findStaticSingleStringArgMethodWithName(c.getClass(), "fromYaml");
        Preconditions.checkState(m != null, "No fromJson(String) method is defined on class " + c.getClass().getName() +
                " or any superclasses of this class. All classes implementing TextConfig should also have a static fromYaml(String) method");
        return m;
    }

    private static Method findStaticSingleStringArgMethodWithName(Class<?> c, String methodName){
        Class<?> current = c;
        while(current != Object.class){
            Method[] methods = current.getDeclaredMethods();
            for(Method m : methods){
                if(m.getName().equals(methodName) && m.getParameterCount() == 1 && Modifier.isStatic(m.getModifiers()) &&
                        !Modifier.isAbstract(m.getModifiers()) && m.getParameterTypes()[0] == String.class){
                    return m;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @Test
    public void testInferenceConfiguration(){
        InferenceConfiguration conf = InferenceConfiguration.builder()
                .step(PythonStep.builder()
                        .pythonConfig("x", PythonConfig.builder().pythonCode("import numpy as np\nreturn np.ones(1)").build())
                        .inputNames(Arrays.asList("x"))
                        .outputNames(Arrays.asList("z"))
                        .build())
                .step(ModelStep.builder().modelConfig(DL4JConfig.builder().modelConfigType(ModelConfigType.dl4j("/my/model/path.bin")).build()).build())
                .servingConfig(ServingConfig.builder().httpPort(12345).logTimings(true).build())
                .build();

//        System.out.println(conf.toYaml());

        testConfigSerDe(conf);
    }

    @Test
    public void testServingConfig(){
        testConfigSerDe(ServingConfig.builder().httpPort(12345).logTimings(true)
                .metricTypes(Arrays.asList(MetricType.JVM_GC, MetricType.CLASS_LOADER, MetricType.JVM_MEMORY, MetricType.JVM_THREAD))
                .outputDataFormat(Output.DataFormat.JSON).build());
    }

    @Test
    public void testArrayConcatenationStep(){
        testConfigSerDe(ArrayConcatenationStep.builder().concatDimension(1,2).build());
    }

    @Test
    public void testJsonExpanderTransformStep(){
        testConfigSerDe(JsonExpanderTransformStep.builder()
                .inputNames(Arrays.asList("x", "y"))
                .outputNames(Arrays.asList("z","a"))
                .inputSchema("x", Arrays.asList(SchemaType.NDArray, SchemaType.Boolean))
                .outputSchema("z", Arrays.asList(SchemaType.Image, SchemaType.Categorical))
        .build());
    }

    @Test
    public void testTransformProcessStep(){
        testConfigSerDe(TransformProcessStep.builder()
                .transformProcess("x", new TransformProcess.Builder(new Schema.Builder().addColumnString("col").build())
                        .stringToCategorical("col", Arrays.asList("a","b","c"))
                        .categoricalToOneHot("col")
                        .build())
            .build());
    }

    @Test
    public void testCustomPipelineStep(){
        testConfigSerDe(CustomPipelineStep.builder().customUdfClazz("my.clazz.name.Here").build());
    }

    @Test
    public void testPmmlStep(){
        testConfigSerDe(PmmlStep.builder().inputName("x").outputNames(Arrays.asList("y","z")).build());
    }

    @Test
    public void testPythonStep(){
        testConfigSerDe(PythonStep.builder()
                .pythonConfig("x", PythonConfig.builder().pythonCode("import numpy as np\nreturn np.ones(1)").build())
                .inputNames(Collections.singletonList("x"))
                .outputNames(Collections.singletonList("z"))
                .build());
    }

    @Test
    public void testImageLoadingStep(){
        testConfigSerDe(ImageLoadingStep.builder()
                .inputNames(Collections.singletonList("image_tensor"))
                .outputNames(Collections.singletonList("detection_classes"))
                .objectDetectionConfig(ObjectDetectionConfig.builder().numLabels(80).build())
                .build());
    }

    @Test
    public void testModelStep(){
        testConfigSerDe(ModelStep.builder().modelConfig(DL4JConfig.builder()
                .modelConfigType(ModelConfigType.dl4j("/my/path/here")).build()).build());
    }

    @Test
    public void testMemMapConfig(){
        testConfigSerDe(MemMapConfig.builder().arrayPath("/my/array/path").initialMemmapSize(100000).unkVectorPath("/my/array/unknown").build());
    }

    @Test
    public void testParallelInferenceConfig(){
        testConfigSerDe(ParallelInferenceConfig.defaultConfig());
        testConfigSerDe(ParallelInferenceConfig.builder().batchLimit(5).workers(3).queueLimit(2).inferenceMode(InferenceMode.SEQUENTIAL).build());
    }

    @Test
    public void testDL4JConfig(){
        DL4JConfig d = DL4JConfig.builder()
                .modelConfigType(ModelConfigType.dl4j("/Some/Path/Here"))
                .tensorDataTypesConfig(TensorDataTypesConfig.builder()
                        .inputDataType("in", TensorDataType.FLOAT)
                        .outputDataType("out", TensorDataType.FLOAT)
                        .build())
                .build();

        testConfigSerDe(d);
    }

    @Test
    public void testKerasConfig(){
        testConfigSerDe(KerasConfig.builder().modelConfigType(ModelConfigType.keras("/path/to/model.kdf5"))
                .tensorDataTypesConfig(TensorDataTypesConfig.builder().inputDataType("x",TensorDataType.DOUBLE).build())
                .build());
    }

    @Test
    public void testPmmlConfig(){
        testConfigSerDe(PmmlConfig.defaultConfig());
        testConfigSerDe(PmmlConfig.builder().evaluatorFactoryName("my.factory.class").build());
    }

    @Test
    public void testSameDiffConfig(){
        testConfigSerDe(SameDiffConfig.builder().modelConfigType(ModelConfigType.sameDiff("/my/model/path.fb")).build());
    }

    @Test
    public void testTensorFlowConfig(){
        testConfigSerDe(TensorFlowConfig.builder().build());
    }

    @Test
    public void testModelConfigType(){
        testConfigSerDe(ModelConfigType.keras("/path/to/keras.hdf5"));
        testConfigSerDe(ModelConfigType.tensorFlow("/path/to/tensorflow.pb"));
    }

    @Test
    public void testBertConfig() {
        testConfigSerDe(BertStep.builder().modelPath("/path/to/bert.zip").vocabPath("/path/to/vocab.txt").build());
    }
}
