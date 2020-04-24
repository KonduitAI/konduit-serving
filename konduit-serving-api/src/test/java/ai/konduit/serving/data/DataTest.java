package ai.konduit.serving.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataTest {
    @Test
    public void testStringData() {
        final String KEY = "stringData";
        final String VALUE = "Some string data";
        Data container = JData.makeData(KEY, VALUE);
        String value = container.getString(KEY);
        assertEquals(VALUE, value);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoData() {
        Double notSupportedValue = 1.0;
        Data container = JData.makeData("key", notSupportedValue);
    }
}
