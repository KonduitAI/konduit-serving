package ai.konduit.serving;

import org.eclipse.python4j.PythonTypes;
import org.junit.Assert;
import org.junit.Test;

public class PyDataServiceLoaderTest{
    @Test
    public void testPyDataServiceLoader(){
        Assert.assertEquals(PyData.INSTANCE, PythonTypes.get("Data"));
    }
}
