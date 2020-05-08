package ai.konduit.serving.data.image.convert.config;

public enum NDChannels {
    RGB, RGBA, BGR, BGRA, GRAYSCALE;

    public int numChannels(){
        switch (this){
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
