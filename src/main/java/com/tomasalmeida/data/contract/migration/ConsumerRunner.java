package com.tomasalmeida.data.contract.migration;

import com.tomasalmeida.data.contract.common.PropertiesLoader;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_PRODUCTS;

public class ConsumerRunner {
    public static final Logger LOGGER = LoggerFactory.getLogger(ConsumerRunner.class);

    private final Properties properties;

    public ConsumerRunner() throws IOException {
        properties = PropertiesLoader.load("client.properties");
    }

    public void runProductConsumerV1() {
        properties.put(KafkaAvroDeserializerConfig.USE_LATEST_WITH_METADATA, "app_version=1");
        try (Consumer<String, com.tomasalmeida.data.contract.v1.Product> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(TOPIC_PRODUCTS));
            LOGGER.info("Starting Product consumer V1...");
            while (true) {
                ConsumerRecords<String, com.tomasalmeida.data.contract.v1.Product> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, com.tomasalmeida.data.contract.v1.Product> record : records) {
                    LOGGER.info("Product with V1 schema: {}", record.value());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in User Consumer v1", e);
        }
    }
//
//    public void runProductConsumerV2() {
//        try (Consumer<String, com.tomasalmeida.data.contract.v2.Product> consumer = new KafkaConsumer<>(properties)) {
//            consumer.subscribe(Collections.singletonList(TOPIC_PRODUCTS));
//            LOGGER.info("Starting Product consumer V2...");
//            while (true) {
//                ConsumerRecords<String, com.tomasalmeida.data.contract.v2.Product> records = consumer.poll(Duration.ofMillis(1000));
//                for (ConsumerRecord<String, com.tomasalmeida.data.contract.v2.Product> record : records) {
//                    LOGGER.info("Product with V2 schema: {}", record.value());
//                }
//            }
//        } catch (Exception e) {
//            LOGGER.error("Error in User Consumer v2", e);
//        }
//    }

    public static void main(final String[] args) throws IOException {
        ConsumerRunner consumerRunner = new ConsumerRunner();
        consumerRunner.runProductConsumerV1();
//        consumerRunner.runProductConsumerV2();
   }
}
