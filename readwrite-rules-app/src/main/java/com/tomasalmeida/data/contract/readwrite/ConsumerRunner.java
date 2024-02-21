package com.tomasalmeida.data.contract.readwrite;

import com.tomasalmeida.data.contract.Contract;
import com.tomasalmeida.data.contract.User;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
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

    private final KafkaConsumer<String, Contract> contractConsumer;
    private final KafkaConsumer<String, User> userConsumer;
    private Thread constractConsumerThread;
    private Thread userConsumerThread;

    public ConsumerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");

        properties.put("group.id", "contract-consumer-group" + System.currentTimeMillis());
        contractConsumer = new KafkaConsumer<>(properties);

        properties.put("group.id", "user-consumer-group" + System.currentTimeMillis());
        userConsumer = new KafkaConsumer<>(properties);

    }

    private void startUserConsumer() {
        constractConsumerThread = new Thread(() -> runConsumer(userConsumer, TOPIC_USERS, "User"));
        constractConsumerThread.start();
    }

    private void startContractConsumer() {
        userConsumerThread = new Thread(() -> runConsumer(contractConsumer, TOPIC_CONTRACTS, "Contract"));
        userConsumerThread.start();
    }

    private <T> void runConsumer(KafkaConsumer<String, T> consumer, String topic, String recordType) {
        try {
            consumer.subscribe(Collections.singletonList(topic));
            LOGGER.info("Starting " + recordType + " consumer...");
            while (true) {
                ConsumerRecords<String, T> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, T> record : records) {
                    LOGGER.info(recordType + ": {}", record.value());
                }
            }
        } catch (WakeupException e) {
            LOGGER.info("Wake up " + recordType + " consumer...");
        } catch (Exception e) {
            LOGGER.error("Error in " + recordType + " Consumer", e);
        } finally {
            LOGGER.info("Closing " + recordType + " consumer...");
            consumer.close(Duration.ofSeconds(1));
            LOGGER.info("Closed " + recordType + " consumer...");
        }
    }

    private void closeContractConsumer() {
        closeConsumerAndThread(contractConsumer, constractConsumerThread);
    }

    private void closeUserConsumer() {
        closeConsumerAndThread(userConsumer, userConsumerThread);
    }

    private <T> void closeConsumerAndThread(KafkaConsumer<String, T> consumer, Thread thread) {
        consumer.wakeup();
        try {
            thread.join(3000);
        } catch (InterruptedException e) {
            LOGGER.error("Oh man...", e);
        }
    }

    public static void main(final String[] args) throws IOException {
        ConsumerRunner consumerRunner = new ConsumerRunner();
        consumerRunner.startContractConsumer();
        consumerRunner.startUserConsumer();

        //shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumerRunner.closeUserConsumer();
            consumerRunner.closeContractConsumer();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
