/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.data;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.data.image.step.bb.extract.ExtractBoundingBoxStep;
import ai.konduit.serving.data.image.step.capture.CameraFrameCaptureStep;
import ai.konduit.serving.data.image.step.capture.VideoFrameCaptureStep;
import ai.konduit.serving.data.image.step.crop.ImageCropStep;
import ai.konduit.serving.data.image.step.face.DrawFaceKeyPointsStep;
import ai.konduit.serving.data.image.step.grayscale.GrayScaleStep;
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
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JsonCoverageTest extends BaseJsonCoverageTest {


    private ImageToNDArrayConfig c = new ImageToNDArrayConfig()
            .height(256)
            .width(256)
            .channelLayout(NDChannelLayout.RGB)
            .includeMinibatchDim(true)
            .format(NDFormat.CHANNELS_LAST)
            .dataType(NDArrayType.UINT8)
            .aspectRatioHandling(AspectRatioHandling.STRETCH)
            .normalization(new ImageNormalization(ImageNormalization.Type.SUBTRACT_MEAN)
                    .meanRgb(new double[]{0.3, 0.3}).stdRgb(new double[]{0.3, 0.3}).maxValue(3.0));

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
        testConfigSerDe(new ImageToNDArrayStep().config(c).keys("key1","key2")
                .keepOtherValues(false).metadata(true).metadataKey("metadataKey"));
    }


    @Test
    public void testGrayScaleStep() {
        testConfigSerDe(new GrayScaleStep().imageName("2").outputChannels(3));
    }
    @Test
    public void testImageCropStep() {
        testConfigSerDe(new ImageCropStep().cropName("cropName").coordsArePixels(true).imageName("imageName").cropPoints(Collections.singletonList(Point.create(2,2))));
    }

    @Test
    public void testCameraFrameCaptureStep() {
        testConfigSerDe(new CameraFrameCaptureStep().camera(0).outputKey("out").height(128).width(128));
    }

    @Test
    public void testVideoFrameCaptureStep() {
        testConfigSerDe(new VideoFrameCaptureStep().filePath("/file.file").skipFrames(2).outputKey("out").loop(false));
    }

    @Test
    public void testExtractBoundingBoxStep() {

        testConfigSerDe(new ExtractBoundingBoxStep()
                .aspectRatio(0.5)
                .bboxName("foo")
                .imageName("image")
                .keepOtherFields(false)
                .outputName("out")
                .resizeH(360)
                .resizeW(480)
                .imageToNDArrayConfig(c));
    }

    @Test
    public void testDrawFaceKeyPointsStep() {
        testConfigSerDe(new DrawFaceKeyPointsStep()
                                .imageToNDArrayConfig(c)
                                .image("image").landmarkArray("foo")
                                .drawFaceBox(false).faceBoxColor("red")
                                .pointSize(2).pointColor("blue")
                                .resizeH(128).resizeW(128)
                                .scale(DrawFaceKeyPointsStep.Scale.AT_MOST).
                                outputName("out"));
    }

    @Test
    public void testRelativeToAbsoluteStep() {
        testConfigSerDe(new RelativeToAbsoluteStep()
        .imageToNDArrayConfig(c)
        .imageH(128).imageW(128)
        .imageName("image").toConvert("a","b","c"));
    }
    

    @Test
    public void testDrawHeatmapStep() {
        testConfigSerDe(new DrawHeatmapStep()
                .imageToNDArrayConfig(c)
                .keepOtherValues(false)
                .fadingFactor(0.5)
                .width(128)
                .height(128)
                .radius(2)
                .outputName("out")
                .opacity(0.5)
                .points("x","y")
                .image("image")
                .outputName("out"));
    }

    @Test
    public void testPerspectiveTransformStep() {


         List<Point> points = new ArrayList<Point>();
         points.add(Point.create(2,2));
         points.add(Point.create(4,4));


        testConfigSerDe(new PerspectiveTransformStep()
                .sourcePoints(points).sourcePoints(points)
                .inputNames("x","y").outputNames("out")
                .targetPointsName("target").sourcePointsName("src")
                .referenceHeight(128).referenceWidth(128).referenceImage("image").keepOtherFields(false));

    }

    @Test
    public void testImageResizeStep() {
        testConfigSerDe(new ImageResizeStep()
                .inputName("image")
                .inputNames("x","y")
                .width(128).height(128)
                .aspectRatioHandling(AspectRatioHandling.STRETCH));
    }

    @Test
    public void testShowImageStep() {
        testConfigSerDe(new ShowImageStep()
                .displayName("displayName").imageName("imageName")
                .height(128).height(128).allowMultiple(true));
    }

    @Test
    public void testDrawSegmentationStep() {
        testConfigSerDe(new DrawSegmentationStep()
                .image("image")
                .segmentArray("class_idxs")
                .outputName("out")
                .classColors(Arrays.asList("red", "green"))
                .opacity(0.5)
                .backgroundClass(0));
    }

    @Test
    public void testDrawPointsStep() {
        testConfigSerDe(new DrawPointsStep().image("image")
                .imageToNDArrayConfig(c)
                .points(Arrays.asList("img_points"))
                .classColors(Collections.singletonMap("color", "red"))
                .radius(5));
    }

    @Test
    public void testDrawFixedGridStep() {
        testConfigSerDe(new DrawFixedGridStep().borderColor("green")
                .gridColor("blue")
                .coordsArePixels(true)
                .gridX(25)
                .gridY(25)
                .points(Point.create(2,2))
                .imageName("image")
                .borderThickness(4)
                .gridThickness(2));
    }

    @Test
    public void testDrawGridStep() {
        testConfigSerDe(new DrawGridStep()
                .borderColor("green")
                .gridColor("blue")
                .coordsArePixels(false)
                .gridX(3)
                .gridY(10)
                .pointsName("points")
                .imageName("image")
                .borderThickness(10)
                .gridThickness(4));
    }


    @Test
    public void testCropGridStep() {
        testConfigSerDe(new CropGridStep().coordsArePixels(false)
                        .gridX(25)
                        .gridY(25)
                        .pointsName("points")
                        .imageName("image")
                        .keepOtherFields(false)
                        .outputName("output")
                        .boundingBoxName("bbox" )
                        .coordsArePixels(true)
                        .aspectRatio(1.0));
    }


    @Test
    public void testCropFixedGridStep() {
        testConfigSerDe(new CropFixedGridStep()
                .coordsArePixels(false)
                .points(Point.create(2,2))
                .gridX(2)
                .gridY(2)
                .imageName("image")
                .keepOtherFields(false)
                .outputName("out")
                .boundingBoxName("bb")
                .coordsArePixels(true)
                .aspectRatio(1.0));
    }












}
