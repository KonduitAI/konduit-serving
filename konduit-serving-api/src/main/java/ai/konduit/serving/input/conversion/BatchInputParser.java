/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.input.conversion;

import ai.konduit.serving.input.adapter.InputAdapter;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Writable;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.primitives.Pair;

import java.io.IOException;
import java.util.*;

/**
 * Parses a whole multi part upload buffer
 * and converts it to an {@link INDArray}
 * minibatch.
 * <p>
 * Uses {@link InputAdapter} specified by name
 * allowing conversion of each type of input file's
 * raw content to an {@link INDArray}
 *
 * @author Adam Gibson
 */
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchInputParser {

    private Map<String, InputAdapter<Buffer, ?>> converters;
    private Map<String, ConverterArgs> converterArgs;
    private List<String> inputParts;


    /**
     * Create a batch from the {@link RoutingContext}
     *
     * @param routingContext the routing context to create the batch from
     * @return the proper ndarray batch with the ndarrays merged
     * with a batch per input
     * @throws IOException I/O Exception
     */
    public Record[] createBatch(RoutingContext routingContext) throws IOException {
        //partition the input content by name
        Map<String, List<BatchPartInfo>> partInfo = partInfoForUploads(routingContext);
        if (partInfo == null || partInfo.isEmpty()) {
            throw new IllegalArgumentException("No parts resolved for file uploads!");
        } else if (!partInfo.containsKey(inputParts.get(0))) {
            throw new IllegalArgumentException("Illegal part info resolved. Part info keys were " + partInfo.keySet() + " while input parts were " + inputParts);
        }


        int batchSize = partInfo.get(inputParts.get(0)).size();
        //batch size
        Record[] inputBatches = new Record[batchSize];
        for (int j = 0; j < inputBatches.length; j++) {
            inputBatches[j] =
                    new org.datavec.api.records.impl.Record(
                            new ArrayList<>(inputParts.size()),
                            null);

            //pre populate the record to prevent out of index when setting
            for (int k = 0; k < inputParts.size(); k++) {
                inputBatches[j].getRecord().add(null);
            }
        }


        Map<Integer, List<List<Writable>>> missingIndices = new LinkedHashMap<>();
        for (int i = 0; i < inputParts.size(); i++) {
            if (inputParts.get(i) == null || !partInfo.containsKey(inputParts.get(i))) {
                throw new IllegalStateException("No part found for part " + inputParts.get(i)
                        + " available parts " + partInfo.keySet());
            }

            List<BatchPartInfo> batch = partInfo.get(inputParts.get(i));
            for (int j = 0; j < batch.size(); j++) {
                Pair<String, Integer> partNameAndIndex = partNameAndIndex(batch.get(j).getPartName());
                Buffer buffer = loadBuffer(routingContext,
                        batch.get(j).getFileUploadPath());
                Object convert = convert(buffer, partNameAndIndex.getFirst(), null, routingContext);
                Preconditions.checkNotNull(convert, "Converted writable was null!");
                //set the name
                if (convert instanceof Writable) {
                    Writable writable = (Writable) convert;
                    inputBatches[j].getRecord().set(i, writable);

                } else {
                    ArrowWritableRecordBatch arrow = (ArrowWritableRecordBatch) convert;
                    missingIndices.put(j, arrow);
                }

            }
        }

        if (!missingIndices.isEmpty()) {
            List<Record> newRetRecords = new ArrayList<>();
            List<Record> oldRecords = new ArrayList<>();
            for (int i = 0; i < batchSize; i++) {
                if (inputBatches[i] != null) {
                    oldRecords.add(inputBatches[i]);
                }
            }

            for (Map.Entry<Integer, List<List<Writable>>> entry : missingIndices.entrySet()) {
                for (List<Writable> record : entry.getValue()) {
                    newRetRecords.add(new org.datavec.api.records.impl.Record(record, null));
                }
            }


            return newRetRecords.toArray(new Record[newRetRecords.size()]);
        }

        return inputBatches;
    }


    /**
     * Returns a list of {@link BatchPartInfo}
     * for each part by name.
     * The "name" is meant to match 1
     * name per input in to a computation graph
     * such that each part name is:
     * inputName[index]
     *
     * @param ctx the context to get the part info
     *            from
     * @return a map indexing part name to a list of parts
     * for each input
     */
    private Map<String, List<BatchPartInfo>> partInfoForUploads(RoutingContext ctx) {
        if (ctx.fileUploads().isEmpty()) {
            throw new IllegalStateException("No files found for part info!");
        } else {
            log.debug("Found " + ctx.fileUploads().size() + " file uploads");
        }

        Map<String, List<BatchPartInfo>> ret = new LinkedHashMap<>();
        //parse each file upload all at once
        for (FileUpload upload : ctx.fileUploads()) {
            //the part name: inputName[index]
            String name = upload.name();
            //likely a colon for a tensorflow name got passed in
            //verify against the name in the configuration and set it to that
            if (name.contains(" ")) {
                name = name.replace(" ", ":");
                if (!inputParts.contains(name)) {
                    throw new IllegalStateException("Illegal name for multi part passed in " + upload.name());
                } else {
                    log.warn("Corrected input name " + upload.name() + " to " + name);
                }
            }

            //split the input name and the index
            Pair<String, Integer> partNameAndIndex = partNameAndIndex(name);
            //the part info for this particular file
            BatchPartInfo batchPartInfo = new BatchPartInfo(
                    partNameAndIndex.getRight(), upload.uploadedFileName(), name);
            //add the input name and accumulate the part info for each input
            if (!ret.containsKey(partNameAndIndex.getFirst())) {
                ret.put(partNameAndIndex.getFirst(), new ArrayList<>());
            }

            List<BatchPartInfo> batchPartInfos = ret.get(partNameAndIndex.getFirst());
            batchPartInfos.add(batchPartInfo);
        }

        //sort based on index
        for (List<BatchPartInfo> info : ret.values()) {
            Collections.sort(info);
        }

        return ret;
    }

    /**
     * Use the converter specified
     * by name to convert a
     * raw {@link Buffer}
     * to a proper input for inference
     *
     * @param input          the raw content
     * @param name           the name of the input
     *                       converter to use
     * @param params         the params to use where needed
     * @param routingContext RoutingContext
     * @return converted INDArray
     * @throws IOException I/O Exception
     */
    public Object convert(Buffer input, String name, ConverterArgs params, RoutingContext routingContext)
            throws IOException {
        if (!converters.containsKey(name)) {
            throw new IllegalArgumentException("Illegal name for converter " + name + " not found!");
        }

        return converters.get(name).convert(input, params, routingContext.data());
    }

    /**
     * Load the buffer from each file
     *
     * @param ctx              the context to load from
     * @param uploadedFileName the uploaded file path
     * @return the file contents for the file part
     */
    private Buffer loadBuffer(RoutingContext ctx, String uploadedFileName) {
        return ctx.vertx().fileSystem().readFileBlocking(uploadedFileName);
    }

    private Pair<String, Integer> partNameAndIndex(String name) {
        //inputName[partIndex]
        //1 part only
        if (name.indexOf('[') < 0) {
            return Pair.of(name, 0);
        }

        String outputName = name.substring(0, name.indexOf('['));
        int partIndex = Integer.parseInt(name.substring(name.indexOf('[') + 1, name.lastIndexOf(']')));
        return Pair.of(outputName, partIndex);
    }

    @Data
    @AllArgsConstructor
    public static class BatchPartInfo implements Comparable<BatchPartInfo> {
        private int index;
        private String fileUploadPath;
        private String partName;

        @Override
        public int compareTo(BatchPartInfo batchPartInfo) {
            return Integer.compare(index, batchPartInfo.index);
        }
    }

}
