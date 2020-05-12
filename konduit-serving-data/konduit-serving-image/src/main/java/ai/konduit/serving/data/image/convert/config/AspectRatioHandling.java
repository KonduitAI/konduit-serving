package ai.konduit.serving.data.image.convert.config;

/**
 * How We should handle the situation where the input image and output NDArray have different aspect ratios<br>
 * See {@link ai.konduit.serving.data.image.convert.ImageToNDArrayConfig} for more details<br>
 * <ul>
 *     <li>CENTER_CROP: Crop the larger dimension down to the correct aspect ratio (and then resize if necessary).</li>
 *     <li>PAD: Zero pad the smaller dimension to make the aspect ratio match the output (and then resize if necessary)</li>
 *     <li>STRETCH: Simply resize the image to the required aspect ratio, distorting the image if necessary</li>
 * </ul>
 */
public enum AspectRatioHandling {
    CENTER_CROP, PAD, STRETCH
}
