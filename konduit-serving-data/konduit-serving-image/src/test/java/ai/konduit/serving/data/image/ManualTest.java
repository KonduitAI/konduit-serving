package ai.konduit.serving.data.image;

import ai.konduit.serving.data.image.step.capture.CameraFrameCaptureStep;
import ai.konduit.serving.data.image.step.show.ShowImageStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Ignore
public class ManualTest {

    @Test
    public void manualTest() throws Exception {

        //TODO 2020/05/03 Camera resolution config doesn't actually work
        int h = 720;
        int w = 1280;

        Pipeline p = SequencePipeline.builder()
                .add(new CameraFrameCaptureStep()
                        .camera(0)
                        .width(w)
                        .height(h)
                        .outputKey("myImage")
                        )
                .add(new ShowImageStep()
                        .displayName("Image Viewer")
                        .width(w)
                        .height(h)
                        .imageName("myImage")
                        )
                .build();

        Data in = Data.empty();

        PipelineExecutor exec = p.executor();


        for( int i=0; i<100; i++ ) {
            exec.exec(in);
            Thread.sleep(100);
        }

        exec.close();

        String json = p.toJson();
        Pipeline pJson = Pipeline.fromJson(json);
        assertEquals(p, pJson);

        String yaml = p.toYaml();
        Pipeline pYaml = Pipeline.fromYaml(yaml);
        assertEquals(p, pYaml);
    }

}
