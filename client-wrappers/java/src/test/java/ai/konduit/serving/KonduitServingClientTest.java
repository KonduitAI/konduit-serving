package ai.konduit.serving;

import static org.junit.Assert.assertTrue;

import ai.konduit.serving.client.java.invoker.ApiException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for simple KonduitServingClient.
 */
public class KonduitServingClientTest
{
    /**
     * Test predictions with konduit-serving
     */
    @Test
    public void shouldAnswerWithTrue() throws ApiException {
        Map<String, Object> input = new HashMap<>();
        input.put("a", 1);
        input.put("b", 20);

        KonduitServingClient konduitServingClient = KonduitServingClient.builder()
                .useSsl(false)
                .host("localhost")
                .port(8082)
                .build();

        System.out.println(konduitServingClient.predict(input));
    }
}
