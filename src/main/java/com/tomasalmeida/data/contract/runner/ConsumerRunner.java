package com.tomasalmeida.data.contract.runner;

import com.tomasalmeida.data.contract.User;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConsumerRunner extends Thread {
    private static final String TOPIC_USERS = "users";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerRunner.class);

    private final Properties properties;

    public ConsumerRunner() throws IOException {
        properties = PropertiesLoader.load("client.properties");
    }

    @Override
    public void run() {
        try (Consumer<String, User> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(TOPIC_USERS));
            LOGGER.info("Starting consumer...");
            while (true) {
                ConsumerRecords<String, User> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, User> record : records) {
                    LOGGER.info("User: {}", record.value());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in Consumer", e);
        }

    }

    public static void main(final String[] args) throws IOException {
        ConsumerRunner consumerRunner = new ConsumerRunner();
        consumerRunner.run();
    }
}
