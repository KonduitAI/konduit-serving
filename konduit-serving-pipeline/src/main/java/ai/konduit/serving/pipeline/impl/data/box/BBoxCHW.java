package ai.konduit.serving.pipeline.impl.data.box;

import ai.konduit.serving.pipeline.api.data.BoundingBox;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class BBoxCHW implements BoundingBox {

    private final double cx;
    private final double cy;
    private final double h;
    private final double w;
    private final String label;
    private final Double probability;

    public BBoxCHW(double cx, double cy, double h, double w){
        this(cx, cy, h, w, null, null);
    }

    public BBoxCHW(double cx, double cy, double h, double w, String label, Double probability){
        this.cx = cx;
        this.cy = cy;
        this.h = h;
        this.w = w;
        this.label = label;
        this.probability = probability;
    }


    @Override
    public double x1() {
        return cx - w/2;
    }

    @Override
    public double x2() {
        return cx + w/2;
    }

    @Override
    public double y1() {
        return cy - h/2;
    }

    @Override
    public double y2() {
        return cy + h/2;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof BoundingBox))
            return false;
        return BoundingBox.equals(this, (BoundingBox)o);
    }
}
