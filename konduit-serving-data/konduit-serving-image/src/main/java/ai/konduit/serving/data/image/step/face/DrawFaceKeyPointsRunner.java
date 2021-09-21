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

package ai.konduit.serving.data.image.step.face;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.util.ColorUtil;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.bytedeco.opencv.opencv_core.*;

import java.util.List;

import static ai.konduit.serving.data.image.step.face.CropUtil.accountForCrop;
import static ai.konduit.serving.data.image.step.face.CropUtil.scaleIfRequired;

@CanRun(DrawFaceKeyPointsStep.class)
public class DrawFaceKeyPointsRunner implements PipelineStepRunner {


    protected final DrawFaceKeyPointsStep step;

    public DrawFaceKeyPointsRunner(@NonNull DrawFaceKeyPointsStep step) {
        this.step = step;
    }

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {


        Image img = data.getImage(step.image());

        Mat m = img.getAs(Mat.class);

        float[][] landmarkArr = data.getNDArray(step.landmarkArray()).getAs(float[][].class);
        List<BoundingBox> faces_bboxes = data.getListBoundingBox("img_bbox");
        Mat scaled = scaleIfRequired(m, this.step);

        ImageToNDArrayConfig im2ndConf = step.imageToNDArrayConfig();


        if (!faces_bboxes.isEmpty()) {
            for (BoundingBox face_bbox : faces_bboxes) {
                BoundingBox bb = accountForCrop(img, face_bbox, im2ndConf);

                if(step.drawFaceBox()) {
                    double x1 = Math.min(bb.x1(), bb.x2());
                    double y1 = Math.min(bb.y1(), bb.y2());

                    int x = (int) (x1 * scaled.cols());
                    int y = (int) (y1 * scaled.rows());
                    int h = (int) Math.round(bb.height() * scaled.rows());
                    int w = (int) Math.round(bb.width() * scaled.cols());
                    Rect r = new Rect(x, y, w, h);

                    Scalar s;
                    if(step.faceBoxColor() == null){
                        s = ColorUtil.stringToColor(DrawFaceKeyPointsStep.DEFAULT_BOX_COLOR);
                    } else {
                        s = ColorUtil.stringToColor(step.faceBoxColor());
                    }


                    org.bytedeco.opencv.global.opencv_imgproc.rectangle(scaled, r, s, 2, 8, 0);
                }

                int prod = landmarkArr.length * landmarkArr[0].length;
                float[][] keypoints = new float[prod/2][2];
                int pos = 0;
                for(int i=0; i<landmarkArr.length; i++ ){
                    for( int j=0; j<landmarkArr[0].length; j++ ){
                        keypoints[pos/2][pos%2] = landmarkArr[i][j];
                        pos++;
                    }
                }

                for (int i = 0; i < keypoints.length; i++) {
                    //Currently, keypoints coordinates are specified in terms of the face bounding box.
                    //We need to translate them to overall image pixels
                    double xp = (bb.x1() + keypoints[i][0] * bb.width()) * img.width();
                    double yp = (bb.y1() + keypoints[i][1] * bb.height()) * img.height();

                    Point point = new Point((int)xp, (int)yp);

                    Scalar s;
                    if(step.pointColor() == null){
                        s = ColorUtil.stringToColor(DrawFaceKeyPointsStep.DEFAULT_POINT_COLOR);
                    } else {
                        s = ColorUtil.stringToColor(step.pointColor());
                    }
                    int size = step.pointSize();

                    org.bytedeco.opencv.global.opencv_imgproc.circle(scaled, point, size, s);
                }

                if (im2ndConf.listHandling() == ImageToNDArrayConfig.ListHandling.FIRST || im2ndConf.listHandling() == ImageToNDArrayConfig.ListHandling.NONE) {
                    break;
                }
            }
        }
        String outputName = step.outputName();
        if (outputName == null) {
            outputName = DrawFaceKeyPointsStep.DEFAULT_OUTPUT_NAME;
        }

        return Data.singleton(step.image(), Image.create(scaled));

    }
}


