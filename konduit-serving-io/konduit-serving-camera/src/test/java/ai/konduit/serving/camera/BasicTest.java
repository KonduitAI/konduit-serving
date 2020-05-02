package ai.konduit.serving.camera;

import org.bytedeco.ffmpeg.presets.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.Test;

import static org.bytedeco.opencv.global.opencv_core.cvFlip;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvSaveImage;

public class BasicTest {

    @Test
    public void basicTest() throws Exception {


//        FrameGrabber grabber = FrameGrabber.createDefault(0);
//        grabber.start();
//
//        Thread.sleep(2000);

//        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
//
//        Mat grabbedImage = converter.convert(grabber.grab());
//        int height = grabbedImage.rows();
//        int width = grabbedImage.cols();

        int height = 720;
        int width = 1280;
        FrameRecorder recorder = FrameRecorder.createDefault("C:/Temp/javacv/output_" + System.currentTimeMillis() + ".avi", width, height);
        recorder.start();

        Thread.sleep(5000);

        recorder.stop();
    }

    @Test
    public void test2() throws Exception {
        String FILENAME = "C:/Temp/javacv/test_" + System.currentTimeMillis() + ".mp4";

        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();
        Frame grabbedImage = grabber.grab();

        CanvasFrame canvasFrame = new CanvasFrame("Cam");
        canvasFrame.setCanvasSize(grabbedImage.imageWidth, grabbedImage.imageHeight);

        System.out.println("framerate = " + grabber.getFrameRate());
        grabber.setFrameRate(grabber.getFrameRate());
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(FILENAME,  grabber.getImageWidth(),grabber.getImageHeight());

        recorder.setVideoCodec(13);
        recorder.setFormat("mp4");
//        recorder.setPixelFormat(avutil.PIX_FMT_YUV420P);
        recorder.setFrameRate(30);
        recorder.setVideoBitrate(10 * 1024 * 1024);

        recorder.start();
        while (canvasFrame.isVisible() && (grabbedImage = grabber.grab()) != null) {
            canvasFrame.showImage(grabbedImage);
            recorder.record(grabbedImage);
        }
        recorder.stop();
        grabber.stop();
        canvasFrame.dispose();

    }

    @Test
    public void test3() throws Exception {
        //1920 x 1080

        CanvasFrame canvas = new CanvasFrame("WebCam Demo");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        VideoInputFrameGrabber webcam = new VideoInputFrameGrabber(0); // 1 for next camera

        FrameGrabber grabber = webcam;

        grabber.start();
        Frame img;
        while (true) {
            img = grabber.grab();
            if (img != null) {
                canvas.showImage(img);
            }
        }
    }


    final int INTERVAL = 100;///you may use interval
    CanvasFrame canvas = new CanvasFrame("Web Cam");

    @Test
    public void test4() throws Exception {
        FrameGrabber grabber = new VideoInputFrameGrabber(0); // 1 for next camera
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        //OpenCVFrameConverter.ToMat converter1 = new OpenCVFrameConverter.ToMat();
        IplImage img;
        int i = 0;
        try {
            grabber.start();
            while (true) {
                Frame frame = grabber.grab();

                img = converter.convert(frame);

                //the grabbed frame will be flipped, re-flip to make it right
                cvFlip(img, img, 1);// l-r = 90_degrees_steps_anti_clockwise

                //save
                cvSaveImage("C:/Temp/javacv/img_" + (i++) + ".jpg", img);


                canvas.showImage(converter.convert(img));

                Thread.sleep(INTERVAL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
