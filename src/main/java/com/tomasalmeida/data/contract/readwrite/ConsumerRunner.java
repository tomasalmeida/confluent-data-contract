package com.tomasalmeida.data.contract.readwrite;

import com.tomasalmeida.data.contract.Contract;
import com.tomasalmeida.data.contract.User;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
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

import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_CONTRACTS;
import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_USERS;

public class ConsumerRunner {
    public static final Logger LOGGER = LoggerFactory.getLogger(ConsumerRunner.class);

    private final Properties properties;

    public ConsumerRunner() throws IOException {
        properties = PropertiesLoader.load("client.properties");
    }

    public void runUserConsumer() {
        try (Consumer<String, User> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(TOPIC_USERS));
            LOGGER.info("Starting User consumer...");
            while (true) {
                ConsumerRecords<String, User> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, User> record : records) {
                    LOGGER.info("User: {}", record.value());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in User Consumer", e);
        }
    }

    public void runContractConsumer() {
        try (Consumer<String, Contract> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(TOPIC_CONTRACTS));
            LOGGER.info("Starting Contract consumer...");
            while (true) {
                ConsumerRecords<String, Contract> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, Contract> record : records) {
                    LOGGER.info("Contract: {}", record.value());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in Contract Consumer", e);
        }
    }

    public static void main(final String[] args) throws IOException {
        ConsumerRunner consumerRunner = new ConsumerRunner();
        Thread constractConsumerThread = new Thread(consumerRunner::runContractConsumer);
        Thread userConsumerThread = new Thread(consumerRunner::runUserConsumer);
        userConsumerThread.start();
        constractConsumerThread.start();

    }
}
