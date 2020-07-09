/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.pipeline.impl.step.bbox.yolo;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.util.DataUtils;
import ai.konduit.serving.pipeline.util.NDArrayUtils;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@CanRun(YoloToBoundingBoxStep.class)
@AllArgsConstructor
public class YoloToBoundingBoxRunner implements PipelineStepRunner {

    private final YoloToBoundingBoxStep step;

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        String input = step.input();
        if(input == null){

            String errMultipleKeys = "YoloToBoundingBoxStep input array field \"input\" was not provided in the config and " +
                    "could not be inferred: multiple NDArray fields exist: %s and %s";
            String errNoKeys = "YoloToBoundingBoxStep input array field \"input\" was not provided and could not be inferred: no NDArray fields exist";
            input = DataUtils.inferField(data, ValueType.NDARRAY, false, errMultipleKeys, errNoKeys);
        } else {
            Preconditions.checkState(data.has(input), "YoloToBoundingBoxStep: Data does not have an input field with name \"%s\"", input);
            Preconditions.checkState(data.type(input) == ValueType.NDARRAY, "YoloToBoundingBoxStep: Data input field \"%s\" has type %s but expected NDARRAY type", input, data.type(input));
        }

        NDArray arr = data.getNDArray(input);
        Preconditions.checkState(arr.rank() == 4, "YoloToBoundingBoxStep: Data field \"%s\" is NDArray but must be rank 4. Got array with rank %s,shape %s", arr.rank(), arr.shape());

        Preconditions.checkState(step.numClasses() != null || step.classLabels() != null, "YoloToBoundingBoxStep: either numClasses" +
                " field or classLabels field must be set");

        int numClasses = step.numClasses() != null ? step.numClasses() : step.classLabels().size();
        Preconditions.checkState(numClasses > 0, "YoloToBoundingboxStep: Number of classes must be > 0");

        if(!step.nchw()){
            arr = NDArray.create(NDArrayUtils.nhwcToNchw(arr.getAs(float[][][][].class)));
        }

        //Activation have format: [mb, B*(5+C), H, W]
        long n = arr.size(0);
        long b5c = arr.size(1);
        long h = arr.size(2);
        long w = arr.size(3);

        int b = (int) (b5c / (numClasses + 5));
        int c = (int) (b5c / b - 5);

        //Reshape to [mb, B, 5+C, H, W]
        SerializedNDArray sa = arr.getAs(SerializedNDArray.class);
        ByteBuffer bb = sa.getBuffer();
        SerializedNDArray sa5 = new SerializedNDArray(sa.getType(), new long[]{n, b, 5+c, h, w}, bb);
        float[][][][][] f5 = NDArray.create(sa5).getAs(float[][][][][].class);

        List<String> classLabels = step.classLabels();

        List<BoundingBox> out = new ArrayList<>();
        for( int i=0; i<n; i++ ) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int box = 0; box < b; box++) {
                        float conf = f5[i][box][4][y][x];
                        if(conf < step.threshold())
                            continue;

                        float px = f5[i][box][0][y][x]; //Originally: in 0 to 1 in grid cell
                        float py = f5[i][box][1][y][x]; //Originally: in 0 to 1 in grid cell
                        float pw = f5[i][box][2][y][x]; //In grid units (for example, 0 to 13)
                        float ph = f5[i][box][3][y][x]; //In grid units (for example, 0 to 13)

                        //Convert the "position in grid cell" to "position in image (in grid cell units)"
                        px += x;
                        py += y;

                        //Probabilities
                        float prob = 0.0f;
                        int pIdx = 0;
                        for( int cl=0; cl<c; cl++){
                            float f = f5[i][box][5+cl][y][x];
                            if(f > prob){
                                prob = f;
                                pIdx = cl;
                            }
                        }

                        String lbl;
                        if(classLabels == null || pIdx >= classLabels.size()){
                            lbl = String.valueOf(pIdx);
                        } else {
                            lbl = classLabels.get(pIdx);
                        }

                        out.add(BoundingBox.create(px/w, py/h, pw/w,ph/h, lbl, (double)prob));
                    }
                }
            }
        }

        Data dOut;
        if(step.keepOtherValues()){
            dOut = data.clone();
        } else {
            dOut = Data.empty();
        }

        String outName = step.output() == null ? YoloToBoundingBoxStep.DEFAULT_OUTPUT_NAME : step.output();
        dOut.putListBoundingBox(outName, out);
        return dOut;
    }
}
