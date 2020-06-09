package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import org.bytedeco.javacpp.BytePointer;
import org.eclipse.python4j.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PILImage extends PythonType<Image> {


    private static Boolean isPillowInstalled = null;

    public static final PILImage INSTANCE = new PILImage();

    private static boolean isPillowInstalled() {
        if (isPillowInstalled == null) {
            try {
                Python.importModule("PIL.Image");
                isPillowInstalled = true;
            } catch (PythonException pe) {
                isPillowInstalled = false;
            }
        }
        return isPillowInstalled;
    }

    private static void installPillow() {
        if (!isPillowInstalled()) {
            try {
                PythonProcess.pipInstall("pillow");
            } catch (Exception e) {
                throw new PythonException("Unable to install pillow.", e);
            }

            if (!isPillowInstalled()) {
                throw new PythonException("Pillow not installed.");
            }
        }
    }

    public PILImage() {
        super("PIL.Image.Image", Image.class);
    }

    @Override
    public Image toJava(PythonObject pythonObject) {
        String typeStr = Python.str(Python.type(pythonObject)).toString();
        if (!typeStr.equals("<class 'PIL.Image.Image'>")) {
            throw new PythonException("Expected PIL.Image.Image, received " + typeStr);
        }
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject bytesIO = Python.importModule("io").attr("BytesIO").call();
            List<PythonObject> args = Collections.singletonList(bytesIO);
            Map<String, String> kwargs = Collections.singletonMap("format", "PNG");
            pythonObject.callWithArgsAndKwargs(args, kwargs);
            PythonObject memview = Python.memoryview(bytesIO.attr("getvalue").call());
            byte[] bytes = PythonTypes.MEMORYVIEW.toJava(memview).getStringBytes();
            return Image.create(new Png(bytes));
        }
    }

    @Override
    public PythonObject toPython(Image javaObject) {
        installPillow();
        Png png = javaObject.getAs(Png.class);
        byte[] bytes = png.getBytes();
        BytePointer bp = new BytePointer(bytes);
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject memview = PythonTypes.MEMORYVIEW.toPython(bp);
            PythonObject bytesIO = Python.importModule("io").attr("BytesIO").call(memview);
            PythonObject pilImage = Python.importModule("PIL.Image").attr("open").call(bytesIO);
            PythonGC.keep(pilImage);
            return pilImage;
        }
    }

    @Override
    public Image adapt(Object javaObject) {
        if (javaObject instanceof Image) {
            return (Image) javaObject;
        }
        throw new PythonException("Cannot cast " + javaObject.getClass() + " to Image.");
    }

    @Override
    public boolean accepts(Object javaObject) {
        return javaObject instanceof Image;
    }


}
