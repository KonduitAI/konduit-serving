/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator;




import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parse a string to extract a set of key, value pairs
 * based on the passed in delimiter. Handles cases where
 * string literals are declared as values.
 *
 * @author Adam Gibson
 */
public class StringSplitter {

    private String delimiter;

    public StringSplitter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Split the given string in to a set of key value pairs
     * handling quoted string literals.
     * When a literal is found, this function will pass the value
     * as is as a value. It is assumed the user will know how to process
     * the given literal.
     * @param input the input to process
     * @return the key value pairs as strings.
     */
    public Map<String,String> splitResult(String input) {
        Map<String,String> ret = new LinkedHashMap<>();
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        StringBuilder currBuff = key;
        boolean inLiteral = false;
        for(int i = 0; i < input.length(); i++) {
            //still in middle of literal
            if(inLiteral && input.charAt(i) != '"') {
                currBuff.append(input.charAt(i));
                continue;
            } else if(input.charAt(i) == delimiter.charAt(0)) {
                //new key and value
                ret.put(key.toString(),value.toString());
                key = new StringBuilder();
                value = new StringBuilder();
                //reset to key as default value
                currBuff = key;
                continue;
            }

            switch(input.charAt(i)) {
                //finished key
                case '=':
                    currBuff = value;
                    break;
                case '"':
                    //begin or end literal
                    inLiteral = !inLiteral;
                    break;
                default:
                    currBuff.append(input.charAt(i));
                    break;
            }
        }

        //put last value
        if(!key.toString().isEmpty() && !value.toString().isEmpty())
            ret.put(key.toString(),value.toString());

        return ret;
    }


}
