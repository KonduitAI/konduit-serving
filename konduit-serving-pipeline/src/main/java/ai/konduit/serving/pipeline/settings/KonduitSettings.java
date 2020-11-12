package ai.konduit.serving.pipeline.settings;

import ai.konduit.serving.pipeline.settings.constants.Constants;
import ai.konduit.serving.pipeline.settings.constants.EnvironmentConstants;
import ai.konduit.serving.pipeline.settings.constants.PropertiesConstants;
import org.nd4j.shade.guava.base.Strings;

public class KonduitSettings {

    public static String getConsumerTopicName() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.CONSUMER_TOPIC_NAME),
                System.getProperty(PropertiesConstants.CONSUMER_TOPIC_NAME),
                Constants.DEFAULT_CONSUMER_TOPIC_NAME
        );
    }

    public static String getKafkaConsumerKeyDeserializerClass() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.KAFKA_CONSUMER_KEY_DESERIALIZER_CLASS),
                System.getProperty(PropertiesConstants.KAFKA_CONSUMER_KEY_DESERIALIZER_CLASS),
                Constants.DEFAULT_KAFKA_CONSUMER_KEY_DESERIALIZER_CLASS
        );
    }

    public static String getKafkaConsumerValueDeserializerClass() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.KAFKA_CONSUMER_VALUE_DESERIALIZER_CLASS),
                System.getProperty(PropertiesConstants.KAFKA_CONSUMER_VALUE_DESERIALIZER_CLASS),
                Constants.DEFAULT_KAFKA_CONSUMER_VALUE_DESERIALIZER_CLASS
        );
    }

    public static String getConsumerGroupId() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.CONSUMER_GROUP_ID),
                System.getProperty(PropertiesConstants.CONSUMER_GROUP_ID),
                Constants.DEFAULT_CONSUMER_GROUP_ID
        );
    }

    public static String getConsumerAutoOffsetReset() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.CONSUMER_AUTO_OFFSET_RESET),
                System.getProperty(PropertiesConstants.CONSUMER_AUTO_OFFSET_RESET),
                Constants.DEFAULT_CONSUMER_AUTO_OFFSET_RESET
        );
    }

    public static String getConsumerAutoCommit() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.CONSUMER_AUTO_COMMIT),
                System.getProperty(PropertiesConstants.CONSUMER_AUTO_COMMIT),
                Constants.DEFAULT_CONSUMER_AUTO_COMMIT
        );
    }

    public static String getProducerTopicName() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.PRODUCER_TOPIC_NAME),
                System.getProperty(PropertiesConstants.PRODUCER_TOPIC_NAME),
                Constants.DEFAULT_CONSUMER_TOPIC_NAME
        );
    }

    public static String getKafkaProducerKeySerializerClass() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.KAFKA_PRODUCER_KEY_SERIALIZER_CLASS),
                System.getProperty(PropertiesConstants.KAFKA_PRODUCER_KEY_SERIALIZER_CLASS),
                Constants.DEFAULT_KAFKA_PRODUCER_KEY_SERIALIZER_CLASS
        );
    }

    public static String getKafkaProducerValueSerializerClass() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.KAFKA_PRODUCER_VALUE_SERIALIZER_CLASS),
                System.getProperty(PropertiesConstants.KAFKA_PRODUCER_VALUE_SERIALIZER_CLASS),
                Constants.DEFAULT_KAFKA_PRODUCER_VALUE_SERIALIZER_CLASS
        );
    }

    public static String getProducerAcks() {
        return KonduitSettings.fetchValueBasedOnPriority(
                System.getenv(EnvironmentConstants.PRODUCER_ACKS),
                System.getProperty(PropertiesConstants.PRODUCER_ACKS),
                Constants.DEFAULT_PRODUCER_ACKS
        );
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
