package ai.konduit.serving.common.test;

import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.Before;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.fail;

@Slf4j
public abstract class BaseSwaggerAnnotationCheck {


    protected static Set<Class<?>> allClasses;
    protected static Set<Class<?>> seen;

    public abstract String getPackageName();

    @Before
    public void runTest() throws ClassNotFoundException {

        if (allClasses == null) {
            //Perform initialization only once
            //Collect all classes implementing TextConfig interface (i.e., has JSON and YAML conversion support)
            allClasses = new LinkedHashSet<>();
            seen = new LinkedHashSet<>();

            Reflections reflections = new Reflections(getPackageName());
            Class<Object> tcClass = (Class<Object>) Class.forName("ai.konduit.serving.pipeline.api.step.PipelineStep");
            Set<Class<?>> subTypes = reflections.getSubTypesOf(tcClass);

            Class<?> schemaClass = Schema.class;

            for (Class<?> c : subTypes) {
                if (ignores().contains(c))
                    continue;   //Skip

                Field[] fields = c.getDeclaredFields();

                for (Field f : fields) {
                    if (Modifier.isStatic(f.getModifiers()))       //Skip static fields
                        continue;

                    boolean foundSchemaAnnotation = false;
                    Annotation[] annotations = f.getDeclaredAnnotations();
                    for (Annotation a : annotations) {
                        if (a.annotationType() == schemaClass) {
                            foundSchemaAnnotation = true;
                            break;
                        }
                    }

                    if (!foundSchemaAnnotation) {
                        fail("MISSING ANNOTATION: " + c + " - field " + f.getName());
                    }
                }


            }
        }


    }

    public Set<Class<?>> ignores() {
        return Collections.emptySet();
    }


}
