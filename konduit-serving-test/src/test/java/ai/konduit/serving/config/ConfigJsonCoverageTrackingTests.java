package ai.konduit.serving.config;

import ai.konduit.serving.model.DL4JConfig;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.model.TensorDataType;
import ai.konduit.serving.model.TensorDataTypesConfig;
import lombok.extern.slf4j.Slf4j;
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

        System.out.println("All subtypes:");
        for(Class<? extends TextConfig> c : subTypes ){
            int mod = c.getModifiers();
            if(Modifier.isAbstract(mod) || Modifier.isInterface(mod))
                continue;
            allClasses.add(c);
//            System.out.println(c);
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
        fail("Not yet implemented");
    }

    @Test
    public void testServingConfig(){
        fail("Not yet implemented");
    }

    @Test
    public void testArrayConcatenationStep(){
        fail("Not yet implemented");
    }

    @Test
    public void testJsonExpanderTransformStep(){
        fail("Not yet implemented");
    }

    @Test
    public void testTransformProcessStep(){
        fail("Not yet implemented");
    }

    @Test
    public void testCustomPipelineStep(){
        fail("Not yet implemented");
    }

    @Test
    public void testPmmlStep(){
        fail("Not yet implemented");
    }

    @Test
    public void testPythonStep(){
        fail("Not yet implemented");
    }

    @Test
    public void testImageLoadingStep(){
        fail("Not yet implemented");
    }

    @Test
    public void testModelStep(){
        fail("Not yet implemented");
    }

    @Test
    public void testMemMapConfig(){
        fail("Not yet implemented");
    }

    @Test
    public void testParallelInferenceConfig(){
        fail("Not yet implemented");
    }

    @Test
    public void testDL4JConfig(){
        DL4JConfig d = DL4JConfig.builder()
                .modelConfigType(ModelConfigType.multiLayerNetwork("/Some/Path/Here"))
                .tensorDataTypesConfig(TensorDataTypesConfig.builder()
                        .inputDataType("in", TensorDataType.FLOAT)
                        .outputDataType("out", TensorDataType.FLOAT)
                        .build())
                .build();

        testConfigSerDe(d);
    }

    @Test
    public void testKerasConfig(){
        fail("Not yet implemented");
    }

    @Test
    public void testPmmlConfig(){
        fail("Not yet implemented");
    }

    @Test
    public void testSameDiffConfig(){
        fail("Not yet implemented");
    }

    @Test
    public void testTensorFlowConfig(){
        fail("Not yet implemented");
    }
}
