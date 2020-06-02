package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.data.Data;
import org.eclipse.python4j.PythonException;
import org.eclipse.python4j.PythonObject;
import org.eclipse.python4j.PythonType;

public class PyData extends PythonType<Data> {

    public static final PyData INSTANCE = new PyData();
    public PyData(){
        super("Data", Data.class);
    }
    @Override
    public Data toJava(PythonObject pythonObject) throws PythonException{
        return null;
    }

    @Override
    public PythonObject toPython(Data javaObject) throws PythonException{
        return null;
    }

    @Override
    public boolean accepts(Object javaObject) throws PythonException{
        return false;
    }

    @Override
    public Data adapt(Object javaObject) throws PythonException{
        return null;
    }





}
