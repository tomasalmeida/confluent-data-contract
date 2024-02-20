package com.tomasalmeida.data.contract.migration;

import com.tomasalmeida.data.contract.Dimension;
import com.tomasalmeida.data.contract.Product;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
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

public class ProducerRunner{

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerRunner.class);

    private final KafkaProducer<String, Product> productProducerV2;

    public ProducerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
        properties.put(KafkaAvroDeserializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        properties.put(KafkaAvroDeserializerConfig.USE_LATEST_VERSION, false);
        properties.put(KafkaAvroSerializerConfig.USE_LATEST_WITH_METADATA, "app_version=2");
        productProducerV2 = new KafkaProducer<>(properties);
    }

    public void produce100Events() {
        try {
            for (int i= 0; i < 100; i++) {
                produceProductV2("product v2 " + i);
            }
            productProducerV2.close();
        } catch (Exception e) {
            LOGGER.error("Ops", e);
        }
    }

    private void produceProductV2(String name) throws InterruptedException {
        try {
            var product = new Product(name, "cat2", new Dimension(21, 22, 23));
            LOGGER.info("Sending Product V2 {}", product);
            ProducerRecord<String, Product> productRecord = new ProducerRecord<>(TOPIC_PRODUCTS, product);
            productProducerV2.send(productRecord);
        } catch (SerializationException serializationException) {
            LOGGER.error("Unable to serialize product v2: {}", serializationException.getCause().getMessage());
        }
        LOGGER.info("================");
        Thread.sleep(1000);
    }


    public static void main(final String[] args) throws IOException {
        ProducerRunner producerRunner = new ProducerRunner();
        producerRunner.produce100Events();
    }
}
