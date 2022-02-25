/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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
package ai.konduit.serving.data.image.step.capture;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;

@Slf4j
@CanRun(CameraFrameCaptureStep.class)
public class FrameCaptureRunner implements PipelineStepRunner {

    protected final PipelineStep step;
    protected final int skip;
    protected final String outputKey;
    protected boolean initialized;
    protected FrameGrabber grabber;
    protected OpenCVFrameConverter.ToIplImage converter;
    protected boolean loop = false;
    private Runnable init;

    public FrameCaptureRunner(CameraFrameCaptureStep step){
        this.outputKey = step.outputKey();
        this.step = step;
        init = () -> {
            this.initOpenCVFrameGrabber(step);
        };
        this.skip = -1;
    }

    public FrameCaptureRunner(VideoFrameCaptureStep step){
        this.outputKey = step.outputKey();
        this.step = step;
        init = () -> {
            this.initFFmpegFrameGrabber(step);
        };
        this.skip = step.skipFrames() == null ? 0 : step.skipFrames();
    }

    @Override
    public synchronized void close() {
        if(initialized){
            initialized = false;
            try {
                grabber.stop();
                grabber.close();
            } catch (Throwable t){
                log.warn("Error stopping/closing FrameGrabber", t);
            }
        }
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public synchronized Data exec(Context ctx, Data data) {
        if(!initialized)
            init.run();

        try {
            Frame frame = grabber.grab();
            if(frame == null && loop){
                frame = grabber.grab();
            }
            frame = frame.clone();  //Clone otherwise buffer will be reused and async overwritten in async pipelines
            Image i = Image.create(frame);
            //System.out.println("IMAGE: h=" + i.height() + ", w=" + i.width());

            if(skip > 0){
                //using setFrameNumber can be costly - seems to require decoding from the last keyframe?
                //i.e., cost of calling setFrameNumber grows linearly over time, then jumps back to a few MS periodically
                // (presumably once it hits next keyframe)
                int maxFrames = grabber.getLengthInFrames();
                if(skip >= 20){     //TODO this threshold should be selected based
                    int currFrame = grabber.getFrameNumber();
                    int setFrame = Math.min(maxFrames, currFrame + skip);
                    grabber.setFrameNumber(setFrame);
                } else {
                    for( int j=0; j<skip; j++ ) {
                        if(grabber.grab() == null)
                            break;
                    }
                }


            }

            return Data.singleton(outputKey, i);
        } catch (Throwable t){
            throw new RuntimeException("Error getting frame", t);
        }
    }

    protected void initOpenCVFrameGrabber(CameraFrameCaptureStep step){
        grabber = new OpenCVFrameGrabber(step.camera());
        converter = new OpenCVFrameConverter.ToIplImage();

        int w = step.width();
        int h = step.height();
        grabber.setImageHeight(h);
        grabber.setImageWidth(w);

        try {
            grabber.start();
        } catch (Throwable t){
            log.error("Failed to start video frame grabber with step {}", step);
            throw new RuntimeException("Failed to start video frame grabber", t);
        }

        initialized = true;
    }

    protected void initFFmpegFrameGrabber(VideoFrameCaptureStep step){
        grabber = new FFmpegFrameGrabber(step.filePath());

        loop = step.loop();
        converter = new OpenCVFrameConverter.ToIplImage();

        try {
            grabber.start();
        } catch (Throwable t){
            log.error("Failed to start video frame grabber with step {}", step);
            throw new RuntimeException("Failed to start video frame grabber", t);
        }

        initialized = true;
    }
}
