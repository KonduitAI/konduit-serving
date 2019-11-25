# Doc test example

Basic Python test:

```python
import konduit as kd

server = kd.Server()
```

Basic Java test:

```java
import java.lang.String;

String str = "foo";
```

Konduit specific Java test:

```java
import ai.konduit.serving.InferenceConfiguration;

InferenceConfiguration config = new InferenceConfiguration();
```

Self-contained Java example:

```java
package ai.konduit.serving;

import java.util.Arrays;

public class BasicConfigurationTest {
    public static void main(String[] args) throws Exception {
        String step = getStep();
        System.out.println(step);
    }
    private static String getStep() {
        return "foo";
    }
}
```