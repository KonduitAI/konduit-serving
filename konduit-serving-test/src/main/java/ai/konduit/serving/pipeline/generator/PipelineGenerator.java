/*
 *       Copyright (c) 2019 Konduit AI.
 *
 *       This program and the accompanying materials are made available under the
 *       terms of the Apache License, Version 2.0 which is available at
 *       https://www.apache.org/licenses/LICENSE-2.0.
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *       WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *       License for the specific language governing permissions and limitations
 *       under the License.
 *
 *       SPDX-License-Identifier: Apache-2.0
 *
 */

package ai.konduit.serving.pipeline.generator;

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.generator.data.DataGenerator;
import lombok.val;
import org.nd4j.linalg.primitives.Pair;

import java.util.HashMap;
import java.util.Map;

public interface PipelineGenerator {


     /**
      * Returns the input {@link DataGenerator}
      * mapped by input name to a {@link Pair}
      * of {@link SchemaType} and {@link DataGenerator}
      * @return
      */
     Map<String, Map<SchemaType,DataGenerator>> inputDataGenerators();


     /**
      * Generates a random {@link PipelineStep}
      * @return
      */
     PipelineStep generate();


     default  Map<String,Object> generateDataSetForName(String name) {
          val generators = inputDataGenerators();
          Map<String,Object> mapPut = new HashMap<>();
          Map<SchemaType, DataGenerator> schemaTypeDataGeneratorMap = generators.get(name);
          for(val entry2 : entry.getValue().entrySet()) {
               mapPut.put(entry.getKey(),generators.get(entry.getKey()).get(entry2.getKey()).generate());

          }
     }


     /**
      * Generate a random dataset
      * mapped by input name.
      * Each entry is based on a pair of column name
      * and {@link SchemaType}
      * @return
      */
     default Map<String,Map<String,Object>> generateRandom() {
          val generators = inputDataGenerators();
          Map<String,Map<String,Object>> ret = new HashMap<>();
          for(val entry : generators.entrySet()) {
               Map<String,Object> mapPut = new HashMap<>();
               for(val entry2 : entry.getValue().entrySet()) {
                    mapPut.put(entry.getKey(),generators.get(entry.getKey()).get(entry2.getKey()).generate());

               }

               ret.put(entry.getKey(),mapPut);
          }

          return ret;
     }

}
