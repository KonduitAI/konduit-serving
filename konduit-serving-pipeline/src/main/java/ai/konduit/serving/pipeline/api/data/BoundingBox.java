package ai.konduit.serving.pipeline.api.data;

import ai.konduit.serving.pipeline.impl.data.box.BBoxCHW;
import ai.konduit.serving.pipeline.impl.data.box.BBoxXY;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

public interface BoundingBox {

    static BoundingBox create(double cx, double cy, double h, double w){
        return create(cx, cy, h, w, null, null);
    }

    static BoundingBox create(double cx, double cy, double h, double w, String label, Double probability){
        return new BBoxCHW(cx, cy, h, w, label, probability);
    }

    static BoundingBox createXY(double x1, double x2, double y1, double y2){
        return createXY(x1, x2, y1, y2, null, null);
    }

    static BoundingBox createXY(double x1, double x2, double y1, double y2, String label, Double probability){
        return new BBoxXY(x1, x2, y1, y2, label, probability);
    }

    double x1();

    double x2();

    double y1();

    double y2();

    double cx();

    double cy();

    String label();

    Double probability();

    static boolean equals(BoundingBox bb1, BoundingBox bb2) {
        return equals(bb1, bb2, 1e-5);
    }

    static boolean equals(BoundingBox bb1, BoundingBox bb2, double eps){
        return Math.abs(bb1.x1() - bb2.x1()) < eps &&
                Math.abs(bb1.x2() - bb2.x2()) < eps &&
                Math.abs(bb1.y1() - bb2.y1()) < eps &&
                Math.abs(bb1.y2() - bb2.y2()) < eps &&
                Objects.equals(bb1.label(), bb2.label()) &&
                ((bb1.probability() == null && bb2.probability() == null) ||
                        (bb1.probability() != null && Math.abs(bb1.probability() - bb2.probability()) < eps) );
    }

}
