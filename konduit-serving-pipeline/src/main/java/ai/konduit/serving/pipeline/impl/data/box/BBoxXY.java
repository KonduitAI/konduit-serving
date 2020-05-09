package ai.konduit.serving.pipeline.impl.data.box;

import ai.konduit.serving.pipeline.api.data.BoundingBox;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class BBoxXY implements BoundingBox {

    private final double x1;
    private final double x2;
    private final double y1;
    private final double y2;
    private final String label;
    private final Double probability;

    public BBoxXY(double x1, double x2, double y1, double y2){
        this(x1, x2, y1, y2, null, null);
    }

    public BBoxXY(double x1, double x2, double y1, double y2, String label, Double probability){
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.label = label;
        this.probability = probability;
    }


    @Override
    public double cx() {
        return (x1+x2)/2;
    }

    @Override
    public double cy() {
        return (y1+y2)/2;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof BoundingBox))
            return false;
        return BoundingBox.equals(this, (BoundingBox)o);
    }
}
