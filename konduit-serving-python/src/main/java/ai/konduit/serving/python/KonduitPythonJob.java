package ai.konduit.serving.python;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.python4j.*;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * PythonJob is the right abstraction for executing multiple python scripts
 * in a multi thread stateful environment. The setup-and-run mode allows your
 * "setup" code (imports, model loading etc) to be executed only once.
 */
@Data
@Slf4j
public class KonduitPythonJob {


    private String code;
    private String name;
    private String context;
    private final boolean setupRunMode;
    private PythonObject runF;
    private final AtomicBoolean setupDone = new AtomicBoolean(false);
    private boolean useGil= false;
    static {
        new PythonExecutioner();
    }

    @Builder
    /**
     * @param name Name for the python job.
     * @param code Python code.
     * @param setupRunMode If true, the python code is expected to have two methods: setup(), which takes no arguments,
     *                     and run() which takes some or no arguments. setup() method is executed once,
     *                     and the run() method is called with the inputs(if any) per transaction, and is expected to return a dictionary
     *                     mapping from output variable names (str) to output values.
     *                     If false, the full script is run on each transaction and the output variables are obtained from the global namespace
     *                     after execution.
     * @param useGil       whether to use {@link PythonGIL#lock()} when executing the job
     */
    public KonduitPythonJob(@Nonnull String name, @Nonnull String code, boolean setupRunMode,boolean useGil) {
        this.name = name;
        this.code = code;
        this.setupRunMode = setupRunMode;
        this.useGil = useGil;
        context = "__job_" + name;
        if (PythonContextManager.hasContext(context)) {
            throw new PythonException("Unable to create python job " + name + ". Context " + context + " already exists!");
        }
    }


    /**
     * Clears all variables in current context and calls setup()
     */
    public void clearState(){
        PythonContextManager.setContext(this.context);
        PythonContextManager.reset();
        setupDone.set(false);
        setup();
    }

    public void setup() {
        if (setupDone.get()) return;
        if(useGil) {
            doSetup();
        }
        else {
            try (PythonGIL gil = PythonGIL.lock()) {
                doSetup();
            }
        }

    }

    private void doSetup() {
        PythonContextManager.setContext(context);
        PythonObject runF = PythonExecutioner.getVariable("run");

        if (runF == null || runF.isNone() || !Python.callable(runF)) {
            PythonExecutioner.exec(code);
            runF = PythonExecutioner.getVariable("run");
        }
        if (runF.isNone() || !Python.callable(runF)) {
            throw new PythonException("run() method not found! " +
                    "If a PythonJob is created with 'setup and run' " +
                    "mode enabled, the associated python code is " +
                    "expected to contain a run() method " +
                    "(with or without arguments).");
        }
        this.runF = runF;
        PythonObject setupF = PythonExecutioner.getVariable("setup");
        if (!setupF.isNone()) {
            setupF.call();
        }
        setupDone.set(true);
    }


    public void exec(List<PythonVariable> inputs, List<PythonVariable> outputs) {
        if (setupRunMode)
            setup();
        if(useGil) {
            try (PythonGIL gil = PythonGIL.lock()) {
                try (PythonGC _ = PythonGC.watch()) {
                    execJob(inputs, outputs);
                }
            }
        }
        else {
            try (PythonGC _ = PythonGC.watch()) {
                execJob(inputs, outputs);
            }

        }


    }

    private void execJob(List<PythonVariable> inputs, List<PythonVariable> outputs) {
        PythonContextManager.setContext(context);

        if (!setupRunMode) {

            PythonExecutioner.exec(code, inputs, outputs);

            return;
        }

        PythonExecutioner.setVariables(inputs);

        PythonObject inspect = Python.importModule("inspect");
        PythonObject getfullargspec = inspect.attr("getfullargspec");
        PythonObject argspec = getfullargspec.call(runF);
        PythonObject argsList = argspec.attr("args");
        PythonObject runargs = Python.dict();
        int argsCount = Python.len(argsList).toInt();
        for (int i = 0; i < argsCount; i++) {
            PythonObject arg = argsList.get(i);
            PythonObject val = Python.globals().get(arg);
            if (val.isNone()) {
                throw new PythonException("Input value not received for run() argument: " + arg.toString());
            }
            runargs.set(arg, val);
        }
        PythonObject outDict = runF.callWithKwargs(runargs);
        PythonObject globals = Python.globals();
        PythonObject updateF = globals.attr("update");
        updateF.call(outDict);
        PythonExecutioner.getVariables(outputs);
    }

    public List<PythonVariable> execAndReturnAllVariables(List<PythonVariable> inputs){
        if (setupRunMode)setup();
        try (PythonGIL gil = PythonGIL.lock()) {
            try (PythonGC _ = PythonGC.watch()) {
                PythonContextManager.setContext(context);
                if (!setupRunMode) {
                    return PythonExecutioner.execAndReturnAllVariables(code, inputs);
                }
                PythonExecutioner.setVariables(inputs);
                PythonObject inspect = Python.importModule("inspect");
                PythonObject getfullargspec = inspect.attr("getfullargspec");
                PythonObject argspec = getfullargspec.call(runF);
                PythonObject argsList = argspec.attr("args");
                PythonObject runargs = Python.dict();
                int argsCount = Python.len(argsList).toInt();
                for (int i = 0; i < argsCount; i++) {
                    PythonObject arg = argsList.get(i);
                    PythonObject val = Python.globals().get(arg);
                    if (val.isNone()) {
                        throw new PythonException("Input value not received for run() argument: " + arg.toString());
                    }
                    runargs.set(arg, val);
                }

                PythonObject outDict = runF.callWithKwargs(runargs);
                PythonObject globals = Python.globals();
                PythonObject updateF = globals.attr("update");
                updateF.call(outDict);
                return PythonExecutioner.getAllVariables();
            }

        }
    }


}