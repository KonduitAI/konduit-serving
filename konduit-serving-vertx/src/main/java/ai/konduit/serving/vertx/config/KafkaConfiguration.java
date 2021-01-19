/*
 * *****************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ****************************************************************************
 */

package ai.konduit.serving.vertx.config;

import ai.konduit.serving.pipeline.settings.constants.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent=true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kafka related configuration.")
public class KafkaConfiguration {
    @Schema(description = "Whether to start an http server alongside the kafka server for some standard endpoints.", defaultValue = "true")
    private boolean startHttpServerForKafka = Constants.DEFAULT_START_HTTP_SERVER_FOR_KAFKA;

    @Schema(description = "Whether to start an http server alongside the kafka server for some standard endpoints.", defaultValue = Constants.DEFAULT_HTTP_KAFKA_HOST)
    private String httpKafkaHost = Constants.DEFAULT_HTTP_KAFKA_HOST;

    @Schema(description = "Whether to start an http server alongside the kafka server for some standard endpoints.", defaultValue = "0")
    private int httpKafkaPort = Constants.DEFAULT_HTTP_KAFKA_PORT;

    @Schema(description = "Topic name for the consumer.", defaultValue = Constants.DEFAULT_CONSUMER_TOPIC_NAME)
    private String consumerTopicName = Constants.DEFAULT_CONSUMER_TOPIC_NAME;

    @Schema(description = "Deserializer class for key that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.", defaultValue = Constants.DEFAULT_KAFKA_CONSUMER_KEY_DESERIALIZER_CLASS)
    private String consumerKeyDeserializerClass = Constants.DEFAULT_KAFKA_CONSUMER_KEY_DESERIALIZER_CLASS;

    @Schema(description = "Deserializer class for value that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.", defaultValue = Constants.DEFAULT_KAFKA_CONSUMER_VALUE_DESERIALIZER_CLASS)
    private String consumerValueDeserializerClass = Constants.DEFAULT_KAFKA_CONSUMER_VALUE_DESERIALIZER_CLASS;

    @Schema(description = "A unique string that identifies the consumer group this consumer belongs to. This property is required if the consumer uses either the group management functionality by using <code>subscribe(topic)</code> or the Kafka-based offset management strategy.", defaultValue = Constants.DEFAULT_CONSUMER_GROUP_ID)
    private String consumerGroupId = Constants.DEFAULT_CONSUMER_GROUP_ID;

    @Schema(description = "What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted): <ul><li>earliest: automatically reset the offset to the earliest offset<li>latest: automatically reset the offset to the latest offset</li><li>none: throw exception to the consumer if no previous offset is found for the consumer's group</li><li>anything else: throw exception to the consumer.</li></ul>", defaultValue = Constants.DEFAULT_CONSUMER_AUTO_OFFSET_RESET)
    private String consumerAutoOffsetReset = Constants.DEFAULT_CONSUMER_AUTO_OFFSET_RESET;

    @Schema(description = "If true the consumer's offset will be periodically committed in the background.", defaultValue = Constants.DEFAULT_CONSUMER_AUTO_COMMIT)
    private String consumerAutoCommit = Constants.DEFAULT_CONSUMER_AUTO_COMMIT;

    @Schema(description = "Topic name for producer", defaultValue = Constants.DEFAULT_PRODUCER_TOPIC_NAME)
    private String producerTopicName = Constants.DEFAULT_PRODUCER_TOPIC_NAME;

    @Schema(description = "Serializer class for key that implements the <code>org.apache.kafka.common.serialization.Serializer</code> interface.", defaultValue = Constants.DEFAULT_KAFKA_PRODUCER_KEY_SERIALIZER_CLASS)
    private String producerKeySerializerClass = Constants.DEFAULT_KAFKA_PRODUCER_KEY_SERIALIZER_CLASS;

    @Schema(description = "Serializer class for value that implements the <code>org.apache.kafka.common.serialization.Serializer</code> interface.", defaultValue = Constants.DEFAULT_KAFKA_PRODUCER_VALUE_SERIALIZER_CLASS)
    private String producerValueSerializerClass = Constants.DEFAULT_KAFKA_PRODUCER_VALUE_SERIALIZER_CLASS;

    @Schema(description = "The number of acknowledgments the producer requires the leader to have received before considering a request complete. This controls the "
            + " durability of records that are sent. The following settings are allowed: "
            + " <ul>"
            + " <li><code>acks=0</code> If set to zero then the producer will not wait for any acknowledgment from the"
            + " server at all. The record will be immediately added to the socket buffer and considered sent. No guarantee can be"
            + " made that the server has received the record in this case, and the <code>retries</code> configuration will not"
            + " take effect (as the client won't generally know of any failures). The offset given back for each record will"
            + " always be set to <code>-1</code>."
            + " <li><code>acks=1</code> This will mean the leader will write the record to its local log but will respond"
            + " without awaiting full acknowledgement from all followers. In this case should the leader fail immediately after"
            + " acknowledging the record but before the followers have replicated it then the record will be lost."
            + " <li><code>acks=all</code> This means the leader will wait for the full set of in-sync replicas to"
            + " acknowledge the record. This guarantees that the record will not be lost as long as at least one in-sync replica"
            + " remains alive. This is the strongest available guarantee. This is equivalent to the acks=-1 setting."
            + "</ul>", defaultValue = Constants.DEFAULT_PRODUCER_ACKS)
    private String producerAcks = Constants.DEFAULT_PRODUCER_ACKS;
}
