package ai.konduit.serving.vertx.protocols.kafka.verticle;

import ai.konduit.serving.pipeline.settings.constants.EnvironmentConstants;
import ai.konduit.serving.vertx.verticle.InferenceVerticle;
import io.vertx.core.Promise;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.HashMap;
import java.util.Map;

import static ai.konduit.serving.pipeline.settings.KonduitSettings.*;

@Slf4j
public class InferenceVerticleKafka extends InferenceVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        vertx.executeBlocking(handler -> {
            try {
                initialize();
                handler.complete();
            } catch (Exception exception) {
                handler.fail(exception);
                startPromise.fail(exception);
            }

        }, resultHandler -> {
            if (resultHandler.failed()) {
                if (resultHandler.cause() != null)
                    startPromise.fail(resultHandler.cause());
                else {
                    startPromise.fail("Failed to start. Unknown cause.");
                }
            } else {
                int port;

                String portEnvValue = System.getenv(EnvironmentConstants.KONDUIT_SERVING_PORT);
                if (portEnvValue != null) {
                    try {
                        port = Integer.parseInt(portEnvValue);
                    } catch (NumberFormatException exception) {
                        log.error("Environment variable \"{}={}\" isn't a valid port number.",
                                EnvironmentConstants.KONDUIT_SERVING_PORT, portEnvValue);
                        startPromise.fail(exception);
                        return;
                    }
                } else {
                    port = inferenceConfiguration.port();
                }

                if (port < 0 || port > 0xFFFF) {
                    startPromise.fail(new Exception("Valid port range is 0 <= port <= 65535. The given port was " + port));
                    return;
                }


                Map<String, String> configConsumer = new HashMap<>();
                configConsumer.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", inferenceConfiguration.host(), port));
                configConsumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, getKafkaConsumerKeyDeserializerClass());
                configConsumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, getKafkaConsumerValueDeserializerClass());
                configConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, getConsumerGroupId());
                configConsumer.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, getConsumerAutoOffsetReset());
                configConsumer.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, getConsumerAutoCommit());

                Map<String, String> configProducer = new HashMap<>();
                configProducer.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", inferenceConfiguration.host(), port));
                configProducer.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, getKafkaProducerKeySerializerClass());
                configProducer.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, getKafkaProducerValueSerializerClass());
                configProducer.put(ProducerConfig.ACKS_CONFIG, getProducerAcks());

                KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, configConsumer);
                KafkaProducer<String, String> producer = KafkaProducer.create(vertx, configProducer);

                consumer.handler(
                        record -> {
                            log.info("Processing key=" + record.key() + ",value=" + record.value() +
                                    ",partition=" + record.partition() + ",offset=" + record.offset());

                            KafkaProducerRecord<String, String> recordOut =
                                    KafkaProducerRecord.create(getProducerTopicName(), "message_=" + record.value());

                            producer.send(recordOut, done -> {
                                if (done.succeeded()) {
                                    RecordMetadata recordMetadata = done.result();
                                    log.info("Message " + record.value() + " written on topic=" + recordMetadata.getTopic() +
                                            ", partition=" + recordMetadata.getPartition() +
                                            ", offset=" + recordMetadata.getOffset());
                                }
                            });
                        }
                );

                consumer.subscribe(getConsumerTopicName(), subscribeHandler -> {
                    if (subscribeHandler.succeeded()) {
                        log.info("subscribed");
                        startPromise.complete();
                    } else {
                        log.error("Could not subscribe", subscribeHandler.cause());
                        startPromise.fail(subscribeHandler.cause());
                    }
                });
            }
        });
    }
}
