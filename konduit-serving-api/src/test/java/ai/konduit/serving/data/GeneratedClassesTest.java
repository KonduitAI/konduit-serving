package ai.konduit.serving.data;

import org.junit.Test;

import generated.Data;
import static org.junit.Assert.assertEquals;

public class GeneratedClassesTest {
    @Test
    public void testsDataSchemeBuilder() {
        Data.DataScheme innerScheme = Data.DataScheme.newBuilder().
                setKey("inner").
                build();

        Data.DataScheme scheme = Data.DataScheme.newBuilder().
                setKey("keygen").
                setMetaData(innerScheme).
                build();

        assertEquals("inner", innerScheme.getKey());
        assertEquals("inner", scheme.getMetaData().getKey());
        assertEquals("keygen", scheme.getKey());
    }

    @Test
    public void testStringData() {
        Data.DataScheme container = Data.DataScheme.newBuilder().
                setKey("ok").
                setSValue("svalue").
                setType(Data.DataScheme.ValueType.STRING).build();
        assertEquals("ok", container.getKey());
    }

    @Test
    public void testBoolData() {
        Data.DataScheme container = Data.DataScheme.newBuilder().
                setKey("ok").
                setBoolValue(false).
                setSValue("svalue").
                setType(Data.DataScheme.ValueType.STRING).build();
        assertEquals("ok", container.getKey());

        assertEquals("svalue", container.getSValue());
        assertEquals(false, container.getBoolValue());

    }
}
