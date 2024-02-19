package com.tomasalmeida.data.contract.migration;

import com.tomasalmeida.data.contract.common.PropertiesLoader;
import com.tomasalmeida.data.contract.v2.Dimension;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_PRODUCTS;

public class ProducerRunner extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerRunner.class);

    private final KafkaProducer<String, com.tomasalmeida.data.contract.v1.Product> productProducerV1;
    private final KafkaProducer<String, com.tomasalmeida.data.contract.v2.Product> productProducerV2;

    public ProducerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put(KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
        properties.put(KafkaAvroDeserializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        properties.put(KafkaAvroDeserializerConfig.USE_LATEST_VERSION, false);
        properties.put(KafkaAvroSerializerConfig.USE_LATEST_WITH_METADATA, "app_version=1");
        productProducerV1 = new KafkaProducer<>(properties);
        properties.put(KafkaAvroSerializerConfig.USE_LATEST_WITH_METADATA, "app_version=2");
        productProducerV2 = new KafkaProducer<>(properties);
    }

    @Override
    public void run() {
        try {
            produceProductV1("product v1 1", "cat1", 111, 112,113);
            produceProductV2("product v2 1", "cat1", 211, 212,213);
            produceProductV1("product v1 2", "cat1", 121, 122,123);
            produceProductV2("product v2 2", "cat1", 221, 222,223);
            produceProductV1("product v1 3", "cat1", 131, 132,133);
            produceProductV2("product v2 3", "cat1", 231, 232,233);
            productProducerV1.close();
            productProducerV2.close();
        } catch (Exception e) {
            LOGGER.error("Ops", e);
        }

    }

    private void produceProductV1(String name, String category, int weight, int height, int width) throws InterruptedException {
        try {
            var product = new com.tomasalmeida.data.contract.v1.Product(name, category, weight, height, width);
            LOGGER.info("Sending Product V1 {}", product);
            ProducerRecord<String, com.tomasalmeida.data.contract.v1.Product> productRecord = new ProducerRecord<>(TOPIC_PRODUCTS, product);
            productProducerV1.send(productRecord);
        } catch (SerializationException serializationException) {
            LOGGER.error("Unable to serialize product v1: {}", serializationException.getCause().getMessage());
        }
        LOGGER.info("================");
        Thread.sleep(1000);
    }

    private void produceProductV2(String name, String category, int weight, int height, int width) throws InterruptedException {
        try {
            var product = new com.tomasalmeida.data.contract.v2.Product(name, category, new Dimension(weight, height, width));
            LOGGER.info("Sending Product V2 {}", product);
            ProducerRecord<String, com.tomasalmeida.data.contract.v2.Product> productRecord = new ProducerRecord<>(TOPIC_PRODUCTS, product);
            productProducerV2.send(productRecord);
        } catch (SerializationException serializationException) {
            LOGGER.error("Unable to serialize product v2: {}", serializationException.getCause().getMessage());
        }
        LOGGER.info("================");
        Thread.sleep(1000);
    }


    public static void main(final String[] args) throws IOException {
        ProducerRunner producerRunner = new ProducerRunner();
        producerRunner.run();
    }
}
