package ai.konduit.serving.data.image.convert.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 *
 * <ul>
 *     <li><b>NONE</b>: No image normalization will be applied</li>
 *     <li><b>SCALE</b>: Divide images by maxValue, or divide by 255 if maxValue is not specified, to give output in range [0,1]. This is the default.</li>
 *     <li><b>SUBTRACT_MEAN</b>: Subtract the channels by the provided meanRgb array, with values [meanRed, meanGreen, meanBlue].
 *         out = (in - mean) for each channel. Note that if the output format is in BGR format, the meanRgb value should
 *         still be provided in RGB order</li>
 *     <li><b>STANDARDIZE</b>: Subtract the channels by the provided meanRgb array, and then divide by stdRgb, where meanRgb
 *         is [meanRed, meanGreen, meanBlue], and stdRgb is [standardDeviationRed, standardDeviationGreen, standardDeviationBlue].
 *         out = (in - mean)/std for each channel. Note that if the output format is in BGR format, the meanRgb and stdRgb
 *         values should still be provided in RGB order.</li>
 *     <li><b>INCEPTION</b>: Applies inception preprocessing for inference/evaluation as described here: <a href="https://github.com/tensorflow/models/blob/master/research/slim/preprocessing/inception_preprocessing.py">inception_preprocessing.py</a>
 *         Specifcally: preprocess_for_eval method: scale to [-1, 1] range.
 *         In practice this is done by dividing by 255 (assuming pixels are in range 0 to 255) to give [0, 1] then subtracting
 *         0.5 and multiplying by 2 to give [-1, 1]. Note uses maxValue (like SCALE) if provided.
 *         </li>
 *     <li><b>VGG_SUBTRACT_MEAN</b>: As per <i>SUBTRACT_MEAN</i> but the fixed values [meanRed, meanGreen, meanBlue] =
 *         [123.68, 116.779, 103.939]. Note the meanRgb array with these values need not be provided explicitly. If the
 *         output format is BGR, these are appropriately reordered before applying to the channels.</li>
 * </ul>
 *
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Builder
public class ImageNormalization {

    protected static final double[] VGG_MEAN_RGB = {123.68, 116.779, 103.939};
    public static double[] getVggMeanRgb(){
        return VGG_MEAN_RGB.clone();
    }


    public enum Type {
        NONE,
        SCALE,
        SUBTRACT_MEAN,
        STANDARDIZE,
        INCEPTION,
        VGG_SUBTRACT_MEAN
    }

    @Builder.Default
    public Type type = Type.SCALE;

    private Double maxValue;
    private double[] meanRgb;
    private double[] stdRgb;

    public ImageNormalization(){
        this(Type.SCALE);
    }

    public ImageNormalization(Type type){
        this.type = type;
    }


}
