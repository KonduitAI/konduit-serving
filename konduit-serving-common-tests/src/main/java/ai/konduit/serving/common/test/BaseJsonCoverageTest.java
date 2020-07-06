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
package ai.konduit.serving.common.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@Slf4j
public abstract  class BaseJsonCoverageTest {

    protected static Set<Class<?>> allClasses;
    protected static Set<Class<?>> seen;

    public abstract String getPackageName();

    public abstract Object fromJson(Class<?> c, String json);
    public abstract Object fromYaml(Class<?> c, String yaml);

    @Before
    public void before() throws Exception {

        if(allClasses == null) {
            //Perform initialization only once
            //Collect all classes implementing TextConfig interface (i.e., has JSON and YAML conversion support)
             allClasses = new LinkedHashSet<>();
             seen = new LinkedHashSet<>();

            Reflections reflections = new Reflections(getPackageName());
            Class<Object> tcClass = (Class<Object>) Class.forName("ai.konduit.serving.pipeline.api.TextConfig");
            Set<Class<?>> subTypes = reflections.getSubTypesOf(tcClass);

            System.out.println(String.format("All subtypes of %s:", tcClass.getCanonicalName()));
            for (Class<?> c : subTypes) {
                if (!ignores().contains(c)) {
                    int mod = c.getModifiers();
                    if (Modifier.isAbstract(mod) || Modifier.isInterface(mod))
                        continue;
                    allClasses.add(c);
                    System.out.println(c);
                }
            }

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

    public void testConfigSerDe(Object o) {
        try{
            testConfigSerDeHelper(o);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    protected void testConfigSerDeHelper(Object o) throws Exception {
        seen.add(o.getClass());     //Record class for coverage tracking

        Class<Object> tcClass = (Class<Object>) Class.forName("ai.konduit.serving.pipeline.api.TextConfig");
        Method toJsonMethod = tcClass.getDeclaredMethod("toJson");
        Method fromYamlMethod = tcClass.getDeclaredMethod("toYaml");

        String json = (String) toJsonMethod.invoke(o);
        String yaml = (String) fromYamlMethod.invoke(o);

        Object fromJson = fromJson(o.getClass(), json);
        Object fromYaml = fromYaml(o.getClass(), yaml);

        assertEquals("to/from JSON object is not equal", o, fromJson);
        assertEquals("to/from YAML object is not equal ", o, fromYaml);
    }

    public Set<Class<?>> ignores(){
        return Collections.emptySet();
    }
}
