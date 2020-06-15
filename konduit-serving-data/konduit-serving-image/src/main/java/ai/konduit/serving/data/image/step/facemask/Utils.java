package ai.konduit.serving.data.image.step.facemask;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class Utils {
    private final double offset=0.5;

    // https://github.com/AIZOOTech/FaceMaskDetection/blob/master/utils/anchor_generator.py#L4
    public static INDArray generateAnchors( double[][] feature_map_sizes, double[][] anchor_sizes,double[] anchor_ratios){

        for (int i=0;i<feature_map_sizes.length;i++){
            INDArray cx = Nd4j.linspace(0,(long) (feature_map_sizes[0][1] - 1),(long) (feature_map_sizes[0][1] + 0.5)).div(feature_map_sizes[0][1]);
            INDArray cy = Nd4j.linspace(0,(long) (feature_map_sizes[0][1] - 1),(long) (feature_map_sizes[0][1] + 0.5)).div(feature_map_sizes[0][1]);


        }
        return null;
    }

    // https://github.com/AIZOOTech/FaceMaskDetection/blob/master/utils/anchor_decode.py#L4
    public static INDArray decodeBBox( INDArray anchors, INDArray y_bboxes_output){

        return null;
}
    // https://github.com/AIZOOTech/FaceMaskDetection/blob/master/utils/nms.py#L4
    public static INDArray singleClassNonMaxSuppression( INDArray bbox, INDArray confidences){

        return null;
    }

}

