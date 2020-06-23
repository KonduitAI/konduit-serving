package ai.konduit.serving.models.tensorflow;

import ai.konduit.serving.models.tensorflow.step.TensorFlowPipelineStep;
import ai.konduit.serving.models.tensorflow.step.TensorFlowStepRunner;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.impl.context.DefaultContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Collections;

public class TensorflowStepModelTest {

    private ai.konduitai.serving.common.test.TestServer testServer;

    @Before
    public void setUp() throws Exception {
        testServer = new ai.konduitai.serving.common.test.TestServer("http://", "localhost", 9090);
        testServer.start();
    }

    @After
    public void tearDown() throws Exception {
        testServer.stop();
    }

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testTFStepRunner() {
        String uri = "http://localhost:9090/src/test/resources/models/frozen_model.pb";
        TensorFlowPipelineStep step = TensorFlowPipelineStep.builder().
                modelUri(uri).
                inputNames(Collections.singletonList("in")).
                outputNames(Collections.singletonList("out")).
                build();
        TensorFlowStepRunner runner = new TensorFlowStepRunner(step);
        INDArray arr = Nd4j.rand(DataType.FLOAT, 3, 4, 4);
        Data data = Data.singleton("in", NDArray.create(arr));

        runner.exec(new DefaultContext(null,null), data);
    }
}
