package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.impl.step.bbox.filter.BoundingBoxFilterStep;
import ai.konduit.serving.pipeline.impl.step.bbox.point.BoundingBoxToPointStep;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.pipeline.impl.step.ml.regression.RegressionOutputStep;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.SSDToBoundingBoxStep;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.common.base.Preconditions;
import org.reflections.Reflections;
import org.slf4j.event.Level;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Slf4j
public class JsonCoverageTest  extends BaseJsonCoverageTest {

    private static final Set<Class<? extends TextConfig>> allClasses = new LinkedHashSet<>();
    private static final Set<Class<? extends TextConfig>> seen = new LinkedHashSet<>();

    @Override
    public String getPackageName() {
        return new Reflections().getClass().getPackage().getName();
    }

    @Before
    public void before() {

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
    public static void afterClass() {
        if(!seen.containsAll(allClasses)) {
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
    public void testBoundingBoxFilterStep(){
        testConfigSerDe(new BoundingBoxFilterStep()
                .classesToKeep(new String[]{"x","y"})
                .inputName("foo")
                .outputName("bar"));
    }

    @Test
    public void testLoggingStep(){
        testConfigSerDe(new LoggingStep().log(LoggingStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO));
    }


    @Test
    public void testBoundingBoxToPointStep(){
        testConfigSerDe(new BoundingBoxToPointStep()
                .bboxName("x")
                .outputName("y"));
    }

    @Test
    public void testSSDToBoundingBoxStep(){
        testConfigSerDe(new SSDToBoundingBoxStep()
                .outputName("y"));
    }

    @Test
    public void testRegressionOutputStep(){
        testConfigSerDe(new RegressionOutputStep()
                .inputName("in")
                .names(Map.Entry())
    }



}
