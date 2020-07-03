package ai.konduit.serving.data;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.step.bb.extract.ExtractBoundingBoxStep;
import ai.konduit.serving.data.image.step.capture.CameraFrameCaptureStep;
import ai.konduit.serving.data.image.step.capture.VideoFrameCaptureStep;
import ai.konduit.serving.data.image.step.crop.ImageCropStep;
import ai.konduit.serving.data.image.step.face.DrawFaceKeyPointsStep;
import ai.konduit.serving.data.image.step.grid.crop.CropFixedGridStep;
import ai.konduit.serving.data.image.step.grid.crop.CropGridStep;
import ai.konduit.serving.data.image.step.grid.draw.DrawFixedGridStep;
import ai.konduit.serving.data.image.step.grid.draw.DrawGridStep;
import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.data.image.step.point.convert.RelativeToAbsoluteStep;
import ai.konduit.serving.data.image.step.point.draw.DrawPointsStep;
import ai.konduit.serving.data.image.step.point.heatmap.DrawHeatmapStep;
import ai.konduit.serving.data.image.step.point.perspective.convert.PerspectiveTransformStep;
import ai.konduit.serving.data.image.step.resize.ImageResizeStep;
import ai.konduit.serving.data.image.step.segmentation.index.DrawSegmentationStep;
import ai.konduit.serving.data.image.step.show.ShowImageStep;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;

public class JsonCoverageTest extends BaseJsonCoverageTest {

    @Override
    public String getPackageName() {
        return "ai.konduit.serving.data.image";
    }

    @Override
    public Object fromJson(Class<?> c, String json) {
        return ObjectMappers.fromJson(json, c);
    }

    @Override
    public Object fromYaml(Class<?> c, String yaml) {
        return ObjectMappers.fromYaml(yaml, c);
    }

    @Test
    public void testImageToNDArrayStep() {
        testConfigSerDe(new ImageToNDArrayStep().config(new ImageToNDArrayConfig()
                .height(128)
                .width(128)));
    }

    @Test
    public void testImageCropStep() {
        testConfigSerDe(new ImageCropStep());
    }

    @Test
    public void testCameraFrameCaptureStep() {
        testConfigSerDe(new CameraFrameCaptureStep());
    }

    @Test
    public void testVideoFrameCaptureStep() {
        testConfigSerDe(new VideoFrameCaptureStep());
    }

    @Test
    public void testExtractBoundingBoxStep() {
        testConfigSerDe(new ExtractBoundingBoxStep());
    }

    @Test
    public void testDrawFaceKeyPointsStep() {
        testConfigSerDe(new DrawFaceKeyPointsStep());
    }

    @Test
    public void testRelativeToAbsoluteStep() {
        testConfigSerDe(new RelativeToAbsoluteStep());
    }
    

    @Test
    public void testDrawHeatmapStep() {
        testConfigSerDe(new DrawHeatmapStep());
    }

    @Test
    public void testPerspectiveTransformStep() {
        testConfigSerDe(new PerspectiveTransformStep());
    }

    @Test
    public void testImageResizeStep() {
        testConfigSerDe(new ImageResizeStep());
    }

    @Test
    public void testShowImageStep() {
        testConfigSerDe(new ShowImageStep());
    }

    @Test
    public void testDrawSegmentationStep() {
        testConfigSerDe(new DrawSegmentationStep());
    }

    @Test
    public void testDrawPointsStep() {
        testConfigSerDe(new DrawPointsStep());
    }

    @Test
    public void testDrawFixedGridStep() {
        testConfigSerDe(new DrawFixedGridStep());
    }

    @Test
    public void testDrawGridStep() {
        testConfigSerDe(new DrawGridStep());
    }


    @Test
    public void testCropGridStep() {
        testConfigSerDe(new CropGridStep());
    }


    @Test
    public void testCropFixedGridStep() {
        testConfigSerDe(new CropFixedGridStep());
    }












}
