package ai.konduit.serving;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ai.konduit.serving.client.java.invoker.ApiException;
import javafx.beans.binding.ObjectBinding;
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
        input.put("a", 1.0);
        input.put("b", 20.0);

        KonduitServingClient konduitServingClient = KonduitServingClient.builder()
                .useSsl(false)
                .host("localhost")
                .port(8082)
                .build();

        Map<String, Object> output = konduitServingClient.predict(input);
        System.out.println(output);

        Map<String, Object> expectedOutput = new HashMap<>();
        expectedOutput.put("c", 21.0);

        assertEquals(expectedOutput, output);
    }
}
