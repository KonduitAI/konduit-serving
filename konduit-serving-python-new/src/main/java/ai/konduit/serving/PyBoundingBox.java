package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.data.BoundingBox;
import org.eclipse.python4j.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PyBoundingBox extends PythonType<BoundingBox> {
    
    
    public static final PyBoundingBox INSTANCE = new PyBoundingBox();
    
    public PyBoundingBox(){
        super("__main__.BoundingBox", BoundingBox.class);
    }
    
    @Override
    public PythonObject toPython(BoundingBox javaObject){
        try(PythonGC gc = PythonGC.watch()){
            PythonObject bboxCls = Python.globals().get("BoundingBox");
            List<PythonObject> args = new ArrayList<>();
            args.add(new PythonObject(javaObject.cx()));
            args.add(new PythonObject(javaObject.cy()));
            args.add(new PythonObject(javaObject.height()));
            args.add(new PythonObject(javaObject.width()));
            Map<String, PythonObject> kwargs = new HashMap<>();
            if (javaObject.label() != null) {
                kwargs.put("label", new PythonObject(javaObject.label()));
            }
            if (javaObject.probability() != null) {
                kwargs.put("probability", new PythonObject(javaObject.probability()));
            }
            PythonObject ret = bboxCls.callWithArgsAndKwargs(args, kwargs);
            PythonGC.keep(ret);
            return ret;
        }
    }
    
    @Override
    public BoundingBox toJava(PythonObject pythonObject){
        try(PythonGC gc = PythonGC.watch()){
            PythonObject label = pythonObject.attr("label");
            PythonObject prob = pythonObject.attr("probability");
            BoundingBox ret = BoundingBox.create(pythonObject.attr("cx").toDouble(),
                    pythonObject.attr("cy").toDouble(),
                    pythonObject.attr("height").toDouble(),
                    pythonObject.attr("width").toDouble(),
                    label.isNone() ? null : label.toString(),
                    prob.isNone() ? null : prob.toDouble());
            return ret;
        }
    }
    
    @Override
    public BoundingBox adapt(Object javaObject){
        if (!(javaObject instanceof BoundingBox)){
            throw new PythonException("Cannot cast " + javaObject.getClass() + " to BoundingBox.");
        }
        return (BoundingBox)javaObject;
    }
    
    @Override
    public boolean accepts(Object javaObject){
        return javaObject instanceof BoundingBox;
    }
    
}
