/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.pipeline.settings;

import ai.konduit.serving.pipeline.settings.constants.Constants;
import ai.konduit.serving.pipeline.settings.constants.EnvironmentConstants;
import ai.konduit.serving.pipeline.settings.constants.PropertiesConstants;
import org.nd4j.shade.guava.base.Strings;

import java.util.UUID;

import static ai.konduit.serving.pipeline.settings.constants.PropertiesConstants.SERVING_ID;

public class KonduitSettings {

    public static boolean getStartHttpServerForKafka() { return getStartHttpServerForKafka(Constants.DEFAULT_START_HTTP_SERVER_FOR_KAFKA); }
    public static boolean getStartHttpServerForKafka(boolean defaultValue) {
        return Boolean.parseBoolean(KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.START_HTTP_SERVER_FOR_KAFKA),
                System.getProperty(PropertiesConstants.START_HTTP_SERVER_FOR_KAFKA),
                String.valueOf(defaultValue))
        );
    }

    public static String getHttpKafkaHost() { return getHttpKafkaHost(null); }
    public static String getHttpKafkaHost(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.HTTP_KAFKA_HOST),
                System.getProperty(PropertiesConstants.HTTP_KAFKA_HOST),
                defaultValue != null ? defaultValue : Constants.DEFAULT_HTTP_KAFKA_HOST
        );
    }

    public static int getHttpKafkaPort() { return getHttpKafkaPort(Constants.DEFAULT_HTTP_KAFKA_PORT); }
    public static int getHttpKafkaPort(int defaultValue) {
        return Integer.parseInt(KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.HTTP_KAFKA_PORT),
                System.getProperty(PropertiesConstants.HTTP_KAFKA_PORT),
                String.valueOf(defaultValue))
        );
    }

    public static String getConsumerTopicName() { return getConsumerTopicName(null); }
    public static String getConsumerTopicName(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.CONSUMER_TOPIC_NAME),
                System.getProperty(PropertiesConstants.CONSUMER_TOPIC_NAME),
                defaultValue != null ? defaultValue : Constants.DEFAULT_CONSUMER_TOPIC_NAME
        );
    }

    public static String getKafkaConsumerKeyDeserializerClass() { return getKafkaConsumerKeyDeserializerClass(null); }
    public static String getKafkaConsumerKeyDeserializerClass(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.KAFKA_CONSUMER_KEY_DESERIALIZER_CLASS),
                System.getProperty(PropertiesConstants.KAFKA_CONSUMER_KEY_DESERIALIZER_CLASS),
                defaultValue != null ? defaultValue : Constants.DEFAULT_KAFKA_CONSUMER_KEY_DESERIALIZER_CLASS
        );
    }

    public static String getKafkaConsumerValueDeserializerClass() { return getKafkaConsumerValueDeserializerClass(null); }
    public static String getKafkaConsumerValueDeserializerClass(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.KAFKA_CONSUMER_VALUE_DESERIALIZER_CLASS),
                System.getProperty(PropertiesConstants.KAFKA_CONSUMER_VALUE_DESERIALIZER_CLASS),
                defaultValue != null ? defaultValue : Constants.DEFAULT_KAFKA_CONSUMER_VALUE_DESERIALIZER_CLASS
        );
    }

    public static String getConsumerGroupId() { return getConsumerGroupId(null); }
    public static String getConsumerGroupId(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.CONSUMER_GROUP_ID),
                System.getProperty(PropertiesConstants.CONSUMER_GROUP_ID),
                defaultValue != null ? defaultValue : Constants.DEFAULT_CONSUMER_GROUP_ID
        );
    }

    public static String getConsumerAutoOffsetReset() { return getConsumerAutoOffsetReset(null); }
    public static String getConsumerAutoOffsetReset(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.CONSUMER_AUTO_OFFSET_RESET),
                System.getProperty(PropertiesConstants.CONSUMER_AUTO_OFFSET_RESET),
                defaultValue != null ? defaultValue : Constants.DEFAULT_CONSUMER_AUTO_OFFSET_RESET
        );
    }

    public static String getConsumerAutoCommit() { return getConsumerAutoCommit(null); }
    public static String getConsumerAutoCommit(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.CONSUMER_AUTO_COMMIT),
                System.getProperty(PropertiesConstants.CONSUMER_AUTO_COMMIT),
                defaultValue != null ? defaultValue : Constants.DEFAULT_CONSUMER_AUTO_COMMIT
        );
    }

    public static String getProducerTopicName() { return getProducerTopicName(null); }
    public static String getProducerTopicName(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.PRODUCER_TOPIC_NAME),
                System.getProperty(PropertiesConstants.PRODUCER_TOPIC_NAME),
                defaultValue != null ? defaultValue : Constants.DEFAULT_PRODUCER_TOPIC_NAME
        );
    }

    public static String getKafkaProducerKeySerializerClass() { return getKafkaProducerKeySerializerClass(null); }
    public static String getKafkaProducerKeySerializerClass(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.KAFKA_PRODUCER_KEY_SERIALIZER_CLASS),
                System.getProperty(PropertiesConstants.KAFKA_PRODUCER_KEY_SERIALIZER_CLASS),
                defaultValue != null ? defaultValue : Constants.DEFAULT_KAFKA_PRODUCER_KEY_SERIALIZER_CLASS
        );
    }

    public static String getKafkaProducerValueSerializerClass() { return getKafkaProducerValueSerializerClass(null); }
    public static String getKafkaProducerValueSerializerClass(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.KAFKA_PRODUCER_VALUE_SERIALIZER_CLASS),
                System.getProperty(PropertiesConstants.KAFKA_PRODUCER_VALUE_SERIALIZER_CLASS),
                defaultValue != null ? defaultValue : Constants.DEFAULT_KAFKA_PRODUCER_VALUE_SERIALIZER_CLASS
        );
    }

    public static String getProducerAcks() { return getProducerAcks(null); }
    public static String getProducerAcks(String defaultValue) {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.PRODUCER_ACKS),
                System.getProperty(PropertiesConstants.PRODUCER_ACKS),
                defaultValue != null ? defaultValue : Constants.DEFAULT_PRODUCER_ACKS
        );
    }

    public static String getServingId() {
        if(System.getProperty(SERVING_ID) == null) {
            System.setProperty(SERVING_ID, "id-not-set-" + UUID.randomUUID());
        }
        return System.getProperty(SERVING_ID);
    }

    /**
     * Fetches the values based on their priority. If the first value is null or an empty string,
     * it will fetch the second value, and if the second value is null or an empty string then it will
     * fetch the default value.
     * @param first first value in priority
     * @param second second value in priority
     * @param defaultValue the default value
     * @return fetched value based on the priority.
     */
    static String fetchValueBasedOnPriority(String first, String second, String defaultValue) {
        if (!Strings.isNullOrEmpty(first)) {
            return first;
        } else if (!Strings.isNullOrEmpty(second)){
            return second;
        } else {
            return defaultValue;
        }
    }
}
