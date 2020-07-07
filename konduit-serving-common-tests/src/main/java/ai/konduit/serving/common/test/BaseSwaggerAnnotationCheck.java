package ai.konduit.serving.common.test;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

@Slf4j
public abstract class BaseSwaggerAnnotationCheck {




    public abstract String getPackageName();

    public void runTest() throws ClassNotFoundException {

            Set<Class<?>> failedClasses = new HashSet<Class<?>>();
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
                        log.warn("MISSING ANNOTATION: " + c + " - field " + f.getName());
                        failedClasses.add(c);

                    }
                }



            }

           if (!failedClasses.isEmpty()){
                fail("There are still " + failedClasses.size() + " classes with missing annotation:\n" + failedClasses.stream()
                        .map(n -> n.getCanonicalName())
                        .collect(Collectors.joining("\n")));

           }

        }






    public Set<Class<?>> ignores() {
        return Collections.emptySet();
    }


}
