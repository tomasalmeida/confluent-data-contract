package com.tomasalmeida.data.contract.runner;

import com.tomasalmeida.data.contract.User;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class ProducerRunner extends Thread {
    private static final String TOPIC_USERS = "users";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerRunner.class);


    private final KafkaProducer<String, User> producer;

    public ProducerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        producer = new KafkaProducer<>(properties);
    }

    @Override
    public void run() {
        try {
            produceUser("Tomas", "Dias Almeida", "Tomas Almeida", 39);
            produceUser("Fernando", "Perez Machado", "", 53);
            produceUser("Amon", "Ra", "", 52);
            produceUser("La", "Fontaine", "", 49);
            produceUser("Young", "Sheldon Cooper", "", 7);
            producer.close();
        } catch (Exception e) {
            LOGGER.error("Ops", e);
        }

    }

    private void produceUser(String firstName, String lastName, String fullName, int age) throws InterruptedException {
        User user = null;
        try {
            user = new User(firstName, lastName, fullName, age);
            LOGGER.info("Sending user {}", user);
            ProducerRecord<String, User> record1 = new ProducerRecord<>(TOPIC_USERS, user);
            producer.send(record1);
        } catch (SerializationException serializationException) {
            LOGGER.error("Unable to serialize user: {}", serializationException.getCause().getMessage());
        }
        LOGGER.info("================");
        Thread.sleep(1000);

    }

    public static void main(final String[] args) throws IOException {
        ProducerRunner producerRunner = new ProducerRunner();
        producerRunner.run();
    }
}
