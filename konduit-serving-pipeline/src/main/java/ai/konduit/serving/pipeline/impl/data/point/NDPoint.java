package ai.konduit.serving.pipeline.impl.data.point;

import ai.konduit.serving.pipeline.api.data.Point;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class NDPoint implements Point {
    private final double[] coords;
    private final String label;
    private final Double probability;

    @Override
    public double get(int n) {
        if(n >= coords.length){
            throw new IllegalArgumentException("Can not access dimension "+n+" of "+coords.length+" dimensional point!");
        }
        return coords[n];
    }

    @Override
    public int dimensions() {
        return coords.length;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Point))
            return false;
        return Point.equals(this, (Point)o);
    }
}
