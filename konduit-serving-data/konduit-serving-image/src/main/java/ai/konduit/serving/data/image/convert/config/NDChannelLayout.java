package ai.konduit.serving.data.image.convert.config;

/**
 * Represents the type (and order) of the channels for an image after it has been converted to an NDArray
 * <ul>
 *     <li>RGB: 3 channels, ordered according to: red, green, blue - most common for TensorFlow, Keras, and some other libraries</li>
 *     <li>BGR: 3 channels, ordered according to: blue, green, red - the default for OpenCV, JavaCV, DL4J</li>
 *     <li>RGBA: 4 channels, ordered according to: red, green, blue, alpha</li>
 *     <li>BGRA: 4 channels, ordered according to: blue, green, red, alpha</li>
 *     <li>GRAYSCALE: 1 channel - grayscale</li>
 * </ul>
 */
public enum NDChannelLayout {
    RGB, RGBA, BGR, BGRA, GRAYSCALE;

    public int numChannels() {
        switch (this) {
            case RGB:
            case BGR:
                return 3;
            case RGBA:
            case BGRA:
                return 4;
            case GRAYSCALE:
                return 1;
            default:
                throw new RuntimeException("Unknown enum value: " + this);
        }
    }
}
