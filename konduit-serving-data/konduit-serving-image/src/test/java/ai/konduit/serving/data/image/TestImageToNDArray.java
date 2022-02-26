package ai.konduit.serving.data.image;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.data.nd4j.util.ND4JUtil;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.bytedeco.opencv.opencv_core.Mat;
import org.datavec.image.loader.NativeImageLoader;
import org.junit.Test;
import org.nd4j.common.primitives.Pair;
import org.nd4j.common.resources.Resources;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TestImageToNDArray {

    @Test
    public void testBasic() throws Exception {

        File f = Resources.asFile("data/5_32x32.png");
        NativeImageLoader loader = new NativeImageLoader();
        org.datavec.image.data.Image image = loader.asImageMatrix(f);
        Image i = Image.create(f);
        assertEquals("Image not correct number of channels.",image.getOrigC(),i.channels());
        Mat as = i.getAs(Mat.class);
        assertTrue(as.channels() <= 3);
        Data in = Data.singleton("image", i);

        Pipeline p = SequencePipeline.builder()
                .add(new ImageToNDArrayStep()
                        .config(new ImageToNDArrayConfig()
                                .height(128)
                                .width(128)
                                .includeMinibatchDim(true)
                        )
                )
                .build();

        PipelineExecutor exec = p.executor();

        Data out = exec.exec(in);

        assertTrue(out.has("image"));
        assertEquals(ValueType.NDARRAY, out.type("image"));
        NDArray arr = out.getNDArray("image");
        assertEquals(NDArrayType.FLOAT, arr.type());
        assertArrayEquals(new long[]{1, 3, 128, 128}, arr.shape());
    }

    @Test
    public void testColorChannels(){
        //Make sure we get right channels order - for given


        int h = 32;
        int w = 28; //Intentionally different for this test

        for(boolean scaleNorm : new boolean[]{false, true}) {

            for (String color : new String[]{"red", "green", "blue"}) {
                int[][][] rgbArr = singleColorRGB(h, w, color);
                BufferedImage bi = toBufferedImage(rgbArr);
                Image i = Image.create(bi);

                Data d = Data.singleton("image", i);

                for (boolean rgb : new boolean[]{true, false}) {
                    for (NDFormat f : NDFormat.values()) {           //Channels first or channels last
                        for (boolean leadingDim : new boolean[]{false, true}) {
                            System.out.println(color + " - rgb=" + rgb + " - " + f + " - scaleNorm = " + scaleNorm);

                            Pipeline p = SequencePipeline.builder()
                                    .add(new ImageToNDArrayStep()
                                            .config( new ImageToNDArrayConfig()
                                                    .height(h)
                                                    .width(w)
                                                    .channelLayout(rgb ? NDChannelLayout.RGB : NDChannelLayout.BGR)
                                                    .format(f)
                                                    .includeMinibatchDim(leadingDim)
                                                    .normalization(new ImageNormalization(scaleNorm ? ImageNormalization.Type.SCALE_01 : ImageNormalization.Type.NONE))
                                            )
                                    )
                                    .build();

                            PipelineExecutor exec = p.executor();

                            Data out = exec.exec(d);

                            assertTrue(out.has("image"));
                            assertEquals(ValueType.NDARRAY, out.type("image"));
                            NDArray arr = out.getNDArray("image");
                            assertEquals(NDArrayType.FLOAT, arr.type());

                            long[] expShape;
                            if (f == NDFormat.CHANNELS_FIRST) {
                                expShape = leadingDim ? new long[]{1, 3, h, w} : new long[]{3, h, w};
                            } else {
                                expShape = leadingDim ? new long[]{1, h, w, 3} : new long[]{h, w, 3};
                            }

                            assertArrayEquals(expShape, arr.shape());

                            //Check actual values:
                            float[][][] fArr;
                            if (leadingDim) {
                                fArr = arr.getAs(float[][][][].class)[0];
                            } else {
                                fArr = arr.getAs(float[][][].class);
                            }
                            int cDim = (f == NDFormat.CHANNELS_FIRST) ? 0 : 2;
                            int idx255 = -1;
                            if (rgb) {
                                switch (color) {
                                    case "red":
                                        idx255 = 0;
                                        break;
                                    case "green":
                                        idx255 = 1;
                                        break;
                                    case "blue":
                                        idx255 = 2;
                                }
                            } else {
                                //BGR
                                switch (color) {
                                    case "red":
                                        idx255 = 2;
                                        break;
                                    case "green":
                                        idx255 = 1;
                                        break;
                                    case "blue":
                                        idx255 = 0;
                                }
                            }

                            float maxExp = scaleNorm ? 1.0f : 255.0f;
                            if (f == NDFormat.CHANNELS_FIRST) {
                                //CHW
                                for (int y = 0; y < h; y++) {
                                    for (int x = 0; x < w; x++) {
                                        for (int c = 0; c < 3; c++) {
                                            if (c == idx255) {
                                                assertEquals(maxExp, fArr[c][y][x], 0.0f);
                                            } else {
                                                assertEquals(0, fArr[c][y][x], 0.0f);
                                            }
                                        }
                                    }
                                }
                            } else {
                                //HWC
                                for (int y = 0; y < h; y++) {
                                    for (int x = 0; x < w; x++) {
                                        for (int c = 0; c < 3; c++) {
                                            if (c == idx255) {
                                                assertEquals(maxExp, fArr[y][x][c], 0.0f);
                                            } else {
                                                assertEquals(0, fArr[y][x][c], 0.0f);
                                            }
                                        }
                                    }
                                }
                            }

                            String json = p.toJson();
                            Pipeline pJson = Pipeline.fromJson(json);
                            assertEquals(p, pJson);

                            Data outPJson = pJson.executor().exec(d);
                            assertEquals(out, outPJson);
                        }
                    }
                }
            }
        }
    }


    @Test
    public void testDataTypes() {
        //Test image -> Float16, Float32, Float64, int8, int16, int32, int64, uint8, unt16, uint32, uint64
        for(NDArrayType t : new NDArrayType[]{NDArrayType.FLOAT, NDArrayType.DOUBLE,
                NDArrayType.INT8, NDArrayType.INT16, NDArrayType.INT32, NDArrayType.INT64,
                //TODO - these types: not yet supported
                //NDArrayType.FLOAT16, NDArrayType.BFLOAT16, NDArrayType.UINT8, NDArrayType.UINT16, NDArrayType.UINT32, NDArrayType.UINT64
        }){

            int h = 32;
            int w = 28; //Intentionally different for this test

            for(String color : new String[]{"red", "green", "blue"}) {
                int[][][] rgbArr = singleColorRGB(h, w, color);
                BufferedImage bi = toBufferedImage(rgbArr);
                Image i = Image.create(bi);

                Data d = Data.singleton("image", i);

                for (boolean rgb : new boolean[]{true, false}) {
                    for (NDFormat f : NDFormat.values()) {           //Channels first or channels last
                        for (boolean leadingDim : new boolean[]{false, true}) {
                            System.out.println(t + " - " + color + " - rgb=" + rgb + " - " + f);
                            Pipeline p = SequencePipeline.builder()
                                    .add(new ImageToNDArrayStep()
                                            .config(new ImageToNDArrayConfig()
                                                    .height(h)
                                                    .width(w)
                                                    .channelLayout(rgb ? NDChannelLayout.RGB : NDChannelLayout.BGR)
                                                    .format(f)
                                                    .includeMinibatchDim(leadingDim)
                                                    .dataType(t)
                                            )
                                    )
                                    .build();

                            PipelineExecutor exec = p.executor();
                            Data out = exec.exec(d);
                            NDArray n = out.getNDArray("image");

                            assertTrue(out.has("image"));
                            assertEquals(ValueType.NDARRAY, out.type("image"));
                            NDArray arr = out.getNDArray("image");
                            assertEquals(t, arr.type());

                            long[] expShape;
                            if(f == NDFormat.CHANNELS_FIRST){
                                expShape = leadingDim ? new long[]{1, 3, h, w} : new long[]{3, h, w};
                            } else {
                                expShape = leadingDim ? new long[]{1, h, w, 3} : new long[]{h, w, 3};
                            }

                            assertArrayEquals(expShape, arr.shape());

                            assertEquals(t, n.type());


                            String json = p.toJson();
                            Pipeline pJson = Pipeline.fromJson(json);
                            assertEquals(p, pJson);

                            Data outPJson = pJson.executor().exec(d);
                            assertEquals(out, outPJson);
                        }
                    }
                }
            }
        }
    }

    public int[][][] singleColorRGB(int h, int w, String color){

        int idx;
        switch (color.toLowerCase()){
            case "red":
                idx = 0;
                break;
            case "green":
                idx = 1;
                break;
            case "blue":
                idx = 2;
                break;
            default:
                throw new RuntimeException();
        }

        int[][][] out = new int[3][h][w];
        for( int y=0; y<h; y++ ){
            for( int x=0; x<w; x++ ){
                out[idx][y][x] = 255;
            }
        }

        return out;
    }


    /**
     * Convert an integer array (CHW, in RGB format) to a BufferedImage
     * @param img
     * @return
     */
    public static BufferedImage toBufferedImage(int[][][] img){

        int h = img[0].length;
        int w = img[0][0].length;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for( int i=0; i<h; i++){
            for( int j=0; j<w; j++ ){
                int rgb = img[0][i][j] << 16 | img[1][i][j] << 8 | img[2][i][j];
                bi.setRGB(j, i, rgb);
            }
        }

        return bi;
    }

    public static BufferedImage createConstantImageRgb(int h, int w, int red, int green, int blue){
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for( int i=0; i<h; i++){
            for( int j=0; j<w; j++ ){
                int rgb = red << 16 | green << 8 | blue;
                bi.setRGB(j, i, rgb);
            }
        }

        return bi;
    }



    @Test
    public void testImageNormalizationRgbBgr(){
        //Test image normalization - RGB and BGR

        int r = 255;
        int g = 128;
        int b = 32;
        BufferedImage bi = createConstantImageRgb(32, 32, r, g, b);

        double[] meanRgb = {128, 200, 50};
        double[] stdRgb = {180, 140, 100};

        Data in = Data.singleton("image", Image.create(bi));

        for(ImageNormalization.Type normType : ImageNormalization.Type.values()){
            float expR, expG, expB;
            switch (normType){
                case NONE:
                    expR = r;
                    expG = g;
                    expB = b;
                    break;
                case SCALE:
                    float f = 255/2.0f;
                    expR = r / f - 1.0f;
                    expG = g / f - 1.0f;
                    expB = b / f - 1.0f;
                    break;
                case SCALE_01:
                    expR = r / 255f;
                    expG = g / 255f;
                    expB = b / 255f;
                    break;
                case SUBTRACT_MEAN:
                    expR = r - (float)meanRgb[0];
                    expG = g - (float)meanRgb[1];
                    expB = b - (float)meanRgb[2];
                    break;
                case STANDARDIZE:
                    expR = (r - (float)meanRgb[0]) / (float)stdRgb[0];
                    expG = (g - (float)meanRgb[1]) / (float)stdRgb[1];
                    expB = (b - (float)meanRgb[2]) / (float)stdRgb[2];
                    break;
                case INCEPTION:
                    expR = ((r / 255f) - 0.5f) * 2.0f;
                    expG = ((g / 255f) - 0.5f) * 2.0f;
                    expB = ((b / 255f) - 0.5f) * 2.0f;
                    break;
                case VGG_SUBTRACT_MEAN:
                    double[] vggmean = ImageNormalization.getVggMeanRgb();
                    expR = r - (float)vggmean[0];
                    expG = g - (float)vggmean[1];
                    expB = b - (float)vggmean[2];
                    break;
                case IMAGE_NET:
                    double[] imagenetRgbMean = ImageNormalization.getImagenetMeanRgb();
                    double[] imagenetRgbStd = ImageNormalization.getImagenetMeanRgb();
                    expR = r - (float)imagenetRgbMean[0] / (float) imagenetRgbStd[0];
                    expG = g - (float)imagenetRgbMean[1] / (float) imagenetRgbStd[1];
                    expB = b - (float)imagenetRgbMean[2] / (float) imagenetRgbStd[2];
                    break;
                default:
                    throw new RuntimeException();
            }

            boolean needsMean = normType == ImageNormalization.Type.SUBTRACT_MEAN || normType == ImageNormalization.Type.STANDARDIZE  || normType == ImageNormalization.Type.IMAGE_NET;
            boolean needsStd = normType == ImageNormalization.Type.STANDARDIZE || normType == ImageNormalization.Type.IMAGE_NET;

            ImageNormalization norm = new ImageNormalization()
                    .type(normType)
                    .meanRgb(needsMean ? meanRgb : null)
                    .stdRgb(needsStd ? stdRgb : null);


            for(boolean rgb : new boolean[]{true, false}) {
                for(NDFormat f : NDFormat.values()) {


                    Pipeline p = SequencePipeline.builder()
                            .add(new ImageToNDArrayStep()
                                    .outputNames(Collections.singletonList("im2ndarray"))
                                    .config(new ImageToNDArrayConfig()
                                            .normalization(norm)
                                            .height(32)
                                            .width(32)
                                            .channelLayout(rgb ? NDChannelLayout.RGB : NDChannelLayout.BGR)
                                            .format(f)
                                            .includeMinibatchDim(false)
                                            .dataType(NDArrayType.FLOAT)
                                    )
                            )
                            .build();

                    PipelineExecutor exec = p.executor();
                    Data out = exec.exec(in);

                    NDArray arr = out.getNDArray("im2ndarray");
                    assertEquals(NDArrayType.FLOAT, arr.type());

                    float[][][] fArr = arr.getAs(float[][][].class);


                    if(f == NDFormat.CHANNELS_FIRST){
                        //CHW
                        for( int c = 0; c<3; c++){
                            float e;
                            if(rgb) {
                                if(c == 0) e = expR;
                                else if(c == 1) e = expG;
                                else e = expB;
                            } else {
                                if(c == 0) e = expB;
                                else if(c == 1) e = expG;
                                else e = expR;
                            }

                            for( int y=0; y<32; y++){
                                for( int x=0; x<32; x++ ){
                                    float a = fArr[c][y][x];
                                    assertEquals(e, a, 1e-4f);
                                }
                            }
                        }
                    } else {
                        //HWC
                        for( int y=0; y<32; y++){
                            for( int x=0; x<32; x++ ){
                                for( int c = 0; c<3; c++){
                                    float e;
                                    if(rgb) {
                                        if(c == 0) e = expR;
                                        else if(c == 1) e = expG;
                                        else e = expB;
                                    } else {
                                        if(c == 0) e = expB;
                                        else if(c == 1) e = expG;
                                        else e = expR;
                                    }

                                    float a = fArr[y][x][c];
                                    assertEquals(e, a, 1e-4f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testMetaData(){

        int inH = 32;
        int inW = 48;

        int r = 255;
        int g = 128;
        int b = 32;
        BufferedImage bi = createConstantImageRgb(inH, inW, r, g, b);

        Data in = Data.singleton("image", Image.create(bi));

        List<Pair<Integer,Integer>> outHWVals = Arrays.asList(Pair.of(32, 48), Pair.of(32,32), Pair.of(32,16), Pair.of(16, 32));

        for(Pair<Integer,Integer> outHW : outHWVals){


            int oH = outHW.getFirst();
            int oW = outHW.getSecond();

            Pipeline p = SequencePipeline.builder()
                    .add(new ImageToNDArrayStep()
                            .metadata(true)
                            .metadataKey("Metakey")
                            .outputNames(Arrays.asList("myNDArray"))
                            .config(new ImageToNDArrayConfig()
                                    .height(oH)
                                    .width(oW)
                                    .includeMinibatchDim(false)
                                    .dataType(NDArrayType.FLOAT)
                            )
                    )
                    .build();

            PipelineExecutor exec = p.executor();

            Data out = exec.exec(in);

            Data meta = out.getMetaData();
            assertNotNull(meta);
            assertTrue(meta.has("Metakey"));

            Data d = meta.getData("Metakey");
            assertTrue(d.has(ImageToNDArrayStep.META_INNAME_KEY));
            assertTrue(d.has(ImageToNDArrayStep.META_OUTNAME_KEY));
            assertTrue(d.has(ImageToNDArrayStep.META_CROP_REGION));
            assertTrue(d.has(ImageToNDArrayStep.META_IMG_H));
            assertTrue(d.has(ImageToNDArrayStep.META_IMG_W));

            assertEquals(inH, d.getLong(ImageToNDArrayStep.META_IMG_H));
            assertEquals(inW, d.getLong(ImageToNDArrayStep.META_IMG_W));
            assertEquals("myNDArray", d.getString(ImageToNDArrayStep.META_OUTNAME_KEY));
            assertEquals("image", d.getString(ImageToNDArrayStep.META_INNAME_KEY));

            BoundingBox bb = d.getBoundingBox(ImageToNDArrayStep.META_CROP_REGION);
//            System.out.println(bb);

            //Check crop region
            double eX1;
            double eX2;
            double eY1;
            double eY2;

            double aspectImage = inW / (double)inH;
            double aspectNDArray = oW / (double)oH;

            if(oW == inW && aspectImage == aspectNDArray){
                eX1 = 0.0;
                eX2 = 1.0;
            } else {
                if(aspectImage > aspectNDArray){
                    //Crop from width dimension
                    int croppedImgW = (int)(aspectNDArray * inH);
                    int delta = inW - croppedImgW;
                    eX1 = (delta / 2) / (double)inW;
                    eX2 = (inW - delta / 2) / (double)inW;
                } else {
                    //Crop from height dimension
                    eX1 = 0.0;
                    eX2 = 1.0;
                }
            }

            if(oH == inH && aspectImage == aspectNDArray){
                eY1 = 0.0;
                eY2 = 1.0;
            } else {
                if(aspectImage > aspectNDArray){
                    //Crop from width dimension
                    eY1 = 0.0;
                    eY2 = 1.0;
                } else {
                    //Crop from height dimension
                    int croppedImgH = (int)(inH / aspectNDArray * aspectImage);
                    double delta = inH - croppedImgH;
                    eY1 = (delta / 2) / (double)inH;
                    eY2 = (inH - delta / 2) / (double)inH;
                }
            }

            double pX1 = eX1 * inW;
            double pX2 = eX2 * inW;
            double pY1 = eY1 * inH;
            double pY2 = eY2 * inH;

            assertEquals(eX1, bb.x1(), 1e-6);
            assertEquals(eX2, bb.x2(), 1e-6);
            assertEquals(eY1, bb.y1(), 1e-6);
            assertEquals(eY2, bb.y2(), 1e-6);
        }


    }

    @Test
    public void testTypes() {
        int oH = 128;
        int oW = 128;

        File f = Resources.asFile("data/mona_lisa.png");

        Pipeline p = SequencePipeline.builder()
                .add(new ImageToNDArrayStep()
                        .metadata(false)
                        .config(new ImageToNDArrayConfig()
                                .height(oH)
                                .width(oW)
                                .includeMinibatchDim(false)
                                .dataType(NDArrayType.FLOAT)
                                .normalization(null)
                        )
                )
                .build();

        Data in = Data.singleton("image", Image.create(f));
        Data out = p.executor().exec(in);

        INDArray expFloat = out.getNDArray("image").getAs(INDArray.class);



        for(NDArrayType t : NDArrayType.values()){
            if(t == NDArrayType.BOOL || t == NDArrayType.UTF8)
                continue;

            //TODO Casting/conversion bug? LOOK INTO + FIX
            if(t == NDArrayType.BFLOAT16 || t == NDArrayType.UINT64 || t == NDArrayType.UINT32 || t == NDArrayType.UINT16)
                continue;

            if(t == NDArrayType.UINT32)     //TODO TEMPORARY IGNORE - https://github.com/KonduitAI/deeplearning4j/pull/458
                continue;

            System.out.println("===== " + t + " =====");

            DataType ndt = ND4JUtil.typeNDArrayTypeToNd4j(t);
            INDArray exp = expFloat.castTo(ndt);

            Pipeline p2 = SequencePipeline.builder()
                    .add(new ImageToNDArrayStep()
                            .metadata(false)
                            .config(new ImageToNDArrayConfig()
                                    .height(oH)
                                    .width(oW)
                                    .includeMinibatchDim(false)
                                    .dataType(t)
                                    .normalization(null)
                            )
                    )
                    .build();

            Data out2 = p2.executor().exec(in);
            INDArray act = out2.getNDArray("image").getAs(INDArray.class);

            assertEquals(exp, act);
        }
    }

    @Test
    public void testBatching(){

        Image i1 = Image.create(createConstantImageRgb(64, 64, 255, 0, 0));
        Image i2 = Image.create(createConstantImageRgb(32, 32, 0, 255, 0));
        Image i3 = Image.create(createConstantImageRgb(16, 16, 0, 0, 255));
        //public static BufferedImage createConstantImageRgb(int h, int w, int red, int green, int blue){

        List<Image> inputImages = Arrays.asList(i1, i2, i3);
        int oH = 48;
        int oW = 48;

        Data in = Data.singletonList("images", inputImages, ValueType.IMAGE);

        double[] meanRgb = {128, 200, 50};
        double[] stdRgb = {180, 140, 100};

        for(ImageToNDArrayConfig.ListHandling lh : ImageToNDArrayConfig.ListHandling.values()){
            for(boolean incMB : new boolean[]{false, true}) {
                for(ImageNormalization.Type normType : ImageNormalization.Type.values()) {
                    for (NDFormat f : NDFormat.values()) {
                        System.out.println(lh + " - incMB=" + incMB + ", normType=" + normType);

                        boolean needsMean = normType == ImageNormalization.Type.SUBTRACT_MEAN || normType == ImageNormalization.Type.STANDARDIZE;
                        boolean needsStd = normType == ImageNormalization.Type.STANDARDIZE;

                        ImageNormalization norm = new ImageNormalization()
                                .type(normType)
                                .meanRgb(needsMean ? meanRgb : null)
                                .stdRgb(needsStd ? stdRgb : null);

                        Pipeline p = SequencePipeline.builder()
                                .add(new ImageToNDArrayStep()
                                        .metadata(false)
                                        .config(new ImageToNDArrayConfig()
                                                .height(oH)
                                                .width(oW)
                                                .includeMinibatchDim(incMB)
                                                .dataType(NDArrayType.FLOAT)
                                                .normalization(norm)
                                                .listHandling(lh)
                                                .format(f)
                                        )
                                        .keys(Collections.singletonList("images"))
                                        .outputNames(Collections.singletonList("out"))
                                )
                                .build();

                        Pipeline pSingle = SequencePipeline.builder()
                                .add(new ImageToNDArrayStep()
                                        .metadata(false)
                                        .config(new ImageToNDArrayConfig()
                                                .height(oH)
                                                .width(oW)
                                                .includeMinibatchDim(incMB)
                                                .dataType(NDArrayType.FLOAT)
                                                .normalization(norm)
                                                .listHandling(ImageToNDArrayConfig.ListHandling.NONE)
                                                .format(f)
                                        )
                                        .keys(Collections.singletonList("images"))
                                        .outputNames(Collections.singletonList("out"))
                                )
                                .build();


                        Data out = null;
                        try {
                            out = p.executor().exec(in);
                        } catch (Throwable t) {
                            if (lh == ImageToNDArrayConfig.ListHandling.NONE) {
//                            t.printStackTrace();
                                String msg = t.getMessage();
                                assertTrue(msg, msg.contains("NONE") && msg.contains("ListHandling"));
                                continue;
                            } else {
                                throw t;
                            }
                        }


                        if (lh == ImageToNDArrayConfig.ListHandling.LIST_OUT) {
                            assertEquals(ValueType.LIST, out.type("out"));
                            assertEquals(ValueType.NDARRAY, out.listType("out"));
                            List<NDArray> l = out.getListNDArray("out");
                            assertEquals(3, l.size());
                            for (NDArray arr : l) {
                                if (incMB) {
                                    if (f == NDFormat.CHANNELS_FIRST) {
                                        assertArrayEquals(new long[]{1, 3, oH, oW}, arr.shape());
                                    } else {
                                        assertArrayEquals(new long[]{1, oH, oW, 3}, arr.shape());
                                    }
                                } else {
                                    if (f == NDFormat.CHANNELS_FIRST) {
                                        assertArrayEquals(new long[]{3, oH, oW}, arr.shape());
                                    } else {
                                        assertArrayEquals(new long[]{oH, oW, 3}, arr.shape());
                                    }
                                }
                            }

                            List<NDArray> outExp = new ArrayList<>();
                            PipelineExecutor exec = pSingle.executor();
                            for (Image i : inputImages) {
                                outExp.add(exec.exec(Data.singleton("images", i)).getNDArray("out"));
                            }
                            assertEquals(outExp, l);
                        } else {
                            //Batch or first
                            assertEquals(ValueType.NDARRAY, out.type("out"));
                            NDArray arr = out.getNDArray("out");
                            PipelineExecutor exec = pSingle.executor();

                            boolean first = lh == ImageToNDArrayConfig.ListHandling.FIRST;
                            if(first){
                                if (incMB) {
                                    if (f == NDFormat.CHANNELS_FIRST) {
                                        assertArrayEquals(new long[]{1, 3, oH, oW}, arr.shape());
                                    } else {
                                        assertArrayEquals(new long[]{1, oH, oW, 3}, arr.shape());
                                    }
                                } else {
                                    if (f == NDFormat.CHANNELS_FIRST) {
                                        assertArrayEquals(new long[]{3, oH, oW}, arr.shape());
                                    } else {
                                        assertArrayEquals(new long[]{oH, oW, 3}, arr.shape());
                                    }
                                }
                            } else {
                                if (f == NDFormat.CHANNELS_FIRST) {
                                    assertArrayEquals(new long[]{3, 3, oH, oW}, arr.shape());
                                } else {
                                    assertArrayEquals(new long[]{3, oH, oW, 3}, arr.shape());
                                }
                            }

                            if(incMB){
                                float[][][][] f4 = arr.getAs(float[][][][].class);
                                int idx = 0;
                                for(Image i : inputImages){
                                    float[][][][] f4a = exec.exec(Data.singleton("images", i)).getNDArray("out").getAs(float[][][][].class);
                                    boolean eq = Arrays.deepEquals(f4[idx++], f4a[0]);
                                    assertTrue(eq);

                                    if(first)
                                        break;;
                                }
                            } else {
                                if(first){
                                    float[][][] f3 = arr.getAs(float[][][].class);
                                    int idx = 0;
                                    float[][][] f3_2 = exec.exec(Data.singleton("images", inputImages.get(0))).getNDArray("out").getAs(float[][][].class);
                                    boolean eq = Arrays.deepEquals(f3, f3_2);
                                    assertTrue(eq);
                                } else {
                                    float[][][][] f4 = arr.getAs(float[][][][].class);
                                    int idx = 0;
                                    for (Image i : inputImages) {
                                        float[][][] f3 = exec.exec(Data.singleton("images", i)).getNDArray("out").getAs(float[][][].class);
                                        boolean eq = Arrays.deepEquals(f4[idx++], f3);
                                        assertTrue(eq);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


    }


    @Test
    public void testImageNormalizationNonRgb(){
        //TODO Test image normalization - RGBA, BGRA, GRAYSCALE

        System.out.println("***** NON-RGB NORMALIZATION NOT YET IMPLEMENTED *****");
    }

}
