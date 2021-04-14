package ai.konduit.serving;

import ai.konduit.serving.client.java.InferenceApi;
import ai.konduit.serving.client.java.invoker.ApiClient;
import ai.konduit.serving.client.java.invoker.ApiException;

import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class KonduitServingClient
{
    public static void main( String[] args )
    {
        InferenceApi apiInstance = new InferenceApi(new ApiClient().setBasePath("http://localhost:9009"));
        Map<String, Object> body = new HashMap<>();
        body.put("a", 1);
        body.put("b", 2);

        try {
            Map result = apiInstance.predict(body);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling InferenceApi#predict");
            e.printStackTrace();
        }
    }
}
