package ai.konduit.serving.pipeline.impl.step.ml;

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;

public class Utils {

    public static NDArray FloatNDArrayToDouble(NDArray ndarr) {
        if (ndarr.type() == NDArrayType.FLOAT || ndarr.type() == NDArrayType.FLOAT16 || ndarr.type() == NDArrayType.BFLOAT16) {
            float[][] farr = ndarr.getAs(float[][].class);
            double[][] darr = new double[(int) ndarr.shape()[0]][(int) ndarr.shape()[1]];
            for (int i = 0; i < farr.length; i++) {
                for (int j = 0; j < farr[i].length; j++) {
                    darr[i][j] = Double.valueOf(farr[i][j]);
                }
            }

            return NDArray.create(darr);
        }
        return ndarr;
    }

    public static double[] squeeze(NDArray arr) {

        // we have [numClasses] array, so do not modify nothing
        if (arr.shape().length == 1) {
            return arr.getAs(double[].class);
        }

        // i.e we have [1, numClasses] array
        if (arr.shape().length == 2 && arr.shape()[0] == 1) {
            return arr.getAs(double[][].class)[0];
        }

        throw new UnsupportedOperationException("Failed squeezing NDArray");

    }

    public static double[] getMaxValueAndIndex(double[] arr) {
        double max = arr[0];
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                maxIdx = i;
            }
        }
        return new double[]{max, maxIdx};
    }
}
